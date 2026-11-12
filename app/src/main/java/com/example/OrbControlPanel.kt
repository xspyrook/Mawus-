package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.OutlineDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * High-polish expanded circular / pie control hub for Orb Controller.
 */
@Composable
fun OrbControlPanel(
    config: OrbConfig,
    onDismiss: () -> Unit,
    onPerformAction: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
    var favoriteApps by remember { mutableStateOf<List<LaunchableApp>>(emptyList()) }

    var isFlashlightOn by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    val cameraManager = remember { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager }

    LaunchedEffect(Unit) {
        favoriteApps = AppLauncherHelper.getFavoriteApps(context, limit = 6)
        isMuted = audioManager?.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audioManager?.ringerMode == AudioManager.RINGER_MODE_VIBRATE
    }

    fun playHaptic() {
        if (!config.hapticEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(25L)
        }
    }

    // Full screen overlay backdrop
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                playHaptic()
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        // Main Control Hub Card (consumes clicks to avoid dismissing when tapping inner panel)
        Card(
            modifier = Modifier
                .padding(20.dp)
                .wrapContentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* no-op consume */ }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 40f) {
                            playHaptic()
                            onDismiss()
                        }
                    }
                }
                .testTag("orb_control_hub_card"),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface.copy(alpha = 0.94f)
            ),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        config.accentColor.copy(alpha = 0.85f),
                        NeonViolet.copy(alpha = 0.5f),
                        config.accentColor.copy(alpha = 0.2f)
                    )
                )
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Top Header Pill / Drag Handle
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(config.accentColor.copy(alpha = 0.6f))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Radial / Pie Menu Wheel
                OrbPieWheel(
                    accentColor = config.accentColor,
                    onCenterClick = {
                        playHaptic()
                        onDismiss()
                    },
                    onActionClick = { actionId ->
                        playHaptic()
                        onPerformAction(actionId)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Favorite Apps Row
                if (favoriteApps.isNotEmpty()) {
                    Text(
                        text = "QUICK APPS",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.8.sp,
                            fontWeight = FontWeight.Bold,
                            color = config.accentColor
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(favoriteApps) { app ->
                            AppIconItem(
                                app = app,
                                onClick = {
                                    playHaptic()
                                    AppLauncherHelper.launchApp(context, app.packageName)
                                    onDismiss()
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Quick Device Utilities (Flashlight, Volume, Settings)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DarkSurfaceVariant.copy(alpha = 0.8f))
                        .border(1.dp, config.accentColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flashlight Toggle
                    IconButton(
                        onClick = {
                            playHaptic()
                            try {
                                val cameraId = cameraManager?.cameraIdList?.firstOrNull()
                                if (cameraId != null) {
                                    isFlashlightOn = !isFlashlightOn
                                    cameraManager.setTorchMode(cameraId, isFlashlightOn)
                                }
                            } catch (e: Exception) {
                                // Torch not available or in use
                            }
                        },
                        modifier = Modifier.size(40.dp).testTag("utility_flashlight")
                    ) {
                        Icon(
                            imageVector = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Flashlight Toggle",
                            tint = if (isFlashlightOn) config.accentColor else TextSecondaryDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Mute / Normal Toggle
                    IconButton(
                        onClick = {
                            playHaptic()
                            try {
                                if (audioManager != null) {
                                    if (isMuted) {
                                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                                        isMuted = false
                                    } else {
                                        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                                        isMuted = true
                                    }
                                }
                            } catch (e: Exception) {
                                // Do not disturb permission restricted
                            }
                        },
                        modifier = Modifier.size(40.dp).testTag("utility_volume")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                            contentDescription = "Volume Toggle",
                            tint = if (isMuted) NeonViolet else TextSecondaryDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Quick Settings Page
                    IconButton(
                        onClick = {
                            playHaptic()
                            onPerformAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
                            onDismiss()
                        },
                        modifier = Modifier.size(40.dp).testTag("utility_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Quick Settings",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Circular radial pie menu arranged around a glowing Orb core.
 */
@Composable
fun OrbPieWheel(
    accentColor: Color,
    onCenterClick: () -> Unit,
    onActionClick: (Int) -> Unit
) {
    val wheelRadius = 104.dp
    val density = LocalDensity.current
    val wheelRadiusPx = with(density) { wheelRadius.toPx() }

    data class PieAction(
        val actionId: Int,
        val label: String,
        val icon: ImageVector,
        val angleDeg: Double,
        val testTag: String
    )

    val actions = remember {
        listOf(
            PieAction(AccessibilityService.GLOBAL_ACTION_HOME, "Home", Icons.Default.Home, -90.0, "pie_action_home"),
            PieAction(AccessibilityService.GLOBAL_ACTION_RECENTS, "Recents", Icons.Default.Apps, -38.0, "pie_action_recents"),
            PieAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "Notifs", Icons.Default.Notifications, 14.0, "pie_action_notifs"),
            PieAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS, "Controls", Icons.Default.Tune, 66.0, "pie_action_quick_settings"),
            PieAction(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT else AccessibilityService.GLOBAL_ACTION_HOME,
                "Capture",
                Icons.Default.CameraAlt,
                118.0,
                "pie_action_screenshot"
            ),
            PieAction(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN else AccessibilityService.GLOBAL_ACTION_BACK,
                "Lock",
                Icons.Default.Lock,
                170.0,
                "pie_action_lock"
            ),
            PieAction(AccessibilityService.GLOBAL_ACTION_BACK, "Back", Icons.AutoMirrored.Filled.ArrowBack, 222.0, "pie_action_back")
        )
    }

    Box(
        modifier = Modifier
            .size(280.dp)
            .testTag("orb_pie_wheel"),
        contentAlignment = Alignment.Center
    ) {
        // Decorative glowing rings
        Box(
            modifier = Modifier
                .size(240.dp)
                .border(
                    BorderStroke(1.dp, Brush.sweepGradient(listOf(accentColor.copy(alpha = 0.4f), NeonViolet.copy(alpha = 0.3f), accentColor.copy(alpha = 0.4f)))),
                    CircleShape
                )
        )

        Box(
            modifier = Modifier
                .size(170.dp)
                .border(
                    BorderStroke(1.5.dp, accentColor.copy(alpha = 0.25f)),
                    CircleShape
                )
        )

        // Radial Pie Action Buttons
        actions.forEach { action ->
            val rad = Math.toRadians(action.angleDeg)
            val offsetX = (wheelRadiusPx * cos(rad)).roundToInt()
            val offsetY = (wheelRadiusPx * sin(rad)).roundToInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX, offsetY) }
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .border(1.5.dp, accentColor.copy(alpha = 0.7f), CircleShape)
                    .clickable { onActionClick(action.actionId) }
                    .testTag(action.testTag),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.label,
                        tint = TextPrimaryDark,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = action.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        ),
                        maxLines = 1
                    )
                }
            }
        }

        // Center Glowing Orb Core
        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(16.dp, CircleShape, spotColor = accentColor)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            accentColor,
                            DarkSurfaceHighlight
                        )
                    )
                )
                .border(2.dp, accentColor, CircleShape)
                .clickable { onCenterClick() }
                .testTag("orb_center_core_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Collapse Orb Control Hub",
                tint = CosmicBlack,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Item for launching a favorite pre-installed app.
 */
@Composable
fun AppIconItem(
    app: LaunchableApp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(54.dp)
            .clickable(onClick = onClick)
            .testTag("app_icon_${app.packageName}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DarkSurfaceHighlight)
                .border(1.dp, OutlineDark, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (app.iconBitmap != null) {
                Image(
                    bitmap = app.iconBitmap,
                    contentDescription = app.label,
                    modifier = Modifier.size(34.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = app.label,
                    tint = CyberCyan,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = TextSecondaryDark
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
