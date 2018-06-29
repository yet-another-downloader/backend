package com.nbakaev.yad.gateway.download;

import com.nbakaev.yad.gateway.download.dto.DownloadItemDto;
import com.nbakaev.yad.gateway.download.dto.DownloadRequestDto;
import com.nbakaev.yad.gateway.download.dto.DownloadUploadStatusDto;
import org.bson.types.ObjectId;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequestMapping("/v1/downloader")
@RestController
public class DownloaderController {

    private final DownloadRepository downloadRepository;
    private final DownloaderService downloaderService;

    public DownloaderController(DownloadRepository downloadRepository, DownloaderService downloaderService) {
        this.downloadRepository = downloadRepository;
        this.downloaderService = downloaderService;
    }

    @PostMapping("")
    Mono<Void> create(@RequestBody Mono<DownloadItemDto> personStream) {
        return this.downloadRepository.insert( personStream.map(this::mapYoutubeItemDtoToDBo) ).then();
    }

    @GetMapping("/list_items")
    Flux<DownloadItemDto> list() {
        return this.downloadRepository.findAll().map(this::mapYoutubeItemDboToDto);
    }

    @GetMapping("/person/{id}")
    Mono<DownloadItemDto> findById(@PathVariable String id) {
        return this.downloadRepository.findOneById(id).map(this::mapYoutubeItemDboToDto);
    }

    @GetMapping(value = "/download_video/{id}/{type}", produces = "application/stream+json")
    Flux<DownloadUploadStatusDto> downloadVideo(@PathVariable("id") String id, @PathVariable("type") String type) {
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
