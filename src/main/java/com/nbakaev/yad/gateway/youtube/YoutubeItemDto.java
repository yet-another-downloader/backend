package com.nbakaev.yad.gateway.youtube;

import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class YoutubeItemDto {

    private String id;

    private String name;

    private String url;

    private String img;

}
