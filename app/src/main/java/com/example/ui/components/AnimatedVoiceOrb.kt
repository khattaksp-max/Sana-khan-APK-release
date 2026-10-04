package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CompanionMode
import com.example.data.model.VoiceState
import com.example.ui.theme.OrbGradientCenter
import com.example.ui.theme.OrbGradientEnd
import com.example.ui.theme.OrbGradientStart
import com.example.ui.theme.OrbRomanticEnd
import com.example.ui.theme.OrbRomanticStart
import com.example.ui.theme.SanaCyanGlow
import com.example.ui.theme.SanaCyanPrimary
import com.example.ui.theme.SanaListeningGreen
import com.example.ui.theme.SanaRomanticGlow
import com.example.ui.theme.SanaRomanticRose
import com.example.ui.theme.SanaVioletGlow
import com.example.ui.theme.SanaVioletSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedVoiceOrb(
    voiceState: VoiceState,
    companionMode: CompanionMode,
    amplitude: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "orb_anim")

    // Slow organic breathing
    val breathScale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )

    // Orbital ring rotation
    val rotationAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (voiceState) {
                    VoiceState.THINKING -> 2000
                    VoiceState.LISTENING -> 4500
                    VoiceState.SPEAKING -> 3500
                    else -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Particle ripple expansion
    val rippleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple"
    )

    // Determine colors based on mode & state
    val isRomantic = companionMode == CompanionMode.ROMANTIC

    val (coreColor1, coreColor2, glowColor) = when (voiceState) {
        VoiceState.LISTENING -> Triple(
            SanaListeningGreen,
            SanaCyanGlow,
            SanaListeningGreen.copy(alpha = 0.5f)
        )
        VoiceState.SPEAKING -> {
            if (isRomantic) {
                Triple(SanaRomanticRose, SanaRomanticGlow, SanaRomanticGlow.copy(alpha = 0.6f))
            } else {
                Triple(SanaVioletGlow, SanaCyanGlow, SanaCyanPrimary.copy(alpha = 0.6f))
            }
        }
        VoiceState.THINKING -> Triple(
            SanaVioletSecondary,
            SanaCyanPrimary,
            SanaVioletGlow.copy(alpha = 0.4f)
        )
        VoiceState.ERROR -> Triple(
            Color(0xFFF87171),
            Color(0xFFEF4444),
            Color(0x66EF4444)
        )
        VoiceState.IDLE -> {
            if (isRomantic) {
                Triple(OrbRomanticStart, OrbRomanticEnd, SanaRomanticGlow.copy(alpha = 0.35f))
            } else {
                Triple(OrbGradientStart, OrbGradientEnd, SanaCyanPrimary.copy(alpha = 0.35f))
            }
        }
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false, radius = 120.dp),
                onClick = onClick
            )
            .testTag("sana_voice_orb"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val baseRadius = size.minDimension / 4f

            // Dynamic amplitude amplification
            val ampFactor = (amplitude * 1.5f).coerceIn(0f, 1.2f)
            val activeRadius = baseRadius * breathScale * (1f + ampFactor * 0.4f)

            // 1. Ambient outer aura glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = center,
                    radius = activeRadius * 1.9f
                ),
                radius = activeRadius * 1.9f,
                center = center
            )

            // 2. Ripple waves when listening or speaking
            if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) {
                val rippleRadius = activeRadius + (activeRadius * 0.8f * rippleProgress)
                val rippleAlpha = (1f - rippleProgress) * 0.7f
                drawCircle(
                    color = coreColor1.copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
            }

            // 3. Orbital Rotating Wave Rings
            rotate(rotationAngle, pivot = center) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            coreColor1.copy(alpha = 0.2f),
                            coreColor2.copy(alpha = 0.8f),
                            coreColor1.copy(alpha = 0.2f)
                        ),
                        center = center
                    ),
                    radius = activeRadius * 1.35f,
                    center = center,
                    style = Stroke(width = 2f)
                )

                // Satellite energy nodes on orbital ring
                val nodeCount = 3
                for (i in 0 until nodeCount) {
                    val angle = (i * 2 * PI / nodeCount).toFloat()
                    val nodeRadius = activeRadius * 1.35f
                    val nodeCenter = Offset(
                        x = center.x + nodeRadius * cos(angle),
                        y = center.y + nodeRadius * sin(angle)
                    )
                    drawCircle(
                        color = coreColor2,
                        radius = 4.dp.toPx(),
                        center = nodeCenter
                    )
                }
            }

            // Counter-rotating inner ring
            rotate(-rotationAngle * 1.4f, pivot = center) {
                drawCircle(
                    color = coreColor2.copy(alpha = 0.45f),
                    radius = activeRadius * 1.15f,
                    center = center,
                    style = Stroke(width = 1.5f)
                )
            }

            // 4. Central Luminous Core
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        coreColor1.copy(alpha = 0.85f),
                        coreColor2.copy(alpha = 0.9f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = activeRadius
                ),
                radius = activeRadius,
                center = center
            )

            // 5. Soundwave bars inside core when speaking or listening
            if (voiceState == VoiceState.SPEAKING || voiceState == VoiceState.LISTENING) {
                val barCount = 5
                val barWidth = 3.dp.toPx()
                val totalWidth = barCount * 12.dp.toPx()
                val startX = center.x - (totalWidth / 2f)

                for (i in 0 until barCount) {
                    val progress = sin((rotationAngle * 0.1f) + i).toFloat()
                    val barHeight = (12.dp.toPx() + (30.dp.toPx() * (ampFactor + (progress * 0.2f).coerceAtLeast(0f))))
                        .coerceAtMost(activeRadius * 1.1f)

                    val x = startX + (i * 12.dp.toPx())
                    drawLine(
                        color = Color.White.copy(alpha = 0.9f),
                        start = Offset(x, center.y - (barHeight / 2f)),
                        end = Offset(x, center.y + (barHeight / 2f)),
                        strokeWidth = barWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
        }
    }
}
