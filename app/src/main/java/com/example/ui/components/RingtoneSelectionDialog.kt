package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.service.alarm.AlarmSoundManager
import com.example.service.alarm.RingtoneOption
import com.example.ui.theme.AlarmAmber
import com.example.ui.theme.NightCardBorder
import com.example.ui.theme.NightCardSurface
import com.example.ui.theme.ReadyGreen
import com.example.ui.theme.TransitCyan

/**
 * Animated equalizer visualizer for audio preview indication.
 */
@Composable
fun RingtoneEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = TransitCyan
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val bar1 = remember { Animatable(0.3f) }
        val bar2 = remember { Animatable(0.7f) }
        val bar3 = remember { Animatable(0.5f) }

        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                while (true) {
                    bar1.animateTo(
                        1.0f,
                        animationSpec = tween(180, easing = LinearEasing)
                    )
                    bar1.animateTo(
                        0.2f,
                        animationSpec = tween(220, easing = LinearEasing)
                    )
                }
            } else {
                bar1.snapTo(0.3f)
            }
        }

        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                while (true) {
                    bar2.animateTo(
                        0.3f,
                        animationSpec = tween(150, easing = LinearEasing)
                    )
                    bar2.animateTo(
                        0.95f,
                        animationSpec = tween(200, easing = LinearEasing)
                    )
                }
            } else {
                bar2.snapTo(0.6f)
            }
        }

        LaunchedEffect(isPlaying) {
            if (isPlaying) {
                while (true) {
                    bar3.animateTo(
                        0.85f,
                        animationSpec = tween(240, easing = LinearEasing)
                    )
                    bar3.animateTo(
                        0.25f,
                        animationSpec = tween(170, easing = LinearEasing)
                    )
                }
            } else {
                bar3.snapTo(0.4f)
            }
        }

        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * bar1.value).dp.coerceAtLeast(4.dp))
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * bar2.value).dp.coerceAtLeast(4.dp))
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height((16 * bar3.value).dp.coerceAtLeast(4.dp))
                .clip(RoundedCornerShape(2.dp))
                .background(barColor)
        )
    }
}

/**
 * Dialog for selecting and previewing custom and built-in ringtones.
 */
@Composable
fun RingtoneSelectionDialog(
    currentRingtoneId: String,
    currentRingtoneTitle: String,
    currentRingtoneUri: String?,
    previewVolume: Float = 0.9f,
    onRingtoneSelected: (id: String, title: String, uriString: String?) -> Unit,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val isPlaying by AlarmSoundManager.isPlayingPreview.collectAsState()
    val currentlyPlayingId by AlarmSoundManager.currentPlayingId.collectAsState()

    var availableOptions by remember {
        mutableStateOf<List<RingtoneOption>>(emptyList())
    }
    var selectedId by remember { mutableStateOf(currentRingtoneId) }
    var selectedTitle by remember { mutableStateOf(currentRingtoneTitle) }
    var selectedUri by remember { mutableStateOf(currentRingtoneUri) }

    var selectedFilter by remember { mutableStateOf("All") } // "All", "Transit Themes", "Device Alarms"

    // Stop audio preview when dialog is closed
    DisposableEffect(Unit) {
        onDispose {
            AlarmSoundManager.stopAudio()
        }
    }

    LaunchedEffect(Unit) {
        availableOptions = AlarmSoundManager.getAvailableRingtones(context)
    }

    // System ringtone picker launcher
    val systemRingtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                val title = ringtone?.getTitle(context) ?: "Custom System Alarm"
                selectedId = "system_custom_${System.currentTimeMillis()}"
                selectedTitle = title
                selectedUri = uri.toString()

                // Add to options list if not already present
                val customOption = RingtoneOption(
                    id = selectedId,
                    title = title,
                    subtitle = "System Custom Ringtone",
                    category = "Custom Sounds",
                    uriString = uri.toString(),
                    isCustom = true
                )
                availableOptions = listOf(customOption) + availableOptions

                // Automatically preview selected sound
                AlarmSoundManager.playPreview(context, customOption, previewVolume)
            }
        }
    }

    // Custom audio file picker launcher (.mp3, .wav, .ogg, .m4a)
    val audioFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "Custom Audio Track"
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }
            } catch (_: Exception) {}

            selectedId = "custom_file_${System.currentTimeMillis()}"
            selectedTitle = fileName
            selectedUri = uri.toString()

            val customOption = RingtoneOption(
                id = selectedId,
                title = fileName,
                subtitle = "Custom Audio File",
                category = "Custom Sounds",
                uriString = uri.toString(),
                isCustom = true
            )
            availableOptions = listOf(customOption) + availableOptions

            // Automatically preview the custom audio file
            AlarmSoundManager.playPreview(context, customOption, previewVolume)
        }
    }

    val filteredOptions = remember(availableOptions, selectedFilter) {
        when (selectedFilter) {
            "Transit Themes" -> availableOptions.filter { it.category == "Transit Themes" }
            "Device Alarms" -> availableOptions.filter { it.category == "Device Alarms" || it.category == "System Alarms" }
            else -> availableOptions
        }
    }

    Dialog(
        onDismissRequest = {
            AlarmSoundManager.stopAudio()
            onDismissRequest()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .testTag("ringtone_selection_dialog"),
            colors = CardDefaults.cardColors(containerColor = NightCardSurface),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, NightCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AlarmAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Audiotrack,
                                contentDescription = null,
                                tint = AlarmAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Wake-Up Ringtone",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Select or preview arrival alarm audio",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            AlarmSoundManager.stopAudio()
                            onDismissRequest()
                        },
                        modifier = Modifier.testTag("close_ringtone_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Audio Picker Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Ringtone")
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            }
                            systemRingtonePickerLauncher.launch(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_system_ringtone_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = TransitCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "System Picker",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            audioFilePickerLauncher.launch("audio/*")
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_audio_file_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AlarmAmber
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Audio File",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Transit Themes", "Device Alarms").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = { Text(text = filter, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // List of Ringtone Options
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredOptions, key = { it.id }) { option ->
                        val isSelected = selectedId == option.id
                        val isThisOptionPlaying = isPlaying && currentlyPlayingId == option.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedId = option.id
                                    selectedTitle = option.title
                                    selectedUri = option.uriString
                                }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) TransitCyan else NightCardBorder.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .testTag("ringtone_option_${option.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) TransitCyan.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedId = option.id
                                        selectedTitle = option.title
                                        selectedUri = option.uriString
                                    },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = TransitCyan,
                                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = option.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) TransitCyan else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (option.isCustom) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        AlarmAmber.copy(alpha = 0.2f),
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "CUSTOM",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AlarmAmber
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = option.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Preview button for this option
                                IconButton(
                                    onClick = {
                                        if (isThisOptionPlaying) {
                                            AlarmSoundManager.stopAudio()
                                        } else {
                                            AlarmSoundManager.playPreview(context, option, previewVolume)
                                        }
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isThisOptionPlaying) AlarmAmber.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .testTag("preview_button_${option.id}")
                                ) {
                                    if (isThisOptionPlaying) {
                                        RingtoneEqualizerBars(
                                            isPlaying = true,
                                            modifier = Modifier.size(16.dp),
                                            barColor = AlarmAmber
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Preview Tone",
                                            tint = if (isSelected) TransitCyan else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            AlarmSoundManager.stopAudio()
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            AlarmSoundManager.stopAudio()
                            onRingtoneSelected(selectedId, selectedTitle, selectedUri)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("apply_ringtone_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TransitCyan, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use Ringtone", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
