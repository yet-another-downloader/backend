package com.nbakaev.yad.gateway.download.dto;

import lombok.Data;

@Data
public class DownloadRequestDto {

    private String url;

    // can be null
    private String type;

}
