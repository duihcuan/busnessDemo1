package com.meishan.agri.rag.intent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class IntentService {
    private static final Map<IntentType, List<String>> RULES = Map.of(
            IntentType.GREETING, List.of("你好", "您好", "在吗", "hello", "hi", "嗨"),
            IntentType.PRODUCT, List.of("商品", "购买", "价格", "多少钱", "下单", "买"),
            IntentType.ORDER, List.of("订单", "物流", "发货", "快递", "收货"),
            IntentType.AFTER_SALE, List.of("退款", "售后", "退货", "换货", "质量"),
            IntentType.POLICY, List.of("补贴", "政策", "申报", "助农"),
            IntentType.TECH, List.of("种植", "储存", "保鲜", "病虫害", "采收", "分级"),
            IntentType.PAICAI, List.of("泡菜", "发酵"),
            IntentType.LIVE, List.of("直播"),
            IntentType.STORE, List.of("开店", "入驻", "店铺", "上架")
    );

    public IntentType classify(String question) {
        if (question == null || question.isBlank()) return IntentType.FALLBACK;
        String q = question.toLowerCase();
        for (IntentType type : IntentType.values()) {
            if (type == IntentType.FALLBACK) continue;
            for (String kw : RULES.getOrDefault(type, List.of())) {
                if (q.contains(kw)) return type;
            }
        }
        return IntentType.FALLBACK;
    }
}