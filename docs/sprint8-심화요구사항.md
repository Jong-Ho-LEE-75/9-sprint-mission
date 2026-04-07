# 스프린트 8 심화요구사항

## 1. 이미지 최적화하기

### 1-1. 멀티 스테이지 빌드로 이미지 크기 축소
- [ ] 멀티 스테이지(빌드, 런타임) 빌드를 활용해 이미지의 크기를 줄인다.
- 태그명: `local-slim`
- 이전에 빌드한 이미지(`1.2-M8` 또는 `local`)와 크기를 비교한다.

### 1-2. 이미지 레이어 캐시 최적화
- [ ] 이미지 레이어 캐시를 고려해 Dockerfile을 수정한다.

---

## 2. GitHub Actions를 활용한 CI/CD 파이프라인 구축

### 2-1. CI (지속적 통합) 워크플로우
- [ ] `.github/workflows/test.yml` 파일을 생성한다.
- [ ] `main` 브랜치에 PR이 생성되면 실행되도록 설정한다.
- [ ] 테스트를 실행하는 Job을 정의한다.
- [ ] CodeCov를 통해 테스트 커버리지 뱃지를 README에 추가한다.
  - 뱃지 예시: ![codecov 83%](codecov-badge-example.png)

### 2-2. CD (지속적 배포) 워크플로우
- [ ] `.github/workflows/deploy.yml` 파일을 생성한다.
- [ ] `release` 브랜치에 코드가 푸시되면 실행되도록 설정한다.

#### 2-2-1. AWS 정보 설정
- [ ] GitHub 레포지토리 설정을 통해 **시크릿**을 추가한다.
  - `AWS_ACCESS_KEY`: IAM 사용자의 액세스 키
  - `AWS_SECRET_KEY`: IAM 사용자의 시크릿 키
- [ ] GitHub 레포지토리 설정을 통해 **변수**를 추가한다.
  - `AWS_REGION`: AWS 리전 (`ap-northeast-2`)
  - `ECR_REPOSITORY_URI`: ECR 레포지토리 URI
  - `ECS_CLUSTER`: ECS 클러스터 이름 (`discodeit-cluster`)
  - `ECS_SERVICE`: ECS 서비스 이름 (`discodeit-service`)
  - `ECS_TASK_DEFINITION`: ECS 태스크 정의 이름 (`discodeit-task`)

#### 2-2-2. Docker 이미지 빌드 및 푸시
- [ ] Docker 이미지를 빌드하고 푸시하는 Job을 정의한다.
- [ ] AWS CLI를 설정하는 Step을 추가한다.
  - **Public ECR에 배포해야 하므로 리전은 `us-east-1`으로 설정해야 한다.**
- [ ] ECR 로그인 Step을 추가한다.
  - **Public ECR에 로그인해야 한다.**
- [ ] Docker 이미지 빌드 및 푸시하는 과정을 Step으로 추가한다.
  - **빌드 시간 단축을 위해 멀티 플랫폼 옵션은 제외한다.**
  - GitHub Actions의 런타임 OS와 배포할 ECS는 모두 **x86_64**이다.
- [ ] 이미지 태그는 `latest`와 **GitHub 커밋 해시**를 사용하도록 설정한다.

#### 2-2-3. ECS 서비스 업데이트
- [ ] ECS 서비스를 업데이트하는 Job을 정의한다.
- [ ] AWS CLI를 설정하는 Step을 추가한다.
  - ECS 클러스터에 접근해야 하므로 리전은 `AWS_REGION`으로 설정한다.
- [ ] 태스크 정의를 업데이트하는 Step을 추가한다.
  - 기존의 태스크 정의를 기반으로 새 이미지를 사용하도록 업데이트한다.
- [ ] 프리티어 리소스를 고려해 AWS CLI를 사용해 기존에 구동 중인 서비스를 중단하는 Step을 추가한다.
  - `aws ecs update-service --desired-count` 옵션을 활용한다.
- [ ] 새로 등록한 태스크 정의를 사용하도록 ECS 서비스를 업데이트하는 Step을 추가한다.
- [ ] AWS 콘솔을 통해 새로 등록된 태스크 정의로 배포되었는지 확인한다.
