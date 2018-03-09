package com.nbakaev.yad.gateway.youtube;

import lombok.Data;
import org.apache.kafka.common.protocol.types.Field;

@Data
public class YoutubeUploadStatus {

    private Double percent;
    private String msg;

}
