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
        // Credencial de Jenkins
        JWT_SECRET_VAL = credentials('jwt-secret-api')
    }

    stages {
        stage('Checkout') {
            steps {
                dir('backend') {
                    git 'https://github.com/Jose-ant-1/proyecto-uma'
                }
                dir('frontend') {
                    git 'https://github.com/Jose-ant-1/interfaz-uma'
                }
                dir('movil') {
                    git 'https://github.com/PTenav/proyectoUMA'
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

        stage('Prepare Network & DB') {
            steps {
                script {
                    sh "docker network create jenkins-sonar-net || true"
                    // Aseguramos que la DB esté en la red correcta
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

                    // Borrado forzoso: evita el error 125 por nombre duplicado
                    sh "docker rm -f api-container || true"

                    // IMPORTANTE: Usamos comillas simples triples para evitar el error de interpolación
                    // y el puerto 8081 para no chocar con el 8080 de Jenkins
                    sh '''
                    docker run -d --name api-container \
                    --network jenkins-sonar-net \
                    -p 8081:8080 \
                    -e SPRING_DATASOURCE_URL="jdbc:mysql://db-api:3306/''' + DB_NAME + '''?createDatabaseIfNotExist=true" \
                    -e SPRING_DATASOURCE_USERNAME="''' + DB_USER + '''" \
                    -e SPRING_DATASOURCE_PASSWORD="''' + DB_PASS + '''" \
                    -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
                    -e JWT_SECRET="''' + JWT_SECRET_VAL + '''" \
                    ''' + "${DOCKER_USER}/${IMAGE_NAME}:latest"
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

                    sh """
                    docker run -d --name front-container \
                    --network jenkins-sonar-net \
                    -p 80:80 \
                    ${DOCKER_USER}/front_uma:latest
                    """
                }
            }
        }

        stage('Build Mobile App') {
            steps {
                dir('movil') {
                    script {
                        sh "chmod +x gradlew"
                        sh "./gradlew assembleRelease"
                    }
                }
            }
        }

        stage('Automate APK Download') {
            steps {
                script {
                    // Esperamos un par de segundos a que el front esté listo
                    sleep 2
                    sh "docker exec front-container mkdir -p /usr/share/nginx/html/downloads"

                    sh """
                    apk_file=\$(find movil/app/build/outputs/apk/release/ -name '*.apk' | head -n 1)
                    if [ -z "\$apk_file" ]; then
                        echo "ERROR: No se generó la APK"
                        exit 1
                    else
                        docker cp \$apk_file front-container:/usr/share/nginx/html/downloads/uma-app.apk
                    fi
                    """
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