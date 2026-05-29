Tóm tắt lỗi / logs (tổng hợp) — dự án Dien-may

Ngày: 2026-05-29
Người tạo: GitHub Copilot (thực hiện thay bạn trong workspace)

1. Lỗi nghiêm trọng: 500 khi add-to-cart (SQLite NOT NULL constraint)

- Triệu chứng: POST /api/cart với dev header (`X-Dev-Uid`) trả 500, log Hibernate:
  `SQLITE_CONSTRAINT_NOTNULL: cart_items.user_id` (hoặc tương tự)
- Nguyên nhân thực tế (phân tích): entity `CartItem` map `@JoinColumn(name="user_uid", referencedColumnName="uid", nullable=false)` nhưng SQL insert thông báo `user_id` NULL -> mismatch tên cột giữa schema hiện tại và mapping hoặc `user` relation chưa được set/đồng bộ đúng khi persist.
- File liên quan: `src/main/java/.../entity/CartItem.java`, `User.java`, `CartService.java`, `FirebaseTokenFilter.java`
- Hiện trạng: Chặn (blocker) cho các test E2E có user authenticated.
- Hành động đã thực hiện: kiểm tra mapping, thêm dev-header flow, log stacktrace.
- Đề xuất sửa nhanh: 1) Kiểm tra schema thực tế trong DB: `PRAGMA table_info(cart_items);` — nếu cột là `user_id`, sửa `@JoinColumn(name="user_id")` hoặc migrate DB để dùng `user_uid`. 2) Thêm logging trước khi save trong `CartService.addToCart` để verify `cartItem.getUser().getUid()` không null.

2. Lỗi quyền / principal mismatch → ClassCastException (trước)

- Triệu chứng: ClassCastException khi controller đọc `SecurityContext.getAuthentication().getPrincipal()` vì loại khác nhau.
- Nguyên nhân: một phần code trước đây giả định principal là `String` nhưng filter đôi khi đặt `CustomUserDetails`.
- File liên quan: `FirebaseTokenFilter.java`, `CartController.java`, `OrderController.java`.
- Hành động: Chuẩn hóa `FirebaseTokenFilter` để đặt principal là `String firebaseUid` cho cả dev và prod; thêm hỗ trợ `CustomUserDetails` fallback trong controller helper `getCurrentFirebaseUid()`.
- Kết quả: Vấn đề này đã được khắc phục.

3. Favicon 404 gây nhiều logs

- Triệu chứng: Nhiều 404/favicons gây nhiễu log.
- Hành động: đã tắt `spring.mvc.favicon` trong `application.yml` và thêm placeholder `favicon.ico` tĩnh; cập nhật `SecurityConfig` permit path.
- Kết quả: giảm noisy logs.

4. Encoding/charset (Tiếng Việt) hiển thị/ghi log không đúng

- Triệu chứng: Log/console có ký tự bị mã hóa sai.
- Hành động: Thêm `-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8` trong `JAVA_TOOL_OPTIONS` (mvnw wrapper), set `server.servlet.encoding.charset: UTF-8` và thiết lập Maven reporting/build encoding.
- Kết quả: Vấn đề đã được khắc phục (build và run với UTF-8).

5. application.yml duplicate key lỗi khi khởi động

- Triệu chứng: DuplicateKeyException khi Spring đọc cấu hình (do hai khối `spring:` lặp).
- Hành động: Đã gộp file `application.yml`, loại bỏ duplicate keys.
- Kết quả: Khởi động thành công.

6. API trả 401 cho thao tác cart khi chưa auth (client behaviour)

- Triệu chứng: UI gọi `/api/cart` trước khi Firebase auth sẵn sàng dẫn đến 401 (alert UX).
- Hành động: Di chuyển `updateCartBadge()` để chạy sau `onAuthStateChanged`, `addToCart()` redirect /login nếu thiếu token.
- Kết quả: UX cải thiện, tránh gọi API khi chưa auth.

7. Các lỗi nhỏ khác đã fix

- Thêm dev-header (`X-Dev-Uid`, `X-Dev-Email`, `X-Dev-Name`, `X-Dev-Role`) để dễ test flows.
- Thêm GlobalExceptionHandler để xuất JSON error cho request lỗi và 404 tĩnh im lặng.
- Implement Admin controllers (orders/users) và UI view scaffolding (orders.html, users.html).

Hướng dẫn tái tạo (reproduce) lỗi add-to-cart (ví dụ):

1. Chạy app (profile `dev`) và gửi request curl (dùng file JSON để tránh quoting issues):

```powershell
curl.exe -i -s -H "X-Dev-Uid: dev-user-1" -H "Content-Type: application/json" --data-binary @D:\temp_cart.json http://localhost:8080/api/cart -w "\nHTTP_CODE:%{http_code}\n"
```

- `D:\temp_cart.json` chứa payload giống client (productId, quantity...)
- Kiểm tra log console, stacktrace Hibernate để xác định SQL bind parameters.

Kiểm tra schema SQLite (từ máy dev):

```sql
sqlite3 db.sqlite "PRAGMA table_info(cart_items);"
```

(hoặc kết nối DB file tương ứng)

Các bước tiếp theo ưu tiên (recommended):

- 1. Kiểm tra schema `cart_items` (PRAGMA) — nếu cột hiện tại là `user_id` thì sửa mapping hoặc migrate DB.
- 2. Thêm logging tạm trong `CartService.addToCart` để in `user.getUid()` trước `save`.
- 3. Sau sửa mapping, chạy lại `mvnw -DskipTests spring-boot:run` và re-run E2E tests 4–6.

File liên quan chính (để đọc nhanh):

- `src/main/java/.../entity/CartItem.java`
- `src/main/java/.../entity/User.java`
- `src/main/java/.../service/CartService.java`
- `src/main/java/.../security/FirebaseTokenFilter.java`
- `src/main/resources/application.yml`
- `src/main/resources/templates/admin/orders.html`, `templates/admin/users.html`

Nếu bạn muốn, tôi có thể:

- (A) Tự chạy `PRAGMA table_info(cart_items)` và báo kết quả ngay bây giờ.
- (B) Thêm logging tạm trong `CartService.addToCart` và thử lại một lần POST add-to-cart.
- (C) Áp patch sửa mapping `@JoinColumn(name="user_id")` nếu bạn muốn thay đổi nhanh để tương thích schema hiện tại.

Chọn (A)/(B)/(C) hoặc yêu cầu khác.
