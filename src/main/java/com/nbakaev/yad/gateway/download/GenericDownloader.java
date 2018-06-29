package com.nbakaev.yad.gateway.download;

import com.nbakaev.yad.gateway.download.dto.DownloadRequestDto;
import com.nbakaev.yad.gateway.download.dto.DownloadUploadStatusDto;
import reactor.core.publisher.Flux;

public interface GenericDownloader {

    /**
     *
     * @param req generic download request
     * @return upload statuses
     */
    Flux<DownloadUploadStatusDto> downloadVideo(DownloadRequestDto req);

    String getType();

}
