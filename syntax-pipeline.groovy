pipeline {
    agent any
    stages {
        stage('Approval') {
            steps {
                script {
                    try {
                        def approval = input(
                                id: 'wait-approval',
                                message: 'Approve?',
                                submitterParameter: 'approver',
                                parameters: [choice(choices: ['Cancel', 'Deploy'],description: 'Are you sure?',name: 'choice')]
                        )
                        if (approval['approver'] != 'admin') {
                            throw new Exception('You do not have permission.')
                        }

                        if (approval['choice'] == 'Deploy') {
                            print('choice deploy')
                            currentBuild.result = 'Success'
                        } else {
                            throw new Exception('Choosed cancel')
                        }
                    } catch(Exception e) {
                        error e
                        currentBuild.result = 'Fail'
                    }
                }
            }
        }
    }
}