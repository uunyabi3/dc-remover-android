package com.nyabi.dcremover.data.network

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

object DcApiConstants {
    const val DC_BASE_URL = "https://www.dcinside.com/"
    const val GALLOG_BASE_URL = "https://gallog.dcinside.com/"
    const val SIGN_BASE_URL = "https://sign.dcinside.com/"
}

// www.dcinside.com API
interface DcMainApi {
    @GET("/")
    @Headers("Referer: https://www.dcinside.com/")
    suspend fun getMainPage(): Response<ResponseBody>
}

// sign.dcinside.com API
interface DcSignApi {
    @FormUrlEncoded
    @POST("login/member_check")
    @Headers(
        "X-Requested-With: XMLHttpRequest",
        "Content-Type: application/x-www-form-urlencoded; charset=UTF-8",
        "Referer: https://www.dcinside.com/"
    )
    suspend fun login(
        @FieldMap fields: Map<String, String>
    ): Response<ResponseBody>
}

interface GallogApi {
    @GET("{userId}/{postType}")
    suspend fun getGallogPage(
        @Path("userId") userId: String,
        @Path("postType") postType: String
    ): Response<ResponseBody>
    
    @GET("{userId}/{postType}/index")
    suspend fun getPosts(
        @Path("userId") userId: String,
        @Path("postType") postType: String,
        @Query("cno") galleryId: String? = null,
        @Query("p") page: Int = 1
    ): Response<ResponseBody>
    
    @FormUrlEncoded
    @POST("{userId}/ajax/log_list_ajax/delete")
    @Headers(
        "Accept: application/json, text/javascript, */*; q=0.01",
        "Accept-Encoding: gzip, deflate, br",
        "Accept-Language: ko-KR,ko;q=0.9",
        "Connection: keep-alive",
        "Content-Type: application/x-www-form-urlencoded; charset=UTF-8",
        "Host: gallog.dcinside.com",
        "Origin: https://gallog.dcinside.com",
        "Sec-Fetch-Dest: empty",
        "Sec-Fetch-Mode: cors",
        "Sec-Fetch-Site: same-origin",
        "X-Requested-With: XMLHttpRequest"
    )
    suspend fun deletePost(
        @Path("userId") userId: String,
        @Header("Referer") referer: String,
        @FieldMap fields: Map<String, String>
    ): Response<ResponseBody>
}

interface CaptchaApi {
    @GET("in.php")
    suspend fun createTwoCaptchaTask(
        @Query("key") apiKey: String,
        @Query("method") method: String = "userrecaptcha",
        @Query("googlekey") siteKey: String,
        @Query("pageurl") pageUrl: String,
        @Query("json") json: Int = 1
    ): Response<ResponseBody>
    
    @GET("res.php")
    suspend fun getTwoCaptchaResult(
        @Query("key") apiKey: String,
        @Query("action") action: String = "get",
        @Query("id") taskId: String,
        @Query("json") json: Int = 1
    ): Response<ResponseBody>
}

interface AntiCaptchaApi {
    @POST("createTask")
    suspend fun createTask(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>
    
    @POST("getTaskResult")
    suspend fun getTaskResult(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>
}
