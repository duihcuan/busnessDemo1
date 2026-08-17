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
