package com.meishan.agri;

import com.meishan.agri.rag.intent.IntentService;
import com.meishan.agri.rag.intent.IntentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntentServiceTest {

    private final IntentService service = new IntentService();

    @Test
    void classifiesGreetingAndBusinessIntents() {
        assertEquals(IntentType.GREETING, service.classify("你好"));
        assertEquals(IntentType.TECH, service.classify("柑橘怎么储存保鲜"));
        assertEquals(IntentType.ORDER, service.classify("我的订单到哪了"));
        assertEquals(IntentType.POLICY, service.classify("助农补贴怎么申报"));
        assertEquals(IntentType.PAICAI, service.classify("泡菜发酵多久"));
        assertEquals(IntentType.FALLBACK, service.classify("随便聊聊"));
    }
}