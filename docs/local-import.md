# 로컬 PC로 작업 내용 가져오기

아래 절차 중 **가능한 방법 하나**를 선택해서 이 저장소의 변경 사항을 본인 PC 로컬 저장소에 반영할 수 있습니다.

## 방법 1) 원격 브랜치가 있는 경우 (권장)

1. 로컬에서 해당 저장소를 최신으로 가져옵니다.
   ```bash
   git fetch <remote> <branch>
   ```
2. 해당 브랜치로 이동하거나, 필요한 커밋만 가져옵니다.
   ```bash
   git checkout <branch>
   # 또는 특정 커밋만 필요할 때
   git cherry-pick <commit-hash>
   ```

## 방법 2) 패치로 적용하는 경우

1. 변경이 들어있는 환경에서 패치를 만듭니다.
   ```bash
   git format-patch -1 <commit-hash>
   ```
2. 로컬 저장소에서 패치를 적용합니다.
   ```bash
   git apply <patch-file>
   ```

## 참고
- `<remote>`, `<branch>`, `<commit-hash>` 값은 환경에 맞게 치환하세요.
- `git log --oneline`으로 커밋 해시를 확인할 수 있습니다.
