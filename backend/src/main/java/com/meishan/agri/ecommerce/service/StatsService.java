package com.meishan.agri.ecommerce.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.ecommerce.entity.Orders;
import com.meishan.agri.ecommerce.entity.Product;
import com.meishan.agri.ecommerce.mapper.OrderMapper;
import com.meishan.agri.ecommerce.mapper.ProductMapper;
import com.meishan.agri.live.entity.LiveRoom;
import com.meishan.agri.live.mapper.LiveRoomMapper;
import com.meishan.agri.system.entity.User;
import com.meishan.agri.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatsService {
    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final LiveRoomMapper liveRoomMapper;
    private final UserMapper userMapper;

    public Map<String, Object> sellerStats(Long sellerId) {
        List<Orders> orders = orderMapper.selectList(Wrappers.<Orders>lambdaQuery()
                .eq(Orders::getSellerId, sellerId));
        long productCount = productMapper.selectCount(Wrappers.<Product>lambdaQuery()
                .eq(Product::getSellerId, sellerId));
        long liveCount = liveRoomMapper.selectCount(Wrappers.<LiveRoom>lambdaQuery()
                .eq(LiveRoom::getSellerId, sellerId).eq(LiveRoom::getStatus, "LIVE"));
        Map<String, Object> map = new HashMap<>();
        map.put("orderCount", orders.size());
        map.put("salesAmount", orders.stream()
                .filter(o -> !"PENDING_PAY".equals(o.getStatus()) && !"CANCELLED".equals(o.getStatus()))
                .map(Orders::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        map.put("productCount", productCount);
        map.put("liveCount", liveCount);
        return map;
    }

    public Map<String, Object> adminStats() {
        Map<String, Object> map = new HashMap<>();
        map.put("userCount", userMapper.selectCount(Wrappers.<User>lambdaQuery()));
        map.put("productCount", productMapper.selectCount(Wrappers.<Product>lambdaQuery()));
        map.put("orderCount", orderMapper.selectCount(Wrappers.<Orders>lambdaQuery()));
        map.put("liveCount", liveRoomMapper.selectCount(Wrappers.<LiveRoom>lambdaQuery()));
        return map;
    }
}
