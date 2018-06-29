package com.nbakaev.yad.gateway.download.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DownloadItemDto {

    private String id;

    private String name;

    private String url;

    private String partUrl;

    private String status;

    private String img;

    private double uploadedPercentage;

    private LocalDateTime createdDate;

    private LocalDateTime lastUpdateDate;

}
