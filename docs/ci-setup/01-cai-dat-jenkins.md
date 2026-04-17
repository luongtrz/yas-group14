# 01 - Cài đặt Jenkins và các Plugin cần thiết

## Mục tiêu

Cài đặt Jenkins server và tất cả plugin cần thiết để chạy được Jenkinsfile của dự án YAS.

---

## Cách 1: Chạy Jenkins bằng Docker (khuyến nghị)

### Bước 1: Tạo Docker Compose cho Jenkins

Tạo file `docker-compose.jenkins.yml` (hoặc chạy trực tiếp):

```bash
docker run -d \
  --name jenkins \
  -p 8080:8080 \
  -p 50000:50000 \
  -v jenkins_home:/var/jenkins_home \
  -v /var/run/docker.sock:/var/run/docker.sock \
  jenkins/jenkins:lts-jdk21
```

> **Lưu ý**: Mount docker.sock để Jenkins có thể chạy Docker commands (Gitleaks, OWASP).

### Bước 2: Lấy mật khẩu admin ban đầu

```bash
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

### Bước 3: Truy cập Jenkins

Mở trình duyệt: `http://localhost:8080`

Nhập mật khẩu admin → Chọn **Install suggested plugins** → Tạo tài khoản admin.

---

## Cách 2: Cài đặt trực tiếp trên máy

### Ubuntu/Debian

```bash
# Thêm Jenkins repository
curl -fsSL https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key | sudo tee /usr/share/keyrings/jenkins-keyring.asc > /dev/null
echo deb [signed-by=/usr/share/keyrings/jenkins-keyring.asc] https://pkg.jenkins.io/debian-stable binary/ | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null

# Cài đặt
sudo apt update
sudo apt install jenkins

# Khởi động
sudo systemctl start jenkins
sudo systemctl enable jenkins
```

---

## Cài đặt Plugin bắt buộc

Sau khi Jenkins chạy, vào **Manage Jenkins → Plugins → Available plugins**, tìm và cài đặt:

### Plugin bắt buộc (pipeline sẽ lỗi nếu thiếu)

| Plugin | Mục đích | Dùng ở stage |
|--------|----------|-------------|
| **JaCoCo** | Hiển thị code coverage report | Test |
| **JUnit** | Hiển thị test results | Test |
| **Warnings Next Generation** | Hiển thị Checkstyle results (`recordIssues`) | Code Quality |
| **SonarQube Scanner** | Kết nối với SonarQube server | Code Quality |
| **OWASP Dependency-Check** | Publish dependency check results | Security Scan |
| **Docker Pipeline** | Chạy Docker trong pipeline | Security Scan (Gitleaks) |
| **Pipeline** | Hỗ trợ Jenkinsfile | Toàn bộ |
| **Git** | Clone repository | Toàn bộ |
| **Multibranch Pipeline** | Scan branches tự động | Toàn bộ (yêu cầu 4) |

### Plugin khuyến nghị (không bắt buộc nhưng hữu ích)

| Plugin | Mục đích |
|--------|----------|
| **GitHub Branch Source** | Tích hợp sâu với GitHub cho Multibranch |
| **Blue Ocean** | Giao diện hiện đại cho pipeline |
| **Pipeline Stage View** | Hiển thị visual các stage |
| **Timestamper** | Thêm timestamp vào console output |
| **Build Discarder** | Tự động xóa build cũ |

### Cách cài nhiều plugin cùng lúc

Vào **Manage Jenkins → Script Console**, chạy:

```groovy
def plugins = [
  'jacoco', 'junit', 'warnings-ng', 'sonar',
  'dependency-check-jenkins-plugin', 'docker-workflow',
  'workflow-aggregator', 'git', 'workflow-multibranch',
  'github-branch-source', 'blueocean', 'pipeline-stage-view',
  'timestamper', 'build-discarder'
]

def pm = jenkins.model.Jenkins.instance.pluginManager
def uc = jenkins.model.Jenkins.instance.updateCenter

plugins.each { pluginName ->
  if (!pm.getPlugin(pluginName)) {
    def plugin = uc.getPlugin(pluginName)
    if (plugin) {
      plugin.deploy()
      println "Installing: ${pluginName}"
    } else {
      println "Not found: ${pluginName}"
    }
  } else {
    println "Already installed: ${pluginName}"
  }
}
```

Sau đó **restart Jenkins**:

```bash
# Docker
docker restart jenkins

# Hoặc trên máy
sudo systemctl restart jenkins
```

---

## Kiểm tra cài đặt thành công

1. Vào **Manage Jenkins → Plugins → Installed plugins**
2. Kiểm tra tất cả plugin trong bảng trên đã có
3. Vào **Manage Jenkins → System** → cuộn xuống kiểm tra section **SonarQube servers** đã xuất hiện
4. Vào **Manage Jenkins → Tools** → kiểm tra section **JDK** và **Maven** đã xuất hiện

---

## Tiếp theo

→ [02-cau-hinh-jenkins-tools.md](./02-cau-hinh-jenkins-tools.md) - Cấu hình JDK 25 + Maven
