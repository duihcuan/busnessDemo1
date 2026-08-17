# 眉山泡菜柑橘助农电商平台 · 系统设计文档

- 日期：2026-08-17
- 版本：v1
- 状态：待用户审查

## 1. 背景与目标

本项目为个人毕业设计，围绕眉山本地产业（东坡泡菜、丹棱桔橙、眉山春橘），实现一个助农电商平台的核心演示系统。系统覆盖三大业务：消费端电商交易、RAG 智能客服、模拟直播带货，并保留产品溯源查询作为平台特色功能。

设计原则：演示级、可离线兜底、可答辩讲清每一环。所有支付、物流、登录等外部能力均以模拟或可降级方式实现，避免演示时依赖真实渠道与网络。

成功标准：

1. 小程序端可完整走通"逛商品 → 直播间边看边买 → 下单 → 模拟支付 → 商家发货 → 确认收货"闭环。
2. RAG 客服能基于知识库回答助农相关问题，并展示引用来源；断网或无 API key 时可降级为离线检索回答。
3. 模拟直播可展示视频、弹幕、挂载商品与直播专享价购买。
4. 溯源查询可按编号展示种植/加工/质检/物流四阶段记录。
5. 管理后台可完成商品、订单、直播、知识库四大管理任务。

## 2. 范围

### 2.1 范围内

- 消费端微信小程序：商城浏览、购物车、下单、模拟支付、物流查看、确认收货、模拟退款、溯源查询、模拟直播、RAG 客服、微信登录（可降级）。
- Web 管理后台：商品管理、订单管理（发货/退款）、直播管理、溯源数据维护、知识库管理（RAG）、仪表盘。
- 后端 Spring Boot 单体：system / ecommerce / live / rag 四个业务模块 + 初始化演示数据。

### 2.2 范围外（明确不做）

- 真实支付渠道、真实物流对接、真实直播推流（SRS/腾讯云）。
- B2B 大宗交易、合作社社员管理、加工企业 OEM 代工、采购商比价、政府监管端、运营活动营销玩法。
- 分布式中间件（Redis、ES、RocketMQ、Nacos）、微服务拆分、容器化部署。
- 多语言、国际化、真实高并发与安全加固。

## 3. 总体架构（方案 A：模块化单体）

```text
┌─────────────────────────────┐    ┌──────────────────────────┐
│  微信小程序（原生）            │    │  Web 管理后台（Vue3）       │
│  商城/直播/客服/溯源/登录      │    │  商品/订单/直播/知识库       │
└─────────────┬───────────────┘    └────────────┬─────────────┘
              │  HTTPS JSON / SSE               │  HTTP JSON
              └───────────────┬─────────────────┘
                              ▼
                 ┌──────────────────────────┐
                 │  Spring Boot 单体后端        │
                 │  system │ ecommerce │ live │
                 │  rag    │ (Sa-Token 认证)   │
                 └──────┬──────────┬──────────┘
                        │          │
              ┌─────────▼───┐   ┌──▼──────────────┐
              │ MySQL 8     │   │ 本地文件存储      │
              │ 14 张业务表   │   │ 图片/预录视频     │
              └─────────────┘   └─────────────────┘
              ┌────────────────────────────────────┐
              │ AI 层（rag 模块内部）                  │
              │ DeepSeek chat API（联网，可降级）      │
              │ 本地 BGE 向量模型（ONNX，离线）         │
              │ 向量存 MySQL + 关键词检索兜底           │
              └────────────────────────────────────┘
```

一个后端进程 + 一个 MySQL 即可运行完整演示。图片与预录视频存本地目录并通过静态资源映射访问。

## 4. 技术选型

| 端/层 | 选型 | 说明 |
|---|---|---|
| 小程序 | 微信原生小程序（WXML/WXSS/JS） | 不引入 uni-app，减少框架依赖 |
| 管理后台 | Vue 3 + Vite + Element Plus + Pinia + Axios | 标准组合，管理端 UI 现成 |
| 后端 | Spring Boot 3.x + JDK 17+（本机 21） | 稳定 LTS 组合 |
| ORM | MyBatis-Plus | 中文生态、开发效率高 |
| 认证 | Sa-Token（JWT 风格 token） | 与 ragent 一致，轻量、文档全 |
| 数据库 | MySQL 8 | 单库 |
| AI 生成 | DeepSeek chat API（deepseek-chat） | 仅对话接口，SSE 流式 |
| AI 向量 | 本地 BGE 中文向量模型（ONNX Runtime） | 离线可用；模型缺失时走关键词检索兜底 |
| 文件 | 本地磁盘 + Spring 静态映射 | 图片、预录视频 |
| 测试 | JUnit 5 + MockMvc | 核心服务与接口冒烟 |

## 5. 后端模块与职责

| 模块 | 职责 |
|---|---|
| system | 用户、商家入驻、角色权限（Sa-Token）、微信登录与模拟登录、后台登录、地址管理 |
| ecommerce | 商品/分类、购物车、订单、模拟支付、物流发货、确认收货、模拟退款、溯源查询 |
| live | 直播间、挂载商品与直播专享价、弹幕（预置+实时）、开播/下播 |
| rag | 知识库文档入库（切分→向量化）、向量+关键词混合检索、DeepSeek 生成（SSE）、会话、引用来源、反馈 |

模块之间通过 service 接口调用，不跨模块直接依赖对方 controller。

## 6. 数据模型（MySQL，14 张表）

| 表 | 关键字段 |
|---|---|
| sys_user | id, username, password(BCrypt), nickname, avatar, phone, role(CONSUMER/SELLER/ADMIN), wx_openid, status, created_at |
| seller_info | id, user_id, shop_name, shop_logo, shop_desc, qualification_img, status(PENDING/APPROVED/REJECTED) |
| user_address | id, user_id, receiver, phone, province, city, district, detail, is_default |
| product_category | id, name(泡菜专区/柑橘专区/助农特产), sort |
| product | id, seller_id, category_id, name, main_image, images(JSON), spec_text, price, stock, origin, trace_code, description, status(ON_SALE/OFF_SALE), sold_count |
| orders | id, order_no, user_id, seller_id, total_amount, status(PENDING_PAY/PAID/SHIPPED/COMPLETED/REFUNDED/CANCELLED), receiver_name, receiver_phone, receiver_address, logistics_company, logistics_no, pay_time, ship_time, finish_time, remark |
| order_item | id, order_id, product_id, product_name, product_image, spec_text, price, quantity, subtotal |
| live_room | id, seller_id, title, cover_url, video_url, status(LIVE/OFFLINE), created_at |
| live_product | id, room_id, product_id, live_price, sort |
| live_danmaku | id, room_id, user_id, content, created_at |
| trace_record | id, trace_code, product_id, stage(PLANT/PROCESS/QC/LOGISTICS), title, content, record_date, operator |
| knowledge_doc | id, title, category(TECH/OPERATION/LIVE/POLICY/FAQ), content, source, status, created_at |
| kb_chunk | id, doc_id, chunk_index, content, vector(LONGBLOB), created_at |
| rag_message | id, user_id, conversation_id, role(USER/ASSISTANT), content, sources(JSON), feedback(UP/DOWN/NONE), created_at |

订单保存收货人快照（JSON），物流轨迹为固定四阶段静态数据，不单独建表。

## 7. 功能与接口

### 7.1 小程序页面

首页（tab）、分类/商品列表、商品详情（含溯源入口）、溯源查询、直播列表、直播间（视频+弹幕+商品轮播+边看边买）、购物车、确认订单、订单列表/详情（模拟支付/物流/确认收货/退款）、我的（tab）、地址管理、RAG 客服聊天（来源引用+反馈）、登录（微信登录，失败降级模拟）。

### 7.2 管理后台页面

登录、仪表盘（订单数/销售额/商品数/直播状态）、商品管理（增删改/上下架/库存）、订单管理（详情/发货/退款）、直播管理（建直播间/传预录视频/挂商品/开播下播/弹幕查看）、溯源数据维护、知识库管理（文档入库/问答测试/用户反馈查看）。

### 7.3 核心接口

| 域 | 接口 |
|---|---|
| 认证 | POST /api/auth/wechat-login；POST /api/auth/mock-login；POST /api/auth/admin-login；POST /api/auth/logout；GET /api/auth/info |
| 商品 | GET /api/categories；GET /api/products（分页+分类+关键字+产地）；GET /api/products/{id} |
| 溯源 | GET /api/trace/{code}；GET /api/trace（管理端列表）；POST/PUT/DELETE /api/trace/{id}（管理端维护） |
| 文件 | POST /api/files/upload（图片，返回 URL；直播视频同样走此接口） |
| 购物车 | GET/POST /api/cart；PUT/DELETE /api/cart/{id} |
| 地址 | GET/POST /api/addresses；PUT/DELETE /api/addresses/{id} |
| 订单 | POST /api/orders；POST /api/orders/{id}/pay；POST /api/orders/{id}/cancel；GET /api/orders；GET /api/orders/{id}；POST /api/orders/{id}/confirm；POST /api/orders/{id}/refund |
| 商家/管理员 | POST/PUT/DELETE /api/products；GET /api/seller/orders；GET /api/seller/orders/{id}；PUT /api/orders/{id}/ship；GET /api/seller/stats；GET /api/admin/stats |
| 直播 | GET /api/live/rooms；GET /api/live/rooms/{id}；POST/PUT /api/live/rooms；PUT /api/live/rooms/{id}/products；POST/GET /api/live/rooms/{id}/danmaku |
| RAG | POST /api/rag/documents（入库）；GET/DELETE /api/rag/documents；POST /api/rag/chat?stream=true（SSE/JSON）；GET /api/rag/messages；POST /api/rag/messages/{id}/feedback |

所有需登录接口由 Sa-Token 拦截；商家只能操作自己的商品、订单与直播间（数据归属校验）。

## 8. 关键流程

### 8.1 下单支付闭环

商品详情/直播间 → 加购或立即购买 → 选择收货地址 → 提交订单（PENDING_PAY）→ 模拟支付（支付渠道 MOCK_WECHAT/MOCK_BALANCE，后端直接置为 PAID，PAID 即"已支付待发货"）→ 商家后台发货（填物流公司+单号，置 SHIPPED）→ 用户查看物流（静态四阶段）→ 确认收货（COMPLETED）。未支付订单可取消（CANCELLED，回补库存）；已支付/已发货/已完成订单可发起模拟退款，自动通过并回补库存（REFUNDED）。

### 8.2 RAG 问答链路（参考 ragent 简化）

管理后台录入/上传知识库文档 → 按段落切分（固定长度上限）→ 本地 BGE 向量化（模型缺失时跳过）→ 写入 kb_chunk → 用户提问时拼接会话历史 → 向量余弦相似度 top-k 检索 + 关键词检索融合 → 组装 Prompt（知识片段+历史+角色设定）→ DeepSeek SSE 流式生成 → 返回回答与引用来源 → 用户点赞/点踩写入反馈。

### 8.3 模拟直播

后台创建直播间（封面+预录视频+挂载商品+直播专享价）→ 开播 → 小程序直播列表可见 → 进房播放视频、弹幕滚动（预置脚本+用户实时发送）→ 点击商品跳详情或直播价直接下单。

### 8.4 溯源查询

商品详情页"溯源"入口或输入溯源编号 → 按 trace_code 查询 → 展示种植/加工/质检/物流四阶段记录时间线（预置数据）。

## 9. 错误处理与演示兜底

1. 微信登录：code2session 未配置或失败时自动降级为模拟登录（任意手机号+验证码），前端无感。
2. DeepSeek：无 API key、断网或超时时降级为"检索片段摘要+固定话术"的离线回答，仍展示引用来源。
3. 本地 BGE 模型缺失或加载失败：自动切换关键词检索，功能不中断。
4. 支付/物流/退款：全模拟，无外部依赖。
5. 图片/视频缺失：前端展示占位图；预录视频可先用本地生成的示例 MP4。
6. 演示数据：data.sql 初始化脚本预置分类、商品（泡菜/柑橘/礼盒）、溯源记录、直播间、知识库文档（产业技术/店铺运营/直播/政策/FAQ）及 admin/seller 示例账号。

## 10. 测试与验收

### 10.1 后端测试

- JUnit 单测覆盖：订单状态流转（含非法状态迁移拒绝）、模拟支付与退款、RAG 检索与离线降级、登录认证。
- MockMvc 冒烟：认证、商品、订单、直播、RAG 关键接口返回正确。

### 10.2 演示验收脚本（答辩顺序）

1. 微信/模拟登录进入小程序。
2. 首页浏览泡菜/柑橘商品与分类。
3. 进入直播间，边看边买（直播专享价下单）。
4. 提交订单 → 模拟支付 → 管理后台发货 → 小程序查看物流 → 确认收货。
5. 商品溯源查询，展示四阶段记录。
6. RAG 客服提问（如"柑橘怎么储存"），展示回答与引用来源；断网演示离线兜底。
7. 管理后台完成商品上架、订单发货、创建直播间、知识库文档入库。

## 11. 假设与默认决策

- 小程序端使用原生微信小程序，不引入 uni-app。
- 管理后台使用 Vue 3 + Element Plus，不做移动端适配。
- 认证统一 Sa-Token，微信登录与模拟登录并存。
- DeepSeek 仅用于对话生成；向量化使用本地 BGE（ONNX），无额外 API key 依赖。
- 向量存 MySQL（kb_chunk.vector），数据量小，余弦相似度全量计算可接受；不做 Milvus/ES。
- 支付、物流、退款、直播均为模拟实现。
- 溯源查询 v1 提供"输入编号"与"商品详情直达"两个入口，不做二维码扫码生成。
- 预置演示数据由 data.sql 提供，账号与内容随文档附在初始化脚本。
- 项目目录同时作为管理后台、小程序、后端三部分代码的仓库根目录。
