package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedTest extends BaseTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allTablesAndSeedDataLoaded() {
        // 直接对每张核心表做 COUNT 查询，表不存在会抛异常导致测试失败
        assertEquals(3, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_user", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM seller_info", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_address", Integer.class));
        Integer categories = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product_category", Integer.class);
        assertEquals(3, categories);
        Integer products = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM product", Integer.class);
        assertTrue(products >= 6);
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class));
        assertEquals(1, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM live_room", Integer.class));
        assertEquals(0, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM kb_chunk", Integer.class));
    }
}
