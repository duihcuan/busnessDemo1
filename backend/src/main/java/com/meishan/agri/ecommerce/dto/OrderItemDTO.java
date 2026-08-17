package com.meishan.agri.ecommerce.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long productId;
    private Integer quantity;
}
