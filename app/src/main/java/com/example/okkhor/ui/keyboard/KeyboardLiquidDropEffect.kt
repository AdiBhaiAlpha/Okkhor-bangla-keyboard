package com.example.okkhor.ui.keyboard

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.okkhor.data.model.KeyboardTheme
import kotlin.math.hypot

class LiquidDropRipple(
    val id: Long,
    val keyId: String,
    val keyBounds: Rect,
    val impactPoint: Offset,
    val maxRadius: Float = computeLiquidMaxRadius(impactPoint, keyBounds),
    var startTimeNanos: Long = -1L,
    val durationNanos: Long = DEFAULT_DURATION_NANOS
) {
    companion object {
        const val DEFAULT_DURATION_NANOS = 360_000_000L
    }
}

internal fun clampPointToKeyBounds(point: Offset, bounds: Rect, insetPx: Float = 4f): Offset {
    if (bounds.width <= 0f || bounds.height <= 0f) return point
    val minX = bounds.left + minOf(insetPx, bounds.width * 0.25f)
    val maxX = bounds.right - minOf(insetPx, bounds.width * 0.25f)
    val minY = bounds.top + minOf(insetPx, bounds.height * 0.25f)
    val maxY = bounds.bottom - minOf(insetPx, bounds.height * 0.25f)
    return Offset(
        x = point.x.coerceIn(minX, maxX),
        y = point.y.coerceIn(minY, maxY)
    )
}

internal fun computeLiquidMaxRadius(impact: Offset, bounds: Rect): Float {
    val dTopLeft = hypot(impact.x - bounds.left, impact.y - bounds.top)
    val dTopRight = hypot(bounds.right - impact.x, impact.y - bounds.top)
    val dBottomLeft = hypot(impact.x - bounds.left, bounds.bottom - impact.y)
    val dBottomRight = hypot(bounds.right - impact.x, bounds.bottom - impact.y)
    val farthestCorner = maxOf(maxOf(dTopLeft, dTopRight), maxOf(dBottomLeft, dBottomRight))
    val wideKeyCap = maxOf(bounds.height * 2.25f, bounds.width * 0.54f)
    return minOf(farthestCorner * 1.04f, wideKeyCap).coerceAtLeast(24f)
}

private fun easeOutQuart(t: Float): Float {
    val inv = 1f - t.coerceIn(0f, 1f)
    val inv2 = inv * inv
    return 1f - inv2 * inv2
}

/**
 * Lightweight calm-water droplet impact & concentric capillary ripple overlay.
 * Mutates drop timestamps in-place without recomposing key composables or
 * allocating Path/Brush objects during frame rendering.
 */
@Composable
fun KeyboardLiquidDropOverlay(
    surfaceState: KeyboardSurfaceTouchState,
    theme: KeyboardTheme,
    cornerRadius: Dp,
    modifier: Modifier = Modifier
) {
    val hasActiveDrops = surfaceState.hasActiveLiquidDrops
    val frameTimeState = remember { mutableLongStateOf(0L) }
    val clipRoundPath = remember { Path() }

    LaunchedEffect(surfaceState, hasActiveDrops) {
        if (!hasActiveDrops) return@LaunchedEffect
        val drops = surfaceState.liquidDropsInternal
        while (drops.isNotEmpty()) {
            withFrameNanos { frameNanos ->
                frameTimeState.longValue = frameNanos
                var idx = 0
                while (idx < drops.size) {
                    val drop = drops[idx]
                    if (drop.startTimeNanos < 0L) {
                        drop.startTimeNanos = frameNanos
                        idx++
                    } else if (frameNanos - drop.startTimeNanos >= drop.durationNanos) {
                        drops.removeAt(idx)
                    } else {
                        idx++
                    }
                }
            }
        }
        surfaceState.markLiquidDropsIdle()
    }

    val accent = Color(theme.accentColor)
    val refractionColor = remember(accent, theme.isDark) {
        if (theme.isDark) {
            lerp(accent, Color.White, 0.36f)
        } else {
            lerp(accent, Color(0xFF0E2A47), 0.24f)
        }
    }
    val crestColor = remember(accent, theme.isDark) {
        if (theme.isDark) {
            Color.White
        } else {
            lerp(accent, Color.White, 0.68f)
        }
    }
    val peakSheenAlpha = if (theme.isDark) 0.12f else 0.09f
    val peakRefractionAlpha = if (theme.isDark) 0.20f else 0.16f
    val peakCrestAlpha = if (theme.isDark) 0.30f else 0.24f
    val peakTrailingAlpha = if (theme.isDark) 0.18f else 0.14f

    Canvas(
        modifier = if (hasActiveDrops) {
            modifier.testTag("keyboard_liquid_drop_overlay")
        } else {
            modifier
        }
    ) {
        if (!hasActiveDrops) return@Canvas
        val drops = surfaceState.liquidDropsInternal
        if (drops.isEmpty()) return@Canvas

        val nowNanos = frameTimeState.longValue
        val cornerPx = cornerRadius.toPx()

        for (i in 0 until drops.size) {
            val drop = drops[i]
            val bounds = drop.keyBounds
            if (bounds.width <= 1f || bounds.height <= 1f) continue

            val startNanos = if (drop.startTimeNanos < 0L) nowNanos else drop.startTimeNanos
            val elapsed = (nowNanos - startNanos).coerceAtLeast(0L)
            val progress = (elapsed.toFloat() / drop.durationNanos.toFloat()).coerceIn(0f, 1f)
            if (progress >= 1f) continue

            val center = drop.impactPoint
            val maxRadius = drop.maxRadius

            clipRoundPath.reset()
            clipRoundPath.addRoundRect(
                RoundRect(
                    rect = bounds,
                    cornerRadius = CornerRadius(cornerPx, cornerPx)
                )
            )

            clipPath(clipRoundPath) {
                // 1. Subtle droplet impact dimple & translucent surface sheen
                if (progress < 0.28f) {
                    val p0 = progress / 0.28f
                    val fade0 = (1f - p0) * (1f - p0)
                    val sheenRadius = (3f + 7f * easeOutQuart(p0)).dp.toPx()
                    drawCircle(
                        color = refractionColor.copy(alpha = fade0 * peakSheenAlpha),
                        radius = sheenRadius,
                        center = center
                    )
                    val beadRadius = (2.8f - 1.8f * p0).dp.toPx().coerceAtLeast(0.75f)
                    drawCircle(
                        color = crestColor.copy(alpha = fade0 * peakCrestAlpha * 0.85f),
                        radius = beadRadius,
                        center = center
                    )
                }

                // 2. Primary calm capillary ripple (soft refraction band + crisp meniscus crest)
                if (progress < 0.88f) {
                    val p1 = (progress / 0.88f).coerceIn(0f, 1f)
                    val r1 = 2.5f.dp.toPx() + (maxRadius - 2.5f.dp.toPx()) * easeOutQuart(p1)
                    val decay1 = (1f - p1) * (1f - p1)

                    val haloAlpha = decay1 * peakRefractionAlpha
                    if (haloAlpha > 0.01f) {
                        drawCircle(
                            color = refractionColor.copy(alpha = haloAlpha),
                            radius = r1,
                            center = center,
                            style = Stroke(width = (2.6f - 1.5f * p1).dp.toPx().coerceAtLeast(0.7f))
                        )
                    }

                    val crestAlpha = decay1 * peakCrestAlpha
                    if (crestAlpha > 0.01f) {
                        drawCircle(
                            color = crestColor.copy(alpha = crestAlpha),
                            radius = r1,
                            center = center,
                            style = Stroke(width = (1.1f - 0.6f * p1).dp.toPx().coerceAtLeast(0.5f))
                        )
                    }
                }

                // 3. Secondary trailing harmonic wavelet
                if (progress > 0.16f) {
                    val p2 = ((progress - 0.16f) / 0.84f).coerceIn(0f, 1f)
                    val r2 = 1.5f.dp.toPx() + (maxRadius * 0.70f) * easeOutQuart(p2)
                    val decay2 = (1f - p2) * (1f - p2)
                    val alpha2 = decay2 * peakTrailingAlpha

                    if (alpha2 > 0.01f) {
                        drawCircle(
                            color = refractionColor.copy(alpha = alpha2),
                            radius = r2,
                            center = center,
                            style = Stroke(width = (1.0f - 0.5f * p2).dp.toPx().coerceAtLeast(0.45f))
                        )
                    }
                }
            }
        }
    }
}
