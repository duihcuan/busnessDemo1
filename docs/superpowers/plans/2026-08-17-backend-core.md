# 后端商城核心实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 交付可运行的后端 API（认证、商家入驻、商品分类、购物车、订单闭环、溯源、直播、文件上传、统计）与演示种子数据，供后续小程序与管理后台联调。

**架构：** Spring Boot 3 模块化单体（按包划分 system / ecommerce / live / common），Sa-Token 认证，MyBatis-Plus 操作 MySQL 单库，图片与视频存本地 uploads 目录并静态映射。

**技术栈：** Java 17+（本机为 21）、Spring Boot 3.2、MyBatis-Plus 3.5、Sa-Token 1.38、MySQL 8、Lombok、JUnit 5 + MockMvc（测试用 H2 内存库）。

---

## 文件结构

```text
backend/
  pom.xml                                   # 依赖与 Spring Boot parent
  src/main/java/com/meishan/agri/
    AgroApplication.java                    # 启动类（@MapperScan）
    common/
      Result.java                           # 统一响应 {code,message,data}
      BizException.java                     # 业务异常
      GlobalExceptionHandler.java           # 异常 → Result
      ShaUtil.java                          # SHA-256(password + salt)
    config/
      SaTokenConfig.java                    # 拦截器/CORS/静态资源映射
      MybatisPlusConfig.java                # 分页插件
    system/
      controller/AuthController.java        # 登录三接口 + 退出 + 当前用户
      controller/AddressController.java     # 收货地址 CRUD
      controller/SellerController.java      # 入驻申请 + 审核（管理员）
      service/AuthService.java              # 微信登录（可降级）/模拟登录/后台登录
      service/AddressService.java
      service/SellerService.java
      entity/User.java, SellerInfo.java, UserAddress.java
      mapper/UserMapper.java, SellerInfoMapper.java, UserAddressMapper.java
      dto/LoginRequest.java, WechatLoginRequest.java, AdminLoginRequest.java,
          LoginResponse.java, AddressDTO.java, SellerApplyDTO.java
    ecommerce/
      controller/ProductController.java     # 公开查询 + 商家管理
      controller/CategoryController.java    # 公开列表 + 管理员管理
      controller/CartController.java
      controller/OrderController.java       # 用户订单 + 商家发货
      controller/TraceController.java       # 公开查询 + 管理员维护
      controller/FileController.java        # 上传
      service/ProductService.java, CartService.java, OrderService.java,
              TraceService.java, FileService.java, StatsService.java
      entity/Category.java, Product.java, CartItem.java, Orders.java,
              OrderItem.java, TraceRecord.java
      mapper/CategoryMapper.java, ProductMapper.java, CartItemMapper.java,
              OrderMapper.java, OrderItemMapper.java, TraceRecordMapper.java
      dto/ProductDTO.java, CartDTO.java, OrderCreateDTO.java, OrderItemDTO.java,
              OrderDTO.java, ShipDTO.java
      OrderStatus.java                      # 订单状态枚举 + 合法迁移表
    live/
      controller/LiveController.java
      service/LiveService.java
      entity/LiveRoom.java, LiveProduct.java, LiveDanmaku.java
      mapper/LiveRoomMapper.java, LiveProductMapper.java, LiveDanmakuMapper.java
      dto/RoomDTO.java, RoomProductDTO.java, DanmakuDTO.java
  src/main/resources/
    application.yml
    db/schema.sql                           # 15 张表（DROP + CREATE）
    db/data.sql                             # 演示种子数据
  src/test/java/com/meishan/agri/
    BaseTest.java                           # @SpringBootTest + MockMvc + H2
    AuthTest.java, SellerAddressTest.java, ProductTest.java, CartTest.java,
    OrderTest.java, TraceTest.java, LiveTest.java, FileUploadTest.java,
    SmokeTest.java
  src/test/resources/application-test.yml   # H2 内存库
```

统一约定：所有接口返回 `Result<T>`；登录态用 Sa-Token，业务接口按需标注 `@SaCheckLogin` / `@SaCheckRole("SELLER")` / `@SaCheckRole("ADMIN")`；商家数据归属校验统一在 service 层按当前登录用户过滤。

---

### 任务 1：项目脚手架与通用层

**文件：**
- 创建：`backend/pom.xml`
- 创建：`backend/src/main/java/com/meishan/agri/AgroApplication.java`
- 创建：`backend/src/main/resources/application.yml`
- 创建：`backend/src/main/java/com/meishan/agri/common/Result.java`、`BizException.java`、`GlobalExceptionHandler.java`
- 创建：`backend/src/main/java/com/meishan/agri/config/SaTokenConfig.java`
- 创建：`backend/src/main/java/com/meishan/agri/system/controller/AuthController.java`（先放 Ping）
- 创建：`backend/src/test/java/com/meishan/agri/BaseTest.java`、`backend/src/test/resources/application-test.yml`

- [ ] **步骤 1：编写失败的冒烟测试**

```java
package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BaseTest {
    @Autowired
    protected MockMvc mockMvc;

    @Test
    void pingReturnsOk() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=BaseTest`
预期：FAIL，404（`/api/ping` 不存在，工程尚未创建时还会因缺 pom 直接失败，属预期）

- [ ] **步骤 3：创建 pom.xml、启动类与配置**

`backend/pom.xml` 关键内容：

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>
<groupId>com.meishan</groupId>
<artifactId>agri-ecommerce</artifactId>
<version>1.0.0</version>
<properties>
    <java.version>17</java.version>
</properties>
<dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>com.baomidou</groupId><artifactId>mybatis-plus-spring-boot3-starter</artifactId><version>3.5.7</version></dependency>
    <dependency><groupId>com.mysql</groupId><artifactId>mysql-connector-j</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>cn.dev33</groupId><artifactId>sa-token-spring-boot3-starter</artifactId><version>1.38.0</version></dependency>
    <dependency><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><scope>provided</scope></dependency>
    <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>test</scope></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
</dependencies>
```

`AgroApplication.java`：

```java
package com.meishan.agri;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.meishan.agri.**.mapper")
public class AgroApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgroApplication.class, args);
    }
}
```

`application.yml`：

```yaml
server:
  port: 8080
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/agri_ecommerce?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  sql:
    init:
      mode: always
      schema-locations: classpath:db/schema.sql
      data-locations: classpath:db/data.sql
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 25MB
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: Asia/Shanghai
sa-token:
  token-name: satoken
  timeout: 2592000
  is-concurrent: true
  is-log: false
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
wx:
  appid: ""
  secret: ""
```

`common/Result.java`：

```java
package com.meishan.agri.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) { return new Result<>(200, "ok", data); }
    public static <T> Result<T> fail(int code, String message) { return new Result<>(code, message, null); }
}
```

`common/BizException.java`：

```java
package com.meishan.agri.common;

public class BizException extends RuntimeException {
    public BizException(String message) { super(message); }
}
```

`common/GlobalExceptionHandler.java`：

```java
package com.meishan.agri.common;

import cn.dev33.satoken.exception.NotLoginException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public Result<Void> biz(BizException e) { return Result.fail(400, e.getMessage()); }

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> notLogin(NotLoginException e) { return Result.fail(401, "未登录或登录已过期"); }

    @ExceptionHandler(cn.dev33.satoken.exception.NotRoleException.class)
    public Result<Void> notRole(cn.dev33.satoken.exception.NotRoleException e) { return Result.fail(403, "无权限"); }

    @ExceptionHandler(Exception.class)
    public Result<Void> other(Exception e) { return Result.fail(500, "系统异常"); }
}
```

`config/SaTokenConfig.java`：

```java
package com.meishan.agri.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOriginPatterns("*").allowedMethods("*");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**").addResourceLocations("file:uploads/");
    }
}
```

`AuthController.java`（本任务仅 Ping，认证接口在任务 3 补全）：

```java
package com.meishan.agri.system.controller;

import com.meishan.agri.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {
    @GetMapping("/api/ping")
    public Result<Map<String, String>> ping() {
        return Result.ok(Map.of("msg", "pong"));
    }
}
```

`src/test/resources/application-test.yml`：

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:agri;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password: ""
  sql:
    init:
      mode: always
      schema-locations: classpath:db/schema.sql
      data-locations: classpath:db/data.sql
sa-token:
  token-name: satoken
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && mvn test -Dtest=BaseTest`
预期：PASS（注意：spring.sql.init 会先找 db/schema.sql，任务 2 才会创建，因此本任务通过前需先创建空占位文件 `src/main/resources/db/schema.sql` 与 `data.sql`，内容为合法注释即可，任务 2 会替换）

- [ ] **步骤 5：Commit**

```bash
git add backend
git commit -m "feat: 后端脚手架与通用响应层"
```

---

### 任务 2：数据库 schema 与种子数据

**文件：**
- 修改：`backend/src/main/resources/db/schema.sql`、`backend/src/main/resources/db/data.sql`
- 创建：`backend/src/test/java/com/meishan/agri/SeedTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
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
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=SeedTest`
预期：FAIL（表不存在，或数量不符）

- [ ] **步骤 3：编写 schema.sql（15 张表，DROP + CREATE）**

```sql
DROP TABLE IF EXISTS rag_message;
DROP TABLE IF EXISTS kb_chunk;
DROP TABLE IF EXISTS knowledge_doc;
DROP TABLE IF EXISTS live_danmaku;
DROP TABLE IF EXISTS live_product;
DROP TABLE IF EXISTS live_room;
DROP TABLE IF EXISTS trace_record;
DROP TABLE IF EXISTS order_item;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS cart_item;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS product_category;
DROP TABLE IF EXISTS user_address;
DROP TABLE IF EXISTS seller_info;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(64),
  password VARCHAR(128),
  nickname VARCHAR(64),
  avatar VARCHAR(255),
  phone VARCHAR(20),
  role VARCHAR(16) NOT NULL,
  wx_openid VARCHAR(64),
  status TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE seller_info (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  shop_name VARCHAR(128),
  shop_logo VARCHAR(255),
  shop_desc VARCHAR(500),
  qualification_img VARCHAR(255),
  status VARCHAR(16) DEFAULT 'PENDING'
);

CREATE TABLE user_address (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  receiver VARCHAR(64),
  phone VARCHAR(20),
  province VARCHAR(64),
  city VARCHAR(64),
  district VARCHAR(64),
  detail VARCHAR(255),
  is_default TINYINT DEFAULT 0
);

CREATE TABLE product_category (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(64) NOT NULL,
  sort INT DEFAULT 0
);

CREATE TABLE product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  seller_id BIGINT NOT NULL,
  category_id BIGINT,
  name VARCHAR(128) NOT NULL,
  main_image VARCHAR(255),
  images TEXT,
  spec_text VARCHAR(128),
  price DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  origin VARCHAR(64),
  trace_code VARCHAR(64),
  description TEXT,
  status VARCHAR(16) DEFAULT 'ON_SALE',
  sold_count INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cart_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_no VARCHAR(32) NOT NULL,
  user_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  total_amount DECIMAL(10,2) NOT NULL,
  status VARCHAR(16) NOT NULL,
  receiver_name VARCHAR(64),
  receiver_phone VARCHAR(20),
  receiver_address VARCHAR(500),
  logistics_company VARCHAR(64),
  logistics_no VARCHAR(64),
  pay_time DATETIME,
  ship_time DATETIME,
  finish_time DATETIME,
  remark VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_item (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  product_name VARCHAR(128),
  product_image VARCHAR(255),
  spec_text VARCHAR(128),
  price DECIMAL(10,2),
  quantity INT,
  subtotal DECIMAL(10,2)
);

CREATE TABLE trace_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  trace_code VARCHAR(64) NOT NULL,
  product_id BIGINT,
  stage VARCHAR(16) NOT NULL,
  title VARCHAR(128),
  content TEXT,
  record_date DATETIME,
  operator VARCHAR(64)
);

CREATE TABLE live_room (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  seller_id BIGINT NOT NULL,
  title VARCHAR(128),
  cover_url VARCHAR(255),
  video_url VARCHAR(255),
  status VARCHAR(16) DEFAULT 'OFFLINE',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE live_product (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  live_price DECIMAL(10,2),
  sort INT DEFAULT 0
);

CREATE TABLE live_danmaku (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_id BIGINT NOT NULL,
  user_id BIGINT,
  nickname VARCHAR(64),
  content VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE knowledge_doc (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(128),
  category VARCHAR(16),
  content TEXT,
  source VARCHAR(64),
  status VARCHAR(16) DEFAULT 'ENABLED',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE kb_chunk (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  doc_id BIGINT NOT NULL,
  chunk_index INT,
  content TEXT,
  vector BLOB,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rag_message (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  conversation_id VARCHAR(64),
  role VARCHAR(16),
  content TEXT,
  sources TEXT,
  feedback VARCHAR(16) DEFAULT 'NONE',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

- [ ] **步骤 4：编写 data.sql（种子数据）**

密码统一为 `sha256("123456" + salt "meishan2026") = d65a170641d33c4f82dddba5a3a8990c4624291f13ed8a5e9577b1187025f7a7`：

```sql
INSERT INTO sys_user (id, username, password, nickname, phone, role) VALUES
(1, 'admin',   'd65a170641d33c4f82dddba5a3a8990c4624291f13ed8a5e9577b1187025f7a7', '平台管理员', '13700000000', 'ADMIN'),
(2, 'seller1', 'd65a170641d33c4f82dddba5a3a8990c4624291f13ed8a5e9577b1187025f7a7', '东坡泡菜合作社', '13800000001', 'SELLER'),
(3, 'user1',   'd65a170641d33c4f82dddba5a3a8990c4624291f13ed8a5e9577b1187025f7a7', '演示用户', '13900000001', 'CONSUMER');

INSERT INTO seller_info (id, user_id, shop_name, shop_desc, status) VALUES
(1, 2, '东坡泡菜合作社', '眉山本地泡菜、柑橘直供', 'APPROVED');

INSERT INTO user_address (id, user_id, receiver, phone, province, city, district, detail, is_default) VALUES
(1, 3, '张三', '13900000001', '四川省', '眉山市', '东坡区', '演示路 1 号', 1);

INSERT INTO product_category (id, name, sort) VALUES
(1, '泡菜专区', 1), (2, '柑橘专区', 2), (3, '助农特产', 3);

INSERT INTO product (id, seller_id, category_id, name, main_image, spec_text, price, stock, origin, trace_code, description, status) VALUES
(1, 2, 1, '东坡泡菜·坛装老坛酸菜', '', '2.5kg/坛', 29.90, 100, '东坡区', 'MS-PC-001', '传统坛泡发酵 60 天', 'ON_SALE'),
(2, 2, 1, '东坡泡菜·礼盒装', '', '6 瓶装/盒', 99.00, 50, '东坡区', 'MS-PC-002', '节日送礼精选', 'ON_SALE'),
(3, 2, 2, '丹棱桔橙·精选 5 斤装', '', '单果 80-90mm', 39.90, 200, '丹棱县', 'MS-GJ-001', '丹棱标准分级 A 级果', 'ON_SALE'),
(4, 2, 2, '眉山春橘·10 斤家庭装', '', '单果 75-85mm', 59.90, 150, '青神县', 'MS-GJ-002', '应季春橘现摘直发', 'ON_SALE'),
(5, 2, 3, '柑橘果干·无添加', '', '500g/袋', 25.00, 80, '丹棱县', 'MS-GG-001', '低温烘干保留果香', 'ON_SALE'),
(6, 2, 3, '助农丰收礼盒', '', '泡菜+果干组合', 128.00, 30, '眉山市', 'MS-ZN-001', '一份礼盒一份爱心', 'ON_SALE');

INSERT INTO trace_record (id, trace_code, product_id, stage, title, content, record_date, operator) VALUES
(1, 'MS-PC-001', 1, 'PLANT', '原料种植', '眉山本地青菜标准化种植，全程记录施肥与灌溉', '2026-03-01 09:00:00', '东坡区农业农村局'),
(2, 'MS-PC-001', 1, 'PROCESS', '坛泡发酵', '传统陶坛发酵 60 天，发酵车间温湿度受控', '2026-05-10 10:00:00', '东坡泡菜合作社'),
(3, 'MS-PC-001', 1, 'QC', '出厂质检', '亚硝酸盐、微生物指标检测合格', '2026-05-12 15:00:00', '眉山市产品质量检验所'),
(4, 'MS-PC-001', 1, 'LOGISTICS', '冷链发货', '恒温冷链车运输至销地仓', '2026-05-14 08:00:00', '平台物流部');

INSERT INTO live_room (id, seller_id, title, cover_url, video_url, status) VALUES
(1, 2, '东坡泡菜产地直播', '', 'https://www.w3schools.com/html/mov_bbb.mp4', 'LIVE');

INSERT INTO live_product (id, room_id, product_id, live_price, sort) VALUES
(1, 1, 1, 26.90, 1), (2, 1, 3, 36.90, 2);

INSERT INTO live_danmaku (id, room_id, user_id, nickname, content, create_time) VALUES
(1, 1, 3, '演示用户', '这个泡菜发酵多久呀？', '2026-05-14 10:00:00'),
(2, 1, NULL, '眉山老乡', '支持家乡农产品！', '2026-05-14 10:00:05');
```

- [ ] **步骤 5：运行测试验证通过并 Commit**

运行：`cd backend && mvn test -Dtest=SeedTest`
预期：PASS

```bash
git add backend/src/main/resources/db
git commit -m "feat: 数据库 schema 与演示种子数据"
```

---

### 任务 3：Sa-Token 认证（模拟登录 / 微信登录可降级 / 后台登录）

**文件：**
- 创建：`backend/src/main/java/com/meishan/agri/system/entity/User.java`、`mapper/UserMapper.java`
- 创建：`backend/src/main/java/com/meishan/agri/common/ShaUtil.java`
- 创建：`backend/src/main/java/com/meishan/agri/system/dto/LoginRequest.java`、`WechatLoginRequest.java`、`AdminLoginRequest.java`、`LoginResponse.java`
- 创建：`backend/src/main/java/com/meishan/agri/system/service/AuthService.java`
- 修改：`backend/src/main/java/com/meishan/agri/system/controller/AuthController.java`
- 创建：`backend/src/test/java/com/meishan/agri/AuthTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthTest extends BaseTest {

    @Test
    void mockLoginReturnsTokenAndInfoWorks() throws Exception {
        String token = mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900000001\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/auth/info").header("satoken", token))
                .andExpect(jsonPath("$.data.role").value("CONSUMER"));
    }

    @Test
    void infoWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminLoginRejectsConsumer() throws Exception {
        mockMvc.perform(post("/api/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user1\",\"password\":\"123456\"}"))
                .andExpect(jsonPath("$.code").value(400));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=AuthTest`
预期：FAIL（接口不存在或返回 404）

- [ ] **步骤 3：实现认证**

`ShaUtil.java`：

```java
package com.meishan.agri.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ShaUtil {
    private static final String SALT = "meishan2026";

    public static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((raw + SALT).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
```

`User.java`：

```java
package com.meishan.agri.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private String phone;
    private String role;
    private String wxOpenid;
    private Integer status;
    private LocalDateTime createTime;
}
```

`UserMapper.java`：

```java
package com.meishan.agri.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meishan.agri.system.entity.User;

public interface UserMapper extends BaseMapper<User> {
}
```

DTO（`LoginRequest`/`WechatLoginRequest`/`AdminLoginRequest`/`LoginResponse`）：

```java
// LoginRequest.java
package com.meishan.agri.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank private String phone;
    @NotBlank private String code;
}
```

```java
// WechatLoginRequest.java
package com.meishan.agri.system.dto;

import lombok.Data;

@Data
public class WechatLoginRequest {
    private String code;   // wx.login 的 code，为空或调用失败时降级
    private String phone;  // 降级模拟登录使用的手机号
}
```

```java
// AdminLoginRequest.java
package com.meishan.agri.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminLoginRequest {
    @NotBlank private String username;
    @NotBlank private String password;
}
```

```java
// LoginResponse.java
package com.meishan.agri.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String role;
    private Long userId;
    private String nickname;
}
```

`AuthService.java`：

```java
package com.meishan.agri.system.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.common.BizException;
import com.meishan.agri.common.ShaUtil;
import com.meishan.agri.system.dto.*;
import com.meishan.agri.system.entity.User;
import com.meishan.agri.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;

    @Value("${wx.appid:}") private String appid;
    @Value("${wx.secret:}") private String secret;

    public LoginResponse mockLogin(LoginRequest req) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getPhone, req.getPhone()));
        if (user == null) {
            user = new User();
            user.setPhone(req.getPhone());
            user.setNickname("用户" + req.getPhone().substring(7));
            user.setRole("CONSUMER");
            user.setStatus(1);
            userMapper.insert(user);
        }
        return doLogin(user);
    }

    public LoginResponse wechatLogin(WechatLoginRequest req) {
        try {
            String openid = code2Session(req.getCode());
            User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                    .eq(User::getWxOpenid, openid));
            if (user == null) {
                user = new User();
                user.setWxOpenid(openid);
                user.setNickname("微信用户");
                user.setRole("CONSUMER");
                user.setStatus(1);
                userMapper.insert(user);
            }
            return doLogin(user);
        } catch (Exception e) {
            LoginRequest fallback = new LoginRequest();
            fallback.setPhone(req.getPhone());
            fallback.setCode("123456");
            return mockLogin(fallback);
        }
    }

    public LoginResponse adminLogin(AdminLoginRequest req) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, req.getUsername()));
        if (user == null || !user.getPassword().equals(ShaUtil.hash(req.getPassword()))) {
            throw new BizException("用户名或密码错误");
        }
        if (!"ADMIN".equals(user.getRole())) {
            throw new BizException("非管理员账号");
        }
        return doLogin(user);
    }

    public User currentUser() {
        Long id = StpUtil.getLoginIdAsLong();
        return userMapper.selectById(id);
    }

    private LoginResponse doLogin(User user) {
        StpUtil.login(user.getId());
        return new LoginResponse(StpUtil.getTokenValue(), user.getRole(),
                user.getId(), user.getNickname());
    }

    private String code2Session(String code) {
        if (appid == null || appid.isBlank() || code == null || code.isBlank()) {
            throw new BizException("未配置微信登录");
        }
        Map<?, ?> resp = RestClient.builder().build().get()
                .uri("https://api.weixin.qq.com/sns/jscode2session?appid={a}&secret={s}&js_code={c}&grant_type=authorization_code",
                        appid, secret, code)
                .retrieve().body(Map.class);
        Object openid = resp == null ? null : resp.get("openid");
        if (openid == null) throw new BizException("微信登录失败");
        return openid.toString();
    }
}
```

`AuthController.java` 补全：

```java
package com.meishan.agri.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.meishan.agri.common.Result;
import com.meishan.agri.system.dto.*;
import com.meishan.agri.system.entity.User;
import com.meishan.agri.system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @GetMapping("/ping")
    public Result<String> ping() { return Result.ok("pong"); }

    @PostMapping("/mock-login")
    public Result<LoginResponse> mockLogin(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.mockLogin(req));
    }

    @PostMapping("/wechat-login")
    public Result<LoginResponse> wechatLogin(@RequestBody WechatLoginRequest req) {
        return Result.ok(authService.wechatLogin(req));
    }

    @PostMapping("/admin-login")
    public Result<LoginResponse> adminLogin(@Valid @RequestBody AdminLoginRequest req) {
        return Result.ok(authService.adminLogin(req));
    }

    @PostMapping("/logout")
    public Result<Void> logout() { StpUtil.logout(); return Result.ok(null); }

    @SaCheckLogin
    @GetMapping("/info")
    public Result<User> info() { return Result.ok(authService.currentUser()); }
}
```

（`@SaCheckLogin` 与 `@SaCheckRole` 引入：`import cn.dev33.satoken.annotation.*;`）

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && mvn test -Dtest=AuthTest`
预期：PASS（3 个用例全绿）

- [ ] **步骤 5：Commit**

```bash
git add backend
git commit -m "feat: Sa-Token 认证与三种登录方式"
```

---

### 任务 4：商家入驻审核与收货地址

**文件：**
- 创建：`backend/src/main/java/com/meishan/agri/system/entity/SellerInfo.java`、`UserAddress.java`
- 创建：`backend/src/main/java/com/meishan/agri/system/mapper/SellerInfoMapper.java`、`UserAddressMapper.java`
- 创建：`backend/src/main/java/com/meishan/agri/system/dto/SellerApplyDTO.java`、`AddressDTO.java`
- 创建：`backend/src/main/java/com/meishan/agri/system/service/SellerService.java`、`AddressService.java`
- 创建：`backend/src/main/java/com/meishan/agri/system/controller/SellerController.java`、`AddressController.java`
- 创建：`backend/src/test/java/com/meishan/agri/SellerAddressTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class SellerAddressTest extends BaseTest {

    private String login(String phone) throws Exception {
        return mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void applyAndApproveMakesSeller() throws Exception {
        String userToken = login("13500000001");
        String applyResult = mockMvc.perform(post("/api/seller/apply")
                        .header("satoken", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shopName\":\"丹棱果园\",\"shopDesc\":\"自家果园直供\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        String sellerInfoId = applyResult.replaceAll(".*\"id\":(\\d+).*", "$1");

        String adminToken = mockMvc.perform(post("/api/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/admin/sellers/" + sellerInfoId + "/approve").header("satoken", adminToken))
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/seller/info").header("satoken", userToken))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void addressCrudWithDefault() throws Exception {
        String token = login("13900000001");
        mockMvc.perform(post("/api/addresses")
                        .header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiver\":\"李四\",\"phone\":\"13900000001\",\"province\":\"四川省\",\"city\":\"眉山市\",\"district\":\"丹棱县\",\"detail\":\"果园路 2 号\",\"isDefault\":1}"))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/addresses").header("satoken", token))
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=SellerAddressTest`
预期：FAIL（接口不存在）

- [ ] **步骤 3：实现商家与地址**

`SellerInfo.java` / `UserAddress.java`：与表结构一一对应（`@TableId(type = IdType.AUTO)`、`@TableName("seller_info"/"user_address")`），字段与 schema 相同。

`SellerService.java` 核心逻辑：

```java
package com.meishan.agri.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.common.BizException;
import com.meishan.agri.system.dto.SellerApplyDTO;
import com.meishan.agri.system.entity.SellerInfo;
import com.meishan.agri.system.entity.User;
import com.meishan.agri.system.mapper.SellerInfoMapper;
import com.meishan.agri.system.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SellerService {
    private final SellerInfoMapper sellerInfoMapper;
    private final UserMapper userMapper;

    @Transactional
    public SellerInfo apply(Long userId, SellerApplyDTO dto) {
        SellerInfo info = sellerInfoMapper.selectOne(
                Wrappers.<SellerInfo>lambdaQuery().eq(SellerInfo::getUserId, userId));
        if (info == null) {
            info = new SellerInfo();
            info.setUserId(userId);
        }
        info.setShopName(dto.getShopName());
        info.setShopDesc(dto.getShopDesc());
        info.setStatus("PENDING");
        if (info.getId() == null) sellerInfoMapper.insert(info);
        else sellerInfoMapper.updateById(info);
        return info;
    }

    public SellerInfo myInfo(Long userId) {
        return sellerInfoMapper.selectOne(
                Wrappers.<SellerInfo>lambdaQuery().eq(SellerInfo::getUserId, userId));
    }

    public List<SellerInfo> listByStatus(String status) {
        return sellerInfoMapper.selectList(
                Wrappers.<SellerInfo>lambdaQuery()
                        .eq(status != null && !status.isBlank(), SellerInfo::getStatus, status));
    }

    @Transactional
    public void approve(Long sellerInfoId) {
        setStatus(sellerInfoId, "APPROVED");
    }

    @Transactional
    public void reject(Long sellerInfoId) {
        setStatus(sellerInfoId, "REJECTED");
    }

    private void setStatus(Long sellerInfoId, String status) {
        SellerInfo info = sellerInfoMapper.selectById(sellerInfoId);
        if (info == null) throw new BizException("商家信息不存在");
        info.setStatus(status);
        sellerInfoMapper.updateById(info);
        if ("APPROVED".equals(status)) {
            User user = userMapper.selectById(info.getUserId());
            user.setRole("SELLER");
            userMapper.updateById(user);
        }
    }
}
```

`AddressService.java` 核心逻辑：

```java
package com.meishan.agri.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.system.dto.AddressDTO;
import com.meishan.agri.system.entity.UserAddress;
import com.meishan.agri.system.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final UserAddressMapper addressMapper;

    public List<UserAddress> list(Long userId) {
        return addressMapper.selectList(Wrappers.<UserAddress>lambdaQuery()
                .eq(UserAddress::getUserId, userId).orderByDesc(UserAddress::getIsDefault));
    }

    @Transactional
    public UserAddress add(Long userId, AddressDTO dto) {
        UserAddress address = new UserAddress();
        copy(address, dto);
        address.setUserId(userId);
        if (address.getIsDefault() == 1) clearDefault(userId);
        addressMapper.insert(address);
        return address;
    }

    @Transactional
    public void update(Long userId, Long id, AddressDTO dto) {
        UserAddress address = owned(userId, id);
        copy(address, dto);
        if (address.getIsDefault() == 1) clearDefault(userId);
        addressMapper.updateById(address);
    }

    @Transactional
    public void remove(Long userId, Long id) {
        addressMapper.deleteById(owned(userId, id).getId());
    }

    public UserAddress owned(Long userId, Long id) {
        UserAddress address = addressMapper.selectById(id);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new com.meishan.agri.common.BizException("地址不存在");
        }
        return address;
    }

    private void clearDefault(Long userId) {
        List<UserAddress> list = addressMapper.selectList(
                Wrappers.<UserAddress>lambdaQuery().eq(UserAddress::getUserId, userId));
        list.forEach(a -> { a.setIsDefault(0); addressMapper.updateById(a); });
    }

    private void copy(UserAddress target, AddressDTO dto) {
        target.setReceiver(dto.getReceiver());
        target.setPhone(dto.getPhone());
        target.setProvince(dto.getProvince());
        target.setCity(dto.getCity());
        target.setDistrict(dto.getDistrict());
        target.setDetail(dto.getDetail());
        target.setIsDefault(dto.getIsDefault());
    }
}
```

`SellerController.java`（路由：`POST /api/seller/apply`、`GET /api/seller/info`，均 `@SaCheckLogin`；管理员路由 `GET /api/admin/sellers`、`POST /api/admin/sellers/{id}/approve|reject`，`@SaCheckRole("ADMIN")`；id 均取 `StpUtil.getLoginIdAsLong()`）。

`AddressController.java`：`GET/POST /api/addresses`、`PUT/DELETE /api/addresses/{id}`，均 `@SaCheckLogin`，service 方法一一转发。

`AddressDTO.java`：`receiver/phone/province/city/district/detail/isDefault` 与 entity 字段同名；`SellerApplyDTO.java`：`shopName/shopDesc/shopLogo/qualificationImg`。

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && mvn test -Dtest=SellerAddressTest`
预期：PASS（`addressCrudWithDefault` 断言地址数为 2：种子 1 条 + 新增 1 条）

- [ ] **步骤 5：Commit**

```bash
git add backend
git commit -m "feat: 商家入驻审核与收货地址"
```

---

### 任务 5：商品与分类

**文件：**
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/entity/Category.java`、`Product.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/mapper/CategoryMapper.java`、`ProductMapper.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/dto/ProductDTO.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/service/ProductService.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/controller/ProductController.java`、`CategoryController.java`
- 创建：`backend/src/main/java/com/meishan/agri/config/MybatisPlusConfig.java`
- 创建：`backend/src/test/java/com/meishan/agri/ProductTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class ProductTest extends BaseTest {

    @Test
    void publicListFiltersOffSale() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "1").param("size", "10"))
                .andExpect(jsonPath("$.data.total").value(6));
    }

    @Test
    void sellerCanCreateAndOffSale() throws Exception {
        String token = mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000001\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        String body = mockMvc.perform(post("/api/products")
                        .header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":2,\"name\":\"丹棱桔橙·家庭装\",\"specText\":\"5kg/箱\",\"price\":45.00,\"stock\":60,\"origin\":\"丹棱县\",\"traceCode\":\"MS-GJ-003\",\"description\":\"现摘直发\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        String id = body.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(put("/api/products/" + id + "/status")
                        .header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFF_SALE\"}"))
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/products").param("categoryId", "2"))
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void nonSellerCannotCreateProduct() throws Exception {
        String token = mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900000001\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/products").header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"price\":1.00,\"stock\":1}"))
                .andExpect(jsonPath("$.code").value(403));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=ProductTest`
预期：FAIL（接口不存在 / 未加角色校验）

- [ ] **步骤 3：实现商品与分类**

`Category.java`：`id/name/sort`（`@TableName("product_category")`）；`Product.java` 与 schema 字段一一对应（`@TableName("product")`）。

`ProductDTO.java`：

```java
package com.meishan.agri.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {
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
}
```

`ProductService.java` 核心逻辑：

```java
package com.meishan.agri.ecommerce.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meishan.agri.common.BizException;
import com.meishan.agri.ecommerce.dto.ProductDTO;
import com.meishan.agri.ecommerce.entity.Product;
import com.meishan.agri.ecommerce.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductMapper productMapper;

    public Page<Product> page(Long categoryId, String keyword, String origin, int page, int size, boolean onlyOnSale) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        qw.eq(categoryId != null, Product::getCategoryId, categoryId)
          .like(keyword != null && !keyword.isBlank(), Product::getName, keyword)
          .eq(origin != null && !origin.isBlank(), Product::getOrigin, origin)
          .eq(onlyOnSale, Product::getStatus, "ON_SALE")
          .orderByDesc(Product::getCreateTime);
        return productMapper.selectPage(Page.of(page, size), qw);
    }

    public Product getById(Long id) {
        Product p = productMapper.selectById(id);
        if (p == null) throw new BizException("商品不存在");
        return p;
    }

    public Product create(Long sellerId, ProductDTO dto) {
        Product p = new Product();
        apply(p, dto);
        p.setSellerId(sellerId);
        p.setStatus("ON_SALE");
        p.setSoldCount(0);
        productMapper.insert(p);
        return p;
    }

    public void update(Long sellerId, Long id, ProductDTO dto) {
        Product p = owned(sellerId, id);
        apply(p, dto);
        productMapper.updateById(p);
    }

    public void changeStatus(Long sellerId, Long id, String status) {
        if (!"ON_SALE".equals(status) && !"OFF_SALE".equals(status)) throw new BizException("非法状态");
        Product p = owned(sellerId, id);
        p.setStatus(status);
        productMapper.updateById(p);
    }

    public void delete(Long sellerId, Long id) {
        productMapper.deleteById(owned(sellerId, id).getId());
    }

    public Product owned(Long sellerId, Long id) {
        Product p = productMapper.selectById(id);
        if (p == null || !p.getSellerId().equals(sellerId)) throw new BizException("商品不存在");
        return p;
    }

    private void apply(Product p, ProductDTO dto) {
        p.setCategoryId(dto.getCategoryId());
        p.setName(dto.getName());
        p.setMainImage(dto.getMainImage());
        p.setImages(dto.getImages());
        p.setSpecText(dto.getSpecText());
        p.setPrice(dto.getPrice());
        p.setStock(dto.getStock());
        p.setOrigin(dto.getOrigin());
        p.setTraceCode(dto.getTraceCode());
        p.setDescription(dto.getDescription());
    }
}
```

`ProductController.java`：

```java
package com.meishan.agri.ecommerce.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meishan.agri.common.BizException;
import com.meishan.agri.common.Result;
import com.meishan.agri.ecommerce.dto.ProductDTO;
import com.meishan.agri.ecommerce.entity.Product;
import com.meishan.agri.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping("/api/products")
    public Result<Page<Product>> page(@RequestParam(required = false) Long categoryId,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String origin,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return Result.ok(productService.page(categoryId, keyword, origin, page, size, true));
    }

    @GetMapping("/api/products/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @PostMapping("/api/products")
    public Result<Product> create(@RequestBody ProductDTO dto) {
        return Result.ok(productService.create(StpUtil.getLoginIdAsLong(), dto));
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @PutMapping("/api/products/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ProductDTO dto) {
        productService.update(StpUtil.getLoginIdAsLong(), id, dto);
        return Result.ok(null);
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @PutMapping("/api/products/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        productService.changeStatus(StpUtil.getLoginIdAsLong(), id, body.get("status"));
        return Result.ok(null);
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @DeleteMapping("/api/products/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(StpUtil.getLoginIdAsLong(), id);
        return Result.ok(null);
    }
}
```

`CategoryController.java`：`GET /api/categories` 公开返回全部分类（按 sort 升序）；`POST/PUT/DELETE /api/admin/categories` 标注 `@SaCheckRole("ADMIN")`。

分页插件需注册：`config/MybatisPlusConfig.java` 添加：

```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && mvn test -Dtest=ProductTest`
预期：PASS（种子 6 个 ON_SALE 商品；新建"丹棱桔橙·家庭装"下架后，柑橘分类公开可见仍为种子 1 个：id=3）

- [ ] **步骤 5：Commit**

```bash
git add backend
git commit -m "feat: 商品与分类模块"
```

---

### 任务 6：购物车

**文件：**
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/entity/CartItem.java`、`mapper/CartItemMapper.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/dto/CartDTO.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/service/CartService.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/controller/CartController.java`
- 创建：`backend/src/test/java/com/meishan/agri/CartTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class CartTest extends BaseTest {

    private String login() throws Exception {
        return mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900000001\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void addUpdateListRemove() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/cart").header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":2}"))
                .andExpect(jsonPath("$.code").value(200));
        String list = mockMvc.perform(get("/api/cart").header("satoken", token))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].productName").value("东坡泡菜·坛装老坛酸菜"))
                .andReturn().getResponse().getContentAsString();
        String cartId = list.replaceAll(".*\"id\":(\\d+).*", "$1");
        mockMvc.perform(put("/api/cart/" + cartId).header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(delete("/api/cart/" + cartId).header("satoken", token))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/cart").header("satoken", token))
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=CartTest`
预期：FAIL

- [ ] **步骤 3：实现购物车**

`CartItem.java`：`id/userId/productId/quantity/createTime`（`@TableName("cart_item")`）。

`CartDTO.java`：

```java
package com.meishan.agri.ecommerce.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CartDTO {
    private Long id;
    private Long productId;
    private Integer quantity;
    private String productName;
    private String productImage;
    private String specText;
    private BigDecimal price;
    private Integer stock;
}
```

`CartService.java`：

```java
package com.meishan.agri.ecommerce.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.common.BizException;
import com.meishan.agri.ecommerce.dto.CartDTO;
import com.meishan.agri.ecommerce.entity.CartItem;
import com.meishan.agri.ecommerce.entity.Product;
import com.meishan.agri.ecommerce.mapper.CartItemMapper;
import com.meishan.agri.ecommerce.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;

    public List<CartDTO> list(Long userId) {
        return cartItemMapper.selectList(Wrappers.<CartItem>lambdaQuery()
                        .eq(CartItem::getUserId, userId).orderByDesc(CartItem::getCreateTime))
                .stream().map(item -> {
                    Product p = productMapper.selectById(item.getProductId());
                    CartDTO dto = new CartDTO();
                    dto.setId(item.getId());
                    dto.setProductId(item.getProductId());
                    dto.setQuantity(item.getQuantity());
                    if (p != null) {
                        dto.setProductName(p.getName());
                        dto.setProductImage(p.getMainImage());
                        dto.setSpecText(p.getSpecText());
                        dto.setPrice(p.getPrice());
                        dto.setStock(p.getStock());
                    }
                    return dto;
                }).toList();
    }

    public CartItem add(Long userId, Long productId, Integer quantity) {
        Product p = productMapper.selectById(productId);
        if (p == null || !"ON_SALE".equals(p.getStatus())) throw new BizException("商品不可购买");
        CartItem exist = cartItemMapper.selectOne(Wrappers.<CartItem>lambdaQuery()
                .eq(CartItem::getUserId, userId).eq(CartItem::getProductId, productId));
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + quantity);
            cartItemMapper.updateById(exist);
            return exist;
        }
        CartItem item = new CartItem();
        item.setUserId(userId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        cartItemMapper.insert(item);
        return item;
    }

    public void updateQuantity(Long userId, Long id, Integer quantity) {
        CartItem item = owned(userId, id);
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
    }

    public void remove(Long userId, Long id) {
        cartItemMapper.deleteById(owned(userId, id).getId());
    }

    private CartItem owned(Long userId, Long id) {
        CartItem item = cartItemMapper.selectById(id);
        if (item == null || !item.getUserId().equals(userId)) throw new BizException("购物车项不存在");
        return item;
    }
}
```

`CartController.java`：`GET /api/cart`、`POST /api/cart`（body：productId/quantity）、`PUT /api/cart/{id}`（body：quantity）、`DELETE /api/cart/{id}`，全部 `@SaCheckLogin`。

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && mvn test -Dtest=CartTest`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend
git commit -m "feat: 购物车模块"
```

---

### 任务 7：订单闭环（核心状态机）

**文件：**
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/OrderStatus.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/entity/Orders.java`、`OrderItem.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/mapper/OrderMapper.java`、`OrderItemMapper.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/dto/OrderCreateDTO.java`、`OrderItemDTO.java`、`OrderDTO.java`、`ShipDTO.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/service/OrderService.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/controller/OrderController.java`
- 创建：`backend/src/test/java/com/meishan/agri/OrderTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class OrderTest extends BaseTest {

    private String login(String phone) throws Exception {
        return mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void fullOrderLifecycle() throws Exception {
        String buyer = login("13900000001");
        String order = mockMvc.perform(post("/api/orders").header("satoken", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":1,\"items\":[{\"productId\":1,\"quantity\":2}]}"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAY"))
                .andReturn().getResponse().getContentAsString();
        String orderId = order.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(post("/api/orders/" + orderId + "/pay").header("satoken", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"MOCK_WECHAT\"}"))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        String seller = login("13800000001");
        mockMvc.perform(put("/api/orders/" + orderId + "/ship").header("satoken", seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logisticsCompany\":\"顺丰\",\"logisticsNo\":\"SF123456\"}"))
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));

        mockMvc.perform(post("/api/orders/" + orderId + "/confirm").header("satoken", buyer))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void illegalTransitionRejectedAndStockRestoredOnCancel() throws Exception {
        String buyer = login("13900000001");
        String order = mockMvc.perform(post("/api/orders").header("satoken", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":1,\"items\":[{\"productId\":2,\"quantity\":1}]}"))
                .andReturn().getResponse().getContentAsString();
        String orderId = order.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(post("/api/orders/" + orderId + "/confirm").header("satoken", buyer))
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel").header("satoken", buyer))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void refundRestoresStock() throws Exception {
        String buyer = login("13900000001");
        String order = mockMvc.perform(post("/api/orders").header("satoken", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":1,\"items\":[{\"productId\":5,\"quantity\":5}]}"))
                .andReturn().getResponse().getContentAsString();
        String orderId = order.replaceAll(".*\"id\":(\\d+).*", "$1");
        mockMvc.perform(post("/api/orders/" + orderId + "/pay").header("satoken", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"MOCK_BALANCE\"}"))
                .andExpect(jsonPath("$.data.status").value("PAID"));
        mockMvc.perform(post("/api/orders/" + orderId + "/refund").header("satoken", buyer))
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));

        mockMvc.perform(get("/api/products/5"))
                .andExpect(jsonPath("$.data.stock").value(80));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=OrderTest`
预期：FAIL

- [ ] **步骤 3：实现订单状态机与服务**

`OrderStatus.java`：

```java
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
```

`Orders.java` / `OrderItem.java`：与 schema 字段一一对应（`@TableName("orders"/"order_item")`）。

`OrderCreateDTO.java` / `OrderItemDTO.java` / `ShipDTO.java`：

```java
// OrderCreateDTO.java
package com.meishan.agri.ecommerce.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderCreateDTO {
    private Long addressId;
    private String remark;
    private List<OrderItemDTO> items;
}
```

```java
// OrderItemDTO.java
package com.meishan.agri.ecommerce.dto;

import lombok.Data;

@Data
public class OrderItemDTO {
    private Long productId;
    private Integer quantity;
}
```

```java
// ShipDTO.java
package com.meishan.agri.ecommerce.dto;

import lombok.Data;

@Data
public class ShipDTO {
    private String logisticsCompany;
    private String logisticsNo;
}
```

`OrderService.java`（核心）：

```java
package com.meishan.agri.ecommerce.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meishan.agri.common.BizException;
import com.meishan.agri.ecommerce.OrderStatus;
import com.meishan.agri.ecommerce.dto.*;
import com.meishan.agri.ecommerce.entity.Orders;
import com.meishan.agri.ecommerce.entity.OrderItem;
import com.meishan.agri.ecommerce.entity.Product;
import com.meishan.agri.ecommerce.mapper.OrderItemMapper;
import com.meishan.agri.ecommerce.mapper.OrderMapper;
import com.meishan.agri.ecommerce.mapper.ProductMapper;
import com.meishan.agri.system.entity.UserAddress;
import com.meishan.agri.system.mapper.UserAddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final UserAddressMapper addressMapper;

    @Transactional
    public Orders create(Long userId, OrderCreateDTO dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty()) throw new BizException("订单不能为空");
        UserAddress address = addressMapper.selectById(dto.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) throw new BizException("收货地址不存在");

        BigDecimal total = BigDecimal.ZERO;
        Long sellerId = null;
        for (OrderItemDTO item : dto.getItems()) {
            Product p = productMapper.selectById(item.getProductId());
            if (p == null || !"ON_SALE".equals(p.getStatus())) throw new BizException("商品不可购买");
            if (p.getStock() < item.getQuantity()) throw new BizException("商品库存不足：" + p.getName());
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            sellerId = p.getSellerId();
        }

        Orders order = new Orders();
        order.setOrderNo("M" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(1000, 10000));
        order.setUserId(userId);
        order.setSellerId(sellerId);
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.PENDING_PAY.name());
        order.setReceiverName(address.getReceiver());
        order.setReceiverPhone(address.getPhone());
        order.setReceiverAddress(address.getProvince() + address.getCity() + address.getDistrict() + address.getDetail());
        order.setRemark(dto.getRemark());
        orderMapper.insert(order);

        for (OrderItemDTO item : dto.getItems()) {
            Product p = productMapper.selectById(item.getProductId());
            OrderItem oi = new OrderItem();
            oi.setOrderId(order.getId());
            oi.setProductId(p.getId());
            oi.setProductName(p.getName());
            oi.setProductImage(p.getMainImage());
            oi.setSpecText(p.getSpecText());
            oi.setPrice(p.getPrice());
            oi.setQuantity(item.getQuantity());
            oi.setSubtotal(p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            orderItemMapper.insert(oi);
            p.setStock(p.getStock() - item.getQuantity());
            productMapper.updateById(p);
        }
        return order;
    }

    @Transactional
    public Orders pay(Long userId, Long orderId, String channel) {
        Orders o = ownedByUser(userId, orderId);
        o.setStatus(o.getStatus() == null ? null : OrderStatus.valueOf(o.getStatus()).to(OrderStatus.PAID).name());
        o.setPayTime(LocalDateTime.now());
        orderMapper.updateById(o);
        return o;
    }

    @Transactional
    public Orders cancel(Long userId, Long orderId) {
        Orders o = ownedByUser(userId, orderId);
        transition(o, OrderStatus.CANCELLED);
        restoreStock(o);
        orderMapper.updateById(o);
        return o;
    }

    @Transactional
    public Orders ship(Long sellerId, Long orderId, ShipDTO dto) {
        Orders o = ownedBySeller(sellerId, orderId);
        o.setStatus(OrderStatus.valueOf(o.getStatus()).to(OrderStatus.SHIPPED).name());
        o.setLogisticsCompany(dto.getLogisticsCompany());
        o.setLogisticsNo(dto.getLogisticsNo());
        o.setShipTime(LocalDateTime.now());
        orderMapper.updateById(o);
        return o;
    }

    @Transactional
    public Orders confirm(Long userId, Long orderId) {
        Orders o = ownedByUser(userId, orderId);
        transition(o, OrderStatus.COMPLETED);
        o.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(o);
        return o;
    }

    @Transactional
    public Orders refund(Long userId, Long orderId) {
        Orders o = ownedByUser(userId, orderId);
        transition(o, OrderStatus.REFUNDED);
        restoreStock(o);
        orderMapper.updateById(o);
        return o;
    }

    public Page<Orders> pageByUser(Long userId, int page, int size) {
        return orderMapper.selectPage(Page.of(page, size),
                Wrappers.<Orders>lambdaQuery().eq(Orders::getUserId, userId).orderByDesc(Orders::getCreateTime));
    }

    public Page<Orders> pageBySeller(Long sellerId, int page, int size) {
        return orderMapper.selectPage(Page.of(page, size),
                Wrappers.<Orders>lambdaQuery().eq(Orders::getSellerId, sellerId).orderByDesc(Orders::getCreateTime));
    }

    public OrderDTO detail(Long userId, Long orderId, boolean seller) {
        Orders o = seller ? ownedBySeller(userId, orderId) : ownedByUser(userId, orderId);
        OrderDTO dto = new OrderDTO();
        dto.setOrder(o);
        dto.setItems(orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, orderId)));
        return dto;
    }

    private Orders ownedByUser(Long userId, Long orderId) {
        Orders o = orderMapper.selectById(orderId);
        if (o == null || !o.getUserId().equals(userId)) throw new BizException("订单不存在");
        return o;
    }

    private Orders ownedBySeller(Long sellerId, Long orderId) {
        Orders o = orderMapper.selectById(orderId);
        if (o == null || !o.getSellerId().equals(sellerId)) throw new BizException("订单不存在");
        return o;
    }

    private void transition(Orders o, OrderStatus target) {
        o.setStatus(OrderStatus.valueOf(o.getStatus()).to(target).name());
    }

    private void restoreStock(Orders o) {
        List<OrderItem> items = orderItemMapper.selectList(
                Wrappers.<OrderItem>lambdaQuery().eq(OrderItem::getOrderId, o.getId()));
        for (OrderItem item : items) {
            Product p = productMapper.selectById(item.getProductId());
            if (p != null) {
                p.setStock(p.getStock() + item.getQuantity());
                productMapper.updateById(p);
            }
        }
    }
}
```

`OrderDTO.java`：`Orders order` + `List<OrderItem> items`。

`OrderController.java`：

```java
package com.meishan.agri.ecommerce.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meishan.agri.common.Result;
import com.meishan.agri.ecommerce.dto.*;
import com.meishan.agri.ecommerce.entity.Orders;
import com.meishan.agri.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @SaCheckLogin
    @PostMapping("/orders")
    public Result<Orders> create(@RequestBody OrderCreateDTO dto) {
        return Result.ok(orderService.create(StpUtil.getLoginIdAsLong(), dto));
    }

    @SaCheckLogin
    @PostMapping("/orders/{id}/pay")
    public Result<Orders> pay(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(orderService.pay(StpUtil.getLoginIdAsLong(), id, body.getOrDefault("channel", "MOCK_WECHAT")));
    }

    @SaCheckLogin
    @PostMapping("/orders/{id}/cancel")
    public Result<Orders> cancel(@PathVariable Long id) {
        return Result.ok(orderService.cancel(StpUtil.getLoginIdAsLong(), id));
    }

    @SaCheckLogin
    @PostMapping("/orders/{id}/confirm")
    public Result<Orders> confirm(@PathVariable Long id) {
        return Result.ok(orderService.confirm(StpUtil.getLoginIdAsLong(), id));
    }

    @SaCheckLogin
    @PostMapping("/orders/{id}/refund")
    public Result<Orders> refund(@PathVariable Long id) {
        return Result.ok(orderService.refund(StpUtil.getLoginIdAsLong(), id));
    }

    @SaCheckLogin
    @GetMapping("/orders")
    public Result<Page<Orders>> myOrders(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.pageByUser(StpUtil.getLoginIdAsLong(), page, size));
    }

    @SaCheckLogin
    @GetMapping("/orders/{id}")
    public Result<OrderDTO> myOrder(@PathVariable Long id) {
        return Result.ok(orderService.detail(StpUtil.getLoginIdAsLong(), id, false));
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @GetMapping("/seller/orders")
    public Result<Page<Orders>> sellerOrders(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.pageBySeller(StpUtil.getLoginIdAsLong(), page, size));
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @GetMapping("/seller/orders/{id}")
    public Result<OrderDTO> sellerOrder(@PathVariable Long id) {
        return Result.ok(orderService.detail(StpUtil.getLoginIdAsLong(), id, true));
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @PutMapping("/orders/{id}/ship")
    public Result<Orders> ship(@PathVariable Long id, @RequestBody ShipDTO dto) {
        return Result.ok(orderService.ship(StpUtil.getLoginIdAsLong(), id, dto));
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && mvn test -Dtest=OrderTest`
预期：PASS（三条用例：完整生命周期、非法迁移拒绝+取消回补库存、退款回补库存）

- [ ] **步骤 5：Commit**

```bash
git add backend
git commit -m "feat: 订单状态机与交易闭环"
```

---

### 任务 8：溯源

**文件：**
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/entity/TraceRecord.java`、`mapper/TraceRecordMapper.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/service/TraceService.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/controller/TraceController.java`
- 创建：`backend/src/test/java/com/meishan/agri/TraceTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.meishan.agri;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class TraceTest extends BaseTest {

    @Test
    void queryByCodeReturnsFourStages() throws Exception {
        mockMvc.perform(get("/api/trace/MS-PC-001"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].stage").value("PLANT"));
    }

    @Test
    void unknownCodeReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/trace/NOT-EXIST"))
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=TraceTest`
预期：FAIL

- [ ] **步骤 3：实现溯源**

`TraceService.java`：

```java
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
                .orderByAsc(TraceRecord::getStage)
                .orderByAsc(TraceRecord::getRecordDate));
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
```

`TraceController.java`：`GET /api/trace/{code}` 公开；`GET /api/trace`、`POST /api/trace`、`PUT /api/trace/{id}`、`DELETE /api/trace/{id}` 标注 `@SaCheckRole("ADMIN")`。

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && mvn test -Dtest=TraceTest`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend
git commit -m "feat: 产品溯源查询"
```

---

### 任务 9：直播（模拟房间）

**文件：**
- 创建：`backend/src/main/java/com/meishan/agri/live/entity/LiveRoom.java`、`LiveProduct.java`、`LiveDanmaku.java`
- 创建：`backend/src/main/java/com/meishan/agri/live/mapper/LiveRoomMapper.java`、`LiveProductMapper.java`、`LiveDanmakuMapper.java`
- 创建：`backend/src/main/java/com/meishan/agri/live/dto/RoomDTO.java`、`RoomProductDTO.java`、`DanmakuDTO.java`
- 创建：`backend/src/main/java/com/meishan/agri/live/service/LiveService.java`
- 创建：`backend/src/main/java/com/meishan/agri/live/controller/LiveController.java`
- 创建：`backend/src/test/java/com/meishan/agri/LiveTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class LiveTest extends BaseTest {

    private String login(String phone) throws Exception {
        return mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void publicRoomListOnlyShowsLive() throws Exception {
        String seller = login("13800000001");
        mockMvc.perform(post("/api/live/rooms").header("satoken", seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"丹棱桔橙采摘直播\",\"videoUrl\":\"https://example.com/v.mp4\"}"))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/live/rooms"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void danmakuFlow() throws Exception {
        String user = login("13900000001");
        mockMvc.perform(post("/api/live/rooms/1/danmaku").header("satoken", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"主播好！\"}"))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/live/rooms/1/danmaku"))
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void sellerSetsRoomProducts() throws Exception {
        String seller = login("13800000001");
        mockMvc.perform(put("/api/live/rooms/1/products").header("satoken", seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"productId\":2,\"livePrice\":88.00,\"sort\":1}]"))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/live/rooms/1"))
                .andExpect(jsonPath("$.data.products.length()").value(1))
                .andExpect(jsonPath("$.data.products[0].productId").value(2));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=LiveTest`
预期：FAIL

- [ ] **步骤 3：实现直播服务**

`LiveService.java`（核心）：

```java
package com.meishan.agri.live.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.common.BizException;
import com.meishan.agri.live.dto.RoomDTO;
import com.meishan.agri.live.dto.RoomProductDTO;
import com.meishan.agri.live.entity.LiveDanmaku;
import com.meishan.agri.live.entity.LiveProduct;
import com.meishan.agri.live.entity.LiveRoom;
import com.meishan.agri.live.mapper.LiveDanmakuMapper;
import com.meishan.agri.live.mapper.LiveProductMapper;
import com.meishan.agri.live.mapper.LiveRoomMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LiveService {
    private final LiveRoomMapper roomMapper;
    private final LiveProductMapper productMapper;
    private final LiveDanmakuMapper danmakuMapper;

    public List<LiveRoom> listLiveRooms() {
        return roomMapper.selectList(Wrappers.<LiveRoom>lambdaQuery()
                .eq(LiveRoom::getStatus, "LIVE").orderByDesc(LiveRoom::getCreateTime));
    }

    public RoomDTO detail(Long roomId) {
        LiveRoom room = roomMapper.selectById(roomId);
        if (room == null) throw new BizException("直播间不存在");
        RoomDTO dto = new RoomDTO();
        dto.setRoom(room);
        dto.setProducts(productMapper.selectList(Wrappers.<LiveProduct>lambdaQuery()
                .eq(LiveProduct::getRoomId, roomId).orderByAsc(LiveProduct::getSort)));
        return dto;
    }

    public LiveRoom create(Long sellerId, RoomDTO dto) {
        LiveRoom room = new LiveRoom();
        room.setSellerId(sellerId);
        room.setTitle(dto.getRoom().getTitle());
        room.setCoverUrl(dto.getRoom().getCoverUrl());
        room.setVideoUrl(dto.getRoom().getVideoUrl());
        room.setStatus("OFFLINE");
        roomMapper.insert(room);
        return room;
    }

    public void changeStatus(Long sellerId, Long roomId, String status) {
        if (!"LIVE".equals(status) && !"OFFLINE".equals(status)) throw new BizException("非法状态");
        LiveRoom room = owned(sellerId, roomId);
        room.setStatus(status);
        roomMapper.updateById(room);
    }

    @Transactional
    public void setProducts(Long sellerId, Long roomId, List<RoomProductDTO> items) {
        owned(sellerId, roomId);
        productMapper.delete(Wrappers.<LiveProduct>lambdaQuery().eq(LiveProduct::getRoomId, roomId));
        items.forEach(item -> {
            LiveProduct lp = new LiveProduct();
            lp.setRoomId(roomId);
            lp.setProductId(item.getProductId());
            lp.setLivePrice(item.getLivePrice());
            lp.setSort(item.getSort());
            productMapper.insert(lp);
        });
    }

    public LiveDanmaku send(Long roomId, Long userId, String nickname, String content) {
        LiveDanmaku d = new LiveDanmaku();
        d.setRoomId(roomId);
        d.setUserId(userId);
        d.setNickname(nickname);
        d.setContent(content);
        danmakuMapper.insert(d);
        return d;
    }

    public List<LiveDanmaku> danmaku(Long roomId) {
        return danmakuMapper.selectList(Wrappers.<LiveDanmaku>lambdaQuery()
                .eq(LiveDanmaku::getRoomId, roomId).orderByAsc(LiveDanmaku::getCreateTime));
    }

    private LiveRoom owned(Long sellerId, Long roomId) {
        LiveRoom room = roomMapper.selectById(roomId);
        if (room == null || !room.getSellerId().equals(sellerId)) throw new BizException("直播间不存在");
        return room;
    }
}
```

`RoomDTO.java`：`LiveRoom room` + `List<LiveProduct> products`；`RoomProductDTO.java`：`productId/livePrice/sort`；`DanmakuDTO.java`：`content`。

`LiveController.java`：`GET /api/live/rooms`、`GET /api/live/rooms/{id}`、`GET /api/live/rooms/{id}/danmaku` 公开；`POST /api/live/rooms`、`PUT /api/live/rooms/{id}`、`PUT /api/live/rooms/{id}/status`（body：status）、`PUT /api/live/rooms/{id}/products` 标注 `@SaCheckLogin @SaCheckRole("SELLER")`；`POST /api/live/rooms/{id}/danmaku` 标注 `@SaCheckLogin`（nickname 取当前用户昵称）。

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && mvn test -Dtest=LiveTest`
预期：PASS（公开列表仅 1 个 LIVE 房间；弹幕从 2 条变 3 条；房间商品替换为 1 个）

- [ ] **步骤 5：Commit**

```bash
git add backend
git commit -m "feat: 模拟直播模块"
```

---

### 任务 10：文件上传

**文件：**
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/service/FileService.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/controller/FileController.java`
- 创建：`backend/src/test/java/com/meishan/agri/FileUploadTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class FileUploadTest extends BaseTest {

    private String login() throws Exception {
        return mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000001\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void uploadImageReturnsUrl() throws Exception {
        String token = login();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/files/upload").file(file).header("satoken", token))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/uploads/")));
    }

    @Test
    void uploadExeRejected() throws Exception {
        String token = login();
        MockMultipartFile file = new MockMultipartFile("file", "a.exe", "application/octet-stream", new byte[]{1});
        mockMvc.perform(multipart("/api/files/upload").file(file).header("satoken", token))
                .andExpect(jsonPath("$.code").value(400));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=FileUploadTest`
预期：FAIL

- [ ] **步骤 3：实现上传**

`FileService.java`：

```java
package com.meishan.agri.ecommerce.service;

import com.meishan.agri.common.BizException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "webp", "mp4", "mov");
    private static final long MAX_SIZE = 20L * 1024 * 1024;

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BizException("文件不能为空");
        if (file.getSize() > MAX_SIZE) throw new BizException("文件不能超过 20MB");
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED.contains(ext)) throw new BizException("不支持的文件类型");

        String dir = "uploads/" + LocalDate.now().toString().replace("-", "");
        try {
            Path path = Paths.get(dir);
            Files.createDirectories(path);
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext;
            file.transferTo(path.resolve(filename).toFile());
            return "/" + dir + "/" + filename;
        } catch (IOException e) {
            throw new BizException("文件保存失败");
        }
    }
}
```

`FileController.java`：`POST /api/files/upload`（`@SaCheckLogin`，multipart 参数名 `file`），返回 `Result.ok(Map.of("url", url))`。

运行测试前在项目根创建 `uploads/` 目录（gitignore 中 `uploads/` 追加一行）。

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && mvn test -Dtest=FileUploadTest`
预期：PASS（写入 `uploads/yyyyMMdd/`，返回 `/uploads/...` 路径）

- [ ] **步骤 5：Commit**

```bash
git add backend .gitignore
git commit -m "feat: 本地文件上传与静态访问"
```

---

### 任务 11：统计接口与端到端冒烟

**文件：**
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/service/StatsService.java`
- 创建：`backend/src/main/java/com/meishan/agri/ecommerce/controller/StatsController.java`
- 创建：`backend/src/test/java/com/meishan/agri/SmokeTest.java`

- [ ] **步骤 1：编写失败的冒烟测试**

```java
package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class SmokeTest extends BaseTest {

    @Test
    void sellerStatsAndAdminStats() throws Exception {
        String seller = mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000001\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(get("/api/seller/stats").header("satoken", seller))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.productCount").value(6));

        String admin = mockMvc.perform(post("/api/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(get("/api/admin/stats").header("satoken", admin))
                .andExpect(jsonPath("$.data.userCount").value(3));
    }

    @Test
    void fullDemoFlow() throws Exception {
        // 完整演示流程：登录 → 下单 → 支付 → 发货 → 收货 → 溯源 → 直播间
        String buyer = mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900000001\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        String order = mockMvc.perform(post("/api/orders").header("satoken", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":1,\"items\":[{\"productId\":3,\"quantity\":1}]}"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAY"))
                .andReturn().getResponse().getContentAsString();
        String orderId = order.replaceAll(".*\"id\":(\\d+).*", "$1");

        mockMvc.perform(post("/api/orders/" + orderId + "/pay").header("satoken", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"MOCK_WECHAT\"}"))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        String seller = mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000001\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(put("/api/orders/" + orderId + "/ship").header("satoken", seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logisticsCompany\":\"顺丰\",\"logisticsNo\":\"SF999\"}"))
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));

        mockMvc.perform(post("/api/orders/" + orderId + "/confirm").header("satoken", buyer))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/trace/MS-GJ-001"))
                .andExpect(jsonPath("$.data.length()").value(4));

        mockMvc.perform(get("/api/live/rooms"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=SmokeTest`
预期：FAIL（统计接口不存在）

- [ ] **步骤 3：实现统计**

`StatsService.java`：

```java
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
                .map(Orders::getTotalAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add));
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
```

`StatsController.java`：`GET /api/seller/stats`（`@SaCheckLogin @SaCheckRole("SELLER")`）、`GET /api/admin/stats`（`@SaCheckLogin @SaCheckRole("ADMIN")`）。

追加 `.gitignore` 一行：`uploads/`

- [ ] **步骤 4：运行全部测试验证通过**

运行：`cd backend && mvn test`
预期：PASS（全部用例：Base/Seed/Auth/SellerAddress/Product/Cart/Order/Trace/Live/FileUpload/Smoke）

- [ ] **步骤 5：Commit**

```bash
git add backend .gitignore
git commit -m "feat: 统计接口与端到端冒烟测试"
```

---

## 验收标准

1. `mvn test` 全绿（11 个测试类）。
2. 启动后访问 `http://localhost:8080/api/ping` 返回 `{"code":200}`。
3. 按 SmokeTest 脚本用 curl/Postman 走通：模拟登录 → 下单 → 支付 → 发货 → 确认收货 → 溯源 → 直播列表。
4. 种子账号：`admin/123456`（管理员）、`seller1/123456`（商家）、`13900000001/123456`（消费者，验证码任意）。

## 后续计划衔接

- 计划 2：RAG 智能客服模块（knowledge_doc/kb_chunk/rag_message 三张表已建好，等待文档入库、向量化、检索、DeepSeek 生成与 SSE）。
- 计划 3：微信小程序端。
- 计划 4：Vue3 管理后台。
