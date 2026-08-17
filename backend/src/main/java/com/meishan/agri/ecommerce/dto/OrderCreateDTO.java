package com.meishan.agri.ecommerce.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderCreateDTO {
    private Long addressId;
    private String remark;
    private List<OrderItemDTO> items;
}
