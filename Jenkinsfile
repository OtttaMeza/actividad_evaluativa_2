pipeline {
    agent any
    // Recomendado en entornos con varios agentes: agent { label 'maven' }

    tools {
        maven 'Maven'
        jdk 'JDK17'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        disableConcurrentBuilds()
        timestamps()
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/OtttaMeza/actividad_evaluativa_2.git'
            }
        }

        stage('Build, Test & Analyze') {
            steps {
                withSonarQubeEnv('sonarcloud') {
                    sh 'mvn -B clean verify sonar:sonar'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Publish Reports') {
            steps {
                junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                recordCoverage tools: [[parser: 'JACOCO', pattern: 'target/site/jacoco/jacoco.xml']]
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: true
            }
        }
    }

    post {
        always {
            echo "Resultado: ${currentBuild.currentResult}"
        }
        success {
            echo 'Pipeline OK — Quality Gate aprobado'
        }
        failure {
            echo 'Pipeline FAILED — revisar logs y SonarCloud'
            // mail to: 'otalvaro.jose.meza@gmail.com',
            //      subject: "FALLO ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            //      body: "Ver: ${env.BUILD_URL}"
        }
        cleanup {
            cleanWs()
        }
    }
}