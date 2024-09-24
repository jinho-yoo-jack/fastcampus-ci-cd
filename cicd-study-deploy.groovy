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

        stage('Change UpStream Conf: all-up to green-down') {
            steps {
                sshagent(credentials: ['controller-ssh-private-key']) {
                    sh '''
                    ssh -o StrictHostKeyChecking=no ubuntu@${TARGET_HOST} '
                    export NGINX_CONF_PATH="/etc/nginx/conf.d"
                    docker exec ubuntu-api-gateway-1 cp ${NGINX_CONF_PATH}/green-shutdown.conf ${NGINX_CONF_PATH}/fastcampus-cicd.conf
                    RELOAD_RESULT=`docker exec ubuntu-api-gateway-1 service nginx reload`
                    echo $RELOAD_RESULT
                    if [[ "$RELOAD_RESULT" == "Reloading nginx: nginx." ]]; then
                        docker-compose -f docker-compose-app.yml down app-green
                        docker rmi ${DOCKERHUB_REPOSITORY}:latest
                        docker pull ${DOCKERHUB_REPOSITORY}
                        docker-compose -f docker-compose-app.yml up app-green -d
                    fi
                    '
                '''
                }
            }
        }



        stage('Check App Green Status') {
            steps {
                sshagent(credentials: ['controller-ssh-private-key']) {
                    sh '''
                    ssh -o StrictHostKeyChecking=no ubuntu@${TARGET_HOST} '
                    export NGINX_CONF_PATH="/etc/nginx/conf.d"
                    for retry_count in \$(seq 5)
	                    do
	                        if curl -s "http://localhost:8082/health" > /dev/null
	                        then
	                            echo "Health Check Successful"
	                            break
	                        fi
	
	                        if [ \$retry_count -eq 10 ]
	                        then
	                            echo "Health Check Failed"
	                            exit 1
	                        fi
	
	                        echo "Retry After 10 Secs"
	                        sleep 10
	                    done
                    '
                '''
                }
            }
        }

        stage('Apply New Release Blue') {
            input {
                message "Do you want to continue the deployment?"
            }
            steps {
                script {
                    try {
                        def approval = input(
                                id: 'wait-approval',
                                message: 'Approve?',
                                submitterParameter: 'approver',
                                parameters: [choice(choices: ['Cancel', 'Deploy'], description: 'Are you sure?', name: 'choice')]
                        )
                        print(approval['approver'])

                        if (approval['choice'] == 'Deploy') {
                            print('choice deploy')
                            stage('Deploy') {
                                sshagent(credentials: ['controller-ssh-private-key']) {
                                    sh '''
                                        ssh -o StrictHostKeyChecking=no ubuntu@${TARGET_HOST} '
                                            export NGINX_CONF_PATH="/etc/nginx/conf.d"
                                            docker exec ubuntu-api-gateway-1 cp ${NGINX_CONF_PATH}/blue-shutdown.conf ${NGINX_CONF_PATH}/fastcampus-cicd.conf
                                            RELOAD_RESULT=`docker exec ubuntu-api-gateway-1 service nginx reload`
                                            if [[ "$RELOAD_RESULT" == "Reloading nginx: nginx." ]]; then
                                                docker-compose -f docker-compose-app.yml down app-blue
                                                docker-compose -f docker-compose-app.yml up app-blue -d
                                            fi
                                        '
                                    '''
                                }
                            }
                        } else {
                            throw new Exception('Choosed cancel')
                        }
                    } catch (Exception e) {
                        error e
                        currentBuild.result = 'Fail'
                    }
                }
//                sshagent(credentials: ['controller-ssh-private-key']) {
//                    sh '''
//                        ssh -o StrictHostKeyChecking=no ubuntu@${TARGET_HOST} '
//                            export NGINX_CONF_PATH="/etc/nginx/conf.d"
//                            docker exec ubuntu-api-gateway-1 cp ${NGINX_CONF_PATH}/blue-shutdown.conf ${NGINX_CONF_PATH}/fastcampus-cicd.conf
//                            RELOAD_RESULT=`docker exec ubuntu-api-gateway-1 service nginx reload`
//                            if [[ "$RELOAD_RESULT" == "Reloading nginx: nginx." ]]; then
//                                docker-compose -f docker-compose-app.yml down app-blue
//                                docker-compose -f docker-compose-app.yml up app-blue -d
//                            fi
//                        '
//                    '''
//                }
            }
        }

        stage('Change Status to All-Up') {
            steps {
                sshagent(credentials: ['controller-ssh-private-key']) {
                    sh '''
                        ssh -o StrictHostKeyChecking=no ubuntu@${TARGET_HOST} '
                            export NGINX_CONF_PATH="/etc/nginx/conf.d"
                            docker exec ubuntu-api-gateway-1 cp ${NGINX_CONF_PATH}/all-up.conf ${NGINX_CONF_PATH}/fastcampus-cicd.conf
                            docker exec ubuntu-api-gateway-1 service nginx reload
                        '
                    '''
                }
            }
        }
    }
}