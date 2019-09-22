package com.nbakaev.yad.gateway.youtube;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "yad.youtube")
public class YoutubePropertyConfiguration {

    private String pathDownload;

    private String format;

    private String baseUrl;

    private String apiKey;

}
