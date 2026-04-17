# 06 - Cấu hình Jenkins Credentials

## Mục tiêu

Tạo tất cả credentials cần thiết trong Jenkins để pipeline có thể:
- Clone repository từ GitHub
- Report status về GitHub PR
- Kết nối SonarQube

---

## Truy cập Credentials

**Manage Jenkins → Credentials → System → Global credentials (unrestricted)**

Hoặc URL: `http://<jenkins-url>/manage/credentials/store/system/domain/_/`

---

## Credential 1: GitHub Personal Access Token

### Mục đích
- Clone private repository
- Report CI status trên GitHub PR
- Scan branches trong Multibranch Pipeline

### Tạo Token trên GitHub

1. Vào **GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)**
2. Nhấn **Generate new token (classic)**
3. Điền:
   - **Note**: `Jenkins CI - yas-group14`
   - **Expiration**: Chọn thời gian phù hợp (ví dụ: 90 days)
4. Chọn scopes:
   - ✅ `repo` (Full control of private repositories)
   - ✅ `admin:repo_hook` (Full control of repository hooks)
   - ✅ `admin:org_hook` (nếu dùng organization)
5. Nhấn **Generate token**
6. **Sao chép token** (chỉ hiển thị 1 lần!)

### Thêm vào Jenkins

1. Nhấn **Add Credentials**
2. Điền:
   - **Kind**: `Username with password`
   - **Scope**: `Global`
   - **Username**: `luongtrz` (GitHub username)
   - **Password**: Token vừa tạo
   - **ID**: `github-credentials`
   - **Description**: `GitHub PAT for yas-group14`
3. Nhấn **Create**

---

## Credential 2: SonarQube Token

### Mục đích
- Gửi kết quả phân tích code lên SonarQube server

### Tạo Token

Xem chi tiết tại [05-cau-hinh-sonarqube.md](./05-cau-hinh-sonarqube.md) - Phần 2.

### Thêm vào Jenkins

1. Nhấn **Add Credentials**
2. Điền:
   - **Kind**: `Secret text`
   - **Scope**: `Global`
   - **Secret**: `sqp_xxxxxxxxxxxx` (SonarQube token)
   - **ID**: `sonarqube-token`
   - **Description**: `SonarQube Analysis Token`
3. Nhấn **Create**

---

## Credential 3: GitHub App (thay thế cho PAT - nâng cao)

Nếu muốn bảo mật hơn, dùng GitHub App thay vì Personal Access Token:

### Tạo GitHub App

1. Vào **GitHub → Settings → Developer settings → GitHub Apps → New GitHub App**
2. Điền:
   - **GitHub App name**: `Jenkins CI YAS`
   - **Homepage URL**: `http://<jenkins-url>`
   - **Webhook URL**: `http://<jenkins-url>/github-webhook/`
3. Permissions:
   - **Repository permissions**:
     - Contents: `Read`
     - Metadata: `Read`
     - Pull requests: `Read & Write`
     - Commit statuses: `Read & Write`
   - **Subscribe to events**: `Pull request`, `Push`
4. Nhấn **Create GitHub App**
5. Ghi lại **App ID**
6. **Generate a private key** → Download file `.pem`

### Thêm vào Jenkins

1. **Kind**: `GitHub App`
2. **App ID**: (số vừa ghi)
3. **Key**: Upload file `.pem`
4. **ID**: `github-app`

---

## Tổng hợp Credentials cần tạo

| ID | Kind | Mục đích | Bắt buộc |
|----|------|----------|----------|
| `github-credentials` | Username with password | Clone repo + report status | ✅ Có |
| `sonarqube-token` | Secret text | SonarQube analysis | ✅ Có (nếu dùng SonarQube) |

---

## Kiểm tra Credentials

1. Vào **Manage Jenkins → Credentials**
2. Kiểm tra tất cả credentials đã tạo
3. Nhấn vào từng credential → **Update** → kiểm tra thông tin đúng
4. Test bằng cách tạo Multibranch Pipeline scan (xem [03-tao-multibranch-pipeline.md](./03-tao-multibranch-pipeline.md))

---

## Bảo mật

- **Không bao giờ** đặt token trực tiếp trong Jenkinsfile
- Sử dụng `credentials()` function trong pipeline nếu cần
- Giới hạn scope của token (chỉ cấp quyền cần thiết)
- Đặt expiration cho token
- Rotate token định kỳ

---

## Tiếp theo

→ [07-kiem-tra-pipeline.md](./07-kiem-tra-pipeline.md) - Kiểm tra và chạy thử pipeline
