package com.nbakaev.yad.gateway.youtube;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class YoutubeDownloader {

    private final Pattern progressPattern;

    public YoutubeDownloader() {
        // TODO: check if youtube-dl in PATH
        // tested on youtube-dl  >youtube-dl --version //2018.03.03
        progressPattern = Pattern.compile("\\[download\\]\\s+(?<percentage>\\d+(?:\\.\\d+)?%)\\s+of\\s+(?<size>\\d+(?:\\.\\d+)?(?:K|M|G)iB)(?:\\s+at\\s+(?<speed>\\d+(?:\\.\\d+)?(?:K|M|G)iB\\/s))?(?:\\s+ETA\\s+(?<eta>[\\d]{2}:[\\d]{2}))?");
    }

    private static final Logger logger = LoggerFactory.getLogger(YoutubeDownloader.class);

    // TODO: from config
    private String outputPath = "F:\\test\\%(title)s-%(id)s.%(ext)s";

    public Flux<YoutubeUploadStatus> downloadVideo(String id) {
        return Flux.create(sink -> {
            try {
                ArrayList<String> objects = new ArrayList<>();
                objects.add("youtube-dl");
                objects.add("-o");
                objects.add(outputPath);
                objects.add("https://www.youtube.com/watch?v=" + id);

                ProcessBuilder processBuilder = new ProcessBuilder(objects);
                Process p = processBuilder.start();
//            System.out.println(p.getPid());
                InputStream inputStream = p.getInputStream();

                byte[] buffer = new byte[1000];

                while (p.isAlive()) {
                    int read = inputStream.read(buffer);
                    if (read == -1) {
                        // closed stream
                        break;
                    }

                    YoutubeUploadStatus youtubeUploadStatus = new YoutubeUploadStatus();

                    // TODO: better
                    Matcher matcher = progressPattern.matcher(new String(buffer));
                    if (!matcher.find()) {
                        continue;
                    }

                    String percentage = matcher.group("percentage");
                    String size = matcher.group("size");
                    if (percentage != null) {
                        youtubeUploadStatus.setPercent(Double.valueOf(percentage.replace("%", "")));
                        youtubeUploadStatus.setMsg(size);
                        sink.next(youtubeUploadStatus);
                    }
                }

                p.waitFor();
                sink.complete();
            } catch (Exception e) {
                logger.error("Error download youtube video {}", id, e);
            }
        });
    }

}
