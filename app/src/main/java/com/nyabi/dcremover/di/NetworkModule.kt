package com.nyabi.dcremover.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nyabi.dcremover.data.network.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()
    
    @Provides
    @Singleton
    fun provideOkHttpClient(cookieManager: CookieManager): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        // Trust all certificates (like Rust's danger_accept_invalid_certs)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, java.security.SecureRandom())
        }
        
        return OkHttpClient.Builder()
            .cookieJar(cookieManager)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Connection", "keep-alive")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }
    
    @Provides
    @Singleton
    @Named("dc")
    fun provideDcRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(DcApiConstants.DC_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    @Named("sign")
    fun provideSignRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(DcApiConstants.SIGN_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    @Named("gallog")
    fun provideGallogRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(DcApiConstants.GALLOG_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    @Named("twoCaptcha")
    fun provideTwoCaptchaRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("http://2captcha.com/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    @Named("antiCaptcha")
    fun provideAntiCaptchaRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.anti-captcha.com/")
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    @Provides
    @Singleton
    fun provideDcMainApi(@Named("dc") retrofit: Retrofit): DcMainApi {
        return retrofit.create(DcMainApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideDcSignApi(@Named("sign") retrofit: Retrofit): DcSignApi {
        return retrofit.create(DcSignApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideGallogApi(@Named("gallog") retrofit: Retrofit): GallogApi {
        return retrofit.create(GallogApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideCaptchaApi(@Named("twoCaptcha") retrofit: Retrofit): CaptchaApi {
        return retrofit.create(CaptchaApi::class.java)
    }
    
    @Provides
    @Singleton
    fun provideAntiCaptchaApi(@Named("antiCaptcha") retrofit: Retrofit): AntiCaptchaApi {
        return retrofit.create(AntiCaptchaApi::class.java)
    }
}
