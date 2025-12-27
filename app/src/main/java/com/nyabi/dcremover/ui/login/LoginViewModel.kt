package com.nyabi.dcremover.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nyabi.dcremover.data.model.LoginResult
import com.nyabi.dcremover.domain.repository.DcRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val id: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: DcRepository
) : ViewModel() {
    
    var uiState by mutableStateOf(LoginUiState())
        private set
    
    fun updateId(id: String) {
        uiState = uiState.copy(id = id, error = null)
    }
    
    fun updatePassword(password: String) {
        uiState = uiState.copy(password = password, error = null)
    }
    
    fun login() {
        if (uiState.id.isBlank() || uiState.password.isBlank()) {
            uiState = uiState.copy(error = "아이디와 비밀번호를 입력해주세요")
            return
        }
        
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, error = null)
            
            when (val result = repository.login(uiState.id, uiState.password)) {
                LoginResult.Success -> {
                    uiState = uiState.copy(isLoading = false, isLoggedIn = true)
                }
                LoginResult.InvalidCredentials -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = "로그인 실패. 아이디/비밀번호를 확인해주세요."
                    )
                }
                is LoginResult.Error -> {
                    uiState = uiState.copy(
                        isLoading = false,
                        error = "오류: ${result.message}"
                    )
                }
            }
        }
    }
}
