# 04 - Cấu hình GitHub Branch Protection Rules

## Mục tiêu

Cấu hình để:
- Không cho phép push trực tiếp vào main branch
- Mỗi PR cần ít nhất 2 reviewer approve
- CI phải pass mới cho phép merge

**Đáp ứng yêu cầu 3**: *Cấu hình github để không cho phép push trực tiếp vào main branch. Mỗi PR cần ít nhất 2 reviewer approve và CI pass mới cho phép merge vào main branch.*

---

## Điều kiện tiên quyết

- Repository phải là **public** hoặc tài khoản có **GitHub Pro/Team/Enterprise**
  - Với repo private miễn phí: Branch protection có giới hạn (không có "Require approvals")
  - **Giải pháp**: Đặt repo thành public cho đồ án

---

## Bước 1: Vào Branch Protection Settings

1. Truy cập: `https://github.com/luongtrz/yas-group14/settings/branches`
2. Hoặc: **Repository → Settings → Branches**
3. Trong section **Branch protection rules**, nhấn **Add branch protection rule**

---

## Bước 2: Cấu hình Rule cho main

### Branch name pattern

```
main
```

### Protect matching branches

Tick các tùy chọn sau:

#### ✅ Require a pull request before merging

- ✅ **Require approvals**: `2`
  - Mỗi PR cần ít nhất 2 người approve
- ✅ **Dismiss stale pull request approvals when new commits are pushed**
  - Khi push commit mới, các approval cũ sẽ bị hủy (buộc review lại)
- ✅ **Require review from Code Owners**
  - Bắt buộc người trong CODEOWNERS phải review (file `.github/CODEOWNERS` đã tạo)

#### ✅ Require status checks to pass before merging

- ✅ **Require branches to be up to date before merging**
- Trong ô **Search for status checks**, tìm và thêm:
  - `Build` (tên job từ Jenkinsfile)
  - Hoặc tên check tương ứng từ Jenkins GitHub plugin

> **Lưu ý**: Status check chỉ xuất hiện sau khi Jenkins đã chạy ít nhất 1 lần trên repository và report status về GitHub.

#### ✅ Require conversation resolution before merging

- Buộc giải quyết tất cả comment trước khi merge

#### ✅ Do not allow bypassing the above settings

- Ngay cả admin cũng không thể bypass rules

---

## Bước 3: Lưu cấu hình

Nhấn **Create** (hoặc **Save changes** nếu đang sửa rule có sẵn).

---

## Bước 4: Thêm thành viên nhóm làm Collaborators

Để có đủ 2 reviewer, cần thêm thành viên nhóm:

1. Vào **Settings → Collaborators and teams**
2. Nhấn **Add people**
3. Nhập GitHub username của từng thành viên
4. Chọn role: **Write** (để có thể review và approve PR)

> Cần ít nhất 3 thành viên (1 tạo PR + 2 reviewer).

---

## Bước 5: Cập nhật CODEOWNERS

File `.github/CODEOWNERS` đã được tạo sẵn. Cần cập nhật với username thực tế của các thành viên:

```
# Thay @luongtrz bằng username thực tế của các thành viên
* @member1 @member2 @member3

/media/**     @member1
/product/**   @member2
/cart/**       @member3
```

---

## Quy trình làm việc sau khi cấu hình

```
1. Developer tạo branch mới từ main
   git checkout -b feat/add-media-tests

2. Commit và push
   git push origin feat/add-media-tests

3. Tạo Pull Request trên GitHub
   → Jenkins tự động chạy CI
   → CI kết quả hiển thị trên PR

4. 2 thành viên khác review + approve

5. CI pass + 2 approvals → Nút "Merge" mở khóa

6. Merge vào main
```

---

## Kiểm tra cấu hình thành công

### Test 1: Push trực tiếp vào main (phải bị chặn)

```bash
git checkout main
echo "test" > test.txt
git add test.txt
git commit -m "test direct push"
git push origin main
```

**Kết quả mong đợi**: Push bị từ chối với lỗi:
```
remote: error: GH006: Protected branch update failed for 'refs/heads/main'.
```

### Test 2: Tạo PR không đủ reviewer (phải bị chặn merge)

1. Tạo PR từ branch bất kỳ
2. Kiểm tra nút **Merge** bị disabled
3. Hiển thị: "2 approving reviews required"

### Test 3: CI fail → không cho merge

1. Tạo PR với code lỗi (ví dụ: compile error)
2. CI chạy và fail
3. Kiểm tra nút **Merge** vẫn bị disabled

---

## Troubleshooting

| Lỗi | Nguyên nhân | Cách sửa |
|-----|------------|----------|
| Không tìm thấy status check | Jenkins chưa report status về GitHub | Chạy pipeline 1 lần trước, hoặc cấu hình GitHub plugin trong Jenkins |
| "Require approvals" không có | Repo private + tài khoản free | Chuyển repo thành public |
| Admin vẫn push được | Chưa tick "Do not allow bypassing" | Tick option đó |
| CODEOWNERS không hoạt động | File sai đường dẫn | Phải nằm ở `.github/CODEOWNERS` |

---

## Tiếp theo

→ [05-cau-hinh-sonarqube.md](./05-cau-hinh-sonarqube.md) - Cấu hình SonarQube
