package com.nyabi.dcremover.domain.repository

import com.nyabi.dcremover.data.model.*
import kotlinx.coroutines.flow.Flow

interface DcRepository {
    val isLoggedIn: Boolean
    val userId: String
    
    suspend fun login(id: String, pw: String): LoginResult
    
    suspend fun getGalleries(postType: PostType): Result<List<GalleryInfo>>
    
    suspend fun getPostIds(
        postType: PostType,
        galleryId: String? = null,
        page: Int = 1
    ): Result<List<String>>
    
    fun deletePostsFlow(
        postType: PostType,
        galleryId: String?,
        captchaConfig: CaptchaConfig?
    ): Flow<CleaningProgress>
    
    fun logout()
}
