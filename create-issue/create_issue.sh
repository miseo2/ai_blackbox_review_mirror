#!/bin/bash

# 현재 스크립트 위치 찾기
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PYTHON_SCRIPT="$SCRIPT_DIR/create_issue.py"

# 실행 권한 확인 및 부여
if [ ! -x "$PYTHON_SCRIPT" ]; then
    chmod +x "$PYTHON_SCRIPT"
fi

# 사용법을 표시하는 함수
show_usage() {
    echo "이슈 생성 및 브랜치 생성 자동화 도구"
    echo
    echo "사용법:"
    echo "  $0 [옵션] [이슈 설명]"
    echo
    echo "옵션:"
    echo "  -d, --dry-run     이슈 내용만 생성하고 실제로 GitLab에 이슈와 브랜치를 생성하지 않음"
    echo "  -h, --help        도움말 표시"
    echo
    echo "예시:"
    echo "  $0 \"파일 변경 유형 감지 로직 개선\""
    echo "  $0 --dry-run \"문서 업데이트\""
}

# 명령행 인자 처리
DRY_RUN=false
ISSUE_INPUT=""

while (( "$#" )); do
    case "$1" in
        -d|--dry-run)
            DRY_RUN=true
            shift
            ;;
        -h|--help)
            show_usage
            exit 0
            ;;
        -*) # 알 수 없는 옵션
            echo "오류: 알 수 없는 옵션 $1" >&2
            show_usage
            exit 1
            ;;
        *) # 이슈 설명 (마지막 인자)
            ISSUE_INPUT="$*"
            break
            ;;
    esac
done

# 파이썬 스크립트 실행
CMD="python \"$PYTHON_SCRIPT\""

if [ "$DRY_RUN" = true ]; then
    CMD="$CMD --dry-run"
fi

if [ -n "$ISSUE_INPUT" ]; then
    CMD="$CMD --input \"$ISSUE_INPUT\""
fi

# 명령 실행
eval $CMD 