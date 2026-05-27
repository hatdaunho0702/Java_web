-- Bước 1: Thêm cột uid có thể NULL trước
ALTER TABLE users ADD COLUMN uid VARCHAR(128);

-- Bước 2: Xóa toàn bộ dữ liệu cũ vì uid cũ không hợp lệ
-- (user cũ không có Firebase UID thật)
DELETE FROM cart_items;
DELETE FROM orders;
DELETE FROM order_items;
DELETE FROM reviews;
DELETE FROM notifications;
DELETE FROM user_addresses;
DELETE FROM users;