package com.example

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SolarAmber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class OrbConfig(
    val bubbleSizeDp: Int = 56,
    val bubbleOpacity: Float = 0.88f,
    val snapToEdge: Boolean = true,
    val hapticEnabled: Boolean = true,
    val accentColorIndex: Int = 0,
    val lockScreenEnabled: Boolean = true,
    val screenshotEnabled: Boolean = true
) {
    val accentColor: Color
        get() = when (accentColorIndex) {
            0 -> CyberCyan
            1 -> NeonViolet
            2 -> EmeraldGlow
            3 -> SolarAmber
            else -> CyberCyan
        }
}

class OrbSettings private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("orb_controller_prefs", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<OrbConfig> = _config.asStateFlow()

    private fun loadConfig(): OrbConfig {
        return OrbConfig(
            bubbleSizeDp = prefs.getInt(KEY_BUBBLE_SIZE, 56),
            bubbleOpacity = prefs.getFloat(KEY_BUBBLE_OPACITY, 0.88f),
            snapToEdge = prefs.getBoolean(KEY_SNAP_TO_EDGE, true),
            hapticEnabled = prefs.getBoolean(KEY_HAPTIC, true),
            accentColorIndex = prefs.getInt(KEY_ACCENT_COLOR, 0),
            lockScreenEnabled = prefs.getBoolean(KEY_LOCK_SCREEN, true),
            screenshotEnabled = prefs.getBoolean(KEY_SCREENSHOT, true)
        )
    }

    fun updateConfig(newConfig: OrbConfig) {
        prefs.edit()
            .putInt(KEY_BUBBLE_SIZE, newConfig.bubbleSizeDp)
            .putFloat(KEY_BUBBLE_OPACITY, newConfig.bubbleOpacity)
            .putBoolean(KEY_SNAP_TO_EDGE, newConfig.snapToEdge)
            .putBoolean(KEY_HAPTIC, newConfig.hapticEnabled)
            .putInt(KEY_ACCENT_COLOR, newConfig.accentColorIndex)
            .putBoolean(KEY_LOCK_SCREEN, newConfig.lockScreenEnabled)
            .putBoolean(KEY_SCREENSHOT, newConfig.screenshotEnabled)
            .apply()
        _config.value = newConfig
    }

    companion object {
        private const val KEY_BUBBLE_SIZE = "key_bubble_size"
        private const val KEY_BUBBLE_OPACITY = "key_bubble_opacity"
        private const val KEY_SNAP_TO_EDGE = "key_snap_to_edge"
        private const val KEY_HAPTIC = "key_haptic"
        private const val KEY_ACCENT_COLOR = "key_accent_color"
        private const val KEY_LOCK_SCREEN = "key_lock_screen"
        private const val KEY_SCREENSHOT = "key_screenshot"

        @Volatile
        private var INSTANCE: OrbSettings? = null

        fun getInstance(context: Context): OrbSettings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OrbSettings(context).also { INSTANCE = it }
            }
        }
    }
}
