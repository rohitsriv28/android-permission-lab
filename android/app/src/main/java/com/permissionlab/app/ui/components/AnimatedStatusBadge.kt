package com.permissionlab.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.permissionlab.app.model.PermissionStatus
import com.permissionlab.app.ui.theme.StatusDenied
import com.permissionlab.app.ui.theme.StatusGranted
import com.permissionlab.app.ui.theme.StatusInactive
import com.permissionlab.app.ui.theme.StatusPartial

/**
 * Animated pill-shaped status badge with color transitions and pulse glow.
 */
@Composable
fun AnimatedStatusBadge(status: PermissionStatus) {
    val (text, color) = when (status) {
        PermissionStatus.GRANTED -> "Granted" to StatusGranted
        PermissionStatus.NOT_GRANTED -> "Not Granted" to StatusDenied
        PermissionStatus.PARTIAL -> "Partial" to StatusPartial
        PermissionStatus.NOT_CONNECTED -> "Inactive" to StatusInactive
    }

    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = tween(durationMillis = 400),
        label = "badgeColor"
    )

    // Pulse alpha for NOT_GRANTED status
    val pulseAlpha = if (status == PermissionStatus.NOT_GRANTED) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        alpha
    } else {
        1f
    }

    Box(
        modifier = Modifier
            .alpha(pulseAlpha)
            .border(
                width = 1.dp,
                color = animatedColor.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                color = animatedColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = animatedColor
        )
    }
}
