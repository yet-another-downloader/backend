package com.nbakaev.yad.gateway.youtube;

import com.nbakaev.yad.gateway.download.DownloadItemDbo;
import com.nbakaev.yad.gateway.download.DownloadRepository;
import com.nbakaev.yad.gateway.download.DownloadStatuses;
import com.nbakaev.yad.gateway.download.GenericDownloader;
import com.nbakaev.yad.gateway.download.dto.DownloadRequestDto;
import com.nbakaev.yad.gateway.download.dto.DownloadUploadStatusDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class YoutubeDownloader implements GenericDownloader {

    private static final String DOWNLOAD_TYPE_YOUTUBE = "YOUTUBE";

    private final Pattern progressPattern;
    private final String outputPath;
    private static final Logger logger = LoggerFactory.getLogger(YoutubeDownloader.class);

    private final DownloadRepository downloadRepository;

    private final Scheduler downloadBlockingSchedulers = Schedulers.elastic();

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
        return "https://www.youtube.com/watch?v=" + id;
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

        return downloadRepository.insert(item).publishOn(downloadBlockingSchedulers).flatMapMany(insert -> Flux.create(sink -> {
            var state = new Object() {
                volatile InputStream inputStream = null;
                volatile InputStream errorInputStream = null;
                volatile Process p = null;

                volatile Exception encouragedError = null;
            };

            try {
                // TODO: insert shared between thread !!! - volatile
                List<String> objects = new ArrayList<>();
                objects.add("youtube-dl");

                objects.add("-f");
                objects.add("bestvideo+bestaudio/best");

                objects.add("-o");
                objects.add(outputPath);

                objects.add(getYoutubeUrlById(id));

                ProcessBuilder processBuilder = new ProcessBuilder(objects);

                logger.info("Start download youtube {}", String.join(" ", objects));
                state.p = processBuilder.start();

                logger.info("Start process PID={}", state.p.pid());

                state.inputStream = state.p.getInputStream();
                state.errorInputStream = state.p.getErrorStream();

                Thread outputMonitoring = new Thread(() -> {
                    while (state.p.isAlive() && !Thread.currentThread().isInterrupted()) {
                        byte[] buffer = new byte[1000];
                        int read = 0;
                        try {
                            read = state.inputStream.read(buffer);

                            if (read == -1) {
                                // closed stream
                                break;
                            }

                            // TODO: better
                            String processOutputBuffer = new String(buffer);
                            logger.trace("youtube-dl output {}", processOutputBuffer);
                            Matcher matcher = progressPattern.matcher(processOutputBuffer);
                            if (!matcher.find()) {
                                continue;
                            }

                            String percentage = matcher.group("percentage");
                            String size = matcher.group("size");
                            if (percentage != null) {
                                DownloadUploadStatusDto downloadUploadStatusDto = new DownloadUploadStatusDto();

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
                        } catch (IOException e) {
                            logger.error("Error monitoring id={}", insert.getId(), e);
                            state.encouragedError = e;
                            state.p.destroy();
                        }
                    }
                });

                Thread errorMonitoring = new Thread(() -> {
                    while (state.p.isAlive() && !Thread.currentThread().isInterrupted()) {
                        byte[] buffer = new byte[1000];
                        int read = 0;
                        try {
                            read = state.errorInputStream.read(buffer);

                            if (read == -1) {
                                // closed stream
                                break;
                            }

                            String processOutputBuffer = new String(buffer);
                            logger.error("youtube-dl error output {}", processOutputBuffer);
                            state.encouragedError = new RuntimeException(processOutputBuffer);

                            DownloadUploadStatusDto downloadUploadStatusDto = new DownloadUploadStatusDto();

                            downloadUploadStatusDto.setMsg(processOutputBuffer);
                            downloadUploadStatusDto.setDownloadId(insert.getId().toString());
                            sink.next(downloadUploadStatusDto);

                            insert.setStatus(DownloadStatuses.DOWNLOAD_STATUS_ERROR);
                            item.setLastUpdateDate(LocalDateTime.now());
                            // TODO: block or optimistic update; order must be matter
                            downloadRepository.save(insert).subscribe();

                            // destroy download process on error
                            state.p.destroy();
                        } catch (IOException e) {
                            logger.error("Error errorMonitoring id={}", insert.getId(), e);
                            state.encouragedError = e;
                            state.p.destroy();
                        }
                    }
                });

                outputMonitoring.setName("youtube-dl-" + insert.getId());
                outputMonitoring.start();

                errorMonitoring.setName("youtube-dl-error-mon-" + insert.getId());
                errorMonitoring.start();

                state.p.waitFor();

                outputMonitoring.interrupt();
                errorMonitoring.interrupt();

                if (state.encouragedError == null) {
                    insert.setStatus(DownloadStatuses.DOWNLOAD_STATUS_DONE);
                    item.setLastUpdateDate(LocalDateTime.now());

                    downloadRepository.save(insert).subscribe(re -> {
                        logger.info("Completed download id={}", id);
                        sink.complete();
                    });
                } else {
                    throw state.encouragedError;
                }

            } catch (Exception e) {
                sendError(e, id, item, insert);
                logger.error("Error download youtube video {}", id, e);
                sink.error(e);
            } finally {
                if (state.p != null && state.p.isAlive()) {
                    state.p.destroy();
                }

                try {
                    if (state.inputStream != null) {
                        state.inputStream.close();
                    }

                    if (state.errorInputStream != null) {
                        state.errorInputStream.close();
                    }
                } catch (IOException e) {
                    logger.error("Error close resources", e);
                }
            }
        }));

    }

    @Override
    public String getType() {
        return DOWNLOAD_TYPE_YOUTUBE;
    }

    private void sendError(Exception e, String id, DownloadItemDbo item, DownloadItemDbo insert) {
        logger.error("Error download youtube video {}", id, e);

        insert.setStatus(DownloadStatuses.DOWNLOAD_STATUS_ERROR);
        item.setLastUpdateDate(LocalDateTime.now());
        // TODO: block or optimistic update
        downloadRepository.save(insert).subscribe();
    }

}
