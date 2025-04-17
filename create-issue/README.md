# 이슈 및 브랜치 자동 생성 도구

OpenAI API와 GitLab API를 활용하여 이슈 생성부터 브랜치 생성까지 자동화하는 도구입니다.
사용자가 이슈에 대한 간단한 설명만 입력하면, LLM이 팀 컨벤션에 맞는 이슈 내용을 생성하고 GitLab에 이슈를 생성한 후 해당 이슈에 대한 브랜치까지 자동으로 생성합니다.

## 자동화 워크플로우

1. 이슈 설명 입력 → 사용자가 이슈에 대한 간단한 설명 입력
2. 이슈 내용 생성 → OpenAI가 팀 컨벤션에 맞는 이슈 제목과 상세 설명 생성
3. GitLab 이슈 생성 → GitLab API를 통해 이슈 자동 생성 및 이슈 번호 할당
4. 브랜치 자동 생성 → 이슈 번호와 제목을 사용하여 Git 브랜치 자동 생성

## 설치 방법

1. 필요한 Python 패키지 설치:
   ```
   pip install requests
   ```

2. 환경 변수 설정:
   ```
   cp utils/.env.example .env
   # .env 파일 편집
   ```

## 환경 변수 설정

`.env` 파일에 다음 값들을 설정해야 합니다:

```
# OpenAI API 키 (필수)
OPENAI_API_KEY=your_openai_api_key_here

# GitLab 설정
GITLAB_URL=https://gitlab.com
GITLAB_TOKEN=your_gitlab_personal_access_token
GITLAB_PROJECT_ID=your_project_id
```

## 사용 방법

### 기본 사용법:

```bash
./create_issue.sh "파일 변경 유형 감지 로직 개선"
```

### 드라이 런 모드 (실제 생성 없이 내용만 확인):

```bash
./create_issue.sh -d "문서 업데이트"
```

### 도움말 보기:

```bash
./create_issue.sh --help
```

## 출력 예시

```
📝 이슈 내용 생성 중...

✅ 이슈 내용이 생성되었습니다
제목: Feat/File_Change_Detection
설명: ## 목적 현재 구현된 파일 변경 유형 감지 로직을 개선하여 더 정확한 커밋 타입...

📤 GitLab에 이슈 생성 중...

✅ GitLab 이슈가 생성되었습니다
이슈 번호: #42
이슈 URL: https://gitlab.com/your-group/your-project/-/issues/42

🔄 이슈로부터 브랜치 생성 중...
develop 브랜치로 전환 중...
최신 정보 받아오기...
새 브랜치 '#42/Feat/File_Change_Detection' 생성 중...

✅ 브랜치 '#42/Feat/File_Change_Detection'가 생성되었습니다
이제 이 브랜치에서 작업을 시작할 수 있습니다.
```

## 커스터마이징

`.env` 파일에서 다음 설정을 변경할 수 있습니다:

- `MODEL`: 사용할 OpenAI 모델 (기본값: gpt-4.1)
- `TEMPERATURE`: 응답 다양성 설정 (0.0~1.0)
- `MAX_TOKENS`: 최대 토큰 수 (기본값: 500) 