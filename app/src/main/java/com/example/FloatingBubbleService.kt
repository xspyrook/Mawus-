package com.example

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonViolet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.hypot

/**
 * Foreground Service that manages the floating bubble overlay and expandable control hub
 * using WindowManager and Jetpack Compose.
 */
class FloatingBubbleService : Service() {

    companion object {
        private const val TAG = "FloatingBubbleService"
        private const val NOTIFICATION_ID = 2048
        private const val CHANNEL_ID = "orb_controller_channel"

        const val ACTION_START_SERVICE = "com.example.ACTION_START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.ACTION_STOP_SERVICE"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private lateinit var lifecycleOwner: ServiceLifecycleOwner
    private lateinit var orbSettings: OrbSettings
    private var vibrator: Vibrator? = null

    private lateinit var windowParams: WindowManager.LayoutParams
    private var isExpanded by mutableStateOf(false)

    private var bubbleSizePx: Int = 0
    private var lastBubbleX: Int = 0
    private var lastBubbleY: Int = 0

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "FloatingBubbleService onCreate")
        _isServiceRunning.value = true

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        orbSettings = OrbSettings.getInstance(this)

        lifecycleOwner = ServiceLifecycleOwner()
        lifecycleOwner.onCreate()

        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        createOverlayWindow()
    }

    private fun buildForegroundNotification(): Notification {
        val channelName = getString(R.string.service_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Orb Controller floating bubble overlay service"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }

        val openActivityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenIntent = PendingIntent.getActivity(
            this,
            0,
            openActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, FloatingBubbleService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStopIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Orb Controller Active")
            .setContentText("Floating control hub is running over your screen")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingOpenIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Service", pendingStopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createOverlayWindow() {
        val density = resources.displayMetrics.density
        val config = orbSettings.config.value
        bubbleSizePx = (config.bubbleSizeDp * density).toInt()

        val displayMetrics = resources.displayMetrics
        lastBubbleX = displayMetrics.widthPixels - bubbleSizePx - (16 * density).toInt()
        lastBubbleY = displayMetrics.heightPixels / 3

        windowParams = WindowManager.LayoutParams(
            bubbleSizePx,
            bubbleSizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = lastBubbleX
            y = lastBubbleY
        }

        composeView = ComposeView(this).apply {
            lifecycleOwner.attachTo(this)

            setContent {
                val currentConfig by orbSettings.config.collectAsState()

                MyApplicationTheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (isExpanded) {
                            OrbControlPanel(
                                config = currentConfig,
                                onDismiss = { collapseToBubble() },
                                onPerformAction = { actionId ->
                                    OrbAccessibilityService.performAction(actionId)
                                }
                            )
                        } else {
                            FloatingBubbleView(
                                config = currentConfig,
                                bubbleSizePx = bubbleSizePx
                            )
                        }
                    }
                }
            }

            setOnTouchListener(createTouchListener())
        }

        try {
            windowManager.addView(composeView, windowParams)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view to WindowManager", e)
        }
    }

    private fun createTouchListener(): View.OnTouchListener {
        return object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var touchStartTime = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                if (isExpanded) {
                    // Let Compose handle touches inside the expanded panel
                    return false
                }

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = windowParams.x
                        initialY = windowParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        touchStartTime = System.currentTimeMillis()
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        windowParams.x = initialX + dx
                        windowParams.y = initialY + dy
                        updateWindowLayout()
                        return true
                    }

                    MotionEvent.ACTION_UP -> {
                        val totalDx = event.rawX - initialTouchX
                        val totalDy = event.rawY - initialTouchY
                        val distance = hypot(totalDx.toDouble(), totalDy.toDouble())
                        val duration = System.currentTimeMillis() - touchStartTime

                        // Click detection threshold
                        val density = resources.displayMetrics.density
                        val clickThreshold = 14 * density

                        if (distance < clickThreshold && duration < 350) {
                            // Tap detected -> expand
                            playHaptic()
                            expandToControlHub()
                        } else {
                            // Drag released -> snap to nearest edge
                            lastBubbleX = windowParams.x
                            lastBubbleY = windowParams.y
                            if (orbSettings.config.value.snapToEdge) {
                                snapBubbleToEdge()
                            }
                        }
                        return true
                    }
                }
                return false
            }
        }
    }

    private fun snapBubbleToEdge() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val density = displayMetrics.density
        val margin = (12 * density).toInt()

        val targetX = if (windowParams.x + (bubbleSizePx / 2) < screenWidth / 2) {
            margin
        } else {
            screenWidth - bubbleSizePx - margin
        }

        val startX = windowParams.x
        val animator = ValueAnimator.ofInt(startX, targetX).apply {
            duration = 240
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                windowParams.x = animation.animatedValue as Int
                lastBubbleX = windowParams.x
                updateWindowLayout()
            }
        }
        animator.start()
    }

    private fun expandToControlHub() {
        isExpanded = true
        windowParams.width = WindowManager.LayoutParams.MATCH_PARENT
        windowParams.height = WindowManager.LayoutParams.MATCH_PARENT
        windowParams.x = 0
        windowParams.y = 0
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        updateWindowLayout()
    }

    private fun collapseToBubble() {
        isExpanded = false
        val density = resources.displayMetrics.density
        bubbleSizePx = (orbSettings.config.value.bubbleSizeDp * density).toInt()

        windowParams.width = bubbleSizePx
        windowParams.height = bubbleSizePx
        windowParams.x = lastBubbleX
        windowParams.y = lastBubbleY
        windowParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        updateWindowLayout()
    }

    private fun updateWindowLayout() {
        val view = composeView ?: return
        try {
            if (view.isAttachedToWindow) {
                windowManager.updateViewLayout(view, windowParams)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating window layout", e)
        }
    }

    private fun playHaptic() {
        if (!orbSettings.config.value.hapticEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(25L)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!isExpanded && orbSettings.config.value.snapToEdge) {
            snapBubbleToEdge()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
        try {
            composeView?.let {
                if (it.isAttachedToWindow) {
                    windowManager.removeView(it)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing overlay view", e)
        }
        lifecycleOwner.onDestroy()
        Log.i(TAG, "FloatingBubbleService destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

/**
 * Collapsed floating bubble appearance (56dp) with glowing sci-fi aura.
 */
@Composable
fun FloatingBubbleView(
    config: OrbConfig,
    bubbleSizePx: Int
) {
    val accentColor = config.accentColor

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(config.bubbleOpacity),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing shadow disc
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(12.dp, CircleShape, spotColor = accentColor, ambientColor = accentColor)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            accentColor.copy(alpha = 0.9f),
                            DarkSurface.copy(alpha = 0.95f),
                            CosmicBlack
                        )
                    )
                )
                .border(2.dp, accentColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Inner navigation core icon / graphic
            Icon(
                imageVector = Icons.Default.Navigation,
                contentDescription = "Orb Floating Bubble",
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
