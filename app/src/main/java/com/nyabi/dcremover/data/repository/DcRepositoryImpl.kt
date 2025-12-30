package com.nyabi.dcremover.data.repository

import com.nyabi.dcremover.data.model.*
import com.nyabi.dcremover.data.network.*
import com.nyabi.dcremover.domain.repository.DcRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.Jsoup

@Singleton
class DcRepositoryImpl
@Inject
constructor(
        private val dcMainApi: DcMainApi,
        private val dcSignApi: DcSignApi,
        private val gallogApi: GallogApi,
        private val cookieManager: CookieManager,
        private val captchaSolverFactory: CaptchaSolverFactory
) : DcRepository {

    companion object {
        private const val DCINSIDE_SITE_KEY = "6LcJyr4UAAAAAOy9Q_e9sDWPSHJ_aXus4UnYLfgL"
    }

    private var _userId: String = ""
    override val userId: String
        get() = _userId
    override val isLoggedIn: Boolean
        get() = _userId.isNotEmpty()

    override suspend fun login(id: String, pw: String): LoginResult {
        return try {
            // Get main page to get form fields (www.dcinside.com)
            val mainPageResponse = dcMainApi.getMainPage()
            val mainPageHtml =
                    mainPageResponse.body()?.string()
                            ?: return LoginResult.Error("Failed to load main page")

            // Parse form inputs
            val doc = Jsoup.parse(mainPageHtml)
            val formInputs = doc.select("#login_process > input")
            val formData = mutableMapOf<String, String>()

            formInputs.forEach { input ->
                val name = input.attr("name")
                val value = input.attr("value")
                if (name.isNotEmpty()) {
                    formData[name] = value
                }
            }

            formData["user_id"] = id.lowercase()
            formData["pw"] = pw

            // Perform login (sign.dcinside.com)
            dcSignApi.login(formData)

            // Check if login was successful (www.dcinside.com)
            val checkResponse = dcMainApi.getMainPage()
            val checkHtml =
                    checkResponse.body()?.string()
                            ?: return LoginResult.Error("Failed to verify login")
            val checkDoc = Jsoup.parse(checkHtml)

            if (checkDoc.select(".logout").isNotEmpty()) {
                _userId = id.lowercase()
                LoginResult.Success
            } else {
                LoginResult.InvalidCredentials
            }
        } catch (e: Exception) {
            LoginResult.Error(e.message ?: "Unknown error")
        }
    }

    override suspend fun getGalleries(postType: PostType): Result<List<GalleryInfo>> {
        return try {
            val response = gallogApi.getGallogPage(_userId, postType.path)
            val html =
                    response.body()?.string() ?: return Result.failure(Exception("Empty response"))

            val doc = Jsoup.parse(html)
            val galleryElements = doc.select("div.option_sort.gallog > div > ul > li")

            val galleries =
                    galleryElements.mapNotNull { element ->
                        val dataValue = element.attr("data-value")
                        val name = element.text().trim()

                        if (dataValue.isNotEmpty()) {
                            GalleryInfo(id = dataValue, name = name)
                        } else null
                    }

            Result.success(galleries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPostIds(
            postType: PostType,
            galleryId: String?,
            page: Int
    ): Result<List<String>> {
        return try {
            val response = gallogApi.getPosts(_userId, postType.path, galleryId, page)
            val html =
                    response.body()?.string() ?: return Result.failure(Exception("Empty response"))

            val doc = Jsoup.parse(html)
            val postElements = doc.select(".cont_listbox > li")

            val postIds =
                    postElements.mapNotNull { element ->
                        element.attr("data-no").takeIf { it.isNotEmpty() }
                    }

            Result.success(postIds)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun deletePostsFlow(
            postType: PostType,
            galleryId: String?,
            captchaConfig: CaptchaConfig?
    ): Flow<CleaningProgress> = flow {
        emit(CleaningProgress(0, 0, "게시물 목록 수집 중..."))

        // Collect all post IDs
        val allPostIds = mutableListOf<String>()
        var page = 1

        while (true) {
            val result = getPostIds(postType, galleryId, page)
            val postIds = result.getOrNull() ?: break

            if (postIds.isEmpty()) break

            allPostIds.addAll(postIds)
            page++
        }

        val total = allPostIds.size

        if (total == 0) {
            emit(CleaningProgress(0, 0, "삭제할 게시물이 없습니다"))
            return@flow
        }

        emit(CleaningProgress(0, total, "총 ${total}개 발견, 삭제 시작..."))

        val captchaSolver = captchaConfig?.let { captchaSolverFactory.getSolver(it) }
        var deleted = 0
        var failed = 0
        var needsCaptcha = false
        var consecutiveCaptchaFailures = 0
        val maxConsecutiveCaptchaFailures = 3

        for ((index, postNo) in allPostIds.withIndex()) {
            val result = deletePost(postNo, postType, needsCaptcha, captchaSolver)

            when (result) {
                CleaningResult.Success -> {
                    deleted++
                    needsCaptcha = false
                    consecutiveCaptchaFailures = 0

                    emit(
                            CleaningProgress(
                                    current = index + 1,
                                    total = total,
                                    message = "삭제 중... (${index + 1}/$total)",
                                    captchaState = CaptchaState.NONE,
                                    successCount = deleted,
                                    failCount = failed
                            )
                    )
                }
                CleaningResult.CaptchaRequired -> {
                    if (captchaSolver == null) {
                        // 캡챠 API 키가 없으면 즉시 중단
                        emit(
                                CleaningProgress(
                                        current = index + 1,
                                        total = total,
                                        message = "캡챠가 필요합니다. 캡챠 API 키를 입력해주세요.",
                                        captchaState = CaptchaState.REQUIRED,
                                        successCount = deleted,
                                        failCount = failed
                                )
                        )
                        return@flow
                    } else {
                        // 캡챠 API가 있는 경우 다음 시도에서 캡챠를 풀도록 설정
                        needsCaptcha = true
                        consecutiveCaptchaFailures++
                        failed++

                        if (consecutiveCaptchaFailures >= maxConsecutiveCaptchaFailures) {
                            // 연속 캡챠 실패가 너무 많으면 중단
                            emit(
                                    CleaningProgress(
                                            current = index + 1,
                                            total = total,
                                            message =
                                                    "캡챠 풀이 연속 실패 (${consecutiveCaptchaFailures}회). 작업을 중단합니다.",
                                            captchaState = CaptchaState.FAILED,
                                            successCount = deleted,
                                            failCount = failed
                                    )
                            )
                            return@flow
                        }

                        emit(
                                CleaningProgress(
                                        current = index + 1,
                                        total = total,
                                        message = "캡챠 감지됨. 다음 요청에서 풀이 시도...",
                                        captchaState = CaptchaState.SOLVING,
                                        successCount = deleted,
                                        failCount = failed
                                )
                        )
                    }
                }
                is CleaningResult.Failed -> {
                    failed++
                    consecutiveCaptchaFailures = 0

                    emit(
                            CleaningProgress(
                                    current = index + 1,
                                    total = total,
                                    message = "삭제 중... (${index + 1}/$total)",
                                    captchaState = CaptchaState.NONE,
                                    successCount = deleted,
                                    failCount = failed
                            )
                    )
                }
            }

            delay(1000)
        }

        emit(
                CleaningProgress(
                        current = total,
                        total = total,
                        message = "완료! 삭제: ${deleted}개, 실패: ${failed}개",
                        captchaState = CaptchaState.NONE,
                        successCount = deleted,
                        failCount = failed
                )
        )
    }

    private suspend fun deletePost(
            postNo: String,
            postType: PostType,
            solveCaptcha: Boolean,
            captchaSolver: CaptchaSolver?
    ): CleaningResult {
        return try {
            val gallogUrl = "https://gallog.dcinside.com/$_userId/${postType.path}"

            // Get gallog page to refresh ci_c cookie AND extract service_code
            val pageResponse = gallogApi.getGallogPage(_userId, postType.path)
            val pageHtml =
                    pageResponse.body()?.string()
                            ?: return CleaningResult.Failed("Empty page response")

            // Extract ci_c cookie DIRECTLY from response headers (like Rust does)
            // This is critical - we need the FRESH cookie, not the cached one
            var ciC = ""
            val setCookieHeaders = pageResponse.headers().values("Set-Cookie")
            for (header in setCookieHeaders) {
                if (header.startsWith("ci_c=")) {
                    // Parse: ci_c=VALUE; expires=...; path=/; domain=...
                    ciC = header.substringAfter("ci_c=").substringBefore(";")
                    break
                }
            }

            if (ciC.isEmpty()) {
                // Fallback to CookieManager
                ciC = cookieManager.getCookie("gallog.dcinside.com", "ci_c") ?: ""
            }

            if (ciC.isEmpty()) {
                return CleaningResult.Failed("ci_c cookie not found")
            }

            // Extract service_code from HTML (CRITICAL!)
            // <input type="hidden" name="service_code" value="..."/>
            val doc = Jsoup.parse(pageHtml)
            val serviceCodeInput = doc.select("input[name=service_code]").firstOrNull()
            val serviceCode = serviceCodeInput?.attr("value") ?: "undefined"

            val formData =
                    mutableMapOf("ci_t" to ciC, "no" to postNo, "service_code" to serviceCode)

            if (solveCaptcha && captchaSolver != null) {
                val tokenResult = captchaSolver.solve(DCINSIDE_SITE_KEY, gallogUrl)
                tokenResult.getOrNull()?.let { token -> formData["g-recaptcha-response"] = token }
            }

            val response =
                    gallogApi.deletePost(userId = _userId, referer = gallogUrl, fields = formData)

            val responseText =
                    response.body()?.string() ?: return CleaningResult.Failed("Empty response")

            when {
                responseText.contains("\"result\":\"success\"") ||
                        responseText.contains("success") -> CleaningResult.Success
                responseText.contains("captcha") -> CleaningResult.CaptchaRequired
                else -> CleaningResult.Failed(responseText)
            }
        } catch (e: Exception) {
            CleaningResult.Failed(e.message ?: "Unknown error")
        }
    }

    override fun logout() {
        _userId = ""
        cookieManager.clearCookies()
    }
}
