package com.nbakaev.yad.gateway.youtube;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

@Data
public class YoutubeItemDbo {

    @Id
    private ObjectId id;

    private String name;

    private String url;

    private String img;


}
