package com.nbakaev.yad.gateway.youtube;

import com.nbakaev.yad.gateway.download.*;
import com.nbakaev.yad.gateway.download.dto.DownloadRequestDto;
import com.nbakaev.yad.gateway.download.dto.DownloadUploadStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class YoutubeDownloader implements GenericDownloader {

    private static final String DOWNLOAD_TYPE_YOUTUBE = "YOUTUBE";

    private final Pattern progressPattern;
    private final String outputPath;
    private static final Logger logger = LoggerFactory.getLogger(YoutubeDownloader.class);

    private final DownloadRepository downloadRepository;

    public YoutubeDownloader(YoutubePropertyConfiguration config, DownloadRepository downloadRepository) throws IOException, InterruptedException {

        this.outputPath = config.getPathDownload();
        this.downloadRepository = downloadRepository;
        logger.info("Default output path for Youtube {}", this.outputPath);

        // TODO: if youtube-dl in PATH
        ArrayList<String> objects = new ArrayList<>();
        objects.add("youtube-dl");
        objects.add("--version");

        ProcessBuilder processBuilder = new ProcessBuilder(objects);
        processBuilder.inheritIO();
        Process p = processBuilder.start();

        p.waitFor();
        int i = p.exitValue();
        if (i != 0) {
            throw new IOException("youtube-dl is not properly installed");
        }
        // tested on youtube-dl  >youtube-dl --version //2018.03.03
        progressPattern = Pattern.compile("\\[download\\]\\s+(?<percentage>\\d+(?:\\.\\d+)?%)\\s+of\\s+(?<size>\\d+(?:\\.\\d+)?(?:K|M|G)iB)(?:\\s+at\\s+(?<speed>\\d+(?:\\.\\d+)?(?:K|M|G)iB\\/s))?(?:\\s+ETA\\s+(?<eta>[\\d]{2}:[\\d]{2}))?");
    }

    private String getYoutubeUrlById(String id) {
       return  "https://www.youtube.com/watch?v=" + id;
    }

    @Override
    public Flux<DownloadUploadStatusDto> downloadVideo(DownloadRequestDto req) {
        String id = req.getUrl();
        DownloadItemDbo item = new DownloadItemDbo();
        item.setStatus(DownloadStatuses.DOWNLOAD_STATUS_CREATED);
        item.setType(DOWNLOAD_TYPE_YOUTUBE);
        item.setPartUrl(id);
        item.setUrl(getYoutubeUrlById(id));
        item.setCreatedDate(LocalDateTime.now());

        return downloadRepository.insert(item).flatMapMany(insert -> Flux.create(sink -> {
            try {
                ArrayList<String> objects = new ArrayList<>();
                objects.add("youtube-dl");
                objects.add("-o");
                objects.add(outputPath);
                objects.add(getYoutubeUrlById(id));

                ProcessBuilder processBuilder = new ProcessBuilder(objects);
                Process p = processBuilder.start();
                InputStream inputStream = p.getInputStream();

                byte[] buffer = new byte[1000];

                while (p.isAlive()) {
                    int read = inputStream.read(buffer);
                    if (read == -1) {
                        // closed stream
                        break;
                    }

                    DownloadUploadStatusDto downloadUploadStatusDto = new DownloadUploadStatusDto();

                    // TODO: better
                    Matcher matcher = progressPattern.matcher(new String(buffer));
                    if (!matcher.find()) {
                        continue;
                    }

                    String percentage = matcher.group("percentage");
                    String size = matcher.group("size");
                    if (percentage != null) {
                        downloadUploadStatusDto.setPercent(Double.valueOf(percentage.replace("%", "")));
                        downloadUploadStatusDto.setMsg(size);
                        downloadUploadStatusDto.setDownloadId(insert.getId().toString());
                        sink.next(downloadUploadStatusDto);

                        insert.setStatus(DownloadStatuses.DOWNLOAD_STATUS_DOWNLOADING);
                        insert.setUploadedPercentage(downloadUploadStatusDto.getPercent());
                        item.setLastUpdateDate(LocalDateTime.now());
                        // TODO: block or optimistic update; order must be matter
                        downloadRepository.save(insert).subscribe();
                    }
                }

                insert.setStatus(DownloadStatuses.DOWNLOAD_STATUS_DONE);
                item.setLastUpdateDate(LocalDateTime.now());
                downloadRepository.save(insert).flatMap(l -> {
                    try {
                        p.waitFor();
                    } catch (InterruptedException e) {

                        sendError(item, insert);
                        logger.error("Error download youtube video {}", id, e);
                    }
                    sink.complete();
                    return Mono.empty();
                }).subscribe();
            } catch (Exception e) {
                sendError(item, insert);
                logger.error("Error download youtube video {}", id, e);
            }
        }));

    }

    @Override
    public String getType() {
        return DOWNLOAD_TYPE_YOUTUBE;
    }

    private void sendError(DownloadItemDbo item, DownloadItemDbo insert) {
        insert.setStatus(DownloadStatuses.DOWNLOAD_STATUS_ERROR);
        item.setLastUpdateDate(LocalDateTime.now());
        // TODO: block or optimistic update
        downloadRepository.save(insert).subscribe();
    }

}
