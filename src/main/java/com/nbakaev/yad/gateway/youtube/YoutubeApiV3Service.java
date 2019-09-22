package com.nbakaev.yad.gateway.youtube;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class YoutubeApiV3Service {

    private final YoutubePropertyConfiguration props;

    public Mono<YoutubeVideoItemApi3Dto> getById(String id) {
        return WebClient.builder()
                .baseUrl(String.format("https://www.googleapis.com/youtube/v3/videos?part=snippet&id=%s&key=%s", id, props.getApiKey()))
                .defaultHeader("Content-Type", "application/json")
                .build()
                .get()
                .exchange()
                .flatMap(x -> x.toEntity(YoutubeVideoItemApi3Dto.class))
                .map(x -> x.getBody());
    }

}
