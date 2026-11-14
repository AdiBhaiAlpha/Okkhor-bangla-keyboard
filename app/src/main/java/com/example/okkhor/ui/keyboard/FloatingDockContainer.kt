package com.example.okkhor.ui.keyboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.okkhor.data.model.KeyboardTheme

/**
 * Shared floating glass-style dock container for Okkhor Bangla Keyboard.
 * Guarantees crisp unblurred foreground content, consistent translucent borders,
 * soft shadow, and responsive corner rounding inside keyboard bounds.
 */
@Composable
fun FloatingDockContainer(
    visible: Boolean,
    theme: KeyboardTheme,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 8.dp,
    cornerRadius: Dp = 20.dp,
    testTag: String = "floating_dock_container",
    content: @Composable ColumnScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(140)) + scaleIn(
            initialScale = 0.92f,
            animationSpec = spring(
                dampingRatio = 0.80f,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = fadeOut(animationSpec = tween(110)) + scaleOut(
            targetScale = 0.92f,
            animationSpec = tween(110)
        ),
        modifier = modifier
    ) {
        val dockShape = RoundedCornerShape(cornerRadius)
        val glassTop = Color(theme.popupBackgroundColor).copy(alpha = if (theme.isDark) 0.91f else 0.95f)
        val glassBottom = Color(theme.candidateBarBackgroundColor).copy(alpha = if (theme.isDark) 0.95f else 0.98f)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .shadow(14.dp, dockShape, clip = false)
                .clip(dockShape)
                .background(Brush.verticalGradient(listOf(glassTop, glassBottom)), dockShape)
                .border(
                    width = 1.dp,
                    color = Color(theme.accentColor).copy(alpha = 0.48f),
                    shape = dockShape
                )
                .padding(horizontal = 10.dp, vertical = 9.dp)
                .testTag(testTag),
            content = content
        )
    }
}
