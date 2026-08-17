package com.meishan.agri.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sellerId;
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
    private String status;
    private Integer soldCount;
    private LocalDateTime createTime;
}
