# FastCampus CI/CD 수업
## 프로젝트 구조
- src : 배포용 Spring Boot Application 예제 코드
- groovy : Jenkins Pipeline 예제 코드
- nginx-conf : 무중단 배포 프로세스 구성 시, 필요한 nginx conf 파일
- nginx.conf : 무중단 배포 설정을 위한 nginx.conf
- docker-compose.yml : Jenkins Controller/Agent Node 설정 파일
- docker-compose-app.yml : nginx와 app-blue, app-green이 선언된 파일
- Dockerfile : Spring boot Application의 Jar를 실행하는 컨테이너 이미지 정의 파일