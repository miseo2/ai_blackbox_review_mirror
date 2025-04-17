#!/usr/bin/env python
import os
import subprocess
from pathlib import Path

def load_env_file():
    """
    공통으로 사용할 .env 파일에서 환경 변수를 로드
    """
    # 현재 Git 저장소의 루트 디렉토리 찾기
    try:
        git_root = subprocess.check_output(["git", "rev-parse", "--show-toplevel"]).decode("utf-8").strip()
    except:
        git_root = os.getcwd()  # 실패하면 현재 디렉토리 사용
    
    # 가능한 .env 파일 경로들 (루트 디렉토리의 .env를 우선적으로 찾음)
    env_paths = [
        os.path.join(git_root, '.env'),
        os.path.join(git_root, 'utils', '.env'),
    ]
    
    env_vars = {}
    
    # 존재하는 첫 번째 .env 파일 읽기
    for env_path in env_paths:
        if os.path.exists(env_path):
            print(f".env 파일을 로드합니다: {env_path}")
            with open(env_path, 'r', encoding='utf-8') as f:
                for line in f:
                    line = line.strip()
                    if line and not line.startswith('#'):
                        key, value = line.split('=', 1)
                        env_vars[key.strip()] = value.strip()
            break
    
    return env_vars 