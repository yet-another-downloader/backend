package com.nbakaev.yad.gateway.download.dto;

import lombok.Data;

@Data
public class DownloadUploadStatusDto {

    private Double percent;

    private String msg;

    private String downloadId;

    private Long size;

}
