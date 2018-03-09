package com.nbakaev.yad.gateway.youtube;

import org.bson.types.ObjectId;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping("/v1/youtube")
@RestController
public class YoutubeController {

    private final YoutubeRepository youtubeRepository;
    private final YoutubeDownloader youtubeDownloader;

    public YoutubeController(YoutubeRepository youtubeRepository, YoutubeDownloader youtubeDownloader) {
        this.youtubeRepository = youtubeRepository;
        this.youtubeDownloader = youtubeDownloader;
    }

    @PostMapping("")
    Mono<Void> create(@RequestBody Mono<YoutubeItemDto> personStream) {
        return this.youtubeRepository.insert( personStream.map(this::mapYoutubeItemDtoToDBo) ).then();
    }

    @GetMapping("/list_items")
    Flux<YoutubeItemDto> list() {
        return this.youtubeRepository.findAll().map(this::mapYoutubeItemDboToDto);
    }

    @GetMapping("/person/{id}")
    Mono<YoutubeItemDto> findById(@PathVariable String id) {
        return this.youtubeRepository.findOneById(id).map(this::mapYoutubeItemDboToDto);
    }

    @GetMapping(value = "/download_video/{id}", produces = "application/stream+json")
    Flux<YoutubeUploadStatus> downloadVideo(@PathVariable String id) {
        return this.youtubeDownloader.downloadVideo(id);
    }

    private YoutubeItemDto mapYoutubeItemDboToDto(YoutubeItemDbo dbo) {
        YoutubeItemDto dto = new YoutubeItemDto();

        dto.setId(dbo.getId().toString());
        dto.setName(dbo.getName());
        dto.setImg(dbo.getImg());
        dto.setUrl(dbo.getUrl());

        return dto;
    }

    private YoutubeItemDbo mapYoutubeItemDtoToDBo(YoutubeItemDto dto) {
        YoutubeItemDbo dbo = new YoutubeItemDbo();

        if (!StringUtils.isEmpty(dto.getId())) {
            dbo.setId(new ObjectId(dto.getId()));
        }

        dbo.setName(dto.getName());
        dbo.setImg(dto.getImg());
        dbo.setUrl(dto.getUrl());

        return dbo;
    }

}
