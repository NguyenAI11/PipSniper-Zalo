# PipSniper Zalo Probe V0.2 — Google Play Internal Testing

## Mục tiêu
Phân phối bản Probe qua Google Play Internal Testing để kiểm tra metadata notification của Zalo trên thiết bị Android mà không cần sideload APK.

## Thông tin app
- App name: PipSniper Zalo Probe
- Application ID / package: `com.pipsniper.zaloprobe`
- Version name: `0.2`
- Version code: `2`
- Min SDK: 26
- Target SDK: 35
- Track: Internal testing

## Cấu hình Play Console lần đầu
1. Create app → App name: `PipSniper Zalo Probe` → App → Free.
2. Enroll Play App Signing với app-signing key do Google quản lý.
3. Testing → Internal testing → Create new release.
4. Upload file `PipSniper_Zalo_Probe_V0.2-play-signed.aab`.
5. Release name: `0.2-probe-internal`.
6. Release notes: dùng file `release-notes-vi-VN.txt`.
7. Testers: tạo email list và thêm tài khoản Google dùng trên điện thoại test.
8. Review release → Start rollout to Internal testing.
9. Mở opt-in link bằng đúng tài khoản Google tester và cài từ Play Store.

## Data safety / privacy
Probe không có `android.permission.INTERNET`. Dữ liệu notification chỉ được xử lý và lưu cục bộ. Nội dung/định danh nhạy cảm được băm bằng salt cục bộ trước khi lưu. App không tự động truyền dữ liệu cho developer hoặc bên thứ ba. Người dùng chỉ xuất JSON thủ công khi muốn gửi báo cáo.

## Lưu ý upload key
File AAB upload lên Play phải được ký bằng upload key riêng, không phải debug key. Giữ an toàn file `.jks`, alias và password để ký các bản update tiếp theo. Play App Signing giữ app-signing key phía Google; upload key có thể reset trong Play Console nếu cần.
