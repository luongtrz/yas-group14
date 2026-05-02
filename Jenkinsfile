pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk 'JDK25'
    }

    environment {
        MAVEN_OPTS = '-Dmaven.test.failure.ignore=false'
        SONARQUBE_ENV = 'SonarQube' // SonarQube server name configured in Jenkins
    }

    options {
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        // ============================================================
        // PHASE 1: DETECT CHANGES (monorepo - only build what changed)
        // ============================================================
        stage('Detect Changes') {
            steps {
                script {
                    // Services that have unit tests and can be built
                    def allServices = [
                        'media', 'product', 'cart', 'order', 'customer',
                        'inventory', 'location', 'payment', 'payment-paypal',
                        'promotion', 'rating', 'search', 'tax', 'webhook',
                        'recommendation'
                    ]

                    // Detect which services have changed files
                    changedServices = []
                    commonChanged = false

                    if (env.CHANGE_TARGET) {
                        // PR build: compare against target branch
                        sh "git fetch origin +refs/heads/${env.CHANGE_TARGET}:refs/remotes/origin/${env.CHANGE_TARGET} --no-tags"
                        def changes = sh(
                            script: "git diff --name-only origin/${env.CHANGE_TARGET}...HEAD",
                            returnStdout: true
                        ).trim()

                        if (changes.contains('common-library/') || changes.contains('pom.xml')) {
                            commonChanged = true
                        }

                        for (svc in allServices) {
                            if (changes.contains("${svc}/") || commonChanged) {
                                changedServices.add(svc)
                            }
                        }
                    } else {
                        // Branch build: compare against previous commit
                        def changes = sh(
                            script: "git diff --name-only HEAD~1 HEAD || echo ''",
                            returnStdout: true
                        ).trim()

                        if (changes.contains('common-library/') || changes.contains('pom.xml')) {
                            commonChanged = true
                        }

                        for (svc in allServices) {
                            if (changes.contains("${svc}/") || commonChanged) {
                                changedServices.add(svc)
                            }
                        }
                    }

                    if (changedServices.isEmpty()) {
                        echo "No service changes detected. Skipping build."
                    } else {
                        echo "Changed services: ${changedServices.join(', ')}"
                    }
                }
            }
        }

        // ============================================================
        // PHASE 2: BUILD - Compile and install all changed services
        // ============================================================
        stage('Build') {
            when {
                expression { return !changedServices.isEmpty() }
            }
            steps {
                script {
                    def modules = changedServices.join(',')
                    // Use install (not compile) so common-library JAR goes to ~/.m2
                    // and later stages (sonar, checkstyle) can resolve dependencies
                    sh "./mvnw clean install -pl ${modules} -am -DskipTests"
                }
            }
        }

        // ============================================================
        // PHASE 3: TEST - Run unit tests + generate coverage
        // ============================================================
        stage('Test') {
            when {
                expression { return !changedServices.isEmpty() }
            }
            steps {
                script {
                    def modules = changedServices.join(',')
                    // Run tests + JaCoCo coverage report (verify phase triggers jacoco:report)
                    // -DskipITs: skip Failsafe integration tests (require Docker/Testcontainers)
                    sh "./mvnw verify -pl ${modules} -am -DskipITs"
                }
            }
            post {
                always {
                    // Upload JUnit test results for each changed service
                    junit(
                        testResults: '**/target/surefire-reports/TEST-*.xml, **/target/failsafe-reports/TEST-*.xml',
                        allowEmptyResults: true
                    )

                    // Publish JaCoCo coverage — measure only changed service modules,
                    // not their dependencies (e.g. common-library), for accurate threshold check
                    jacoco(
                        execPattern: changedServices.collect{ "${it}/target/jacoco.exec" }.join(','),
                        classPattern: changedServices.collect{ "${it}/target/classes" }.join(','),
                        sourcePattern: changedServices.collect{ "${it}/src/main/java" }.join(','),
                        exclusionPattern: '**/config/**,**/exception/**,**/constants/**,**/*Application.*',
                        minimumLineCoverage: '70',
                        minimumBranchCoverage: '50',
                        changeBuildStatus: true
                    )
                }
            }
        }

        // ============================================================
        // PHASE 4: CODE QUALITY - Checkstyle + SonarQube
        // ============================================================
        stage('Code Quality') {
            when {
                expression { return !changedServices.isEmpty() }
            }
            parallel {
                stage('Checkstyle') {
                    steps {
                        script {
                            def modules = changedServices.join(',')
                            sh "./mvnw checkstyle:checkstyle -pl ${modules} -am"
                        }
                    }
                    // recordIssues(checkStyle) omitted: warnings-ng plugin does not
                    // register the checkStyle symbol in this Jenkins version
                }

                stage('SonarQube Analysis') {
                    steps {
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                            script {
                                def modules = changedServices.join(',')
                                withSonarQubeEnv("${SONARQUBE_ENV}") {
                                    sh "./mvnw org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -pl ${modules} -am -Dsonar.organization=devop14s -Dsonar.projectKey=devop14s_yas"
                                }
                            }
                        }
                    }
                }
            }
        }

        // SonarQube Quality Gate - wait for result (non-blocking)
        stage('Quality Gate') {
            when {
                expression { return !changedServices.isEmpty() }
            }
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    timeout(time: 5, unit: 'MINUTES') {
                        waitForQualityGate abortPipeline: false
                    }
                }
            }
        }

        // ============================================================
        // PHASE 5: SECURITY SCAN - Gitleaks + Snyk
        // ============================================================
        stage('Security Scan') {
            when {
                expression { return !changedServices.isEmpty() }
            }
            parallel {
                stage('Gitleaks') {
                    steps {
                        sh '''
                            echo "Downloading Gitleaks binary..."
                            curl -sSfL https://github.com/gitleaks/gitleaks/releases/download/v8.18.4/gitleaks_8.18.4_linux_x64.tar.gz \
                                -o /tmp/gitleaks.tar.gz
                            tar -xzf /tmp/gitleaks.tar.gz -C /tmp gitleaks
                            chmod +x /tmp/gitleaks

                            echo "Running Gitleaks scan..."
                            /tmp/gitleaks detect \
                                --source="." \
                                --config=gitleaks.toml \
                                --report-format=json \
                                --report-path=gitleaks-report.json \
                                --verbose || true

                            echo "Gitleaks scan complete."
                        '''
                    }
                    post {
                        always {
                            archiveArtifacts(
                                artifacts: 'gitleaks-report.json',
                                allowEmptyArchive: true
                            )
                        }
                    }
                }

                stage('Snyk Security Scan') {
                    steps {
                        withCredentials([string(credentialsId: 'snyk-token', variable: 'SNYK_TOKEN')]) {
                            sh '''
                                echo "Downloading Snyk CLI..."
                                curl -sSfL https://github.com/snyk/cli/releases/latest/download/snyk-linux \
                                    -o /tmp/snyk
                                chmod +x /tmp/snyk

                                echo "Authenticating Snyk..."
                                /tmp/snyk auth ${SNYK_TOKEN}

                                echo "Running Snyk test..."
                                /tmp/snyk test \
                                    --all-projects \
                                    --severity-threshold=high \
                                    --json-file-output=snyk-report.json || true

                                echo "Snyk scan complete."
                            '''
                        }
                    }
                    post {
                        always {
                            archiveArtifacts(
                                artifacts: 'snyk-report.json',
                                allowEmptyArchive: true
                            )
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline execution finished.'
        }
        success {
            echo "Build successful! Changed services: ${changedServices?.join(', ') ?: 'none'}"
        }
        failure {
            echo 'Build failed. Please check the console output for errors.'
        }
        cleanup {
            cleanWs()
        }
    }
}
