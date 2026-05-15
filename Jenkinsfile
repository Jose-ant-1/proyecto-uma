pipeline {
    agent any

    environment {
        SCANNER_HOME = tool 'sonar-scanner'
        MVN_HOME     = tool 'maven-3'
        DOCKER_USER  = 'Pedro'
        IMAGE_NAME   = 'api_uma'
        DB_NAME      = 'uma_db'
        DB_USER      = 'user_uma'
        DB_PASS      = 'pass_uma'
    }

    stages {
        stage('Checkout') {
            steps {
                dir('backend') { git 'https://github.com/Jose-ant-1/proyecto-uma' }
                dir('frontend') { git 'https://github.com/Jose-ant-1/interfaz-uma' }
                dir('movil') { git 'https://github.com/PTenav/proyectoUMA' }
            }
        }

        stage('Maven Build') {
            steps {
                dir('backend') {
                    sh "${MVN_HOME}/bin/mvn clean compile package -DskipTests"
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir('backend') {
                    withSonarQubeEnv('sonarqube') {
                        sh """
                        ${SCANNER_HOME}/bin/sonar-scanner \
                        -Dsonar.projectKey=proyecto-uma \
                        -Dsonar.sources=src/main/java \
                        -Dsonar.java.binaries=target/classes
                        """
                    }
                }
            }
        }

        stage('Prepare Network & DB') {
            steps {
                script {
                    sh "docker network create jenkins-sonar-net || true"
                    sh "docker network connect jenkins-sonar-net db-api || true"
                }
            }
        }

stage('Build & Deploy API') {
            steps {
                script {
                    dir('backend') {
                        sh "docker build -t ${DOCKER_USER}/${IMAGE_NAME}:latest ."
                    }
                    sh "docker rm -f api-container || true"

                    withCredentials([string(credentialsId: 'jwt-secret-api', variable: 'JWT_SECRET_VAL')]) {
                        // Usamos comillas simples para evitar la advertencia de interpolación
                        sh '''
                        docker run -d --name api-container \
                        --network jenkins-sonar-net \
                        -p 8081:8080 \
                        -e SPRING_DATASOURCE_URL="jdbc:mysql://db-api:3306/${DB_NAME}?createDatabaseIfNotExist=true" \
                        -e SPRING_DATASOURCE_USERNAME="${DB_USER}" \
                        -e SPRING_DATASOURCE_PASSWORD="${DB_PASS}" \
                        -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
                        -e JWT_SECRET="${JWT_SECRET_VAL}" \
                        ${DOCKER_USER}/${IMAGE_NAME}:latest
                        '''
                    }
                }
            }
        }

        stage('Build & Deploy Front') {
            steps {
                script {
                    dir('frontend') {
                        sh "docker build -t ${DOCKER_USER}/front_uma:latest ."
                    }
                    sh "docker rm -f front-container || true"
                    sh "docker run -d --name front-container --network jenkins-sonar-net -p 80:80 ${DOCKER_USER}/front_uma:latest"
                }
            }
        }

        stage('Build Mobile App') {
            steps {
                dir('movil') {
                    sh "chmod +x gradlew && ./gradlew assembleRelease"
                }
            }
        }

        stage('Automate APK Download') {
            steps {
                script {
                    sleep 5
                    sh "docker exec front-container mkdir -p /usr/share/nginx/html/downloads"
                    sh '''
                    apk_file=$(find movil/app/build/outputs/apk/release/ -name "*.apk" | head -n 1)
                    if [ -n "$apk_file" ]; then
                        docker cp "$apk_file" front-container:/usr/share/nginx/html/downloads/uma-app.apk
                    else
                        echo "APK NOT FOUND"
                        exit 1
                    fi
                    '''
                }
            }
        }
    }

    post {
        failure { echo "Pipeline fallido. Revisa: docker logs api-container" }
    }
}