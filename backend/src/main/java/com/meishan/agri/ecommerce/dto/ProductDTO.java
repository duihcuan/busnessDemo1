package com.meishan.agri.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {
    private Long categoryId;
    private String name;
    private String mainImage;
    private String images;
    private String specText;
    private BigDecimal price;
    private Integer stock;
    private String origin;
    private String traceCode;
    private String description;
}
