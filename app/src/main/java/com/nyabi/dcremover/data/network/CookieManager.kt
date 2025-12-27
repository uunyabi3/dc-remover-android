package com.nyabi.dcremover.data.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CookieManager @Inject constructor() : CookieJar {
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookies.forEach { cookie ->
            // Determine storage key based on cookie domain
            val storageKey = when {
                cookie.domain.startsWith(".") -> cookie.domain
                else -> url.host
            }
            
            cookieStore.getOrPut(storageKey) { mutableListOf() }.apply {
                // Remove existing cookie with same name
                removeAll { it.name == cookie.name }
                add(cookie)
            }
            
            // Also store by base domain for dcinside.com
            if (url.host.endsWith("dcinside.com")) {
                val baseDomain = ".dcinside.com"
                if (storageKey != baseDomain) {
                    cookieStore.getOrPut(baseDomain) { mutableListOf() }.apply {
                        removeAll { it.name == cookie.name }
                        add(cookie)
                    }
                }
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        val cookies = mutableListOf<Cookie>()
        val now = System.currentTimeMillis()
        
        // Collect cookies from various matching domains
        val keysToCheck = mutableSetOf<String>()
        
        // Exact host
        keysToCheck.add(host)
        
        // For dcinside.com subdomains, check base domain
        if (host.endsWith("dcinside.com")) {
            keysToCheck.add(".dcinside.com")
            keysToCheck.add("dcinside.com")
        }
        
        // Parent domains
        var currentHost = host
        while (currentHost.contains(".")) {
            currentHost = currentHost.substringAfter(".")
            keysToCheck.add(currentHost)
            keysToCheck.add(".$currentHost")
        }
        
        // Collect all matching non-expired cookies
        keysToCheck.forEach { key ->
            cookieStore[key]?.forEach { cookie ->
                if (cookie.expiresAt > now && cookie.matches(url)) {
                    // Avoid duplicates
                    if (cookies.none { it.name == cookie.name && it.value == cookie.value }) {
                        cookies.add(cookie)
                    }
                }
            }
        }
        
        return cookies
    }

    fun getCookie(host: String, name: String): String? {
        // Check exact host
        cookieStore[host]?.find { it.name == name }?.let { return it.value }
        
        // Check base domain for dcinside.com
        if (host.endsWith("dcinside.com")) {
            cookieStore[".dcinside.com"]?.find { it.name == name }?.let { return it.value }
            cookieStore["dcinside.com"]?.find { it.name == name }?.let { return it.value }
        }
        
        return null
    }

    fun clearCookies() {
        cookieStore.clear()
    }
}
