# 안드로이드 데일리 텍스트 앱

이 앱은 jw.org의 매일의 성경 구절을 보여주는 간단한 안드로이드 애플리케이션입니다.

## 기능

- 매일 새로운 성구 표시(2025년 6월 ~ 12월)
- 앱 실행 시 오늘의 성구 확인 가능
- 날짜 클릭 시 오늘 날짜로 이동
- < > 버튼 클릭 시 다음/이전 날짜로 이동
- jw.org 클릭 시 현재 날짜의 Watchtower Online Library로 이동
- 페이지 이동: 손가락으로 화면을 아래로 살짝 내렸다가 다시 올리면 스크롤바가 활성화되어 빠르게 이동 가능

## 설치 및 사용법

1. 앱을 설치합니다
2. 위젯을 추가하여 오늘의 성경 구절을 확인합니다

## 개발 환경

- Cursor AI
- Kotlin
- Android API 21 이상
- Target API 34

## 빌드 방법

```bash
./gradlew assembleDebug
```

## 테스트

실제 안드로이드 디바이스나 에뮬레이터에서 테스트하세요.

## 라이센스

MIT License 

생성된 APK 파일은 다음 경로에 있습니다:

최신 APK 파일은 아래 Release 페이지에서 다운로드할 수 있습니다:

[👉 Release에서 APK 다운로드](https://github.com/hanjungwoo3/daily_text/releases)

이제 이 APK를 기기나 에뮬레이터에 설치하면,
- 앱 이름이 "daily text 앱"으로 표시되고,
- 위젯 배경이 완전 투명하며,
- 날짜/제목/본문/이동 버튼 UI 및 JSON 연동,
- 날짜 이동, 스크롤, 텍스트 꽉 차게 표시 등
모든 요청하신 기능이 적용된 위젯을 사용할 수 있습니다.

추가로 확인하거나 수정할 사항이 있으면 언제든 말씀해 주세요! 

## 스크린샷

![앱 스크린샷](screenshot/daily_text.png)

## 데이터 업데이트 방법 (PDF → TXT → JSON)

1. jw.org에서 연감(Yearbook) PDF 파일을 다운로드합니다.
2. PDF에서 필요한 날짜별 본문을 복사해 텍스트 파일(예: verses_6_12.txt)로 저장합니다.
3. 저장소의 parse_verses_to_json.py 스크립트를 실행해 JSON 파일을 생성합니다.
   ```bash
   python parse_verses_to_json.py
   ```
   - 입력: verses_6_12.txt
   - 출력: app/src/main/assets/daily_verses_2025.json
4. 앱을 빌드하면 새 JSON 데이터가 적용됩니다.

> PDF → TXT 변환은 PDF 뷰어에서 복사/붙여넣기 또는 온라인 변환 도구를 활용할 수 있습니다.
