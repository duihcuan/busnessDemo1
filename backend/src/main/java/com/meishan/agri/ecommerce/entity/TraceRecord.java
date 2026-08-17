package com.meishan.agri.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("trace_record")
public class TraceRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String traceCode;
    private Long productId;
    private String stage;
    private String title;
    private String content;
    private LocalDateTime recordDate;
    private String operator;
}
