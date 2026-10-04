package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ActionCardData
import com.example.data.model.ActionType
import com.example.ui.theme.SanaCyanPrimary
import com.example.ui.theme.SanaSurfaceElevated
import com.example.ui.theme.SanaSurfaceHighlight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ActionCardView(
    card: ActionCardData,
    onExecute: (ActionCardData) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isWhatsApp = card.type == ActionType.WHATSAPP_SEND || card.type == ActionType.WHATSAPP_OPEN
    val accentColor = if (isWhatsApp) Color(0xFF25D366) else SanaCyanPrimary

    val icon: ImageVector = when (card.type) {
        ActionType.WHATSAPP_SEND -> Icons.AutoMirrored.Filled.Send
        ActionType.WHATSAPP_OPEN -> Icons.AutoMirrored.Filled.Launch
        ActionType.CALL_PHONE -> Icons.Default.Call
        ActionType.SET_ALARM -> Icons.Default.Alarm
        ActionType.SET_REMINDER -> Icons.Default.Event
        ActionType.OPEN_APP -> Icons.AutoMirrored.Filled.Launch
        ActionType.WEB_SEARCH -> Icons.Default.Search
        ActionType.NAVIGATE -> Icons.Default.Navigation
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .testTag("action_card_${card.type.name}"),
        color = SanaSurfaceElevated,
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = card.title,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isWhatsApp) "WHATSAPP ACTION" else "ASSISTANT ACTION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = card.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (card.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SanaSurfaceHighlight)
                        .padding(12.dp)
                ) {
                    Text(
                        text = card.description,
                        fontSize = 14.sp,
                        color = TextPrimary,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    modifier = Modifier.testTag("action_cancel_button")
                ) {
                    Text("Cancel", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { onExecute(card) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (isWhatsApp) Color.White else Color.Black
                    ),
                    modifier = Modifier.testTag("action_execute_button")
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (card.type) {
                            ActionType.WHATSAPP_SEND -> "Send on WhatsApp"
                            ActionType.WHATSAPP_OPEN -> "Open WhatsApp"
                            ActionType.CALL_PHONE -> "Call"
                            ActionType.SET_ALARM -> "Set Alarm"
                            ActionType.SET_REMINDER -> "Set Reminder"
                            ActionType.OPEN_APP -> "Open App"
                            ActionType.WEB_SEARCH -> "Search"
                            ActionType.NAVIGATE -> "Navigate"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
