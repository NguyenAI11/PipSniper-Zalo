# Chính sách quyền riêng tư — PipSniper Zalo Probe

Cập nhật: 21/08/2026

PipSniper Zalo Probe là ứng dụng thử nghiệm nội bộ dùng để đánh giá metadata notification của Zalo trên thiết bị Android.

## Dữ liệu được xử lý
Khi người dùng chủ động cấp Notification Access, ứng dụng có thể đọc notification do ứng dụng Zalo (`com.zing.zalo`) tạo ra. Probe chỉ xử lý dữ liệu cần cho mục đích chẩn đoán khả năng nhận diện hội thoại/nhóm ổn định.

Các chuỗi có thể chứa nội dung hoặc định danh nhạy cảm được chuyển thành fingerprint bằng hàm băm có salt cục bộ trước khi lưu báo cáo. App có thể lưu các metadata kỹ thuật như thời điểm, notification ID, flags, group metadata, conversation metadata và các fingerprint tương ứng.

## Lưu trữ và truyền dữ liệu
Ứng dụng không yêu cầu `android.permission.INTERNET` và không tự động gửi dữ liệu ra khỏi thiết bị. Dữ liệu được lưu cục bộ trên thiết bị. Người dùng có thể chủ động xuất báo cáo JSON và tự quyết định có chia sẻ báo cáo đó hay không.

## Chia sẻ dữ liệu
Developer không tự động nhận, bán hoặc chia sẻ dữ liệu của người dùng với bên thứ ba thông qua ứng dụng Probe.

## Quyền của người dùng
Người dùng có thể thu hồi Notification Access bất kỳ lúc nào trong Android Settings. Người dùng có thể xóa dữ liệu Probe trong ứng dụng hoặc gỡ cài đặt ứng dụng để xóa dữ liệu cục bộ liên quan.

## Phạm vi
Đây là bản thử nghiệm nội bộ, không phải sản phẩm thương mại hoàn chỉnh. Chính sách này sẽ được cập nhật nếu chức năng thu thập, truyền hoặc xử lý dữ liệu thay đổi.
