package com.nbakaev.yad.gateway.download;

import com.nbakaev.yad.gateway.download.dto.DownloadItemDto;
import lombok.Data;

import java.util.List;

@Data
public class DownloadItemResponseDto {

    private List<DownloadItemDto> items;

    private Long count;

}
