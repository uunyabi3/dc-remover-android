package com.nyabi.dcremover.data.network

import com.nyabi.dcremover.data.model.CaptchaConfig
import com.nyabi.dcremover.data.model.CaptchaType
import com.google.gson.Gson
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

interface CaptchaSolver {
    suspend fun solve(siteKey: String, pageUrl: String): Result<String>
}

@Singleton
class CaptchaSolverFactory @Inject constructor(
    private val twoCaptchaSolver: TwoCaptchaSolver,
    private val antiCaptchaSolver: AntiCaptchaSolver
) {
    fun getSolver(config: CaptchaConfig): CaptchaSolver {
        return when (config.type) {
            CaptchaType.TWO_CAPTCHA -> twoCaptchaSolver.apply { apiKey = config.apiKey }
            CaptchaType.ANTI_CAPTCHA -> antiCaptchaSolver.apply { apiKey = config.apiKey }
        }
    }
}

class TwoCaptchaSolver @Inject constructor(
    private val api: CaptchaApi,
    private val gson: Gson
) : CaptchaSolver {
    var apiKey: String = ""
    
    override suspend fun solve(siteKey: String, pageUrl: String): Result<String> {
        return try {
            val createResponse = api.createTwoCaptchaTask(
                apiKey = apiKey,
                siteKey = siteKey,
                pageUrl = pageUrl
            )
            
            val createBody = createResponse.body()?.string() ?: return Result.failure(Exception("Empty response"))
            val createJson = gson.fromJson(createBody, Map::class.java)
            
            if ((createJson["status"] as? Double)?.toInt() != 1) {
                return Result.failure(Exception("2Captcha error: ${createJson["request"]}"))
            }
            
            val taskId = createJson["request"] as String
            
            // Poll for result
            repeat(60) {
                delay(5000)
                
                val resultResponse = api.getTwoCaptchaResult(
                    apiKey = apiKey,
                    taskId = taskId
                )
                
                val resultBody = resultResponse.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val resultJson = gson.fromJson(resultBody, Map::class.java)
                
                if ((resultJson["status"] as? Double)?.toInt() == 1) {
                    return Result.success(resultJson["request"] as String)
                }
                
                val request = resultJson["request"] as? String
                if (request != "CAPCHA_NOT_READY") {
                    return Result.failure(Exception("2Captcha error: $request"))
                }
            }
            
            Result.failure(Exception("2Captcha timeout"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class AntiCaptchaSolver @Inject constructor(
    private val api: AntiCaptchaApi,
    private val gson: Gson
) : CaptchaSolver {
    var apiKey: String = ""
    
    override suspend fun solve(siteKey: String, pageUrl: String): Result<String> {
        return try {
            val createResponse = api.createTask(
                mapOf(
                    "clientKey" to apiKey,
                    "task" to mapOf(
                        "type" to "RecaptchaV2TaskProxyless",
                        "websiteURL" to pageUrl,
                        "websiteKey" to siteKey
                    )
                )
            )
            
            val createBody = createResponse.body()?.string() ?: return Result.failure(Exception("Empty response"))
            val createJson = gson.fromJson(createBody, Map::class.java)
            
            if ((createJson["errorId"] as? Double)?.toInt() != 0) {
                return Result.failure(Exception("AntiCaptcha error: ${createJson["errorDescription"]}"))
            }
            
            val taskId = (createJson["taskId"] as Double).toLong()
            
            // Poll for result
            repeat(60) {
                delay(5000)
                
                val resultResponse = api.getTaskResult(
                    mapOf(
                        "clientKey" to apiKey,
                        "taskId" to taskId
                    )
                )
                
                val resultBody = resultResponse.body()?.string() ?: return Result.failure(Exception("Empty response"))
                val resultJson = gson.fromJson(resultBody, Map::class.java)
                
                if ((resultJson["errorId"] as? Double)?.toInt() != 0) {
                    return Result.failure(Exception("AntiCaptcha error: ${resultJson["errorDescription"]}"))
                }
                
                if (resultJson["status"] == "ready") {
                    @Suppress("UNCHECKED_CAST")
                    val solution = resultJson["solution"] as Map<String, Any>
                    return Result.success(solution["gRecaptchaResponse"] as String)
                }
            }
            
            Result.failure(Exception("AntiCaptcha timeout"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
