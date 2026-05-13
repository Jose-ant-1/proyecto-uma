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
        JWT_SECRET_VAL = credentials('jwt-secret-api')
    }

    stages {
        stage('Checkout') {
            steps {
                // Clonamos ambos repositorios en carpetas separadas
                dir('backend') {
                    git 'https://github.com/Jose-ant-1/proyecto-uma'
                }
                dir('frontend') {
                    git 'https://github.com/Jose-ant-1/interfaz-uma'
                }
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

        stage("Quality Gate") {
            steps {
                // Pausa el pipeline hasta que SonarQube devuelva el estado (OK o ERROR)
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Prepare Network & DB') {
            steps {
                script {
                    sh "docker network create jenkins-sonar-net || true"
                    // Conectamos la DB a la red si ya existe
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
                    
                    sh "docker stop api-container || true && docker rm api-container || true"
                    
                    sh """
                    docker run -d --name api-container \
                    --network jenkins-sonar-net \
                    -p 8081:8080 \
                    -e SPRING_DATASOURCE_URL=jdbc:mysql://db-api:3306/${DB_NAME}?createDatabaseIfNotExist=true \
                    -e SPRING_DATASOURCE_USERNAME=${DB_USER} \
                    -e SPRING_DATASOURCE_PASSWORD=${DB_PASS} \
                    -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
                    -e JWT_SECRET=${JWT_SECRET_VAL} \
                    ${DOCKER_USER}/${IMAGE_NAME}:latest
                    """
                }
            }
        }

        stage('Build & Deploy Front') {
            steps {
                script {
                    dir('frontend') {
                        // El Dockerfile debe estar en la raíz de 'interfaz-uma'
                        sh "docker build -t ${DOCKER_USER}/front_uma:latest ."
                    }
                    
                    sh "docker stop front-container || true && docker rm front-container || true"
                    
                    sh """
                    docker run -d --name front-container \
                    --network jenkins-sonar-net \
                    -p 80:80 \
                    ${DOCKER_USER}/front_uma:latest
                    """
                }
            }
        }

        stage('Automate APK Download') {
            steps {
                script {
                    // 1. Creamos la carpeta de descargas dentro del contenedor del front si no existe
                    sh "docker exec front-container mkdir -p /usr/share/nginx/html/downloads"
                    
                    // 2. Copiamos la APK. Ajusta la ruta 'backend/...' a donde se encuentre tu archivo
                    // Si la APK está en la raíz del repo backend:
                    sh "docker cp backend/app-release.apk front-container:/usr/share/nginx/html/downloads/uma-app.apk"
                    
                    echo "APK desplegada correctamente en el Frontend."
                }
            }
        }
    }
    
    post {
        always {
            echo "Pipeline finalizado."
        }
        failure {
            echo "El pipeline ha fallado. Revisa los logs de SonarQube o Docker."
        }
    }
}
