# DC Remover Android

디시인사이드 갤로그 클리너 Android 버전입니다.

## 기능

- 디시인사이드 로그인
- 게시글/댓글 일괄 삭제
- 갤러리별 필터링
- 캡차 자동 해결 (2Captcha, AntiCaptcha 지원)
- 삭제 진행률 표시
- 다크 모드 지원

## 요구사항

- Android 13 (API 33) 이상

## 설치
                                                                                                                        
[Releases](https://github.com/uunyabi3/dc-remover-android/releases) 페이지에서 최신 APK를 다운로드하세요.

## 빌드

프로젝트를 클론한 후 Android Studio에서 열어 빌드할 수 있습니다.

```bash
git clone https://github.com/uunyabi3/dc-remover-android.git
cd dc-remover-android
```

Android Studio에서 `Build > Generate Signed Bundle / APK`를 선택하여 릴리스 APK를 생성합니다.

## 기술 스택

- Kotlin
- Jetpack Compose
- Material 3
- Hilt
- Retrofit + OkHttp
- Jsoup

## 주의사항

삭제된 게시물과 댓글은 복구할 수 없습니다.

## 라이선스

MIT License
