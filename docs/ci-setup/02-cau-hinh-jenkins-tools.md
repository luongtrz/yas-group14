# 02 - Cấu hình Jenkins Tools (JDK 25 + Maven)

## Mục tiêu

Cấu hình JDK 25 và Maven trong Jenkins để Jenkinsfile có thể sử dụng thông qua directive `tools`.

---

## Tại sao cần cấu hình

Jenkinsfile sử dụng:
```groovy
tools {
    maven 'Maven'
    jdk 'JDK25'
}
```

Jenkins cần biết `Maven` và `JDK25` trỏ đến đâu trên server.

---

## Bước 1: Cấu hình JDK 25

### Cách A: Tự động cài đặt (khuyến nghị)

1. Vào **Manage Jenkins → Tools**
2. Tìm section **JDK installations**
3. Nhấn **Add JDK**
4. Điền:
   - **Name**: `JDK25` (phải khớp với Jenkinsfile)
   - **JAVA_HOME**: Để trống nếu dùng auto-install
5. Tick **Install automatically**
6. Nhấn **Add Installer → Extract *.zip/*.tar.gz**
7. Điền:
   - **Download URL**: `https://api.adoptium.net/v3/binary/latest/25/ga/linux/x64/jdk/hotspot/normal/eclipse`
   - **Subdirectory of extracted archive**: `jdk-25+36` (kiểm tra version cụ thể trên https://adoptium.net)
8. Nhấn **Save**

### Cách B: Cài sẵn trên server

Nếu đã cài JDK 25 trên Jenkins server:

```bash
# Cài JDK 25 Temurin
sudo apt install temurin-25-jdk
# Hoặc dùng SDKMAN
sdk install java 25-tem
```

Sau đó trong Jenkins:
1. **Name**: `JDK25`
2. **JAVA_HOME**: `/usr/lib/jvm/temurin-25-jdk-amd64` (đường dẫn thực tế)
3. **Bỏ tick** Install automatically

---

## Bước 2: Cấu hình Maven

### Cách A: Tự động cài đặt (khuyến nghị)

1. Vẫn ở **Manage Jenkins → Tools**
2. Tìm section **Maven installations**
3. Nhấn **Add Maven**
4. Điền:
   - **Name**: `Maven` (phải khớp với Jenkinsfile)
5. Tick **Install automatically**
6. Chọn version: **3.9.9** (hoặc mới nhất)
7. Nhấn **Save**

### Cách B: Cài sẵn trên server

```bash
sudo apt install maven
# Hoặc
sdk install maven
```

Trong Jenkins:
1. **Name**: `Maven`
2. **MAVEN_HOME**: `/usr/share/maven` (đường dẫn thực tế)
3. **Bỏ tick** Install automatically

---

## Lưu ý: Dùng Maven Wrapper thay vì cài Maven

Jenkinsfile hiện tại dùng `./mvnw` (Maven Wrapper) nên **không thực sự cần** cấu hình Maven tool trong Jenkins. Maven Wrapper sẽ tự download đúng version.

Tuy nhiên, nếu muốn dùng `mvn` trực tiếp (thay vì `./mvnw`), cần cấu hình như trên.

**Nếu chỉ dùng `./mvnw`**: Có thể bỏ block `tools { maven 'Maven' }` trong Jenkinsfile và chỉ giữ `jdk 'JDK25'`.

---

## Kiểm tra

1. Tạo một Pipeline job test đơn giản:

```groovy
pipeline {
    agent any
    tools {
        jdk 'JDK25'
        maven 'Maven'
    }
    stages {
        stage('Verify') {
            steps {
                sh 'java -version'
                sh 'mvn -version'
            }
        }
    }
}
```

2. Chạy job → kiểm tra output hiển thị:
   - `openjdk version "25"` (hoặc tương tự)
   - `Apache Maven 3.9.x`

---

## Troubleshooting

| Lỗi | Nguyên nhân | Cách sửa |
|-----|------------|----------|
| `Tool type "jdk" does not have an install of name "JDK25"` | Tên JDK trong Jenkins không khớp | Kiểm tra tên trong Tools phải là `JDK25` |
| `JAVA_HOME is not defined correctly` | JDK chưa cài hoặc đường dẫn sai | Kiểm tra JAVA_HOME path |
| `mvn: command not found` | Maven chưa cài | Dùng `./mvnw` hoặc cấu hình Maven tool |

---

## Tiếp theo

→ [06-jenkins-credentials.md](./06-jenkins-credentials.md) - Cấu hình Credentials
