package com.meishan.agri.ecommerce.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.ecommerce.entity.TraceRecord;
import com.meishan.agri.ecommerce.mapper.TraceRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TraceService {
    private final TraceRecordMapper traceRecordMapper;

    public List<TraceRecord> findByCode(String code) {
        return traceRecordMapper.selectList(Wrappers.<TraceRecord>lambdaQuery()
                .eq(TraceRecord::getTraceCode, code)
                .orderByAsc(TraceRecord::getRecordDate)
                .orderByAsc(TraceRecord::getId));
    }

    public List<TraceRecord> listAll() {
        return traceRecordMapper.selectList(Wrappers.<TraceRecord>lambdaQuery().orderByDesc(TraceRecord::getId));
    }

    public TraceRecord create(TraceRecord record) {
        traceRecordMapper.insert(record);
        return record;
    }

    public void update(TraceRecord record) {
        traceRecordMapper.updateById(record);
    }

    public void delete(Long id) {
        traceRecordMapper.deleteById(id);
    }
}
