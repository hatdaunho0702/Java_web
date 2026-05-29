BEGIN TRANSACTION;
CREATE TABLE cart_items_new (
  id INTEGER PRIMARY KEY,
  created_at TIMESTAMP,
  quantity INTEGER NOT NULL,
  product_id BIGINT NOT NULL,
  user_uid VARCHAR(128) NOT NULL,
  FOREIGN KEY (product_id) REFERENCES products(id),
  FOREIGN KEY (user_uid) REFERENCES users(uid)
);
INSERT INTO cart_items_new (id, created_at, quantity, product_id, user_uid)
SELECT id, created_at, quantity, product_id, user_uid FROM cart_items;
DROP TABLE cart_items;
ALTER TABLE cart_items_new RENAME TO cart_items;
COMMIT;
