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
