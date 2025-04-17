#!/usr/bin/env python
import os
import sys
import subprocess
import requests
import json
import re
import argparse
from pathlib import Path

# utils 패키지 임포트를 위한 경로 설정
script_dir = os.path.dirname(os.path.abspath(__file__))
git_root = subprocess.check_output(["git", "rev-parse", "--show-toplevel"]).decode("utf-8").strip()
sys.path.append(git_root)

# 공통 환경 변수 로더 임포트
from utils.env_loader import load_env_file

def get_issue_types():
    """이슈 타입 목록 반환"""
    return [
        "Feat", "Fix", "Docs", "Style", "Refactor", "Test", "Chore", 
        "Design", "Comment", "Rename", "Remove", "!BREAKING CHANGE", "!HOTFIX"
    ]

def generate_issue_with_llm(user_input, env_vars=None):
    """LLM API를 사용하여 이슈 생성"""
    if env_vars is None:
        env_vars = {}
    
    # 환경 변수에서 API 키 로드 (우선순위: .env 파일 > 시스템 환경 변수)
    api_key = env_vars.get("OPENAI_API_KEY") or os.environ.get("OPENAI_API_KEY")
    
    if not api_key:
        return None, "OPENAI_API_KEY를 찾을 수 없습니다. .env 파일이나 환경 변수를 확인하세요."
    
    # .env에서 추가 설정 가져오기 (기본값 설정)
    model = env_vars.get("MODEL") or os.environ.get("MODEL") or "gpt-4.1"
    temperature = float(env_vars.get("TEMPERATURE") or os.environ.get("TEMPERATURE") or "0.7")
    max_tokens = int(env_vars.get("MAX_TOKENS") or os.environ.get("MAX_TOKENS") or "500")
    
    issue_types = ", ".join(get_issue_types())
    
    prompt = f"""
프로젝트 이슈 제목과 설명을 작성해주세요. 다음 규칙을 따라야 합니다:

1. 이슈 제목 형식: "<타입>/<이슈_제목>"
   - 타입은 다음 중 하나여야 합니다: {issue_types}
   - 제목은 영어로 작성해야 합니다 (띄어쓰기 대신 "_" 사용)
   - 제목은 25자 이내여야 합니다
   - 첫 글자는 대문자로 시작해야 합니다

2. 이슈 설명 형식:
   - 설명은 마크다운 형식으로 작성합니다
   - "목적", "개선 방향", "예상 결과" 등 섹션으로 구성합니다
   - 구체적이고 명확하게 작성합니다

사용자 입력:
{user_input}

출력 형식:
```
title: <타입>/<이슈_제목>

<이슈 설명 마크다운>
```
"""

    try:
        response = requests.post(
            "https://api.openai.com/v1/chat/completions",
            headers={
                "Authorization": f"Bearer {api_key}",
                "Content-Type": "application/json"
            },
            json={
                "model": model,
                "messages": [{"role": "user", "content": prompt}],
                "temperature": temperature,
                "max_tokens": max_tokens
            },
            timeout=15
        )
        
        if response.status_code == 200:
            result = response.json()
            message = result["choices"][0]["message"]["content"].strip()
            
            # 응답에서 제목과 내용 분리
            title_pattern = r"title:\s*(.*?)\n\n"
            title_match = re.search(title_pattern, message, re.DOTALL)
            
            if title_match:
                title = title_match.group(1).strip()
                description = re.sub(title_pattern, "", message, 1, re.DOTALL).strip()
                # ```로 감싸진 부분 제거
                description = re.sub(r"```.*?\n", "", description)
                description = re.sub(r"```", "", description)
                
                return {"title": title, "description": description.strip()}, None
            else:
                # 패턴이 없는 경우, 첫 줄을 제목으로 사용
                parts = message.split("\n\n", 1)
                title = parts[0].replace("title:", "").strip()
                description = parts[1] if len(parts) > 1 else ""
                
                return {"title": title, "description": description.strip()}, None
        else:
            return None, f"API 오류: {response.status_code} - {response.text}"
    
    except Exception as e:
        return None, f"오류 발생: {str(e)}"

def create_gitlab_issue(title, description, env_vars):
    """GitLab API를 사용하여 이슈 생성"""
    gitlab_url = env_vars.get("GITLAB_URL") or os.environ.get("GITLAB_URL") or "https://gitlab.com"
    gitlab_token = env_vars.get("GITLAB_TOKEN") or os.environ.get("GITLAB_TOKEN")
    gitlab_project_id = env_vars.get("GITLAB_PROJECT_ID") or os.environ.get("GITLAB_PROJECT_ID")
    
    if not gitlab_token:
        return None, "GitLab 개인 토큰을 찾을 수 없습니다. .env 파일을 확인하세요."
    
    if not gitlab_project_id:
        return None, "GitLab 프로젝트 ID를 찾을 수 없습니다. .env 파일을 확인하세요."
    
    url = f"{gitlab_url}/api/v4/projects/{gitlab_project_id}/issues"
    
    headers = {
        "PRIVATE-TOKEN": gitlab_token,
        "Content-Type": "application/json"
    }
    
    data = {
        "title": title,
        "description": description
    }
    
    try:
        response = requests.post(url, headers=headers, json=data, timeout=10)
        
        if response.status_code in [200, 201]:
            issue_data = response.json()
            return issue_data, None
        else:
            return None, f"GitLab API 오류: {response.status_code} - {response.text}"
    
    except Exception as e:
        return None, f"GitLab 이슈 생성 중 오류 발생: {str(e)}"

def create_branch_from_issue(issue_data, env_vars):
    """이슈 정보를 바탕으로 Git 브랜치 생성"""
    try:
        # 이슈 번호와 제목에서 브랜치 이름 생성
        issue_number = issue_data.get("iid")
        issue_title = issue_data.get("title", "")
        
        # 이슈 제목에서 타입 추출 (Feat/Feature_name 형식에서)
        title_parts = issue_title.split("/", 1)
        
        # 브랜치 이름 구성요소 준비
        branch_prefix = f"#{issue_number}"  # 항상 #이슈번호로 시작
        issue_type = ""
        branch_suffix = ""
        
        if len(title_parts) > 1:
            # '<타입>/<이슈_제목>' 형식인 경우
            issue_type = title_parts[0].strip()  # 타입 부분 (Feat, Fix 등)
            branch_suffix = title_parts[1].strip()  # 제목 부분
        else:
            # 형식이 다른 경우 전체 제목 사용
            branch_suffix = issue_title
        
        # 브랜치 이름에 사용할 수 있도록 제목 부분 정리
        branch_suffix = branch_suffix.replace(' ', '_')
        branch_suffix = re.sub(r'[^\w_-]', '', branch_suffix)
        
        # 타입도 특수문자 제거
        if issue_type:
            issue_type = re.sub(r'[^\w_-]', '', issue_type)
        
        # 최종 브랜치 이름 조합
        if issue_type:
            branch_name = f"{branch_prefix}/{issue_type}/{branch_suffix}"
        else:
            branch_name = f"{branch_prefix}/{branch_suffix}"
        
        # 현재 브랜치 확인
        current_branch = subprocess.check_output(
            ["git", "branch", "--show-current"]
        ).decode("utf-8").strip()
        
        # develop 브랜치로 전환 후 최신 정보 받아오기
        print("develop 브랜치로 전환 중...")
        subprocess.run(["git", "checkout", "develop"])
        
        print("최신 정보 받아오기...")
        subprocess.run(["git", "pull", "origin", "develop"])
        
        # 새 브랜치 생성 및 전환
        print(f"새 브랜치 '{branch_name}' 생성 중...")
        subprocess.run(["git", "checkout", "-b", branch_name])
        
        # 원격 저장소에 푸시
        print("브랜치를 원격 저장소에 푸시 중...")
        push_result = subprocess.run(["git", "push", "--set-upstream", "origin", branch_name], 
                                    capture_output=True, text=True)
        
        if push_result.returncode != 0:
            print(f"⚠️ 푸시 중 경고: {push_result.stderr}")
            print("원격 저장소에 푸시하지 못했습니다. 필요시 수동으로 푸시하세요.")
            print(f"명령어: git push --set-upstream origin {branch_name}")
        else:
            print(f"✅ 브랜치가 원격 저장소에 푸시되었습니다.")
        
        return branch_name, None
    
    except Exception as e:
        # 오류 발생 시 원래 브랜치로 돌아가기
        try:
            if current_branch:
                subprocess.run(["git", "checkout", current_branch])
        except:
            pass
        
        return None, f"브랜치 생성 중 오류 발생: {str(e)}"

def main():
    parser = argparse.ArgumentParser(description="이슈 설명을 입력하면 이슈 생성 및 브랜치를 자동으로 생성")
    parser.add_argument("--input", "-i", type=str, help="이슈 생성을 위한 설명이나 요구사항")
    parser.add_argument("--dry-run", "-d", action="store_true", help="이슈 내용만 생성하고 실제로 GitLab에 이슈를 생성하지는 않음")
    args = parser.parse_args()
    
    # 사용자 입력 받기
    user_input = args.input
    if not user_input:
        print("이슈에 대한 설명이나 요구사항을 입력해주세요:")
        user_input = input().strip()
    
    if not user_input:
        print("입력이 없습니다. 프로그램을 종료합니다.")
        sys.exit(1)
    
    # 공통 환경 변수 로드
    env_vars = load_env_file()
    
    # 이슈 생성
    print("\n📝 이슈 내용 생성 중...")
    issue_data, error = generate_issue_with_llm(user_input, env_vars)
    
    if error:
        print(f"⚠️ 이슈 생성 실패: {error}")
        sys.exit(1)
    
    print("\n✅ 이슈 내용이 생성되었습니다")
    print(f"제목: {issue_data['title']}")
    print(f"설명: {issue_data['description'][:100]}..." if len(issue_data['description']) > 100 else f"설명: {issue_data['description']}")
    
    # 드라이 런 모드일 경우 종료
    if args.dry_run:
        print("\n⚠️ 드라이 런 모드: 실제 이슈와 브랜치는 생성되지 않았습니다")
        return
    
    # GitLab에 이슈 생성
    print("\n📤 GitLab에 이슈 생성 중...")
    gitlab_issue, gitlab_error = create_gitlab_issue(
        issue_data['title'], 
        issue_data['description'],
        env_vars
    )
    
    if gitlab_error:
        print(f"⚠️ GitLab 이슈 생성 실패: {gitlab_error}")
        sys.exit(1)
    
    print(f"\n✅ GitLab 이슈가 생성되었습니다")
    print(f"이슈 번호: #{gitlab_issue['iid']}")
    print(f"이슈 URL: {gitlab_issue['web_url']}")
    
    # 브랜치 생성
    print("\n🔄 이슈로부터 브랜치 생성 중...")
    branch_name, branch_error = create_branch_from_issue(gitlab_issue, env_vars)
    
    if branch_error:
        print(f"⚠️ 브랜치 생성 실패: {branch_error}")
        print(f"이슈는 생성되었지만 브랜치는 생성되지 않았습니다. 수동으로 브랜치를 생성하세요.")
        sys.exit(1)
    
    print(f"\n✅ 브랜치 '{branch_name}'가 생성되었습니다")
    print("이제 이 브랜치에서 작업을 시작할 수 있습니다.")

if __name__ == "__main__":
    main() 