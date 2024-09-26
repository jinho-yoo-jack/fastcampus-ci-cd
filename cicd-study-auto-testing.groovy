pipeline {

    agent any

    environment {
        APP_REPO_URL = 'https://github.com/jinho-yoo-jack/fastcampus-ci-cd.git'
        GITHUB_CREDENTIAL_ID = 'github-access-token'
        DOCKERHUB_CREDENTIALS = credentials('docker-hub-access-key') // jenkins에 등록해 놓은 docker hub credentials 이름
        DOCKERHUB_REPOSITORY = "jhy7342/cicd-study"  //docker hub id와 repository 이름
        TARGET_HOST = "ec2-18-219-194-210.us-east-2.compute.amazonaws.com"
    }

    stages {
        stage('Checkout Git As TagName') {
            steps {
                sh "pwd"
                echo "git clone"
                script {
                    try {
                        if ("${env.TAG_NAME}" == "origin/master") {
                            print("selected origin/master")
                            throw new Exception("Tag selection is required")
                        }

                    } catch (err) {
                        echo "Caught: ${err}"
                        currentBuild.result = 'FAILURE'
                    }
                }

                checkout scm: [$class           : 'GitSCM',
                               userRemoteConfigs: [[url          : "${env.APP_REPO_URL}",
                                                    credentialsId: "${env.GITHUB_CREDENTIAL_ID}"]],
                               branches         : [[name: "refs/tags/${params.TAG_NAME}"]]],
                        poll: false
            }
        }

        stage('Build Source Code') {
            steps {
                sh "pwd"
                sh './gradlew clean test bootJar'
            }
        }

        stage('Build docker image') {
            steps {
                sh "docker build --no-cache --platform linux/amd64 -t $DOCKERHUB_REPOSITORY ."
            }
        }

        stage('Login to Docker Hub') {
            steps {
                sh "echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin"
            }
        }

        stage('Deploy Docker image') {
            steps {
                script {
                    sh 'docker push $DOCKERHUB_REPOSITORY' //docker push
                }
            }
        }

        stage('Cleaning up') {
            steps {
                sh "docker rmi $DOCKERHUB_REPOSITORY:latest" // docker image 제거
            }
        }

        stage('Start Application as Docker') {
            steps {
                sshagent(credentials: ['controller-ssh-private-key']) {
                    sh """
                    ssh -o StrictHostKeyChecking=no ubuntu@${TARGET_HOST} '
                    pwd
                    docker-compose down
                    docker rmi ${DOCKERHUB_REPOSITORY}:latest
                    docker pull ${DOCKERHUB_REPOSITORY}
                    docker-compose up -d
                    '
                """
                }
            }
        }
    }
    post {
        success {
            stage('Auto-Testing') {
                steps {
                    withSonarQubeEnv('sonarqube-server') {
                        sh '''
                            ./gradlew sonar \
                                -Dsonar.projectKey=$SONAR_PROJECT_KEY \
                                -Dsonar.host.url=http://sonarqube:9000 \
                                -Dsonar.login=$SONAR_LOGIN
                        '''
                    }
                    junit '**/build/test-results/test/*.xml'
                }
            }
        }
    }
}