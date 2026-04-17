# Hướng dẫn tổng quan - Thiết lập hệ thống CI cho YAS

## Tổng quan đồ án

Đồ án yêu cầu xây dựng hệ thống CI (Continuous Integration) sử dụng Jenkins cho dự án YAS - một ứng dụng microservices. Tài liệu này tổng hợp tất cả các công việc cần thực hiện **thủ công** (ngoài code) để hoàn thiện hệ thống.

---

## Trạng thái hiện tại

### Đã hoàn thành trong code

| STT | Công việc | File |
|-----|-----------|------|
| 1 | Jenkinsfile hoàn chỉnh (5 phase: Detect Changes → Build → Test → Code Quality → Security Scan) | `Jenkinsfile` |
| 2 | Maven wrapper ở root (cho `./mvnw` trong Jenkinsfile) | `mvnw`, `.mvn/wrapper/` |
| 3 | CODEOWNERS (hỗ trợ yêu cầu reviewer) | `.github/CODEOWNERS` |
| 4 | Unit test mới cho Media service (controller + service) | `media/src/test/java/.../controller/MediaControllerTest.java`, `media/src/test/java/.../service/MediaServiceImplTest.java` |
| 5 | Unit test mới cho Tax service (TaxClassService + TaxRateService) | `tax/src/test/java/.../service/TaxClassServiceTest.java`, `tax/src/test/java/.../service/TaxRateServiceTest.java` |
| 6 | Gitleaks config | `gitleaks.toml` (đã có sẵn) |
| 7 | GitHub Actions CI workflows (đã có sẵn từ upstream) | `.github/workflows/` |

### Cần cấu hình thủ công

| STT | Công việc | Mức ưu tiên | Hướng dẫn chi tiết |
|-----|-----------|-------------|---------------------|
| 1 | Cài đặt Jenkins và các plugin cần thiết | **Cao** | [01-cai-dat-jenkins.md](./01-cai-dat-jenkins.md) |
| 2 | Cấu hình Jenkins Tools (JDK 25 + Maven) | **Cao** | [02-cau-hinh-jenkins-tools.md](./02-cau-hinh-jenkins-tools.md) |
| 3 | Tạo Multibranch Pipeline trong Jenkins | **Cao** | [03-tao-multibranch-pipeline.md](./03-tao-multibranch-pipeline.md) |
| 4 | Cấu hình GitHub Branch Protection Rules | **Cao** | [04-github-branch-protection.md](./04-github-branch-protection.md) |
| 5 | Cấu hình SonarQube Server | **Trung bình** | [05-cau-hinh-sonarqube.md](./05-cau-hinh-sonarqube.md) |
| 6 | Cấu hình Jenkins Credentials | **Trung bình** | [06-jenkins-credentials.md](./06-jenkins-credentials.md) |
| 7 | Kiểm tra và chạy thử pipeline | **Cao** | [07-kiem-tra-pipeline.md](./07-kiem-tra-pipeline.md) |

---

## Thứ tự thực hiện (khuyến nghị)

```
1. Cài đặt Jenkins + Plugins      ──→ [01-cai-dat-jenkins.md]
         │
2. Cấu hình JDK 25 + Maven       ──→ [02-cau-hinh-jenkins-tools.md]
         │
3. Cấu hình Credentials           ──→ [06-jenkins-credentials.md]
         │
4. Cài đặt SonarQube              ──→ [05-cau-hinh-sonarqube.md]
         │
5. Tạo Multibranch Pipeline       ──→ [03-tao-multibranch-pipeline.md]
         │
6. Cấu hình GitHub Protection     ──→ [04-github-branch-protection.md]
         │
7. Kiểm tra pipeline              ──→ [07-kiem-tra-pipeline.md]
```

---

## Mapping yêu cầu đồ án → Giải pháp

| Yêu cầu | Giải pháp | Trạng thái |
|----------|-----------|------------|
| 1. Sử dụng Jenkins | Jenkinsfile đã viết, cần cài Jenkins server | ⚠️ Cần setup |
| 2. Fork repo riêng | `luongtrz/yas-group14` | ✅ Hoàn thành |
| 3. Bảo vệ main branch (2 reviewer + CI pass) | CODEOWNERS đã tạo, cần cấu hình Branch Protection | ⚠️ Cần cấu hình |
| 4. Jenkins quét pipeline cho từng branch | Cần tạo Multibranch Pipeline job | ⚠️ Cần cấu hình |
| 5. Pipeline có test + build, upload kết quả | Jenkinsfile có JUnit report + JaCoCo coverage | ✅ Trong code |
| 6. Monorepo: chỉ trigger service thay đổi | Jenkinsfile dùng `git diff` phát hiện thay đổi | ✅ Trong code |
| 7a. Thêm unit test | 44 test cases mới cho media + tax | ✅ Trong code |
| 7b. Coverage > 70% | JaCoCo `minimumLineCoverage: '70'` trong Jenkinsfile | ✅ Trong code |
| 7c. Gitleaks, SonarQube, Snyk/OWASP | Jenkinsfile có Gitleaks + SonarQube + OWASP stages | ✅ Trong code (cần cấu hình server) |

---

## Cấu trúc Jenkinsfile

```
Pipeline
├── Stage 1: Detect Changes        ← Phát hiện service nào thay đổi (monorepo)
├── Stage 2: Build                  ← Compile chỉ service thay đổi
├── Stage 3: Test                   ← Unit test + JaCoCo coverage (>70%)
│   └── Post: JUnit report + JaCoCo report
├── Stage 4: Code Quality (parallel)
│   ├── Checkstyle
│   └── SonarQube Analysis + Quality Gate
└── Stage 5: Security Scan (parallel)
    ├── Gitleaks
    └── OWASP Dependency Check
```

---

## Lưu ý quan trọng

1. **Java 25**: Dự án sử dụng Java 25 (rất mới). Cần đảm bảo Jenkins agent có JDK 25 Temurin.
2. **RAM**: Chạy full docker-compose cần tối thiểu 16GB RAM.
3. **Maven wrapper**: Jenkinsfile dùng `./mvnw` (đã tạo ở root). Lần đầu chạy sẽ tự download Maven.
4. **SonarQube**: Nếu chưa có server, có thể chạy bằng Docker (`docker run -d sonarqube:community`).
5. **Lần chạy đầu**: Pipeline có thể lâu do phải download dependencies. Các lần sau sẽ nhanh hơn nhờ cache.
