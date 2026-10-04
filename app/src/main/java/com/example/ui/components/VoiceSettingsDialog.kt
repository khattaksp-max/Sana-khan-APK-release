package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CompanionMode
import com.example.data.model.LanguageOption
import com.example.data.model.SanaVoice
import com.example.ui.theme.SanaCyanPrimary
import com.example.ui.theme.SanaRomanticRose
import com.example.ui.theme.SanaSurface
import com.example.ui.theme.SanaSurfaceElevated
import com.example.ui.theme.SanaSurfaceHighlight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun VoiceSettingsDialog(
    selectedVoice: SanaVoice,
    onVoiceSelected: (SanaVoice) -> Unit,
    onTestVoice: (SanaVoice) -> Unit,
    selectedMode: CompanionMode,
    onModeSelected: (CompanionMode) -> Unit,
    selectedLanguage: LanguageOption,
    onLanguageSelected: (LanguageOption) -> Unit,
    isBackgroundVoiceEnabled: Boolean,
    onToggleBackgroundVoice: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, SanaCyanPrimary.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                .testTag("voice_settings_dialog"),
            colors = CardDefaults.cardColors(containerColor = SanaSurfaceElevated)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SANA Settings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Voice, Personality & Language",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 1: VOICE SELECTION
                Text(
                    text = "NATIVE GEMINI VOICE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SanaCyanPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                SanaVoice.values().forEach { voice ->
                    val isSelected = voice == selectedVoice
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) SanaCyanPrimary else SanaSurfaceHighlight,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onVoiceSelected(voice) }
                            .testTag("voice_option_${voice.voiceId.lowercase()}"),
                        color = if (isSelected) SanaSurfaceHighlight else SanaSurface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) SanaCyanPrimary else Color.Transparent)
                                        .border(
                                            1.5.dp,
                                            if (isSelected) SanaCyanPrimary else TextMuted,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = voice.displayName,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = voice.description,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = {
                                    onVoiceSelected(voice)
                                    onTestVoice(voice)
                                },
                                modifier = Modifier.testTag("test_voice_${voice.voiceId.lowercase()}"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SanaCyanPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test", fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 2: COMPANION MODE
                Text(
                    text = "PERSONALITY & COMPANION MODE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SanaRomanticRose,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                CompanionMode.values().forEach { mode ->
                    val isSelected = mode == selectedMode
                    val activeColor = if (mode == CompanionMode.ROMANTIC) SanaRomanticRose else SanaCyanPrimary

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) activeColor else SanaSurfaceHighlight,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable { onModeSelected(mode) }
                            .testTag("mode_option_${mode.name.lowercase()}"),
                        color = if (isSelected) SanaSurfaceHighlight else SanaSurface
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) activeColor else Color.Transparent)
                                    .border(
                                        1.5.dp,
                                        if (isSelected) activeColor else TextMuted,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = mode.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) activeColor else TextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mode.badge,
                                        fontSize = 10.sp,
                                        color = activeColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    text = mode.subtitle,
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 3: LANGUAGE SUPPORT
                Text(
                    text = "LANGUAGE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SanaCyanPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LanguageOption.values().take(2).forEach { lang ->
                        val isSelected = lang == selectedLanguage
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) SanaCyanPrimary else SanaSurfaceHighlight,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onLanguageSelected(lang) }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            color = if (isSelected) SanaSurfaceHighlight else SanaSurface
                        ) {
                            Text(
                                text = lang.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SanaCyanPrimary else TextPrimary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LanguageOption.values().drop(2).forEach { lang ->
                        val isSelected = lang == selectedLanguage
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) SanaCyanPrimary else SanaSurfaceHighlight,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onLanguageSelected(lang) }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            color = if (isSelected) SanaSurfaceHighlight else SanaSurface
                        ) {
                            Text(
                                text = lang.displayName,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) SanaCyanPrimary else TextPrimary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 4: BACKGROUND VOICE INTERACTION
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(SanaSurface)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Voice Service",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Continue voice interaction when app is minimized (shows privacy notification)",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = isBackgroundVoiceEnabled,
                        onCheckedChange = onToggleBackgroundVoice,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SanaCyanPrimary,
                            checkedTrackColor = SanaSurfaceHighlight
                        ),
                        modifier = Modifier.testTag("background_voice_switch")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_done_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SanaCyanPrimary, contentColor = Color.Black)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}
