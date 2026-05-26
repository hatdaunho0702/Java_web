-- =====================================================
-- HỆ THỐNG QUẢN LÝ ĐIỆN MÁY
-- Database: MySQL 8.x
-- Đồ án môn: Lập trình Web
-- =====================================================

CREATE DATABASE IF NOT EXISTS dien_may_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE dien_may_db;

-- =====================================================
-- 1. BẢNG NGƯỜI DÙNG (tích hợp Firebase UID)
-- =====================================================
CREATE TABLE users (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    firebase_uid  VARCHAR(128) UNIQUE,           -- UID từ Firebase Auth
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    phone         VARCHAR(15),
    avatar_url    VARCHAR(255),
    role          ENUM('CUSTOMER', 'ADMIN', 'STAFF') DEFAULT 'CUSTOMER',
    is_active     BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- =====================================================
-- 2. BẢNG ĐỊA CHỈ GIAO HÀNG
-- =====================================================
CREATE TABLE user_addresses (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    recipient    VARCHAR(100) NOT NULL,
    phone        VARCHAR(15)  NOT NULL,
    province     VARCHAR(100) NOT NULL,
    district     VARCHAR(100) NOT NULL,
    ward         VARCHAR(100) NOT NULL,
    address_line VARCHAR(255) NOT NULL,
    is_default   BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =====================================================
-- 3. BẢNG THƯƠNG HIỆU
-- =====================================================
CREATE TABLE brands (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    logo_url   VARCHAR(255),
    is_active  BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 4. BẢNG DANH MỤC SẢN PHẨM (hỗ trợ danh mục cha-con)
-- =====================================================
CREATE TABLE categories (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(120) NOT NULL UNIQUE,
    parent_id   BIGINT DEFAULT NULL,              -- NULL = danh mục gốc
    icon_url    VARCHAR(255),
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (parent_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- Ví dụ:
-- Điện tử (parent_id = NULL)
--   ├── Điện thoại (parent_id = 1)
--   ├── Laptop     (parent_id = 1)
--   └── Máy tính bảng (parent_id = 1)
-- Điện lạnh (parent_id = NULL)
--   ├── Tủ lạnh    (parent_id = 2)
--   └── Điều hòa   (parent_id = 2)

-- =====================================================
-- 5. BẢNG SẢN PHẨM
-- =====================================================
CREATE TABLE products (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    slug          VARCHAR(220) NOT NULL UNIQUE,
    description   TEXT,
    category_id   BIGINT NOT NULL,
    brand_id      BIGINT NOT NULL,
    original_price DECIMAL(15, 0) NOT NULL,       -- Giá gốc
    sale_price    DECIMAL(15, 0) DEFAULT NULL,     -- Giá khuyến mãi (NULL = không KM)
    stock_qty     INT NOT NULL DEFAULT 0,
    sold_qty      INT NOT NULL DEFAULT 0,
    is_active     BOOLEAN DEFAULT TRUE,
    is_featured   BOOLEAN DEFAULT FALSE,          -- Sản phẩm nổi bật
    thumbnail_url VARCHAR(255),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(id),
    FOREIGN KEY (brand_id)    REFERENCES brands(id)
);

-- =====================================================
-- 6. BẢNG ẢNH SẢN PHẨM (1 sản phẩm nhiều ảnh)
-- =====================================================
CREATE TABLE product_images (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    image_url  VARCHAR(255) NOT NULL,
    sort_order INT DEFAULT 0,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- =====================================================
-- 7. BẢNG THÔNG SỐ KỸ THUẬT (key-value linh hoạt)
-- =====================================================
CREATE TABLE product_specs (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    spec_key   VARCHAR(100) NOT NULL,             -- VD: "RAM", "Pin", "Màn hình"
    spec_value VARCHAR(255) NOT NULL,             -- VD: "8GB", "5000mAh", "6.5 inch"
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- =====================================================
-- 8. BẢNG MÃ GIẢM GIÁ (COUPON)
-- =====================================================
CREATE TABLE coupons (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    code            VARCHAR(50) NOT NULL UNIQUE,
    description     VARCHAR(255),
    discount_type   ENUM('PERCENT', 'FIXED') NOT NULL,  -- % hoặc tiền cố định
    discount_value  DECIMAL(15, 2) NOT NULL,
    min_order_value DECIMAL(15, 0) DEFAULT 0,           -- Điều kiện đơn tối thiểu
    max_discount    DECIMAL(15, 0) DEFAULT NULL,        -- Giới hạn giảm tối đa (cho %)
    usage_limit     INT DEFAULT NULL,                   -- NULL = không giới hạn
    used_count      INT DEFAULT 0,
    start_date      DATETIME NOT NULL,
    end_date        DATETIME NOT NULL,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 9. BẢNG ĐƠN HÀNG
-- =====================================================
CREATE TABLE orders (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_code       VARCHAR(20) NOT NULL UNIQUE,       -- VD: DM-20241201-0001
    user_id          BIGINT NOT NULL,
    coupon_id        BIGINT DEFAULT NULL,
    -- Địa chỉ giao hàng (snapshot tại thời điểm đặt)
    recipient_name   VARCHAR(100) NOT NULL,
    recipient_phone  VARCHAR(15)  NOT NULL,
    shipping_address TEXT         NOT NULL,
    -- Tiền
    subtotal         DECIMAL(15, 0) NOT NULL,           -- Tổng trước giảm giá
    discount_amount  DECIMAL(15, 0) DEFAULT 0,
    shipping_fee     DECIMAL(15, 0) DEFAULT 0,
    total_amount     DECIMAL(15, 0) NOT NULL,           -- Tổng sau cùng
    -- Trạng thái
    status           ENUM('PENDING','CONFIRMED','SHIPPING','COMPLETED','CANCELLED')
                     DEFAULT 'PENDING',
    payment_method   ENUM('COD', 'MOMO', 'VNPAY', 'BANK') NOT NULL,
    payment_status   ENUM('UNPAID', 'PAID', 'REFUNDED') DEFAULT 'UNPAID',
    note             TEXT,
    created_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id)    REFERENCES users(id),
    FOREIGN KEY (coupon_id)  REFERENCES coupons(id) ON DELETE SET NULL
);

-- =====================================================
-- 10. BẢNG CHI TIẾT ĐƠN HÀNG
-- =====================================================
CREATE TABLE order_items (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id      BIGINT NOT NULL,
    product_id    BIGINT NOT NULL,
    product_name  VARCHAR(200) NOT NULL,           -- Snapshot tên sản phẩm
    product_image VARCHAR(255),
    unit_price    DECIMAL(15, 0) NOT NULL,         -- Giá tại thời điểm mua
    quantity      INT NOT NULL,
    subtotal      DECIMAL(15, 0) NOT NULL,
    FOREIGN KEY (order_id)   REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- =====================================================
-- 11. BẢNG LỊCH SỬ TRẠNG THÁI ĐƠN HÀNG
-- =====================================================
CREATE TABLE order_status_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id    BIGINT NOT NULL,
    old_status  VARCHAR(20),
    new_status  VARCHAR(20) NOT NULL,
    changed_by  BIGINT,                            -- user_id của admin/staff
    note        TEXT,
    changed_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id)   REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (changed_by) REFERENCES users(id)  ON DELETE SET NULL
);

-- =====================================================
-- 12. BẢNG GIỎ HÀNG
-- =====================================================
CREATE TABLE cart_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity   INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_cart (user_id, product_id),
    FOREIGN KEY (user_id)    REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- =====================================================
-- 13. BẢNG ĐÁNH GIÁ SẢN PHẨM
-- =====================================================
CREATE TABLE reviews (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT NOT NULL,
    user_id     BIGINT NOT NULL,
    order_id    BIGINT NOT NULL,                   -- Chỉ review nếu đã mua
    rating      TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment     TEXT,
    status      ENUM('PENDING', 'APPROVED', 'REJECTED') DEFAULT 'PENDING',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_review (product_id, user_id, order_id),
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id)    REFERENCES users(id)    ON DELETE CASCADE,
    FOREIGN KEY (order_id)   REFERENCES orders(id)   ON DELETE CASCADE
);

-- =====================================================
-- 14. BẢNG THÔNG BÁO (Admin notification khi sắp hết hàng)
-- =====================================================
CREATE TABLE notifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    type        ENUM('LOW_STOCK', 'NEW_ORDER', 'CANCELLED_ORDER', 'SYSTEM') NOT NULL,
    title       VARCHAR(200) NOT NULL,
    message     TEXT,
    target_role ENUM('ADMIN', 'STAFF', 'ALL') DEFAULT 'ADMIN',
    is_read     BOOLEAN DEFAULT FALSE,
    ref_id      BIGINT DEFAULT NULL,               -- product_id hoặc order_id liên quan
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- 15. BẢNG LOG NHẬP XUẤT KHO
-- =====================================================
CREATE TABLE inventory_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT NOT NULL,
    change_qty  INT NOT NULL,                      -- Dương = nhập, Âm = xuất
    reason      ENUM('PURCHASE','RETURN','ADJUSTMENT','IMPORT') NOT NULL,
    ref_id      BIGINT DEFAULT NULL,               -- order_id nếu liên quan đơn hàng
    note        TEXT,
    created_by  BIGINT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id),
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- =====================================================
-- INDEXES để tăng hiệu năng query
-- =====================================================
CREATE INDEX idx_products_category  ON products(category_id);
CREATE INDEX idx_products_brand     ON products(brand_id);
CREATE INDEX idx_products_price     ON products(original_price, sale_price);
CREATE INDEX idx_orders_user        ON orders(user_id);
CREATE INDEX idx_orders_status      ON orders(status);
CREATE INDEX idx_orders_created     ON orders(created_at);
CREATE INDEX idx_reviews_product    ON reviews(product_id);

-- =====================================================
-- DỮ LIỆU MẪU (SEED DATA)
-- =====================================================

-- Admin mặc định (firebase_uid sẽ cập nhật sau khi tạo account Firebase)
INSERT INTO users (full_name, email, phone, role) VALUES
('Admin Hệ Thống', 'admin@dienmaydemo.vn', '0901234567', 'ADMIN'),
('Nguyễn Văn A',   'nguyenvana@gmail.com', '0912345678', 'CUSTOMER'),
('Trần Thị B',     'tranthib@gmail.com',   '0923456789', 'CUSTOMER');

-- Thương hiệu
INSERT INTO brands (name) VALUES
('Samsung'), ('Apple'), ('LG'), ('Sony'), ('Panasonic'),
('Xiaomi'), ('ASUS'), ('HP'), ('Dell'), ('Toshiba');

-- Danh mục gốc
INSERT INTO categories (name, slug) VALUES
('Điện thoại & Máy tính bảng', 'dien-thoai-may-tinh-bang'),
('Laptop & Máy tính',          'laptop-may-tinh'),
('Tivi',                       'tivi'),
('Điện lạnh',                  'dien-lanh'),
('Máy giặt',                   'may-giat'),
('Âm thanh',                   'am-thanh');

-- Danh mục con
INSERT INTO categories (name, slug, parent_id) VALUES
('Điện thoại',       'dien-thoai', 1),
('Máy tính bảng',    'may-tinh-bang', 1),
('Laptop',           'laptop', 2),
('Máy tính bàn',     'may-tinh-ban', 2),
('Tủ lạnh',          'tu-lanh', 4),
('Điều hòa',         'dieu-hoa', 4),
('Máy giặt cửa trước', 'may-giat-cua-truoc', 5),
('Loa Bluetooth',    'loa-bluetooth', 6);

-- Mã giảm giá mẫu
INSERT INTO coupons (code, description, discount_type, discount_value, min_order_value, end_date, start_date) VALUES
('WELCOME10', 'Giảm 10% cho khách hàng mới', 'PERCENT', 10, 0, NOW() + INTERVAL 1 YEAR, NOW()),
('SALE500K',  'Giảm 500k cho đơn từ 5 triệu', 'FIXED', 500000, 5000000, NOW() + INTERVAL 6 MONTH, NOW());
