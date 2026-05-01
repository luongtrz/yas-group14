# 07 - Kiểm tra và chạy thử Pipeline

## Mục tiêu

Xác nhận toàn bộ hệ thống CI hoạt động đúng theo yêu cầu đồ án.

---

## Checklist trước khi chạy

Đảm bảo đã hoàn thành:

- [ ] Jenkins server đang chạy
- [ ] Plugin đã cài: JaCoCo, JUnit, Warnings-NG, SonarQube Scanner, OWASP, Docker Pipeline
- [ ] JDK 25 đã cấu hình (tên: `JDK25`)
- [ ] Maven đã cấu hình (tên: `Maven`)
- [ ] GitHub credential đã tạo (ID: `github-credentials`)
- [ ] SonarQube server đã cấu hình (tên: `SonarQube`)
- [ ] SonarQube webhook trỏ về Jenkins
- [ ] Multibranch Pipeline đã tạo và scan thành công
- [ ] GitHub Branch Protection đã bật

---

## Test 1: Pipeline chạy khi có thay đổi ở 1 service

### Bước thực hiện

```bash
# Tạo branch mới
git checkout -b test/media-ci

# Thay đổi nhỏ trong media service
echo "// test trigger CI" >> media/src/main/java/com/yas/media/MediaApplication.java

# Commit và push
git add media/
git commit -m "test: trigger CI for media-service"
git push origin test/media-ci
```

### Kết quả mong đợi

1. Jenkins phát hiện branch mới (qua scan hoặc webhook)
2. Pipeline chạy với output:
   ```
   Changed services: media
   ```
3. Chỉ build và test media service (không phải toàn bộ)
4. **Yêu cầu 6**: ✅ Chỉ trigger service bị thay đổi

---

## Test 2: JUnit test results hiển thị

### Kết quả mong đợi

1. Sau khi pipeline chạy xong, vào job → **Test Results**
2. Hiển thị:
   - Số test passed / failed / skipped
   - Chi tiết từng test class và method
   - Biểu đồ trend qua các lần build

### Nếu không hiển thị

- Kiểm tra plugin JUnit đã cài
- Kiểm tra path pattern: `**/target/surefire-reports/TEST-*.xml`

---

## Test 3: JaCoCo coverage report

### Kết quả mong đợi

1. Vào job → **Code Coverage**
2. Hiển thị:
   - Line coverage percentage
   - Branch coverage percentage
   - Chi tiết coverage theo package/class
   - Biểu đồ trend

3. Nếu coverage < 70%: build UNSTABLE (vàng) hoặc FAILURE (đỏ)

### Kiểm tra threshold

```
minimumLineCoverage: '70'    → Build fail nếu < 70%
minimumBranchCoverage: '50'  → Build fail nếu < 50%
```

**Yêu cầu 7b**: ✅ Coverage > 70% mới pass

---

## Test 4: SonarQube analysis

### Kết quả mong đợi

1. Trên SonarQube dashboard → hiển thị project với kết quả mới
2. Quality Gate: Passed hoặc Failed
3. Trong Jenkins: stage "Quality Gate" hiện ✅ (nếu pass) hoặc ❌ (nếu fail)

### Nếu Quality Gate fail → pipeline fail

**Yêu cầu 7c**: ✅ SonarQube scan chất lượng code

---

## Test 5: Gitleaks security scan

### Kết quả mong đợi

1. Stage "Gitleaks" chạy thành công
2. Artifact `gitleaks-report.json` được lưu
3. Nếu phát hiện secret → report có nội dung

**Yêu cầu 7c**: ✅ Gitleaks scan lỗ hổng bảo mật

---

## Test 6: OWASP Dependency Check

### Kết quả mong đợi

1. Stage "OWASP Dependency Check" chạy thành công
2. Report hiển thị trong Jenkins → **Dependency-Check** tab
3. Nếu có CVE nghiêm trọng (CVSS >= 9) → build fail

**Yêu cầu 7c**: ✅ Scan lỗ hổng dependencies

---

## Test 7: Branch protection + PR workflow

### Bước thực hiện

1. Tạo Pull Request từ branch test → main
2. Kiểm tra trên GitHub PR:
   - CI check đang chạy hoặc đã hoàn thành
   - Nút Merge bị disabled (chưa đủ 2 approval)
3. Nhờ 2 thành viên khác approve
4. Sau 2 approval + CI pass → nút Merge mở khóa

### Kết quả mong đợi

- ❌ Push trực tiếp vào main → Bị chặn
- ❌ Merge PR không đủ approval → Bị chặn
- ❌ Merge PR khi CI fail → Bị chặn
- ✅ Merge PR khi 2 approval + CI pass → Thành công

**Yêu cầu 3**: ✅ Bảo vệ main branch

---

## Test 8: Common-library thay đổi → build lại tất cả

### Bước thực hiện

```bash
git checkout -b test/common-change
echo "// test" >> common-library/src/main/java/com/yas/commonlibrary/utils/MessagesUtils.java
git add common-library/
git commit -m "test: trigger full rebuild"
git push origin test/common-change
```

### Kết quả mong đợi

Pipeline output:
```
Changed services: media, product, cart, order, customer, inventory, location, payment, payment-paypal, promotion, rating, search, tax, webhook, recommendation
```

Tất cả service đều được build và test lại.

---

## Bảng tổng hợp kiểm tra theo yêu cầu đồ án

| Yêu cầu | Test | Kết quả |
|----------|------|---------|
| 1. Dùng Jenkins | Pipeline chạy trên Jenkins | ☐ |
| 2. Fork repo riêng | Đã fork về luongtrz/yas-group14 | ☐ |
| 3. Bảo vệ main (2 reviewer + CI pass) | Test 7: PR workflow | ☐ |
| 4. Jenkins quét pipeline cho từng branch | Multibranch scan tự động | ☐ |
| 5. Pipeline có test + build + results | Test 2 (JUnit) + Test 3 (JaCoCo) | ☐ |
| 6. Monorepo: chỉ trigger service thay đổi | Test 1: chỉ build media | ☐ |
| 7a. Thêm unit test | 44 test cases mới đã viết | ☐ |
| 7b. Coverage > 70% | Test 3: JaCoCo threshold | ☐ |
| 7c. Gitleaks + SonarQube + OWASP | Test 4 + 5 + 6 | ☐ |

---

## Xử lý lỗi thường gặp

| Lỗi | Nguyên nhân | Cách sửa |
|-----|------------|----------|
| `./mvnw: Permission denied` | File chưa có quyền execute | `chmod +x mvnw` |
| `git diff` trả về rỗng | Chưa có commit trước đó | Thêm logic fallback trong Jenkinsfile |
| JaCoCo report trống | Chưa chạy `verify` phase | Đổi `mvn test` thành `mvn verify` |
| Docker command not found | Jenkins agent không có Docker | Mount docker.sock hoặc cài Docker |
| SonarQube timeout | Server chưa khởi động xong | Đợi SonarQube ready trước khi chạy |
| Out of memory | Build quá nhiều service cùng lúc | Tăng RAM cho Jenkins agent |

---

## Hoàn thành

Khi tất cả ô trong bảng tổng hợp đều ☑, hệ thống CI đã hoàn thiện theo yêu cầu đồ án.
