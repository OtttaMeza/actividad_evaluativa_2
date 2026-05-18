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
                withSonarQubeEnv('SONAR_CLOUD') {
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
                publishHTML(target: [
                    allowMissing         : false,
                    alwaysLinkToLastBuild: true,
                    keepAll              : true,
                    reportDir            : 'target/site/jacoco',
                    reportFiles          : 'index.html',
                    reportName           : 'JaCoCo Coverage Report'
                ])
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
            // Encadenamiento CI -> CD: dispara el job de deploy SOLO si el Quality Gate pasó.
            // wait:false  -> el CI no se bloquea esperando al deploy (fire-and-forget).
            // El job 'pipeline-deploy' es independiente: hace su propio checkout + build,
            // por lo que cleanWs() en este pipeline no le afecta.
            build job: 'DPLOYMENT/GCV-PROD', wait: false
        }
        failure {
            echo 'Pipeline FAILED — revisar logs y SonarCloud'
            // mail to: 'otalvaro.jose.meza@gmail.com',
            //      subject: "FALLO ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            //      body: "Ver: ${env.BUILD_URL}"
        }
        cleanup {
            // Se mantiene: el CD es un job independiente que recompila, no depende de este workspace.
            cleanWs()
        }
    }
}