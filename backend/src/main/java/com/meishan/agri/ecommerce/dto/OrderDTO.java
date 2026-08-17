package com.meishan.agri.ecommerce.dto;

import com.meishan.agri.ecommerce.entity.OrderItem;
import com.meishan.agri.ecommerce.entity.Orders;
import lombok.Data;

import java.util.List;

@Data
public class OrderDTO {
    private Orders order;
    private List<OrderItem> items;
}
