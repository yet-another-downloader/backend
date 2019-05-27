package com.nbakaev.yad.gateway.download;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface DownloadRepository extends ReactiveMongoRepository<DownloadItemDbo, String> {

    Mono<DownloadItemDbo> findOneById(String id);

    Mono<DownloadItemDbo> findFirstByPartUrlOrderByLastUpdateDateDesc(String url);

}
