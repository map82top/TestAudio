package com.example.testaudio.ui

import java.util.Collections
import java.util.Random

data class AudioTranslatorUIState(
    val isAllRequiredPermissionsGranted: Boolean = false,
    val isActive: Boolean = false,
    val isLoaded: Boolean = false
) {
    fun getTranslateButtonText(): String {
        return if (isActive) "Stop" else "Start"
    }
}