package com.nbakaev.yad.gateway.youtube;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface YoutubeRepository extends ReactiveMongoRepository<YoutubeItemDbo, String> {

    Mono<YoutubeItemDbo> findOneById(String id);

}
