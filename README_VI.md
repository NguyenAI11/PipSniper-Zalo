# PipSniper Zalo Bridge V1.3

## Mục tiêu
PipSniper Zalo Bridge là ứng dụng Android chuyên làm một việc: **nhận notification Zalo từ các nguồn/chuyên gia đã liên kết và chuyển nguyên văn sang group/channel Telegram được chỉ định**.

Bản V1.3 giữ nguyên lõi Bridge V1.2 và thay giao diện theo phong cách chat-app hiện đại, đồng thời bổ sung launcher icon cánh bướm PipSniper.

## Chức năng chính
- Zalo `NotificationListenerService` chỉ nhận package `com.zing.zalo`.
- Nhiều nguồn Zalo/chuyên gia → nhiều Telegram destination.
- Thêm nguồn Zalo mới trực tiếp từ danh sách notification gần đây.
- Quét group/channel Telegram theo tên, kể cả group private.
- Persistent queue + retry khi mất Internet hoặc Telegram lỗi.
- Dedup notification update để giảm gửi trùng.
- Chuyển text và ảnh notification nếu Zalo cung cấp bitmap.
- Smart Bot Telegram: `/status`, `/health`, `/routes`, `/stats`, `/pause`, `/resume`, `/bridge`, `/whereami`, `/test`.
- Bot Token lưu bằng Android Keystore AES/GCM.
- Không dùng Accessibility Service.
- Không đưa AI vào đường truyền tín hiệu; nội dung Zalo được forward nguyên văn.

## Giao diện V1.3
- Header PipSniper + icon cánh bướm.
- Search chuyên gia / Telegram destination.
- Tabs nhanh: Tất cả / Đang chạy / Chuyên gia / Theo dõi.
- Danh sách chuyên gia theo kiểu chat list với avatar, trạng thái và Telegram đích.
- Dashboard số liệu: Zalo đã thấy, đã forward, queue, lỗi/retry, lần forward gần nhất.
- Thao tác nhanh: Thêm nguồn, Quét Telegram, Kiểm tra kết nối.
- Bottom navigation cho Trang chủ / Kết nối / Telegram / Theo dõi / Smart Bot.

## Thiết lập lần đầu
1. Cài APK và cấp **Notification Access**.
2. Tạo Telegram bot bằng BotFather, thêm bot vào group/channel đích.
3. Trong app mở **Kết nối** → nhập Bot Token → **Lưu & kiểm tra Telegram**.
4. Gửi `/bridge@TenBot` trong group private nếu cần để app nhận diện group.
5. Bấm **Quét Telegram** và chọn group/channel đích.
6. Khi Zalo có notification từ nguồn cần follow, bấm **Thêm nguồn** → chọn nguồn → chọn Telegram đích → bật auto forward.

## Kiến trúc
`Zalo Notification → NotificationParser → RouteStore → DedupStore → QueueStore → TelegramDispatcher → Telegram Bot API`

Smart Bot chạy song song qua `TelegramBotRuntime` và không thay đổi payload của tín hiệu.

## Quyền Android
- `INTERNET`
- `ACCESS_NETWORK_STATE`
- `RECEIVE_BOOT_COMPLETED`
- Notification Listener service với `BIND_NOTIFICATION_LISTENER_SERVICE`

## Giới hạn cần nhớ
Nếu Zalo đang foreground và **không tạo Android notification**, NotificationListener không có event để forward. Đây là giới hạn của kiến trúc notification-only, không phải lỗi routing Telegram.

## Build
CI sử dụng JDK 17, Gradle 8.9, Android SDK 35. Workflow build APK debug, AAB release unsigned, chạy unit tests/static validation, kiểm tra permissions/version/icon và đóng gói toàn bộ source bằng `git archive`.
