package com.meishan.agri.live.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("live_product")
public class LiveProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long productId;
    private BigDecimal livePrice;
    private Integer sort;
}
