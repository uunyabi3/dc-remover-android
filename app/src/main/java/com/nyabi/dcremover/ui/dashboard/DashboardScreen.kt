package com.nyabi.dcremover.ui.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nyabi.dcremover.data.model.CaptchaState
import com.nyabi.dcremover.data.model.CaptchaType
import com.nyabi.dcremover.data.model.GalleryInfo
import com.nyabi.dcremover.data.model.PostType
import com.nyabi.dcremover.ui.theme.AccentGreen
import com.nyabi.dcremover.ui.theme.AccentRed
import com.nyabi.dcremover.ui.theme.AccentYellow

@Composable
fun DashboardScreen(onLogout: () -> Unit, viewModel: DashboardViewModel = hiltViewModel()) {
    val uiState = viewModel.uiState

    Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                    text = "DC Remover",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
            )

            // Post type tabs
            Row(
                    modifier =
                            Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(4.dp)
            ) {
                TabButton(
                        text = "게시글",
                        selected = uiState.postType == PostType.POSTING,
                        onClick = { viewModel.setPostType(PostType.POSTING) }
                )
                TabButton(
                        text = "댓글",
                        selected = uiState.postType == PostType.COMMENT,
                        onClick = { viewModel.setPostType(PostType.COMMENT) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Gallery list
            Column(modifier = Modifier.weight(1f)) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                            text = "갤러리",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.isLoadingGalleries) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        GalleryItem(
                                gallery = null,
                                selected = uiState.selectedGalleryId == null,
                                onClick = { viewModel.selectGallery(null) }
                        )
                    }
                    items(uiState.galleries) { gallery ->
                        GalleryItem(
                                gallery = gallery,
                                selected = uiState.selectedGalleryId == gallery.id,
                                onClick = { viewModel.selectGallery(gallery.id) }
                        )
                    }
                }
            }

            // Control panel
            Column(
                    modifier = Modifier.width(280.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Selected gallery info
                Column {
                    Text(
                            text = "선택됨",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                            text = viewModel.getSelectedGalleryName(),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Captcha settings
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                            text = "캡차 (선택)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                            value = uiState.captchaKey,
                            onValueChange = viewModel::updateCaptchaKey,
                            placeholder = { Text("API Key") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !uiState.isRunning
                    )
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CaptchaTypeButton(
                                text = "2Captcha",
                                selected = uiState.captchaType == CaptchaType.TWO_CAPTCHA,
                                onClick = { viewModel.setCaptchaType(CaptchaType.TWO_CAPTCHA) },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isRunning
                        )
                        CaptchaTypeButton(
                                text = "AntiCaptcha",
                                selected = uiState.captchaType == CaptchaType.ANTI_CAPTCHA,
                                onClick = { viewModel.setCaptchaType(CaptchaType.ANTI_CAPTCHA) },
                                modifier = Modifier.weight(1f),
                                enabled = !uiState.isRunning
                        )
                    }
                }

                // Progress
                if (uiState.isRunning && uiState.progress.total > 0) {
                    Column(
                            modifier = Modifier.animateContentSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                    text = "진행률",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                    text =
                                            "${uiState.progress.current} / ${uiState.progress.total} (${(uiState.progress.current * 100 / uiState.progress.total)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        LinearProgressIndicator(
                                progress = {
                                    uiState.progress.current.toFloat() /
                                            uiState.progress.total.toFloat()
                                },
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }

                // Delete / Stop buttons
                if (uiState.isRunning) {
                    Button(
                            onClick = viewModel::stopCleaning,
                            modifier = Modifier.fillMaxWidth(),
                            colors =
                                    ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                    )
                    ) { Text("중지") }
                } else {
                    Button(
                            onClick = viewModel::startCleaning,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                    ) { Text("삭제 시작") }
                }

                // Status indicator
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusColor =
                            when {
                                uiState.progress.captchaState == CaptchaState.REQUIRED ||
                                        uiState.progress.captchaState == CaptchaState.FAILED ->
                                        AccentRed
                                uiState.progress.captchaState == CaptchaState.SOLVING ->
                                        AccentYellow
                                uiState.isRunning -> AccentYellow
                                else -> AccentGreen
                            }

                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                    Text(
                            text = uiState.progress.message,
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                    if (uiState.progress.captchaState == CaptchaState.REQUIRED ||
                                                    uiState.progress.captchaState ==
                                                            CaptchaState.FAILED
                                    )
                                            AccentRed
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Captcha warning message
                if (uiState.progress.captchaState == CaptchaState.REQUIRED) {
                    Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = AccentRed.copy(alpha = 0.1f)
                    ) {
                        Text(
                                text = "⚠️ 캡챠가 필요합니다!\n캡챠 서비스 API 키를 입력하고 다시 시도해주세요.",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = AccentRed
                        )
                    }
                }
            }
        }

        // Logout button at bottom center
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(
                    onClick = {
                        viewModel.logout()
                        onLogout()
                    },
                    enabled = !uiState.isRunning
            ) { Text("로그아웃") }
        }
    }
}

@Composable
private fun TabButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
            shape = RoundedCornerShape(6.dp),
            color =
                    if (selected) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color =
                        if (selected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun GalleryItem(gallery: GalleryInfo?, selected: Boolean, onClick: () -> Unit) {
    Surface(
            shape = RoundedCornerShape(8.dp),
            color =
                    if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Text(
                text = gallery?.name ?: "전체 갤러리",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color =
                        if (selected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CaptchaTypeButton(
        text: String,
        selected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        enabled: Boolean = true
) {
    OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors =
                    ButtonDefaults.outlinedButtonColors(
                            containerColor =
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface,
                            contentColor =
                                    if (selected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                    )
    ) { Text(text, style = MaterialTheme.typography.labelMedium) }
}
