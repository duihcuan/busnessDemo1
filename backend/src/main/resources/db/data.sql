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

INSERT INTO trace_record (id, trace_code, product_id, stage, title, content, record_date, operator) VALUES
(5, 'MS-GJ-001', 3, 'PLANT', '果园种植', '丹棱桔橙标准化果园，水肥一体管理', '2026-03-15 09:00:00', '丹棱县农业农村局'),
(6, 'MS-GJ-001', 3, 'PROCESS', '分级分选', '按丹棱标准分级，A 级果自动分选线', '2026-06-01 10:00:00', '丹棱桔橙合作社'),
(7, 'MS-GJ-001', 3, 'QC', '农残检测', '农残检测合格，附检测报告', '2026-06-02 15:00:00', '眉山市产品质量检验所'),
(8, 'MS-GJ-001', 3, 'LOGISTICS', '冷链直发', '产地冷链车直发，48 小时送达', '2026-06-03 08:00:00', '平台物流部');

INSERT INTO live_room (id, seller_id, title, cover_url, video_url, status) VALUES
(1, 2, '东坡泡菜产地直播', '', 'https://www.w3schools.com/html/mov_bbb.mp4', 'LIVE');

INSERT INTO live_product (id, room_id, product_id, live_price, sort) VALUES
(1, 1, 1, 26.90, 1), (2, 1, 3, 36.90, 2);

INSERT INTO live_danmaku (id, room_id, user_id, nickname, content, create_time) VALUES
(1, 1, 3, '演示用户', '这个泡菜发酵多久呀？', '2026-05-14 10:00:00'),
(2, 1, NULL, '眉山老乡', '支持家乡农产品！', '2026-05-14 10:00:05');

INSERT INTO knowledge_doc (id, title, category, content, source, status) VALUES
(1, '眉山柑橘种植与采收保鲜', 'TECH', '丹棱桔橙应适时采收，避免早采影响糖酸比。采收后 24 小时内预冷至 4℃ 左右，储运过程保持通风、避免碰撞，可显著延长货架期。分级按丹棱标准：单果 80-90mm 为 A 级。', '眉山农业专家', 'ENABLED'),
(2, '东坡泡菜标准化生产', 'TECH', '东坡泡菜采用传统陶坛发酵，腌制期不低于 60 天。发酵车间保持恒温恒湿，原料青菜须经农残检测合格后入坛。出坛后分装、杀菌、质检合格方可出厂。', '东坡区农业农村局', 'ENABLED'),
(3, '农户零基础开店与商品上架', 'OPERATION', '开店三步：提交个人资质与土地证明，等待平台审核；审核通过后选择柑橘果园或泡菜基地模板；上传商品主图、规格与产地信息。主图建议白底实拍，标题包含品类+规格+卖点。', '平台运营', 'ENABLED'),
(4, '直播带货话术与场景搭建', 'LIVE', '产地直播建议在果园或泡菜车间开播，先展示采摘/发酵场景再上链接。话术结构：痛点开场-现场展示-规格价格-促单收尾。挂载商品使用直播专享价，突出产地直供。', '平台运营', 'ENABLED'),
(5, '眉山助农电商补贴申报', 'POLICY', '农户通过平台提交入驻资质后，可申报眉山市电商助农补贴。流程：平台初审-区县农业农村局复核-公示发放。补贴用于店铺装修、直播设备与冷链包装。', '眉山市商务局', 'ENABLED'),
(6, '商品售后与退换货常见问题', 'FAQ', '鲜活农产品签收后 24 小时内反馈质量问题可申请售后。泡菜类未开封可 7 天无理由退换。退款审核通过后原路退回。物流损坏请保留面单照片申请补发。', '平台客服', 'ENABLED');
