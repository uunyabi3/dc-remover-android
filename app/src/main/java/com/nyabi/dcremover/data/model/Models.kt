package com.nyabi.dcremover.data.model

data class GalleryInfo(
    val id: String,
    val name: String
)

enum class CaptchaState {
    NONE,           // 캡챠 없음
    REQUIRED,       // 캡챠 필요 (API 키 없음)
    SOLVING,        // 캡챠 풀이 중
    SOLVED,         // 캡챠 풀이 성공
    FAILED          // 캡챠 풀이 실패
}

data class CleaningProgress(
    val current: Int,
    val total: Int,
    val message: String,
    val captchaState: CaptchaState = CaptchaState.NONE,
    val successCount: Int = 0,
    val failCount: Int = 0
)

sealed class CleaningResult {
    data object Success : CleaningResult()
    data object CaptchaRequired : CleaningResult()
    data class Failed(val reason: String) : CleaningResult()
}

sealed class LoginResult {
    data object Success : LoginResult()
    data object InvalidCredentials : LoginResult()
    data class Error(val message: String) : LoginResult()
}

enum class PostType(val path: String) {
    POSTING("posting"),
    COMMENT("comment")
}

enum class CaptchaType(val displayName: String) {
    TWO_CAPTCHA("2Captcha"),
    ANTI_CAPTCHA("AntiCaptcha")
}

data class CaptchaConfig(
    val type: CaptchaType,
    val apiKey: String
)
