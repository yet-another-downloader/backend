package com.nbakaev.yad.gateway.download;

import com.nbakaev.yad.gateway.download.dto.DownloadItemDto;
import com.nbakaev.yad.gateway.download.dto.DownloadRequestDto;
import com.nbakaev.yad.gateway.download.dto.DownloadUploadStatusDto;
import com.nbakaev.yad.gateway.youtube.YoutubeApiV3Service;
import com.nbakaev.yad.gateway.youtube.YoutubeVideoItemApi3Dto;
import lombok.RequiredArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RequestMapping("/api/v1/downloader")
@RestController
@RequiredArgsConstructor
public class DownloaderController {

    private final DownloadRepository downloadRepository;
    private final DownloaderService downloaderService;
    private final DownloadService downloadService;
    private final YoutubeApiV3Service youtubeApiV3Service;

//    @PostMapping("")
//    Mono<Void> create(@RequestBody Mono<DownloadItemDto> personStream) {
//        return this.downloadRepository.insert( personStream.map(this::mapYoutubeItemDtoToDBo) ).then();
//    }

    @GetMapping("/list_items")
    Mono<DownloadItemResponseDto> list(@RequestParam(value = "sortBy", required = false) String sortBy,
                                       @RequestParam(value = "limit", required = false) Integer limit,
                                       @RequestParam(value = "offset", required = false) Long offset) {
        return this.downloadService.findAll(sortBy, limit, offset).map(x -> {
            var b = new DownloadItemResponseDto();
            b.setCount(x.getCount());
            b.setItems(x.getItems().stream().map(this::mapYoutubeItemDboToDto).collect(Collectors.toList()));

            return b;
        });
    }

    @GetMapping("/item/{id}")
    Mono<DownloadItemDto> findById(@PathVariable String id) {
        return this.downloadRepository.findOneById(id).map(this::mapYoutubeItemDboToDto);
    }

    @GetMapping("/youtube_direct_details/id/{id}")
    Mono<YoutubeVideoItemApi3Dto> getYoutubeDirectApi(@PathVariable String id) {
        return this.youtubeApiV3Service.getById(id);
    }

    // TODO: post
    @GetMapping(value = "/download_video/{id}/{type}", produces = "application/stream+json")
    Flux<DownloadUploadStatusDto> download(@PathVariable("id") String id, @PathVariable("type") String type) {
        DownloadRequestDto downloadRequestDto = new DownloadRequestDto();
        downloadRequestDto.setUrl(id);
        downloadRequestDto.setType(type);
        return this.downloaderService.downloadVideo(downloadRequestDto);
    }

    private DownloadItemDto mapYoutubeItemDboToDto(DownloadItemDbo dbo) {
        DownloadItemDto dto = new DownloadItemDto();

        dto.setId(dbo.getId().toString());
        dto.setName(dbo.getName());
        dto.setImg(dbo.getImg());
        dto.setUrl(dbo.getUrl());
        dto.setStatus(dbo.getStatus());
        dto.setUploadedPercentage(dbo.getUploadedPercentage());
        dto.setPartUrl(dbo.getPartUrl());
        dto.setCreatedDate(dbo.getCreatedDate());
        dto.setLastUpdateDate(dbo.getLastUpdateDate());
        dto.setSize(dbo.getSize());

        return dto;
    }

    private DownloadItemDbo mapYoutubeItemDtoToDBo(DownloadItemDto dto) {
        DownloadItemDbo dbo = new DownloadItemDbo();

        if (!StringUtils.isEmpty(dto.getId())) {
            dbo.setId(new ObjectId(dto.getId()));
        }

        dbo.setName(dto.getName());
        dbo.setImg(dto.getImg());
        dbo.setUrl(dto.getUrl());
        dbo.setPartUrl(dto.getPartUrl());
        dbo.setStatus(dto.getStatus());
        dbo.setUploadedPercentage(dto.getUploadedPercentage());
        dbo.setCreatedDate(dto.getCreatedDate());
        dbo.setLastUpdateDate(dto.getLastUpdateDate());

        return dbo;
    }

}
