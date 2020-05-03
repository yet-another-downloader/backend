package com.nbakaev.yad.gateway.download;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Data
public class DownloadItemDbo {

    @Id
    private ObjectId id;

    private String name;

    private String url;

    private String partUrl;

    private String status;

    private String type;

    private double uploadedPercentage;

    private String img;

    private LocalDateTime createdDate;

    private LocalDateTime lastUpdateDate;

    private Long size;


}
