package com.rudraksha.shopsphere.media.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PagedMediaResponseDto {
    private String status;
    private int count;
    private List<MediaResponseDto> data;
}
