package com.nbakaev.yad.gateway.youtube;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class YoutubeItemDto {

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
