BEGIN TRANSACTION;

-- 1) orders: remove user_id (keep user_uid)
CREATE TABLE orders_new (
  id INTEGER PRIMARY KEY,
  created_at TIMESTAMP,
  discount_amount NUMERIC(15,0),
  note TEXT,
  order_code VARCHAR(20) NOT NULL UNIQUE,
  payment_method VARCHAR(20) NOT NULL,
  payment_status VARCHAR(20),
  recipient_name VARCHAR(100) NOT NULL,
  recipient_phone VARCHAR(15) NOT NULL,
  shipping_address TEXT NOT NULL,
  shipping_fee NUMERIC(15,0),
  status VARCHAR(20),
  subtotal NUMERIC(15,0) NOT NULL,
  total_amount NUMERIC(15,0) NOT NULL,
  updated_at TIMESTAMP,
  coupon_id BIGINT,
  user_uid VARCHAR(128) NOT NULL
);
INSERT INTO orders_new (id, created_at, discount_amount, note, order_code, payment_method, payment_status, recipient_name, recipient_phone, shipping_address, shipping_fee, status, subtotal, total_amount, updated_at, coupon_id, user_uid)
SELECT id, created_at, discount_amount, note, order_code, payment_method, payment_status, recipient_name, recipient_phone, shipping_address, shipping_fee, status, subtotal, total_amount, updated_at, coupon_id, user_uid
FROM orders;
DROP TABLE orders;
ALTER TABLE orders_new RENAME TO orders;

-- 2) order_status_history: change changed_by bigint -> varchar(128)
CREATE TABLE order_status_history_new (
  id INTEGER PRIMARY KEY,
  changed_at TIMESTAMP,
  new_status VARCHAR(20) NOT NULL,
  note TEXT,
  old_status VARCHAR(20),
  changed_by VARCHAR(128),
  order_id BIGINT NOT NULL
);
INSERT INTO order_status_history_new (id, changed_at, new_status, note, old_status, changed_by, order_id)
SELECT id, changed_at, new_status, note, old_status, CAST(changed_by AS TEXT), order_id
FROM order_status_history;
DROP TABLE order_status_history;
ALTER TABLE order_status_history_new RENAME TO order_status_history;

-- 3) inventory_logs: change created_by bigint -> varchar(128)
CREATE TABLE inventory_logs_new (
  id INTEGER PRIMARY KEY,
  change_qty INTEGER NOT NULL,
  created_at TIMESTAMP,
  note TEXT,
  reason VARCHAR(20) NOT NULL,
  ref_id BIGINT,
  created_by VARCHAR(128),
  product_id BIGINT NOT NULL
);
INSERT INTO inventory_logs_new (id, change_qty, created_at, note, reason, ref_id, created_by, product_id)
SELECT id, change_qty, created_at, note, reason, ref_id, CAST(created_by AS TEXT), product_id
FROM inventory_logs;
DROP TABLE inventory_logs;
ALTER TABLE inventory_logs_new RENAME TO inventory_logs;

-- 4) reviews: remove user_id (keep user_uid)
CREATE TABLE reviews_new (
  id INTEGER PRIMARY KEY,
  comment TEXT,
  created_at TIMESTAMP,
  rating INTEGER NOT NULL,
  status VARCHAR(20),
  order_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  user_uid VARCHAR(128) NOT NULL
);
INSERT INTO reviews_new (id, comment, created_at, rating, status, order_id, product_id, user_uid)
SELECT id, comment, created_at, rating, status, order_id, product_id, user_uid
FROM reviews;
DROP TABLE reviews;
ALTER TABLE reviews_new RENAME TO reviews;

-- 5) user_addresses: remove user_id (keep user_uid)
CREATE TABLE user_addresses_new (
  id INTEGER PRIMARY KEY,
  address_line VARCHAR(255) NOT NULL,
  district VARCHAR(100) NOT NULL,
  is_default BOOLEAN,
  phone VARCHAR(15) NOT NULL,
  province VARCHAR(100) NOT NULL,
  recipient VARCHAR(100) NOT NULL,
  ward VARCHAR(100) NOT NULL,
  user_uid VARCHAR(128) NOT NULL
);
INSERT INTO user_addresses_new (id, address_line, district, is_default, phone, province, recipient, ward, user_uid)
SELECT id, address_line, district, is_default, phone, province, recipient, ward, user_uid
FROM user_addresses;
DROP TABLE user_addresses;
ALTER TABLE user_addresses_new RENAME TO user_addresses;

COMMIT;
