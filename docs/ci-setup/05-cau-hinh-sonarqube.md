# 05 - Cấu hình SonarQube Server

## Mục tiêu

Cài đặt SonarQube server và kết nối với Jenkins để phân tích chất lượng code.

**Đáp ứng yêu cầu 7c**: *Sử dụng SonarQube để scan chất lượng code.*

---

## Phần 1: Cài đặt SonarQube Server

### Chạy SonarQube bằng Docker (khuyến nghị)

```bash
docker run -d \
  --name sonarqube \
  -p 9000:9000 \
  -v sonarqube_data:/opt/sonarqube/data \
  -v sonarqube_logs:/opt/sonarqube/logs \
  -v sonarqube_extensions:/opt/sonarqube/extensions \
  sonarqube:community
```

### Truy cập SonarQube

1. Mở trình duyệt: `http://localhost:9000`
2. Đăng nhập mặc định: `admin` / `admin`
3. Đổi mật khẩu khi được yêu cầu

---

## Phần 2: Tạo Project và Token trên SonarQube

### Bước 1: Tạo Project

1. Vào **Projects → Create Project → Manually**
2. Điền:
   - **Project display name**: `yas-group14`
   - **Project key**: `yas-group14`
   - **Main branch name**: `main`
3. Nhấn **Set Up**

### Bước 2: Tạo Token

1. Vào **My Account → Security → Generate Tokens**
2. Hoặc khi tạo project, chọn **With Jenkins** → chọn **GitHub** → chọn **Maven**
3. Điền:
   - **Name**: `jenkins-yas`
   - **Type**: `Project Analysis Token`
   - **Project**: `yas-group14`
4. Nhấn **Generate**
5. **Sao chép token** (chỉ hiển thị 1 lần!): `sqp_xxxxxxxxxxxx`

---

## Phần 3: Cấu hình SonarQube trong Jenkins

### Bước 1: Thêm SonarQube Token vào Jenkins Credentials

1. Vào **Manage Jenkins → Credentials → System → Global credentials**
2. Nhấn **Add Credentials**
3. Điền:
   - **Kind**: `Secret text`
   - **Secret**: `sqp_xxxxxxxxxxxx` (token từ SonarQube)
   - **ID**: `sonarqube-token`
   - **Description**: `SonarQube Analysis Token`
4. Nhấn **Create**

### Bước 2: Cấu hình SonarQube Server

1. Vào **Manage Jenkins → System**
2. Cuộn xuống section **SonarQube servers**
3. Tick **Environment variables**
4. Nhấn **Add SonarQube**
5. Điền:
   - **Name**: `SonarQube` (phải khớp với `SONARQUBE_ENV` trong Jenkinsfile)
   - **Server URL**: `http://localhost:9000` (hoặc URL SonarQube server)
   - **Server authentication token**: Chọn credential `sonarqube-token` đã tạo
6. Nhấn **Save**

---

## Phần 4: Cấu hình Quality Gate

### Tạo Quality Gate tùy chỉnh (tùy chọn)

1. Trên SonarQube, vào **Quality Gates**
2. Nhấn **Create**
3. Đặt tên: `YAS Gate`
4. Thêm conditions:
   - **Coverage** on **New Code** is less than `70%` → Fail
   - **Duplicated Lines (%)** on **New Code** is greater than `10%` → Fail
   - **Maintainability Rating** on **New Code** is worse than `A` → Fail
   - **Security Rating** on **New Code** is worse than `A` → Fail
5. Gán cho project `yas-group14`: **Project Settings → Quality Gate → chọn "YAS Gate"**

---

## Phần 5: Cấu hình Webhook (Quality Gate callback)

Để Jenkins nhận kết quả Quality Gate từ SonarQube:

1. Trên SonarQube, vào **Administration → Configuration → Webhooks**
2. Nhấn **Create**
3. Điền:
   - **Name**: `Jenkins`
   - **URL**: `http://<jenkins-url>/sonarqube-webhook/`
4. Nhấn **Create**

> Đây là bước bắt buộc để `waitForQualityGate()` trong Jenkinsfile hoạt động.

---

## Phần 6: Kiểm tra

### Chạy thử analysis

Trên máy local (nếu có Maven):

```bash
mvn sonar:sonar \
  -Dsonar.projectKey=yas-group14 \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=sqp_xxxxxxxxxxxx \
  -pl media
```

### Kết quả mong đợi

1. Trên SonarQube dashboard hiện project `yas-group14`
2. Hiển thị: bugs, vulnerabilities, code smells, coverage, duplications
3. Quality Gate hiển thị Passed/Failed

---

## Lưu ý cho SonarCloud (thay thế SonarQube tự host)

Nếu muốn dùng SonarCloud (miễn phí cho open source) thay vì tự host SonarQube:

1. Đăng ký tại https://sonarcloud.io với GitHub account
2. Import repository `luongtrz/yas-group14`
3. Lấy token từ **My Account → Security**
4. Trong Jenkins, đổi Server URL thành `https://sonarcloud.io`
5. Thêm properties vào `pom.xml`:
   ```xml
   <sonar.organization>luongtrz</sonar.organization>
   <sonar.host.url>https://sonarcloud.io</sonar.host.url>
   ```

---

## Troubleshooting

| Lỗi | Nguyên nhân | Cách sửa |
|-----|------------|----------|
| `Not authorized` | Token sai hoặc hết hạn | Tạo token mới |
| `waitForQualityGate` timeout | Thiếu webhook từ SonarQube → Jenkins | Tạo webhook trên SonarQube |
| SonarQube không khởi động | Thiếu RAM (cần ít nhất 2GB) | Tăng RAM cho container |
| `SONARQUBE_ENV` not found | Tên server trong Jenkins không khớp | Đổi tên thành `SonarQube` |

---

## Tiếp theo

→ [06-jenkins-credentials.md](./06-jenkins-credentials.md) - Cấu hình Jenkins Credentials
