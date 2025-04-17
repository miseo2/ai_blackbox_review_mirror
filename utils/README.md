# 공통 유틸리티

이 디렉토리는 프로젝트 전체에서 공통으로 사용되는 유틸리티 모듈을 포함합니다.

## 환경 변수 설정

프로젝트에서 OpenAI API를 사용하기 위해 환경 변수를 설정해야 합니다.

1. `.env.example` 파일을 복사하여 `.env` 파일 생성:
   ```bash
   cp .env.example .env
   ```

2. `.env` 파일을 편집하여 OpenAI API 키와 다른 설정 추가:
   ```
   OPENAI_API_KEY=sk-your_actual_api_key_here
   MODEL=gpt-4.1
   TEMPERATURE=0.7
   MAX_TOKENS=500
   ```

## 활용 모듈

### env_loader.py

이 모듈은 `.env` 파일에서 환경 변수를 로드하는 기능을 제공합니다. 
프로젝트의 다른 스크립트에서 다음과 같이 사용할 수 있습니다:

```python
from utils.env_loader import load_env_file

# 환경 변수 로드
env_vars = load_env_file()

# 환경 변수 사용
api_key = env_vars.get("OPENAI_API_KEY")
```

## 지원 스크립트

이 유틸리티는 다음 스크립트에서 사용됩니다:

1. `pre-commit/prepare-commit-msg.py` - 커밋 메시지 자동 생성
2. `create-issue/create_issue.py` - GitHub 이슈 자동 생성 