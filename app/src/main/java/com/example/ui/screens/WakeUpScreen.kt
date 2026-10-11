package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Subway
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlarmStage
import com.example.model.TransportMode
import com.example.ui.Localization
import com.example.ui.Strings
import com.example.ui.theme.AlarmAmber
import com.example.ui.theme.AlertRed
import com.example.ui.theme.ReadyGreen
import com.example.ui.theme.TransitCyan
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val WakeBlack = Color(0xFF060912)
private val WakeSurface = Color(0xFF101524)
private val WakeBorder = Color(0xFF1E2638)
private val WakeHighContrastText = Color(0xFFFFFFFF)
private val WakeYellowBright = Color(0xFFFFD600)

enum class WakeDismissMethod {
    HOLD_BUTTON,
    COGNITIVE_TEST
}

@Composable
fun WakeUpScreen(
    stage: AlarmStage,
    destinationName: String,
    remainingDistanceMeters: Double? = null,
    remainingMinutes: Int? = null,
    transportMode: TransportMode = TransportMode.BUS,
    language: String,
    onImAwakeClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onEndTripClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    var activeDismissMethod by remember { mutableStateOf(WakeDismissMethod.HOLD_BUTTON) }
    var showSnoozeConfirmDialog by remember { mutableStateOf(false) }

    // Alertness state for Hold-to-Dismiss
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var hasVerifiedAlertness by remember { mutableStateOf(false) }
    var holdCancelledWarning by remember { mutableStateOf(false) }

    // Cognitive Reflex Test state
    val reflexIcons = remember {
        listOf(
            Triple("Bus", Icons.Default.DirectionsBus, TransportMode.BUS),
            Triple("Train", Icons.Default.Train, TransportMode.TRAIN),
            Triple("Metro", Icons.Default.Subway, TransportMode.METRO)
        )
    }
    var targetIconIndex by remember { mutableIntStateOf(0) }
    var reflexErrorCount by remember { mutableIntStateOf(0) }
    var reflexFeedbackMessage by remember { mutableStateOf<String?>(null) }

    // Infinite radar pulse for high-contrast beacon
    val infiniteTransition = rememberInfiniteTransition(label = "wake_pulse")
    val beaconPulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_scale"
    )
    val beaconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_alpha"
    )

    // Stage-specific urgency colors and titles
    val (primaryUrgencyColor, secondaryUrgencyColor, urgencyTitle, urgencySubtitle) = when (stage) {
        AlarmStage.ARRIVAL_CONFIRMED -> Quadruple(
            ReadyGreen,
            TransitCyan,
            "ARRIVAL CONFIRMED",
            "You have reached your destination. Gather your belongings and disembark now!"
        )
        AlarmStage.FINAL_WARNING_TRIGGERED -> Quadruple(
            AlertRed,
            WakeYellowBright,
            "FINAL WAKE-UP CALL",
            "Destination is less than 2 minutes away! Immediate alertness required!"
        )
        else -> Quadruple(
            AlarmAmber,
            TransitCyan,
            "WAKE-UP ALERT",
            "Approaching your selected stop. Time to wake up and get ready."
        )
    }

    val animatedCardBorder by animateColorAsState(
        targetValue = if (isHolding) ReadyGreen else primaryUrgencyColor,
        animationSpec = tween(300),
        label = "border_color"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WakeBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // High-Contrast Radial Radar Background Beacon
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width / 2f, size.height * 0.28f)
            val maxRadius = size.width * 0.45f * beaconPulseScale

            drawCircle(
                color = primaryUrgencyColor.copy(alpha = beaconAlpha * 0.35f),
                radius = maxRadius,
                center = centerOffset
            )
            drawCircle(
                color = primaryUrgencyColor.copy(alpha = beaconAlpha * 0.6f),
                radius = maxRadius * 0.65f,
                center = centerOffset
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Urgency Header Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(primaryUrgencyColor.copy(alpha = 0.2f))
                        .border(1.5.dp, primaryUrgencyColor, RoundedCornerShape(30.dp))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Emergency,
                        contentDescription = null,
                        tint = primaryUrgencyColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = urgencyTitle,
                        color = primaryUrgencyColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 1.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Pulsing Center Icon with Dynamic Waveform
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .scale(beaconPulseScale)
                        .clip(CircleShape)
                        .background(primaryUrgencyColor.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(primaryUrgencyColor, primaryUrgencyColor.copy(alpha = 0.8f))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Pulsing Alarm",
                            tint = Color.Black,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Destination Name in High-Contrast Typography
                Text(
                    text = destinationName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = WakeHighContrastText,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = urgencySubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFC0C7D8),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // High-Contrast Telemetry Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, animatedCardBorder, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = WakeSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Estimated Minutes
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TIME TO STOP",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8E9BB5),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (remainingMinutes != null && remainingMinutes > 0) "$remainingMinutes min" else "Arriving!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = WakeYellowBright
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(WakeBorder)
                    )

                    // Distance
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "DISTANCE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8E9BB5),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val distanceDisplay = when {
                            remainingDistanceMeters == null -> "--"
                            remainingDistanceMeters < 1000 -> "${remainingDistanceMeters.toInt()} m"
                            else -> String.format("%.1f km", remainingDistanceMeters / 1000.0)
                        }
                        Text(
                            text = distanceDisplay,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = TransitCyan
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(36.dp)
                            .width(1.dp)
                            .background(WakeBorder)
                    )

                    // Mode
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "TRANSIT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8E9BB5),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsBus,
                                contentDescription = null,
                                tint = WakeHighContrastText,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = transportMode.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = WakeHighContrastText
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Method Selector: Hold to Dismiss vs Cognitive Reflex Check
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F1424))
                    .border(1.dp, WakeBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeDismissMethod == WakeDismissMethod.HOLD_BUTTON) primaryUrgencyColor.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { activeDismissMethod = WakeDismissMethod.HOLD_BUTTON }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = if (activeDismissMethod == WakeDismissMethod.HOLD_BUTTON) primaryUrgencyColor else Color(0xFF7E8A9E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hold to Verify",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeDismissMethod == WakeDismissMethod.HOLD_BUTTON) WakeHighContrastText else Color(0xFF7E8A9E)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (activeDismissMethod == WakeDismissMethod.COGNITIVE_TEST) TransitCyan.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable {
                            activeDismissMethod = WakeDismissMethod.COGNITIVE_TEST
                            targetIconIndex = (0..2).random()
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = if (activeDismissMethod == WakeDismissMethod.COGNITIVE_TEST) TransitCyan else Color(0xFF7E8A9E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reflex Test",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeDismissMethod == WakeDismissMethod.COGNITIVE_TEST) WakeHighContrastText else Color(0xFF7E8A9E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Alertness Verification Section (Interactive Animation)
            when (activeDismissMethod) {
                WakeDismissMethod.HOLD_BUTTON -> {
                    HoldToDismissSection(
                        progress = holdProgress,
                        isHolding = isHolding,
                        hasVerified = hasVerifiedAlertness,
                        primaryColor = primaryUrgencyColor,
                        language = language,
                        showWarning = holdCancelledWarning,
                        onHoldStart = {
                            isHolding = true
                            holdCancelledWarning = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onProgressUpdate = { p ->
                            holdProgress = p
                            if (p >= 1f && !hasVerifiedAlertness) {
                                hasVerifiedAlertness = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    delay(400)
                                    onImAwakeClick()
                                }
                            }
                        },
                        onHoldCancel = {
                            if (holdProgress in 0.05f..0.95f) {
                                holdCancelledWarning = true
                            }
                            isHolding = false
                            holdProgress = 0f
                        }
                    )
                }
                WakeDismissMethod.COGNITIVE_TEST -> {
                    CognitiveReflexSection(
                        targetIndex = targetIconIndex,
                        options = reflexIcons,
                        feedback = reflexFeedbackMessage,
                        language = language,
                        onSelectOption = { selectedIndex ->
                            if (selectedIndex == targetIconIndex) {
                                reflexFeedbackMessage = "ALERTNESS CONFIRMED!"
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                coroutineScope.launch {
                                    delay(400)
                                    onImAwakeClick()
                                }
                            } else {
                                reflexErrorCount++
                                reflexFeedbackMessage = "Wrong icon! Tap the ${reflexIcons[targetIconIndex].first}!"
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                // Scramble for alertness
                                targetIconIndex = (0..2).random()
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Controls: Snooze & End Trip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SNOOZE 2 MIN (With Safety Warning Confirmation)
                OutlinedButton(
                    onClick = { showSnoozeConfirmDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("wake_snooze_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = WakeYellowBright
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(true).copy(
                        brush = Brush.horizontalGradient(listOf(WakeYellowBright, AlarmAmber))
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Snooze,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Localization.get(Strings.SNOOZE_2_MIN, language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                // END TRIP (For users already at stop)
                OutlinedButton(
                    onClick = onEndTripClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("wake_end_trip_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AlertRed
                    ),
                    border = ButtonDefaults.outlinedButtonBorder(true).copy(
                        brush = Brush.horizontalGradient(listOf(AlertRed, Color(0xFFFF5252)))
                    )
                ) {
                    Text(
                        text = Localization.get(Strings.END_TRIP, language),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Snooze Safety Buffer Modal
        if (showSnoozeConfirmDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showSnoozeConfirmDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .border(2.dp, WakeYellowBright, RoundedCornerShape(24.dp))
                        .clickable(enabled = false) {},
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = WakeSurface)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(WakeYellowBright.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WakeYellowBright,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Snooze 2 Minutes?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = WakeHighContrastText
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val warningMsg = if (remainingMinutes != null && remainingMinutes <= 4) {
                            "Warning: You are very close to $destinationName (${remainingMinutes}m left). Snoozing risks missing your stop!"
                        } else {
                            "The alarm will reactivate in 2 minutes or immediately upon arrival inside the geofence."
                        }

                        Text(
                            text = warningMsg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFC0C7D8),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showSnoozeConfirmDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancel", color = WakeHighContrastText)
                            }

                            Button(
                                onClick = {
                                    showSnoozeConfirmDialog = false
                                    onSnoozeClick()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = WakeYellowBright)
                            ) {
                                Text("Confirm Snooze", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-Contrast Hold-to-Dismiss Section
 * Requires the user to press and hold for 2.0s to confirm genuine wakefulness.
 */
@Composable
private fun HoldToDismissSection(
    progress: Float,
    isHolding: Boolean,
    hasVerified: Boolean,
    primaryColor: Color,
    language: String,
    showWarning: Boolean,
    onHoldStart: () -> Unit,
    onProgressUpdate: (Float) -> Unit,
    onHoldCancel: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var holdJob by remember { mutableStateOf<Job?>(null) }

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            holdJob?.cancel()
            holdJob = coroutineScope.launch {
                val durationMs = 2000L
                val startTime = System.currentTimeMillis()
                while (true) {
                    val elapsed = System.currentTimeMillis() - startTime
                    val currentProgress = (elapsed.toFloat() / durationMs).coerceIn(0f, 1f)
                    animatedProgress.snapTo(currentProgress)
                    onProgressUpdate(currentProgress)
                    if (currentProgress >= 1f) break
                    delay(16)
                }
            }
        } else {
            holdJob?.cancel()
            animatedProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f)
            )
            onProgressUpdate(0f)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Progress Ring with Interactive Touch Target
        Box(
            modifier = Modifier
                .size(160.dp)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown()
                        onHoldStart()
                        val upOrCancelled = waitForUpOrCancellation()
                        if (upOrCancelled == null) {
                            onHoldCancel()
                        } else {
                            onHoldCancel()
                        }
                    }
                }
                .testTag("wake_hold_to_dismiss_trigger"),
            contentAlignment = Alignment.Center
        ) {
            // Background and Active Arc Canvas
            Canvas(modifier = Modifier.size(150.dp)) {
                val strokeWidth = 12.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val topLeft = Offset(
                    (size.width - radius * 2) / 2f,
                    (size.height - radius * 2) / 2f
                )
                val arcSize = Size(radius * 2, radius * 2)

                // Track ring
                drawArc(
                    color = Color(0xFF1B2338),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Progress Arc
                val arcColor = if (hasVerified) ReadyGreen else if (isHolding) WakeYellowBright else primaryColor
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(primaryColor, WakeYellowBright, ReadyGreen)
                    ),
                    startAngle = -90f,
                    sweepAngle = animatedProgress.value * 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Inner Capsule / Button
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasVerified) ReadyGreen
                        else if (isHolding) WakeYellowBright
                        else Color(0xFF141B2D)
                    )
                    .border(
                        2.dp,
                        if (isHolding) WakeHighContrastText else WakeBorder,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (hasVerified) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Awake Verified",
                            tint = Color.Black,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "AWAKE!",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = Color.Black
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Hold to dismiss",
                            tint = if (isHolding) Color.Black else primaryColor,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val percent = (animatedProgress.value * 100).toInt()
                        Text(
                            text = if (isHolding) "$percent%" else "HOLD 2s",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = if (isHolding) Color.Black else WakeHighContrastText
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Instruction / Warning Feedback
        if (showWarning) {
            Text(
                text = "Released too early! Hold continuously for 2s to ensure alertness.",
                color = WakeYellowBright,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        } else {
            Text(
                text = if (isHolding) "Keep holding until full..." else "Hold center circle for 2 seconds to dismiss",
                color = if (isHolding) WakeYellowBright else Color(0xFF8E9BB5),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Cognitive Reflex Test Section
 * Displays 3 randomized transit icons and requires the user to tap the requested icon.
 */
@Composable
private fun CognitiveReflexSection(
    targetIndex: Int,
    options: List<Triple<String, ImageVector, TransportMode>>,
    feedback: String?,
    language: String,
    onSelectOption: (Int) -> Unit
) {
    val targetName = options[targetIndex].first

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WakeSurface)
            .border(1.5.dp, TransitCyan.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = TransitCyan,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "COGNITIVE ALERTNESS CHECK",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Black,
                color = TransitCyan,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Tap the $targetName icon to prove you're awake:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = WakeHighContrastText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEachIndexed { index, item ->
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF182035))
                        .border(1.5.dp, Color(0xFF283452), RoundedCornerShape(16.dp))
                        .clickable { onSelectOption(index) }
                        .testTag("reflex_option_$index"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = item.second,
                            contentDescription = item.first,
                            tint = WakeHighContrastText,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.first,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA5B2CE)
                        )
                    }
                }
            }
        }

        if (feedback != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = feedback,
                color = if (feedback.contains("CONFIRMED")) ReadyGreen else AlertRed,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
