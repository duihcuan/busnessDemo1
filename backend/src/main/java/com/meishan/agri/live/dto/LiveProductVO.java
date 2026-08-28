package com.meishan.agri.live.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LiveProductVO {
    private Long id;
    private Long roomId;
    private Long productId;
    private BigDecimal livePrice;
    private Integer sort;
    private Integer soldCount;
    private String productName;
    private String productImage;
    private String specText;
    private BigDecimal price;
    private Integer stock;
    private String origin;
}