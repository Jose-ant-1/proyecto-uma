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
/*
        stage("Quality Gate") {
            steps {
                // Pausa el pipeline hasta que SonarQube devuelva el estado (OK o ERROR)
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
*/
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
            
            // Borrado forzoso y limpieza de seguridad
            sh "docker rm -f api-container || true"
            
            // Usamos comillas simples para evitar la interpolación insegura de JWT_SECRET_VAL
            sh '''
            docker run -d --name api-container \
            --network jenkins-sonar-net \
            -p 8080:8080 \
            -e SPRING_DATASOURCE_URL="jdbc:mysql://db-api:3306/${DB_NAME}?createDatabaseIfNotExist=true" \
            -e SPRING_DATASOURCE_USERNAME="${DB_USER}" \
            -e SPRING_DATASOURCE_PASSWORD="${DB_PASS}" \
            -e SPRING_JPA_HIBERNATE_DDL_AUTO=update \
            -e JWT_SECRET="${JWT_SECRET_VAL}" \
            ''' + "${DOCKER_USER}/${IMAGE_NAME}:latest"
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

stage('Build Mobile App') {
    steps {
        dir('movil') {
            script {
                // 1. Damos permisos de ejecución al wrapper de Gradle
                sh "chmod +x gradlew"
                
                // 2. Compilamos la versión de despliegue (Release)
                // Usamos -Pandroid.testInstrumentationRunnerArguments.notAnnotation=androidx.test.filters.LargeTest para saltar tests pesados si los hay
                sh "./gradlew assembleRelease"
            }
        }
    }
}

stage('Automate APK Download') {
    steps {
        script {
            sh "docker exec front-container mkdir -p /usr/share/nginx/html/downloads"
            
            // Ahora buscamos en la carpeta de salida estándar de Android
            sh """
            apk_file=\$(find movil/app/build/outputs/apk/release/ -name '*.apk' | head -n 1)
            if [ -z "\$apk_file" ]; then
                echo "ERROR: No se generó la APK tras la compilación"
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
