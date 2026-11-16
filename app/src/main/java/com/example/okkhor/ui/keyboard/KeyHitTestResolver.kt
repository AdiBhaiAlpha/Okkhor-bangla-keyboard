package com.example.okkhor.ui.keyboard

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.BuildConfig
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class KeyHitCategory {
    LETTER,
    NUMBER_OR_SYMBOL,
    SHIFT,
    BACKSPACE,
    SYMBOL_MODE,
    EMOJI,
    LANGUAGE,
    SPACEBAR,
    ENTER
}

data class KeyHitTarget(
    val id: String,
    val category: KeyHitCategory,
    val rowIndex: Int,
    val bounds: Rect,
    val isBottomRow: Boolean = false,
    val isGlideEligible: Boolean = false
)

data class KeyHitToleranceConfig(
    val horizontalMarginPx: Float,
    val topMarginPx: Float,
    val bottomMarginPx: Float,
    val bottomRowExtraBottomMarginPx: Float,
    val standardKeyHalfWidthPx: Float,
    val rectEdgeTieThresholdPx: Float = 1.5f
) {
    companion object {
        fun fromDensity(
            density: Float,
            medianKeyWidthPx: Float = 0f
        ): KeyHitToleranceConfig {
            val safeDensity = density.coerceAtLeast(1f)
            val fallbackHalfW = 18f * safeDensity
            val halfW = if (medianKeyWidthPx > 0f) (medianKeyWidthPx * 0.5f) else fallbackHalfW
            return KeyHitToleranceConfig(
                horizontalMarginPx = 12f * safeDensity,
                topMarginPx = 6f * safeDensity,
                bottomMarginPx = 8f * safeDensity,
                bottomRowExtraBottomMarginPx = 10f * safeDensity,
                standardKeyHalfWidthPx = halfW,
                rectEdgeTieThresholdPx = max(1.5f, 0.75f * safeDensity)
            )
        }
    }
}

data class KeyHitResolution(
    val target: KeyHitTarget,
    val centerDistancePx: Float,
    val rectDistancePx: Float,
    val hitInsideVisualBounds: Boolean
)

/**
 * Pure geometry hit-test resolver for Okkhor keyboard key surfaces.
 *
 * Guarantees:
 * 1. Any touch strictly inside a key's visual bounds resolves directly to that key
 *    (expanded regions never steal touches clearly inside another visual key).
 * 2. Touches on exact edges, corners, inter-key gaps, staggered gutters, or ~1mm+ lower margins
 *    use density-independent forgiving expansion resolved by:
 *    - shortest distance to each key's visual rectangle (`rectDistancePx`), then
 *    - nearest key center (`centerDistancePx`, with horizontal spine clamping for wide keys
 *      like Spacebar so edge keys never steal Spacebar border taps) when boundary distances tie.
 * 3. Zero typed text or key labels representing user content are ever logged; debug diagnostics
 *    record only structural key IDs, coordinates, and distances when enabled in debug builds.
 */
object KeyHitTestResolver {

    private const val TAG = "OkkhorKeyHitTest"

    @Volatile
    var debugLoggingEnabled: Boolean = BuildConfig.DEBUG

    @Volatile
    var diagnosticListener: ((String, Offset, KeyHitResolution?) -> Unit)? = null

    fun resolveHit(
        point: Offset,
        targets: Collection<KeyHitTarget>,
        tolerances: KeyHitToleranceConfig,
        onlyCategories: Set<KeyHitCategory>? = null,
        phase: String = "DOWN"
    ): KeyHitResolution? {
        if (targets.isEmpty()) {
            emitDebug(phase, point, null)
            return null
        }

        // Pass 1: Strict visual interior hit (inclusive on left/top, strict < right/bottom
        // avoids double-claiming shared 0-gap borders; exact borders fall cleanly into Pass 2 Voronoi).
        var strictInnerHit: KeyHitTarget? = null
        for (target in targets) {
            if (onlyCategories != null && target.category !in onlyCategories) continue
            val b = target.bounds
            if (b.width <= 0f || b.height <= 0f) continue
            if (point.x > b.left && point.x < b.right && point.y > b.top && point.y < b.bottom) {
                strictInnerHit = target
                break
            }
        }
        if (strictInnerHit != null) {
            val res = KeyHitResolution(
                target = strictInnerHit,
                centerDistancePx = distanceToAnchor(point, strictInnerHit.bounds, tolerances.standardKeyHalfWidthPx),
                rectDistancePx = 0f,
                hitInsideVisualBounds = true
            )
            emitDebug(phase, point, res)
            return res
        }

        // Pass 2: Expanded forgiving region + rect-distance & nearest-center Voronoi arbitration.
        var bestTarget: KeyHitTarget? = null
        var bestRectDist = Float.MAX_VALUE
        var bestAnchorDist = Float.MAX_VALUE
        var bestInVisualEdge = false

        val tieThreshold = tolerances.rectEdgeTieThresholdPx

        for (target in targets) {
            if (onlyCategories != null && target.category !in onlyCategories) continue
            val b = target.bounds
            if (b.width <= 0f || b.height <= 0f) continue

            val bottomMargin = if (target.isBottomRow) {
                tolerances.bottomMarginPx + tolerances.bottomRowExtraBottomMarginPx
            } else {
                tolerances.bottomMarginPx
            }

            val expLeft = b.left - tolerances.horizontalMarginPx
            val expTop = b.top - tolerances.topMarginPx
            val expRight = b.right + tolerances.horizontalMarginPx
            val expBottom = b.bottom + bottomMargin

            if (point.x < expLeft || point.x > expRight || point.y < expTop || point.y > expBottom) {
                continue
            }

            val dxRect = max(0f, max(b.left - point.x, point.x - b.right))
            val dyRect = max(0f, max(b.top - point.y, point.y - b.bottom))
            val rectDist = if (dxRect == 0f && dyRect == 0f) 0f else hypot(dxRect, dyRect)
            val anchorDist = distanceToAnchor(point, b, tolerances.standardKeyHalfWidthPx)

            val isBetter = when {
                bestTarget == null -> true
                rectDist < bestRectDist - tieThreshold -> true
                abs(rectDist - bestRectDist) <= tieThreshold -> anchorDist < bestAnchorDist
                else -> false
            }

            if (isBetter) {
                bestTarget = target
                bestRectDist = rectDist
                bestAnchorDist = anchorDist
                bestInVisualEdge = (dxRect == 0f && dyRect == 0f)
            }
        }

        val resolution = bestTarget?.let {
            KeyHitResolution(
                target = it,
                centerDistancePx = bestAnchorDist,
                rectDistancePx = bestRectDist,
                hitInsideVisualBounds = bestInVisualEdge
            )
        }
        emitDebug(phase, point, resolution)
        return resolution
    }

    /**
     * Computes Euclidean distance to the key's center for standard 1u keys, and to the
     * nearest point on the horizontal center spine for wider keys (e.g., Spacebar, Shift, Enter)
     * so adjacent 1u keys cannot steal taps at the outer edges of a wide bar.
     */
    fun distanceToAnchor(point: Offset, bounds: Rect, standardHalfWidthPx: Float): Float {
        val anchorX = if (bounds.width > standardHalfWidthPx * 2.9f) {
            val inset = min(bounds.width * 0.5f, standardHalfWidthPx)
            point.x.coerceIn(bounds.left + inset, bounds.right - inset)
        } else {
            bounds.center.x
        }
        val anchorY = bounds.center.y
        return hypot(point.x - anchorX, point.y - anchorY)
    }

    fun computeMedianKeyWidth(targets: Collection<KeyHitTarget>): Float {
        if (targets.isEmpty()) return 0f
        val widths = targets
            .filter { it.category == KeyHitCategory.LETTER || it.category == KeyHitCategory.NUMBER_OR_SYMBOL }
            .map { it.bounds.width }
            .filter { it > 0f }
            .sorted()
        if (widths.isEmpty()) return 0f
        return widths[widths.size / 2]
    }

    private fun emitDebug(phase: String, point: Offset, resolution: KeyHitResolution?) {
        diagnosticListener?.invoke(phase, point, resolution)
        if (!debugLoggingEnabled) return
        runCatching {
            if (resolution != null) {
                Log.d(
                    TAG,
                    "phase=$phase pt=(${point.x.toInt()},${point.y.toInt()}) -> id=${resolution.target.id} " +
                        "cat=${resolution.target.category} row=${resolution.target.rowIndex} " +
                        "inside=${resolution.hitInsideVisualBounds} rectDist=${"%.1f".format(resolution.rectDistancePx)} " +
                        "centerDist=${"%.1f".format(resolution.centerDistancePx)}"
                )
            } else {
                Log.d(TAG, "phase=$phase pt=(${point.x.toInt()},${point.y.toInt()}) -> MISS")
            }
        }
    }
}
