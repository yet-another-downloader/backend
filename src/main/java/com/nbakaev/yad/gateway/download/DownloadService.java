package com.nbakaev.yad.gateway.download;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private final DownloadRepository downloadRepository;
    private final ReactiveMongoOperations mongoOperations;

    public Flux<DownloadItemDbo> findAll(String sortBy) {
        Query query = new Query();

        if (!StringUtils.isEmpty(sortBy)) {
            String[] sorted = sortBy.split(":");
            query.with(Sort.by(Sort.Direction.valueOf(sorted[1]), sorted[0]));
        }

        return mongoOperations.find(query, DownloadItemDbo.class);
    }

}
