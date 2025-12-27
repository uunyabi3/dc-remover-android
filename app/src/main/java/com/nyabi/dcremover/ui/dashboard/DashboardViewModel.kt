package com.nyabi.dcremover.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyabi.dcremover.data.model.*
import com.nyabi.dcremover.domain.repository.DcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val postType: PostType = PostType.POSTING,
    val galleries: List<GalleryInfo> = emptyList(),
    val selectedGalleryId: String? = null,
    val isLoadingGalleries: Boolean = false,
    val captchaKey: String = "",
    val captchaType: CaptchaType = CaptchaType.TWO_CAPTCHA,
    val isRunning: Boolean = false,
    val progress: CleaningProgress = CleaningProgress(0, 0, "대기 중"),
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DcRepository
) : ViewModel() {
    
    var uiState by mutableStateOf(DashboardUiState())
        private set
    
    private var cleaningJob: Job? = null
    
    init {
        loadGalleries()
    }
    
    fun setPostType(postType: PostType) {
        if (uiState.postType != postType) {
            uiState = uiState.copy(
                postType = postType,
                selectedGalleryId = null,
                galleries = emptyList()
            )
            loadGalleries()
        }
    }
    
    fun selectGallery(galleryId: String?) {
        uiState = uiState.copy(selectedGalleryId = galleryId)
    }
    
    fun updateCaptchaKey(key: String) {
        uiState = uiState.copy(captchaKey = key)
    }
    
    fun setCaptchaType(type: CaptchaType) {
        uiState = uiState.copy(captchaType = type)
    }
    
    private fun loadGalleries() {
        viewModelScope.launch {
            uiState = uiState.copy(isLoadingGalleries = true, error = null)
            
            repository.getGalleries(uiState.postType)
                .onSuccess { galleries ->
                    uiState = uiState.copy(
                        galleries = galleries,
                        isLoadingGalleries = false
                    )
                }
                .onFailure { e ->
                    uiState = uiState.copy(
                        isLoadingGalleries = false,
                        error = "갤러리 목록을 불러오는데 실패했습니다"
                    )
                }
        }
    }
    
    fun startCleaning() {
        if (uiState.isRunning) return
        
        val captchaConfig = if (uiState.captchaKey.isNotBlank()) {
            CaptchaConfig(uiState.captchaType, uiState.captchaKey)
        } else null
        
        cleaningJob = viewModelScope.launch {
            uiState = uiState.copy(
                isRunning = true,
                progress = CleaningProgress(0, 0, "시작 중..."),
                error = null
            )
            
            repository.deletePostsFlow(
                postType = uiState.postType,
                galleryId = uiState.selectedGalleryId,
                captchaConfig = captchaConfig
            )
                .catch { e ->
                    uiState = uiState.copy(
                        isRunning = false,
                        error = "오류: ${e.message}"
                    )
                }
                .collect { progress ->
                    uiState = uiState.copy(progress = progress)
                    
                    // Check if completed
                    if (progress.current >= progress.total && progress.total > 0) {
                        uiState = uiState.copy(isRunning = false)
                    }
                    
                    // Handle "no posts" case
                    if (progress.total == 0 && progress.message.contains("없습니다")) {
                        uiState = uiState.copy(isRunning = false)
                    }
                }
        }
    }
    
    fun stopCleaning() {
        cleaningJob?.cancel()
        cleaningJob = null
        uiState = uiState.copy(
            isRunning = false,
            progress = uiState.progress.copy(message = "중지됨")
        )
    }
    
    fun getSelectedGalleryName(): String {
        return if (uiState.selectedGalleryId == null) {
            "전체"
        } else {
            uiState.galleries.find { it.id == uiState.selectedGalleryId }?.name ?: ""
        }
    }
    
    fun logout() {
        cleaningJob?.cancel()
        repository.logout()
    }
}
