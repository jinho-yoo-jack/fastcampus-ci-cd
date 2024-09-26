pipeline {

    agent {
        label 'worker-1'
    }

    environment {
        APP_REPO_URL = 'https://github.com/jinho-yoo-jack/fastcampus-ci-cd.git'
        GITHUB_CREDENTIAL_ID = 'github-access-token'
        DOCKERHUB_CREDENTIALS = credentials('docker-hub-access-key') // jenkins에 등록해 놓은 docker hub credentials 이름
        DOCKERHUB_REPOSITORY = "jhy7342/cicd-study"  //docker hub id와 repository 이름
        TARGET_HOST = "ec2-18-219-194-210.us-east-2.compute.amazonaws.com"
        SONAR_PROJECT_KEY = credentials('sonarProjectKey')
        SONAR_HOST_URL = 'http://sonarqube:9000'
        SONAR_LOGIN = credentials('sonarQubeToken')
    }

    stages {
        stage('Checkout Git As TagName'){
            steps {
                sh "pwd"
                echo "git clone"
                script {
                    try {
                        if("${env.TAG_NAME}" == "origin/master"){
                            print("selected origin/master")
                            throw new Exception("Tag selection is required")
                        }

                    }catch (err) {
                        echo "Caught: ${err}"
                        currentBuild.result = 'FAILURE'
                    }
                }

                checkout scm: [$class: 'GitSCM',
                               userRemoteConfigs: [[url: "${env.APP_REPO_URL}",
                                                    credentialsId: "${env.GITHUB_CREDENTIAL_ID}"]],
                               branches: [[name: "refs/tags/${params.TAG_NAME}"]]],
                        poll: false
            }
        }
        stage('Build Source Code'){
            steps {
                sh "pwd"
                sh './gradlew clean test bootJar'
            }
        }

        stage('Check Code Quality'){
            steps {
                withSonarQubeEnv('sonarqube-server') {
                    sh '''
                    ./gradlew sonar \
                        -Dsonar.projectKey=$SONAR_PROJECT_KEY \
                        -Dsonar.host.url=http://sonarqube:9000 \
                        -Dsonar.login=$SONAR_LOGIN
                    '''
                }
                sh """
                    ./gradlew sonar \
                        -Dsonar.projectKey=cicd-study \
                        -Dsonar.host.url=http://sonarqube:9000 \
                        -Dsonar.login=sqp_5a78c415dec33251712f553b02e3a35c34e93b44
                """
            }
        }
    }
}