package com.meishan.agri.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("product_category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer sort;
}
