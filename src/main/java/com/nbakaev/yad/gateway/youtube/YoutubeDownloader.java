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
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class YoutubeDownloader implements GenericDownloader {

    private static final String DOWNLOAD_TYPE_YOUTUBE = "YOUTUBE";

    private final Pattern progressPattern;
    private final String outputPath;
    private static final Logger logger = LoggerFactory.getLogger(YoutubeDownloader.class);

    private final DownloadRepository downloadRepository;

    private final Scheduler downloadBlockingSchedulers = Schedulers.elastic();

    private final YoutubePropertyConfiguration config;

    private final YoutubeApiV3Service youtubeApiV3Service;

    public YoutubeDownloader(YoutubePropertyConfiguration config, DownloadRepository downloadRepository,
                             YoutubeApiV3Service youtubeApiV3Service) throws IOException, InterruptedException {
        this.youtubeApiV3Service = youtubeApiV3Service;
        testYoutubeDlOnClasspath();

        this.outputPath = config.getPathDownload();

        this.config = config;
        this.downloadRepository = downloadRepository;
        logger.info("Default output path for Youtube {}", this.outputPath);

        // tested on youtube-dl  >youtube-dl --version //2018.03.03
        progressPattern = Pattern.compile("\\[download\\]\\s+(?<percentage>\\d+(?:\\.\\d+)?%)\\s+of\\s+(?<size>\\d+(?:\\.\\d+)?(?:K|M|G)iB)(?:\\s+at\\s+(?<speed>\\d+(?:\\.\\d+)?(?:K|M|G)iB\\/s))?(?:\\s+ETA\\s+(?<eta>[\\d]{2}:[\\d]{2}))?");
    }

    private void testYoutubeDlOnClasspath() throws IOException, InterruptedException {
        // TODO: if youtube-dl in PATH
        List<String> objects = new ArrayList<>();
        objects.add("youtube-dl");
        objects.add("--version");

        var processBuilder = new ProcessBuilder(objects);
        processBuilder.inheritIO();
        var p = processBuilder.start();

        p.waitFor();
        int i = p.exitValue();
        if (i != 0) {
            throw new IOException("youtube-dl is not properly installed");
        }
    }

    private String getYoutubeUrlById(String id) {
        return config.getBaseUrl() + id;
    }

    @Override
    public Flux<DownloadUploadStatusDto> downloadVideo(DownloadRequestDto req) {
        return youtubeApiV3Service.getById(req.getUrl()).flatMap(x -> {
            if (x.getItems().size() != 1) {
                logger.error("Err {}", x);
                return Mono.error(new RuntimeException("Not one youtube element"));
            }
            return Mono.just(x);
        }).flatMapMany(x -> Flux.create(sink -> {
            String id = req.getUrl();
            DownloadItemDbo item = new DownloadItemDbo();
            item.setStatus(DownloadStatuses.DOWNLOAD_STATUS_CREATED);
            item.setType(DOWNLOAD_TYPE_YOUTUBE);
            item.setPartUrl(id);
            item.setUrl(getYoutubeUrlById(id));
            item.setCreatedDate(LocalDateTime.now());

            YoutubeVideoItemApi3Dto.Item.Snippet snippet = x.getItems().get(0).getSnippet();
            YoutubeVideoItemApi3Dto.Item.Snippet.ThumbnailItem thumbnail = snippet.getThumbnails().get("default");
            if (thumbnail != null) {
                item.setImg(thumbnail.getUrl());
            }

            item.setName(snippet.getTitle());

            // use another Subscriber (do in background) to prevent cancel download on http error / page refresh
            // use existing download object (url part) if exists to continue download instead of creating new download
            downloadRepository.findFirstByPartUrlOrderByLastUpdateDateDesc(req.getUrl())
                    .switchIfEmpty(downloadRepository.insert(item))
                    .publishOn(downloadBlockingSchedulers).subscribe(insert -> {
                doDownload(sink, insert);
            }, sink::error);
        }));
    }

    private void doDownload(FluxSink<DownloadUploadStatusDto> sink, DownloadItemDbo insert) {
        // TODO: "insert" parameter is shared and mutated between threads use immutable ???

        var youtubeVideoId = insert.getPartUrl();
        var state = new Object() {
            volatile InputStream inputStream = null;
            volatile InputStream errorInputStream = null;
            volatile Process youtubeDlProcess = null;
            volatile Exception encouragedError = null;
        };

        try {
            List<String> objects = new ArrayList<>();
            objects.add("youtube-dl");

            objects.add("-f");
            objects.add(config.getFormat());

            objects.add("-o");
            objects.add(outputPath);

            objects.add(getYoutubeUrlById(youtubeVideoId));

            var processBuilder = new ProcessBuilder(objects);

            logger.info("Start download youtube {}", String.join(" ", objects));
            state.youtubeDlProcess = processBuilder.start();

            logger.info("Start process PID={}", state.youtubeDlProcess.pid());

            state.inputStream = state.youtubeDlProcess.getInputStream();
            state.errorInputStream = state.youtubeDlProcess.getErrorStream();

            var outputMonitoring = new Thread(() -> {
                while (state.youtubeDlProcess.isAlive() && !Thread.currentThread().isInterrupted()) {
                    var buffer = new byte[1000];
                    int read = 0;
                    try {
                        read = state.inputStream.read(buffer);

                        if (read == -1) {
                            // closed stream
                            break;
                        }

                        var processOutputBuffer = new String(buffer);
                        logger.trace("youtube-dl output {}", processOutputBuffer);
                        var matcher = progressPattern.matcher(processOutputBuffer);
                        if (!matcher.find()) {
                            continue;
                        }

                        var percentage = matcher.group("percentage");
                        var size = matcher.group("size");
                        if (percentage != null) {
                            var downloadUploadStatusDto = new DownloadUploadStatusDto();

                            var sizeInBytes = SizeParser.parse(size);

                            downloadUploadStatusDto.setPercent(Double.valueOf(percentage.replace("%", "")));
                            downloadUploadStatusDto.setMsg(size);
                            downloadUploadStatusDto.setDownloadId(insert.getId().toString());
                            downloadUploadStatusDto.setSize(sizeInBytes);
                            sink.next(downloadUploadStatusDto);

                            insert.setStatus(DownloadStatuses.DOWNLOAD_STATUS_DOWNLOADING);
                            insert.setUploadedPercentage(downloadUploadStatusDto.getPercent());
                            insert.setLastUpdateDate(LocalDateTime.now());
                            insert.setSize(sizeInBytes);
                            // TODO: block or optimistic update; order must be matter
                            downloadRepository.save(insert).subscribe();
                        }
                    } catch (IOException e) {
                        logger.error("Error monitoring youtubeVideoId={}", insert.getId(), e);
                        state.encouragedError = e;
                        state.youtubeDlProcess.destroy();
                    }
                }
            });

            var errorMonitoring = new Thread(() -> {
                while (state.youtubeDlProcess.isAlive() && !Thread.currentThread().isInterrupted()) {
                    var buffer = new byte[1000];
                    int read = 0;
                    try {
                        read = state.errorInputStream.read(buffer);

                        if (read == -1) {
                            // closed stream
                            break;
                        }

                        var processOutputBuffer = new String(buffer);

                        if (processOutputBuffer.contains("WARNING:")) {
                            logger.warn("youtube-dl warning {}", processOutputBuffer);
                            continue;
                        }
                        logger.error("youtube-dl error output {}", processOutputBuffer);

                        state.encouragedError = new RuntimeException(processOutputBuffer);

                        var downloadUploadStatusDto = new DownloadUploadStatusDto();

                        downloadUploadStatusDto.setMsg(processOutputBuffer);
                        downloadUploadStatusDto.setDownloadId(insert.getId().toString());
                        sink.next(downloadUploadStatusDto);

                        insert.setStatus(DownloadStatuses.DOWNLOAD_STATUS_ERROR);
                        insert.setLastUpdateDate(LocalDateTime.now());
                        // TODO: block or optimistic update; order must be matter
                        downloadRepository.save(insert).subscribe();

                        // destroy download process on error
                        state.youtubeDlProcess.destroy();
                    } catch (IOException e) {
                        logger.error("Error errorMonitoring youtubeVideoId={}", insert.getId(), e);
                        state.encouragedError = e;
                        state.youtubeDlProcess.destroy();
                    }
                }
            });

            outputMonitoring.setName("youtube-dl-" + insert.getId());
            outputMonitoring.start();

            errorMonitoring.setName("youtube-dl-error-mon-" + insert.getId());
            errorMonitoring.start();

            state.youtubeDlProcess.waitFor();

            outputMonitoring.interrupt();
            errorMonitoring.interrupt();

            if (state.encouragedError == null) {
                insert.setStatus(DownloadStatuses.DOWNLOAD_STATUS_DONE);
                insert.setLastUpdateDate(LocalDateTime.now());

                downloadRepository.save(insert).subscribe(re -> {
                    logger.info("Completed download youtubeVideoId={}", youtubeVideoId);
                    sink.complete();
                });
            } else {
                throw state.encouragedError;
            }

        } catch (Exception e) {
            sendError(e, youtubeVideoId, insert, insert);
            logger.error("Error download youtube video {}", youtubeVideoId, e);
            sink.error(e);
        } finally {
            if (state.youtubeDlProcess != null && state.youtubeDlProcess.isAlive()) {
                state.youtubeDlProcess.destroy();
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
