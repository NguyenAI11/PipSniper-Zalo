# PipSniper Zalo Probe Android V0.2

## Mục tiêu
Đây là **app Probe**, chưa phải PipSniper Mobile. Nó chỉ kiểm tra xem notification Zalo trên thiết bị thật có metadata/fingerprint đủ ổn định để phân biệt các cuộc trò chuyện hoặc chuyên gia hay không.

## Cách dùng trên điện thoại
1. Cài APK.
2. Mở **PipSniper Zalo Probe**.
3. Bấm **Cấp / kiểm tra quyền** và bật Notification Access cho app.
4. Quay lại app. Trạng thái phải thành `Đã cấp quyền`.
5. Để app thu thập trong vài giờ hoặc 1 ngày trong khi Zalo hoạt động bình thường. Nên có tin từ nhiều group.
6. Mở Probe và bấm **Xuất báo cáo**.
7. Chọn nơi lưu file `.json`, rồi gửi file đó cho ChatGPT để phân tích.

## Quyền riêng tư
- Manifest **không khai báo `android.permission.INTERNET`**.
- Listener lọc cứng package `com.zing.zalo` trước khi ghi.
- Không đọc database, cookie, token hoặc file nội bộ của Zalo.
- Không Accessibility, không click/mở Zalo tự động.
- Text, title, sender, tag/key/groupKey/shortcutId/locusId và scalar extras được hash bằng SHA-256 với **salt cục bộ ngẫu nhiên không xuất ra report**.
- Hash vẫn ổn định trên cùng một cài đặt để so sánh A1/A2/B1/B2, nhưng report không chứa plaintext message.
- Dữ liệu chỉ nằm trong app-private storage cho đến khi chính người dùng bấm **Xuất báo cáo**.

## Thu thập gì
- StatusBarNotification: id, tag(hash), key(hash), groupKey(hash), postTime.
- Notification: group/sortKey/channelId/shortcutId/locusId (hash), flags, category, visibility, group-summary.
- Standard extras: title/text/subText/info/summary/conversationTitle (hash + length).
- Toàn bộ `extras` keys, kiểu dữ liệu, scalar hashes và nested Bundle ở mức giới hạn.
- MessagingStyle nếu Android/Zalo cung cấp: group-conversation flag, conversation title, message tail, sender hashes.
- POSTED và REMOVED callbacks.

## Giới hạn
- 10,000 sự kiện cho một vòng thu thập để tránh file tăng không giới hạn.
- Nếu Zalo không tạo notification cho group bị mute hoặc Android chặn notification, Probe không thể nhìn thấy message đó.
- Probe không suy đoán group bằng AI. Nó chỉ cung cấp bằng chứng metadata để quyết định kiến trúc.
