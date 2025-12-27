# DC Remover Android

디시인사이드 갤로그 클리너 Android 앱

## 요구사항

- **Android 13** (API 33) 이상
- **JDK 17**

## 기술 스택

- Kotlin
- Jetpack Compose + Material 3
- MVVM + Repository 패턴
- Hilt (DI)
- OkHttp + Retrofit
- Jsoup (HTML 파싱)

## 빌드 방법

```bash
# Debug APK 빌드
./gradlew assembleDebug

# Release APK 빌드
./gradlew assembleRelease
```

## 프로젝트 구조

```
app/src/main/java/com/nyabi/dcremover/
├── data/
│   ├── model/         # 데이터 모델
│   ├── network/       # API, CookieManager
│   └── repository/    # Repository 구현체
├── di/                # Hilt 모듈
├── domain/
│   └── repository/    # Repository 인터페이스
├── ui/
│   ├── theme/         # Material 3 테마
│   ├── login/         # 로그인 화면
│   └── dashboard/     # 대시보드 화면
├── DCRemoverApp.kt
└── MainActivity.kt
```

## 기능

- 디시인사이드 로그인
- 갤러리별 게시글/댓글 조회
- 일괄 삭제 (진행률 표시)
- 캡차 자동 해결 (2Captcha, AntiCaptcha)
- 라이트/다크 모드 지원

## 주의사항

⚠️ 삭제된 게시물/댓글은 복구할 수 없습니다.
