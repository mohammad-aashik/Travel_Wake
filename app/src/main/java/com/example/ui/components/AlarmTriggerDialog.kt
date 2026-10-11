package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AlarmStage
import com.example.model.TransportMode
import com.example.ui.screens.WakeUpScreen

@Composable
fun AlarmTriggerDialog(
    stage: AlarmStage,
    destinationName: String,
    language: String,
    remainingDistanceMeters: Double? = null,
    remainingMinutes: Int? = null,
    transportMode: TransportMode = TransportMode.BUS,
    onImAwakeClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onEndTripClick: () -> Unit
) {
    Dialog(
        onDismissRequest = { /* Prevent accidental outside dismissal to ensure alertness */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        WakeUpScreen(
            stage = stage,
            destinationName = destinationName,
            remainingDistanceMeters = remainingDistanceMeters,
            remainingMinutes = remainingMinutes,
            transportMode = transportMode,
            language = language,
            onImAwakeClick = onImAwakeClick,
            onSnoozeClick = onSnoozeClick,
            onEndTripClick = onEndTripClick,
            modifier = Modifier.fillMaxSize()
        )
    }
}
