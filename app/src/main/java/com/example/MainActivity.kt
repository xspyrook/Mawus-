package com.example

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.CosmicBlack
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceHighlight
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.OutlineDark
import com.example.ui.theme.SolarAmber
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.TextTertiaryDark
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                OrbMainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrbMainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val orbSettings = remember { OrbSettings.getInstance(context) }
    val currentConfig by orbSettings.config.collectAsState()

    val isServiceRunning by FloatingBubbleService.isServiceRunning.collectAsState()
    val isAccessibilityBound by OrbAccessibilityService.isServiceConnected.collectAsState()

    var hasOverlayPermission by remember { mutableStateOf(false) }
    var hasAccessibilityPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(true) }

    var selectedTab by remember { mutableIntStateOf(0) }

    // Launcher for Android 13+ Notification Permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    // Refresh permissions on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == LifecycleEventObserver { _, e -> e == Lifecycle.Event.ON_RESUME }.let { Lifecycle.Event.ON_RESUME }) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
                hasAccessibilityPermission = checkAccessibilityEnabled(context) || isAccessibilityBound
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
        hasAccessibilityPermission = checkAccessibilityEnabled(context) || isAccessibilityBound
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun startOrbService() {
        if (!hasOverlayPermission) {
            Toast.makeText(context, "Please grant Overlay Permission first", Toast.LENGTH_SHORT).show()
            openOverlaySettings(context)
            return
        }
        if (!hasAccessibilityPermission) {
            Toast.makeText(context, "Please enable Accessibility Service for global navigation", Toast.LENGTH_LONG).show()
            openAccessibilitySettings(context)
            return
        }
        val intent = Intent(context, FloatingBubbleService::class.java).apply {
            action = FloatingBubbleService.ACTION_START_SERVICE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
        Toast.makeText(context, "Orb Controller Service Started!", Toast.LENGTH_SHORT).show()
    }

    fun stopOrbService() {
        val intent = Intent(context, FloatingBubbleService::class.java)
        context.stopService(intent)
        Toast.makeText(context, "Orb Controller Service Stopped", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("orb_main_scaffold"),
        containerColor = CosmicBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(currentConfig.accentColor)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = CosmicBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Orb Controller",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = TextPrimaryDark
                                )
                            )
                            Text(
                                text = "Floating Navigation Hub",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = currentConfig.accentColor
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                ),
                actions = {
                    IconButton(
                        onClick = {
                            hasOverlayPermission = Settings.canDrawOverlays(context)
                            hasAccessibilityPermission = checkAccessibilityEnabled(context) || isAccessibilityBound
                            Toast.makeText(context, "Status refreshed", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("action_refresh_status")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Status",
                            tint = TextSecondaryDark
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = currentConfig.accentColor,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = currentConfig.accentColor
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Hub Control", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_hub_control")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Simulator", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_simulator")
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("tab_settings")
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> HubControlTab(
                    isServiceRunning = isServiceRunning,
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    accentColor = currentConfig.accentColor,
                    onStartService = { startOrbService() },
                    onStopService = { stopOrbService() },
                    onRequestOverlay = { openOverlaySettings(context) },
                    onRequestAccessibility = { openAccessibilitySettings(context) },
                    onRequestNotification = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )

                1 -> SimulatorTab(
                    config = currentConfig
                )

                2 -> SettingsTab(
                    config = currentConfig,
                    onUpdateConfig = { orbSettings.updateConfig(it) }
                )
            }
        }
    }
}

@Composable
fun HubControlTab(
    isServiceRunning: Boolean,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    hasNotificationPermission: Boolean,
    accentColor: Color,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onRequestNotification: () -> Unit
) {
    val scrollState = rememberScrollState()

    // Pulsing aura animation for active service
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isServiceRunning) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Master Orb Service Status Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("master_service_status_card"),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = DarkSurface
            ),
            border = BorderStroke(
                1.5.dp,
                if (isServiceRunning) accentColor else OutlineDark
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated Orb Icon Indicator
                Box(
                    modifier = Modifier
                        .scale(pulseScale)
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                if (isServiceRunning) listOf(accentColor, DarkSurfaceHighlight)
                                else listOf(DarkSurfaceHighlight, CosmicBlack)
                            )
                        )
                        .border(
                            2.5.dp,
                            if (isServiceRunning) accentColor else OutlineDark,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isServiceRunning) Icons.Default.Navigation else Icons.Default.Stop,
                        contentDescription = null,
                        tint = if (isServiceRunning) CosmicBlack else TextSecondaryDark,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isServiceRunning) "ORB CONTROLLER ACTIVE" else "ORB CONTROLLER STOPPED",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = if (isServiceRunning) accentColor else TextSecondaryDark
                    )
                )

                Text(
                    text = if (isServiceRunning)
                        "Floating bubble is live over all apps. Tap the bubble anywhere on your screen to open the control hub."
                    else
                        "Start the service to display the floating overlay button on your phone.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondaryDark,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Toggle Button
                if (isServiceRunning) {
                    Button(
                        onClick = onStopService,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_stop_service")
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Stop Floating Orb", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onStartService,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = CosmicBlack
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_start_service")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Floating Orb", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Required Permissions Section
        Text(
            text = "SYSTEM PERMISSIONS",
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 1. Overlay Permission Card
        PermissionCard(
            title = "1. Display Over Other Apps",
            description = "Allows the 56dp Orb bubble to float persistently on top of any active screen or game.",
            isGranted = hasOverlayPermission,
            actionLabel = "Grant Overlay",
            onAction = onRequestOverlay,
            testTag = "perm_overlay_card"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. Accessibility Service Card
        PermissionCard(
            title = "2. Accessibility Service",
            description = "Enables executing system-wide navigation commands (Back, Home, Recents, Notifications) from the control hub.",
            isGranted = hasAccessibilityPermission,
            actionLabel = "Enable Service",
            onAction = onRequestAccessibility,
            testTag = "perm_accessibility_card"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Spacer(modifier = Modifier.height(12.dp))
            PermissionCard(
                title = "3. Foreground Notification",
                description = "Keeps the service alive in the background without battery killers interrupting it.",
                isGranted = hasNotificationPermission,
                actionLabel = "Allow Notifications",
                onAction = onRequestNotification,
                testTag = "perm_notification_card"
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Global Navigation Test Lab
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("navigation_test_lab_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, OutlineDark)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Global Action Test Bench",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Verify that the Accessibility Service is bound and can trigger system actions immediately:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            OrbAccessibilityService.performAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
                        },
                        modifier = Modifier.weight(1f).testTag("test_btn_notifs"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Notifs", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            OrbAccessibilityService.performAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                        },
                        modifier = Modifier.weight(1f).testTag("test_btn_recents"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Recents", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            OrbAccessibilityService.performAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
                        },
                        modifier = Modifier.weight(1f).testTag("test_btn_quick_settings"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Settings", fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

/**
 * Interactive In-App Simulator where users can test dragging the bubble,
 * popping the radial pie menu, and trying navigation actions.
 */
@Composable
fun SimulatorTab(
    config: OrbConfig
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    var bubbleOffsetX by remember { mutableFloatStateOf(200f) }
    var bubbleOffsetY by remember { mutableFloatStateOf(300f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LIVE INTERACTIVE SIMULATOR",
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                color = config.accentColor
            )
        )
        Text(
            text = "Drag the bubble anywhere or tap it to test the expanded radial control hub:",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextSecondaryDark,
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(vertical = 6.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Simulated Phone Screen Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(CosmicBlack)
                .border(2.dp, config.accentColor.copy(alpha = 0.5f), RoundedCornerShape(32.dp))
                .testTag("simulator_canvas")
        ) {
            // Simulated fake app background content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Fake App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Demo Phone Display",
                        color = TextPrimaryDark,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text("100% 🔋", color = TextSecondaryDark, fontSize = 12.sp)
                }

                // Fake App Feed Cards
                repeat(3) { index ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Sample Background App #${index + 1}",
                                color = config.accentColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Orb Controller floats smoothly above games, browser, and social apps without interrupting them.",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Draggable Simulated Bubble
            if (!isExpanded) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(bubbleOffsetX.roundToInt(), bubbleOffsetY.roundToInt()) }
                        .size(config.bubbleSizeDp.dp)
                        .shadow(12.dp, CircleShape, spotColor = config.accentColor)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    config.accentColor.copy(alpha = config.bubbleOpacity),
                                    DarkSurface,
                                    CosmicBlack
                                )
                            )
                        )
                        .border(2.dp, config.accentColor, CircleShape)
                        .clickable { isExpanded = true }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                bubbleOffsetX += dragAmount.x
                                bubbleOffsetY += dragAmount.y
                            }
                        }
                        .testTag("simulated_draggable_bubble"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Simulated Bubble",
                        tint = config.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Expanded Hub Simulation
            androidx.compose.animation.AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + androidx.compose.animation.scaleIn(),
                exit = fadeOut() + androidx.compose.animation.scaleOut()
            ) {
                OrbControlPanel(
                    config = config,
                    onDismiss = { isExpanded = false },
                    onPerformAction = { actionId ->
                        Toast.makeText(context, "Executed Action #$actionId", Toast.LENGTH_SHORT).show()
                        OrbAccessibilityService.performAction(actionId)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { isExpanded = !isExpanded },
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkSurfaceVariant,
                contentColor = config.accentColor
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.testTag("toggle_simulator_hub")
        ) {
            Text(if (isExpanded) "Close Simulator Hub" else "Open Simulator Hub")
        }
    }
}

/**
 * Settings Tab for customizing bubble size, opacity, accent colors, and gestures.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsTab(
    config: OrbConfig,
    onUpdateConfig: (OrbConfig) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(18.dp)
    ) {
        Text(
            text = "CUSTOMIZE ORB",
            style = MaterialTheme.typography.labelMedium.copy(
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Bold,
                color = config.accentColor
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Bubble Size Slider
        Card(
            modifier = Modifier.fillMaxWidth().testTag("setting_bubble_size_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, OutlineDark)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bubble Size", fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                    Text("${config.bubbleSizeDp} dp", color = config.accentColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = config.bubbleSizeDp.toFloat(),
                    onValueChange = { onUpdateConfig(config.copy(bubbleSizeDp = it.roundToInt())) },
                    valueRange = 44f..72f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = config.accentColor,
                        activeTrackColor = config.accentColor,
                        inactiveTrackColor = DarkSurfaceHighlight
                    ),
                    modifier = Modifier.testTag("slider_bubble_size")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bubble Opacity Slider
        Card(
            modifier = Modifier.fillMaxWidth().testTag("setting_opacity_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, OutlineDark)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bubble Opacity", fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                    Text("${(config.bubbleOpacity * 100).roundToInt()}%", color = config.accentColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = config.bubbleOpacity,
                    onValueChange = { onUpdateConfig(config.copy(bubbleOpacity = it)) },
                    valueRange = 0.4f..1.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = config.accentColor,
                        activeTrackColor = config.accentColor,
                        inactiveTrackColor = DarkSurfaceHighlight
                    ),
                    modifier = Modifier.testTag("slider_opacity")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Glowing Accent Color Theme
        Card(
            modifier = Modifier.fillMaxWidth().testTag("setting_theme_color_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, OutlineDark)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Glow Color Theme", fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                Spacer(modifier = Modifier.height(12.dp))

                val colors = listOf(
                    "Cyber Cyan" to CyberCyan,
                    "Neon Violet" to NeonViolet,
                    "Emerald" to EmeraldGlow,
                    "Solar Amber" to SolarAmber
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colors.forEachIndexed { index, (label, color) ->
                        val isSelected = config.accentColorIndex == index
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) DarkSurfaceHighlight else DarkSurfaceVariant)
                                .border(
                                    1.5.dp,
                                    if (isSelected) color else OutlineDark,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onUpdateConfig(config.copy(accentColorIndex = index)) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("color_chip_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) color else TextSecondaryDark,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Snap to Edge Switch
        Card(
            modifier = Modifier.fillMaxWidth().testTag("setting_snap_edge_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, OutlineDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Snap to Screen Edges", fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                    Text(
                        "Automatically slides the bubble to the nearest left or right edge when released so it doesn't block content.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = config.snapToEdge,
                    onCheckedChange = { onUpdateConfig(config.copy(snapToEdge = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CosmicBlack,
                        checkedTrackColor = config.accentColor
                    ),
                    modifier = Modifier.testTag("switch_snap_edge")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Haptic Feedback Switch
        Card(
            modifier = Modifier.fillMaxWidth().testTag("setting_haptic_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, OutlineDark)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Haptic Feedback", fontWeight = FontWeight.SemiBold, color = TextPrimaryDark)
                    Text(
                        "Gentle vibration click on bubble tap and pie action execution.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = config.hapticEnabled,
                    onCheckedChange = { onUpdateConfig(config.copy(hapticEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CosmicBlack,
                        checkedTrackColor = config.accentColor
                    ),
                    modifier = Modifier.testTag("switch_haptic")
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

/**
 * Reusable Card component for permission items with status badges.
 */
@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, if (isGranted) EmeraldGlow.copy(alpha = 0.5f) else OutlineDark)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                )

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isGranted) EmeraldGlow.copy(alpha = 0.16f) else Color(0xFFFFB300).copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isGranted) "GRANTED" else "ACTION REQUIRED",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isGranted) EmeraldGlow else Color(0xFFFFB300),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
            )

            if (!isGranted) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = CyberCyan
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().testTag("${testTag}_action")
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(actionLabel, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun checkAccessibilityEnabled(context: Context): Boolean {
    val expectedComponentName = "${context.packageName}/${OrbAccessibilityService::class.java.canonicalName}"
    return try {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        enabledServices.split(':').any {
            it.equals(expectedComponentName, ignoreCase = true) ||
                    it.contains("OrbAccessibilityService", ignoreCase = true)
        }
    } catch (e: Exception) {
        OrbAccessibilityService.isRunning()
    }
}

private fun openOverlaySettings(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val fallbackIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(fallbackIntent)
    }
}

private fun openAccessibilitySettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open Accessibility Settings", Toast.LENGTH_SHORT).show()
    }
}
