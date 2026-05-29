# Database Migrations — Hướng dẫn

## Môi trường: SQLite

## Chạy lần đầu (fresh setup):

```bash
sqlite3 dien_may_db.db < V2__fix_cart_items_remove_user_id.sql
sqlite3 dien_may_db.db < V3__fix_user_columns_all_tables.sql
```

## Lý do migration:

- V2: Entity `CartItem` map `user_uid` (varchar)
  nhưng DB có `user_id` (bigint NOT NULL) thừa
  → gây 500 khi thêm vào giỏ
- V3: Đồng bộ các bảng `orders`, `reviews`,
  `inventory_logs`, `order_status_history`,
  `user_addresses` dùng `user_uid` thay `user_id`

## Ghi chú

- Repo hiện chỉ chứa các migration tăng dần V2/V3.
- Schema nền ban đầu phải có sẵn trước khi chạy các script này.
