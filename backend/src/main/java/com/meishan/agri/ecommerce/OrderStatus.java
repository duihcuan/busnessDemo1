package com.meishan.agri.ecommerce;

import com.meishan.agri.common.BizException;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    PENDING_PAY, PAID, SHIPPED, COMPLETED, REFUNDED, CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(Map.of(
            PENDING_PAY, Set.of(PAID, CANCELLED),
            PAID, Set.of(SHIPPED, REFUNDED),
            SHIPPED, Set.of(COMPLETED, REFUNDED),
            COMPLETED, Set.of(REFUNDED)
    ));

    public OrderStatus to(OrderStatus target) {
        if (!TRANSITIONS.getOrDefault(this, Set.of()).contains(target)) {
            throw new BizException("非法订单状态迁移：" + this + " -> " + target);
        }
        return target;
    }
}
