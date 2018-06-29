package com.nbakaev.yad.gateway.download;

import com.nbakaev.yad.gateway.download.dto.DownloadRequestDto;
import com.nbakaev.yad.gateway.download.dto.DownloadUploadStatusDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DownloaderService {

    private Map<String, GenericDownloader> all = new HashMap<>();

    public DownloaderService(List<GenericDownloader> allDownloaders) {
       allDownloaders.forEach(x -> {
           all.put(x.getType(), x);
       });
    }

    public Flux<DownloadUploadStatusDto> downloadVideo(DownloadRequestDto req) {
        GenericDownloader genericDownloader = all.get(req.getType());
        if (genericDownloader != null) {
            return genericDownloader.downloadVideo(req);
        } else {
            String all = String.join(",", this.all.entrySet().stream().map(x -> x.getKey()).collect(Collectors.toList()));
            throw new IllegalArgumentException("Unknown downloader. Registered " + all);
        }
    }

}
