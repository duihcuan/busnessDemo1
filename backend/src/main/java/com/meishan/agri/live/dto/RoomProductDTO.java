package com.meishan.agri.live.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RoomProductDTO {
    private Long productId;
    private BigDecimal livePrice;
    private Integer sort;
}
