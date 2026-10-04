package com.example.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CompanionMode
import com.example.data.model.VoiceState
import com.example.ui.components.ActionCardView
import com.example.ui.components.AnimatedVoiceOrb
import com.example.ui.components.ChatDrawerOrSheet
import com.example.ui.components.MemoryManagerDialog
import com.example.ui.components.VoiceSettingsDialog
import com.example.ui.theme.SanaBackground
import com.example.ui.theme.SanaCyanGlow
import com.example.ui.theme.SanaCyanPrimary
import com.example.ui.theme.SanaError
import com.example.ui.theme.SanaListeningGreen
import com.example.ui.theme.SanaRomanticGlow
import com.example.ui.theme.SanaRomanticRose
import com.example.ui.theme.SanaSurface
import com.example.ui.theme.SanaSurfaceElevated
import com.example.ui.theme.SanaSurfaceHighlight
import com.example.ui.theme.SanaVioletGlow
import com.example.ui.theme.SanaVioletSecondary
import com.example.ui.theme.SanaWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanaMainScreen(
    viewModel: SanaViewModel,
    modifier: Modifier = Modifier
) {
    val voiceState by viewModel.voiceState.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val companionMode by viewModel.companionMode.collectAsState()
    val languageOption by viewModel.languageOption.collectAsState()
    val isMemoryEnabled by viewModel.isMemoryEnabled.collectAsState()
    val isBackgroundVoiceEnabled by viewModel.isBackgroundVoiceEnabled.collectAsState()
    val statusText by viewModel.statusText.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val memories by viewModel.memories.collectAsState()
    val activeActionCard by viewModel.activeActionCard.collectAsState()
    val combinedAmplitude by viewModel.combinedAmplitude.collectAsState()
    val liveTranscript by viewModel.audioRecorder.partialTranscript.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMemoryDialog by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Microphone permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startListening()
        }
    }

    val isRomantic = companionMode == CompanionMode.ROMANTIC
    val accentColor = if (isRomantic) SanaRomanticRose else SanaCyanPrimary
    val glowColor = if (isRomantic) SanaRomanticGlow else SanaCyanGlow

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("sana_main_screen"),
        containerColor = SanaBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Title and Mode Badge
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SANA",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (voiceState) {
                                        VoiceState.LISTENING -> SanaListeningGreen
                                        VoiceState.SPEAKING -> glowColor
                                        VoiceState.THINKING -> SanaVioletGlow
                                        VoiceState.ERROR -> SanaError
                                        VoiceState.IDLE -> SanaCyanPrimary
                                    }
                                )
                        )
                    }

                    // Mode Badge Pill
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showSettingsDialog = true }
                            .padding(top = 2.dp),
                        color = accentColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = companionMode.badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Quick Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Voice Indicator pill
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showSettingsDialog = true }
                            .border(1.dp, SanaSurfaceHighlight, RoundedCornerShape(16.dp))
                            .testTag("top_voice_selector"),
                        color = SanaSurfaceElevated
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Voice",
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedVoice.displayName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Memory Button
                    IconButton(
                        onClick = { showMemoryDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SanaSurfaceElevated)
                            .testTag("top_memory_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Memory",
                            tint = if (memories.isNotEmpty()) SanaCyanPrimary else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Settings Button
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SanaSurfaceElevated)
                            .testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // CENTER HERO: ANIMATED VOICE ORB & STATUS
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Missing API Key Warning Banner if unconfigured
                if (errorMessage != null && errorMessage!!.contains("configured", ignoreCase = true)) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("api_key_warning_card"),
                        colors = CardDefaults.cardColors(containerColor = SanaSurfaceElevated),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SanaWarning.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Config Notice",
                                tint = SanaWarning,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gemini isn't configured yet.",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Please add the required API configuration in the Secrets panel in AI Studio.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                            IconButton(onClick = { viewModel.checkApiKeyStatus() }) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Retry", tint = SanaCyanPrimary)
                            }
                        }
                    }
                }

                // Voice Orb
                AnimatedVoiceOrb(
                    voiceState = voiceState,
                    companionMode = companionMode,
                    amplitude = combinedAmplitude,
                    onClick = {
                        if (viewModel.audioRecorder.hasRecordPermission()) {
                            viewModel.onOrbOrMicClicked()
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // State Pill Indicator
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(SanaSurfaceElevated, SanaSurfaceHighlight)
                            )
                        )
                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .testTag("voice_state_pill"),
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (voiceState) {
                            VoiceState.LISTENING -> {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SanaListeningGreen)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Listening... (Tap to finish)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SanaListeningGreen
                                )
                            }
                            VoiceState.THINKING -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(12.dp),
                                    strokeWidth = 2.dp,
                                    color = SanaVioletGlow
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Thinking...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SanaVioletGlow
                                )
                            }
                            VoiceState.SPEAKING -> {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(glowColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Speaking (Tap to interrupt)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor
                                )
                            }
                            VoiceState.ERROR -> {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SanaError)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tap to reconnect",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SanaError
                                )
                            }
                            VoiceState.IDLE -> {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(TextMuted)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tap to speak",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Live speech transcript preview
                AnimatedVisibility(
                    visible = liveTranscript.isNotBlank() && voiceState == VoiceState.LISTENING,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp),
                        colors = CardDefaults.cardColors(containerColor = SanaSurfaceElevated),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "\"$liveTranscript\"",
                            fontSize = 14.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Active Action Card (e.g. WhatsApp message confirmation)
                AnimatedVisibility(
                    visible = activeActionCard != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    activeActionCard?.let { card ->
                        Spacer(modifier = Modifier.height(12.dp))
                        ActionCardView(
                            card = card,
                            onExecute = { viewModel.executeAction(it) },
                            onDismiss = { viewModel.dismissActionCard() }
                        )
                    }
                }

                // Last spoken response preview in Voice Mode
                if (messages.isNotEmpty() && activeActionCard == null && liveTranscript.isBlank()) {
                    val lastMsg = messages.last()
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (lastMsg.isUser) "\"${lastMsg.text}\"" else lastMsg.text,
                        fontSize = 13.sp,
                        color = if (lastMsg.isUser) TextSecondary else TextPrimary,
                        maxLines = 3,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable { showChatSheet = true }
                    )
                }
            }

            // BOTTOM BAR: CONTROLS & CHIPS
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Personality Mode Selector Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(CompanionMode.values()) { mode ->
                        val isSelected = mode == companionMode
                        val chipColor = if (mode == CompanionMode.ROMANTIC) SanaRomanticRose else SanaCyanPrimary
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.setCompanionMode(mode) }
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) chipColor else SanaSurfaceHighlight,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .testTag("main_mode_chip_${mode.name.lowercase()}"),
                            color = if (isSelected) chipColor.copy(alpha = 0.15f) else SanaSurfaceElevated
                        ) {
                            Text(
                                text = mode.title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) chipColor else TextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Quick Prompt Chips
                val quickPills = listOf(
                    "Open WhatsApp",
                    "Hey SANA, how are you?",
                    "Send Ali: I'll call you later",
                    "Change voice to Aoede",
                    "What do you remember?",
                    "Sana, aaj kya kar rahi ho?"
                )

                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickPills) { pill ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { viewModel.sendTextMessage(pill) }
                                .border(1.dp, SanaSurfaceHighlight, RoundedCornerShape(16.dp)),
                            color = SanaSurface
                        ) {
                            Text(
                                text = pill,
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Main Action Buttons Row (Chat Sheet Toggle, Primary Mic Button, Interrupt)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Open Full Synchronized Chat Transcript
                    IconButton(
                        onClick = { showChatSheet = true },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(SanaSurfaceElevated)
                            .testTag("open_chat_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = "Chat Mode",
                            tint = SanaCyanPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Large Glowing Animated Mic Button
                    FloatingActionButton(
                        onClick = {
                            if (viewModel.audioRecorder.hasRecordPermission()) {
                                viewModel.onOrbOrMicClicked()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("main_mic_fab"),
                        shape = CircleShape,
                        containerColor = when (voiceState) {
                            VoiceState.LISTENING -> SanaListeningGreen
                            VoiceState.SPEAKING -> glowColor
                            VoiceState.THINKING -> SanaVioletGlow
                            VoiceState.ERROR -> SanaError
                            VoiceState.IDLE -> accentColor
                        },
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp)
                    ) {
                        Icon(
                            imageVector = when (voiceState) {
                                VoiceState.LISTENING -> Icons.Default.Mic
                                VoiceState.SPEAKING -> Icons.Default.Stop
                                VoiceState.THINKING -> Icons.Default.Stop
                                VoiceState.ERROR -> Icons.Default.Refresh
                                VoiceState.IDLE -> Icons.Default.Mic
                            },
                            contentDescription = "Microphone",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Voice switch shortcut (Kore <-> Aoede)
                    IconButton(
                        onClick = {
                            val nextVoice = if (selectedVoice == com.example.data.model.SanaVoice.KORE) {
                                com.example.data.model.SanaVoice.AOEDE
                            } else {
                                com.example.data.model.SanaVoice.KORE
                            }
                            viewModel.setVoice(nextVoice)
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(SanaSurfaceElevated)
                            .testTag("toggle_voice_quick_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Switch Voice",
                            tint = TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    // BOTTOM SHEET: SYNCHRONIZED CHAT & TEXT INPUT
    if (showChatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChatSheet = false },
            sheetState = sheetState,
            containerColor = SanaSurface,
            modifier = Modifier.testTag("chat_bottom_sheet")
        ) {
            ChatDrawerOrSheet(
                messages = messages,
                onSendMessage = { viewModel.sendTextMessage(it) },
                onExecuteAction = { viewModel.executeAction(it) },
                onDismissAction = { viewModel.dismissActionCard() },
                onClearChat = { viewModel.clearChat() },
                onVoiceClick = {
                    showChatSheet = false
                    if (viewModel.audioRecorder.hasRecordPermission()) {
                        viewModel.startListening()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onClose = { showChatSheet = false }
            )
        }
    }

    // DIALOG: SETTINGS (VOICE, MODE, LANGUAGE, BACKGROUND SERVICE)
    if (showSettingsDialog) {
        VoiceSettingsDialog(
            selectedVoice = selectedVoice,
            onVoiceSelected = { viewModel.setVoice(it) },
            onTestVoice = { viewModel.testVoice(it) },
            selectedMode = companionMode,
            onModeSelected = { viewModel.setCompanionMode(it) },
            selectedLanguage = languageOption,
            onLanguageSelected = { viewModel.setLanguage(it) },
            isBackgroundVoiceEnabled = isBackgroundVoiceEnabled,
            onToggleBackgroundVoice = { viewModel.toggleBackgroundVoice(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    // DIALOG: MEMORY MANAGER
    if (showMemoryDialog) {
        MemoryManagerDialog(
            memories = memories,
            isMemoryEnabled = isMemoryEnabled,
            onToggleMemory = { viewModel.toggleMemory(it) },
            onAddMemory = { cat, fact -> viewModel.addManualMemory(cat, fact) },
            onDeleteMemory = { viewModel.deleteMemory(it) },
            onClearAllMemories = { viewModel.clearAllMemories() },
            onDismiss = { showMemoryDialog = false }
        )
    }
}
