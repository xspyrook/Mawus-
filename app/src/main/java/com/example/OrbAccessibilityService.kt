package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Custom AccessibilityService that allows executing system global navigation actions
 * (Back, Home, Recents, Notifications, Quick Settings, Lock Screen, Screenshot)
 * from the Orb Controller floating bubble over any app.
 */
class OrbAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "OrbAccessibilityService"

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        @Volatile
        var instance: OrbAccessibilityService? = null
            private set

        /**
         * Checks if the Accessibility Service is currently active and bound.
         */
        fun isRunning(): Boolean {
            return instance != null && _isServiceConnected.value
        }

        /**
         * Executes a global navigation action on behalf of the user.
         */
        fun performAction(actionId: Int): Boolean {
            val service = instance
            if (service == null) {
                Log.w(TAG, "Cannot perform global action $actionId: OrbAccessibilityService is not running")
                return false
            }
            return service.performGlobalAction(actionId).also { success ->
                Log.d(TAG, "Executed global action $actionId, success: $success")
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true
        Log.i(TAG, "OrbAccessibilityService connected and ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We only require global action execution capability, window content inspection is disabled
    }

    override fun onInterrupt() {
        Log.w(TAG, "OrbAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance == this) {
            instance = null
        }
        _isServiceConnected.value = false
        Log.i(TAG, "OrbAccessibilityService destroyed")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance == this) {
            instance = null
        }
        _isServiceConnected.value = false
        return super.onUnbind(intent)
    }
}
