package com.meishan.agri.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartDTO {
    private Long id;
    private Long productId;
    private Integer quantity;
    private String productName;
    private String productImage;
    private String specText;
    private BigDecimal price;
    private Integer stock;
}
