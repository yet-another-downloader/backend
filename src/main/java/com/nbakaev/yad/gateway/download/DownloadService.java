package com.nbakaev.yad.gateway.download;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DownloadService {

    private final ReactiveMongoOperations mongoOperations;

    public Mono<FindAllContainer> findAll(String sortBy, Integer limit, Long offset) {
        var query = new Query();

        if (!StringUtils.isEmpty(sortBy)) {
            var sorted = sortBy.split(":");
            query.with(Sort.by(Sort.Direction.valueOf(sorted[1]), sorted[0]));
        }

        if (limit != null) {
            query.limit(limit);
        }

        if (offset != null) {
            query.skip(offset);
        }

        return Mono.zip(mongoOperations.count(query, DownloadItemDbo.class), mongoOperations.find(query, DownloadItemDbo.class).collectList()).map(x -> {
            var b = new FindAllContainer();
            b.setCount(x.getT1());
            b.setItems(x.getT2());
            return b;
        });
    }

    @Data
    public static class FindAllContainer {

        private List<DownloadItemDbo> items;
        private long count;

    }

}
