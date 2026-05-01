# 03 - Tạo Multibranch Pipeline trong Jenkins

## Mục tiêu

Tạo Multibranch Pipeline job để Jenkins tự động quét tất cả branch trong repository và chạy pipeline riêng cho mỗi branch khi có thay đổi.

**Đáp ứng yêu cầu 4**: *Configure để Jenkins có thể quét và chạy pipeline cho từng branch.*

---

## Bước 1: Tạo Multibranch Pipeline Job

1. Trên Jenkins Dashboard, nhấn **New Item**
2. Điền tên: `yas-group14` (hoặc tên tùy chọn)
3. Chọn **Multibranch Pipeline**
4. Nhấn **OK**

---

## Bước 2: Cấu hình Branch Sources

Trong trang cấu hình job:

### Section: Branch Sources

1. Nhấn **Add source → GitHub**
2. Điền:
   - **Credentials**: Chọn credential GitHub đã tạo (xem [06-jenkins-credentials.md](./06-jenkins-credentials.md))
   - **Repository HTTPS URL**: `https://github.com/luongtrz/yas-group14.git`
3. Trong **Behaviours**, nhấn **Add** và thêm:
   - **Discover branches**: Strategy = `All branches`
   - **Discover pull requests from origin**: Strategy = `Merging the pull request with the current target branch revision`

---

## Bước 3: Cấu hình Build Configuration

### Section: Build Configuration

1. **Mode**: `by Jenkinsfile`
2. **Script Path**: `Jenkinsfile`

---

## Bước 4: Cấu hình Scan Triggers

### Section: Scan Multibranch Pipeline Triggers

1. Tick **Periodically if not otherwise run**
2. **Interval**: `5 minutes` (quét mỗi 5 phút tìm branch mới)

### Hoặc dùng Webhook (khuyến nghị cho production)

Để Jenkins nhận thông báo ngay khi có push:

1. Trên GitHub repository → **Settings → Webhooks → Add webhook**
2. Điền:
   - **Payload URL**: `http://<jenkins-url>/github-webhook/`
   - **Content type**: `application/json`
   - **Events**: Chọn **Just the push event** và **Pull requests**
3. Nhấn **Add webhook**

> **Lưu ý**: Jenkins server phải có public URL để GitHub gửi webhook. Nếu chạy local, dùng ngrok: `ngrok http 8080`

---

## Bước 5: Cấu hình Orphaned Item Strategy

### Section: Orphaned Item Strategy

1. Tick **Discard old items**
2. **Days to keep old items**: `30`
3. **Max # of old items to keep**: `10`

→ Tự động dọn dẹp branch đã xóa.

---

## Bước 6: Lưu và Scan

1. Nhấn **Save**
2. Jenkins sẽ tự động scan repository lần đầu
3. Mỗi branch có Jenkinsfile sẽ xuất hiện dưới dạng sub-job

---

## Kết quả mong đợi

Sau khi scan xong, giao diện sẽ hiển thị:

```
yas-group14 (Multibranch Pipeline)
├── main                    ← Branch chính
├── test/trigger-media-ci   ← Branch test
├── feat/setup-ci           ← Branch feature
└── ... (các branch khác)
```

Mỗi khi có commit mới trên bất kỳ branch nào, Jenkins sẽ:
1. Phát hiện thay đổi (qua scan hoặc webhook)
2. Chạy Jenkinsfile trên branch đó
3. Jenkinsfile sẽ tự xác định service nào thay đổi (stage "Detect Changes")
4. Chỉ build/test service bị thay đổi

---

## Cách hoạt động với PR

Khi một Pull Request được tạo:
1. Jenkins tự động tạo job cho PR (ví dụ: `PR-3`)
2. Chạy pipeline trên code của PR (merge với target branch)
3. Kết quả (pass/fail) được report lại trên GitHub PR

Kết hợp với **Branch Protection Rules** (yêu cầu 3), PR sẽ không thể merge nếu CI fail.

---

## Troubleshooting

| Lỗi | Nguyên nhân | Cách sửa |
|-----|------------|----------|
| Không tìm thấy branch | Credential không đúng | Kiểm tra GitHub credential |
| Scan xong nhưng không có job | Không tìm thấy Jenkinsfile | Kiểm tra Jenkinsfile nằm ở root của repo |
| Webhook không hoạt động | Jenkins không có public URL | Dùng ngrok hoặc cấu hình DNS |
| PR không được phát hiện | Thiếu behaviour "Discover pull requests" | Thêm behaviour trong Branch Sources |

---

## Tiếp theo

→ [04-github-branch-protection.md](./04-github-branch-protection.md) - Cấu hình GitHub Branch Protection
