package com.nyabi.dcremover.data.model

data class GalleryInfo(
    val id: String,
    val name: String
)

data class CleaningProgress(
    val current: Int,
    val total: Int,
    val message: String
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
