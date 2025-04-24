# S3 Test 애플리케이션

Spring Boot를 사용한 S3 테스트 애플리케이션입니다.

## 도커를 사용한 실행 방법

### 사전 준비

1. Docker 및 Docker Compose가 설치되어 있어야 합니다.
2. 환경 변수 설정을 위해 `.env` 파일이 필요합니다. `.env.example` 파일을 참고하여 `.env` 파일을 생성하세요.

### Docker 빌드 및 실행

```bash
# 애플리케이션 컨테이너 빌드
docker build -t s3-test-app .

# 컨테이너 실행
docker run -p 8001:8001 --env-file .env s3-test-app
```

### Docker Compose를 사용한 실행

```bash
# 모든 서비스 빌드 및 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f app

# 서비스 중지
docker-compose down
```

## 환경 변수 설정

애플리케이션은 다음 환경 변수를 사용합니다:

- `DB_URL`: 데이터베이스 연결 URL
- `DB_USERNAME`: 데이터베이스 사용자 이름
- `DB_PASSWORD`: 데이터베이스 비밀번호
- `AWS_ACCESS_KEY`: AWS 액세스 키
- `AWS_SECRET_KEY`: AWS 시크릿 키
- `AWS_REGION`: AWS 리전
- `S3_BUCKET_NAME`: S3 버킷 이름
- `JWT_SECRET`: JWT 시크릿 키
- `JWT_EXPIRATION_MS`: JWT 만료 시간(밀리초) 