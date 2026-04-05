pipeline {
    agent any 

    environment {
        // Ensuring the environment uses the correct Java version for YAS
        JAVA_HOME = '/opt/java/openjdk'
        MAVEN_OPTS = '-Dmaven.test.failure.ignore=false'
    }

    stages {
        stage('Initial Cleanup') {
            steps {
                echo 'Cleaning up workspace...'
                deleteDir()
            }
        }

        stage('Checkout Source') {
            steps {
                echo 'Cloning repository from GitHub...'
                checkout scm
            }
        }

        stage('CI: Media Service') {
            when {
                changeset "services/media-service/**"
            }
            steps {
                echo 'Building and testing Media Service...'
                sh './mvnw clean test -pl services/media-service -am'
            }
        }

        stage('CI: Product Service') {
            when {
                changeset "services/product-service/**"
            }
            steps {
                echo 'Building and testing Product Service...'
                sh './mvnw clean test -pl services/product-service -am'
            }
        }

        stage('Static Analysis (SonarQube)') {
            steps {
                echo 'Running SonarQube quality gate...'
                script {
                    echo "Quality Gate check skipped until SonarQube server is linked."
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline execution finished. Syncing status with GitHub...'
        }
        success {
            echo 'Build successful! Pull Request is now eligible for merge.'
        }
        failure {
            echo 'Build failed. Lượng, please check the console output for errors.'
        }
    }
}