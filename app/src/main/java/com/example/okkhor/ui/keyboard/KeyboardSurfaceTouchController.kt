package com.example.okkhor.ui.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.okkhor.benchmark.TypingPerformanceTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

data class RegisteredKeyAction(
    val target: KeyHitTarget,
    val letterLookupKey: String? = null,
    val onTap: () -> Unit,
    val onLongPress: (() -> Unit)? = null,
    val onSwipeLeft: (() -> Unit)? = null
)

enum class SpacebarTouchMode {
    NONE,
    LANGUAGE_SWITCH,
    CURSOR_SWIPE,
    CANCELLED
}

private class ActiveKeyPointerSession(
    val pointerId: PointerId,
    val target: KeyHitTarget,
    val downPos: Offset,
    var longPressJob: Job? = null,
    var repeatJob: Job? = null,
    var cursorHoldJob: Job? = null,
    var longPressTriggered: Boolean = false,
    var movedBeyondSlop: Boolean = false,
    var cancelled: Boolean = false,
    var dockOpened: Boolean = false,
    var shiftDragging: Boolean = false,
    var backspaceDragX: Float = 0f,
    var backspaceSwiped: Boolean = false,
    var spaceMode: SpacebarTouchMode = SpacebarTouchMode.NONE,
    var accumulatedCursorDrag: Float = 0f,
    var isGliding: Boolean = false,
    val glideVisited: MutableList<String> = mutableListOf(),
    val glidePoints: MutableList<Offset> = mutableListOf()
)

class KeyboardSurfaceTouchState {
    companion object {
        const val MAX_ACTIVE_LIQUID_DROPS = 6
    }

    val actionsById = mutableMapOf<String, RegisteredKeyAction>()
    val coordsById = mutableMapOf<String, LayoutCoordinates>()
    private val ownerTokenById = mutableMapOf<String, Any>()
    val pressedKeyIds = mutableStateMapOf<String, Int>()
    internal val liquidDropsInternal = ArrayList<LiquidDropRipple>(8)
    val liquidDrops: List<LiquidDropRipple> get() = liquidDropsInternal
    var hasActiveLiquidDrops by mutableStateOf(false)
        private set

    var surfaceCoords: LayoutCoordinates? = null
    private var nextDropId: Long = 1L
    private var cachedTargets: List<KeyHitTarget>? = null
    private var cachedMedianWidth: Float = 0f

    internal fun snapshotTargetsAndMedianWidth(): Pair<List<KeyHitTarget>, Float> {
        val existing = cachedTargets
        if (existing != null) return existing to cachedMedianWidth
        val rebuilt = actionsById.values.map { it.target }
        val median = KeyHitTestResolver.computeMedianKeyWidth(rebuilt)
        cachedTargets = rebuilt
        cachedMedianWidth = median
        return rebuilt to median
    }

    private fun invalidateTargetCache() {
        cachedTargets = null
    }

    fun claimKeyOwner(id: String, token: Any) {
        ownerTokenById[id] = token
    }

    fun registerKeyLayout(
        id: String,
        coords: LayoutCoordinates,
        targetTemplate: KeyHitTarget,
        letterLookupKey: String?,
        onTap: () -> Unit,
        onLongPress: (() -> Unit)?,
        onSwipeLeft: (() -> Unit)?
    ) {
        coordsById[id] = coords
        val surface = surfaceCoords
        val localBounds = if (coords.isAttached && surface != null && surface.isAttached) {
            surface.localBoundingBoxOf(coords, clipBounds = false)
        } else {
            Rect.Zero
        }
        actionsById[id] = RegisteredKeyAction(
            target = targetTemplate.copy(bounds = localBounds),
            letterLookupKey = letterLookupKey,
            onTap = onTap,
            onLongPress = onLongPress,
            onSwipeLeft = onSwipeLeft
        )
        invalidateTargetCache()
    }

    fun updateKeyCallbacks(
        id: String,
        letterLookupKey: String?,
        onTap: () -> Unit,
        onLongPress: (() -> Unit)?,
        onSwipeLeft: (() -> Unit)?
    ) {
        val existing = actionsById[id] ?: return
        actionsById[id] = existing.copy(
            letterLookupKey = letterLookupKey,
            onTap = onTap,
            onLongPress = onLongPress,
            onSwipeLeft = onSwipeLeft
        )
    }

    fun recomputeAllBounds(surface: LayoutCoordinates) {
        surfaceCoords = surface
        if (!surface.isAttached) return
        val keys = coordsById.keys.toList()
        for (id in keys) {
            val coords = coordsById[id] ?: continue
            val existing = actionsById[id] ?: continue
            if (coords.isAttached) {
                val localBounds = surface.localBoundingBoxOf(coords, clipBounds = false)
                actionsById[id] = existing.copy(target = existing.target.copy(bounds = localBounds))
            }
        }
        invalidateTargetCache()
    }

    fun unregisterKeyIfOwner(id: String, token: Any) {
        if (ownerTokenById[id] !== token) return
        ownerTokenById.remove(id)
        unregisterKey(id)
    }

    fun unregisterKey(id: String) {
        coordsById.remove(id)
        actionsById.remove(id)
        ownerTokenById.remove(id)
        pressedKeyIds.remove(id)
        invalidateTargetCache()
    }

    fun resetTransientState() {
        pressedKeyIds.clear()
        liquidDropsInternal.clear()
        hasActiveLiquidDrops = false
    }

    fun clearAll() {
        coordsById.clear()
        actionsById.clear()
        ownerTokenById.clear()
        invalidateTargetCache()
        resetTransientState()
    }

    fun triggerLiquidDrop(keyId: String, keyBounds: Rect, rawTouchPoint: Offset) {
        if (keyBounds.width <= 1f || keyBounds.height <= 1f) return
        val clampedImpact = clampPointToKeyBounds(rawTouchPoint, keyBounds)
        if (liquidDropsInternal.size >= MAX_ACTIVE_LIQUID_DROPS) {
            liquidDropsInternal.removeAt(0)
        }
        liquidDropsInternal.add(
            LiquidDropRipple(
                id = nextDropId++,
                keyId = keyId,
                keyBounds = keyBounds,
                impactPoint = clampedImpact
            )
        )
        if (!hasActiveLiquidDrops) {
            hasActiveLiquidDrops = true
        }
    }

    internal fun markLiquidDropsIdle() {
        if (liquidDropsInternal.isEmpty()) {
            hasActiveLiquidDrops = false
        }
    }

    fun setPressed(id: String, pressed: Boolean) {
        val curr = pressedKeyIds[id] ?: 0
        if (pressed) {
            pressedKeyIds[id] = curr + 1
        } else {
            if (curr <= 1) pressedKeyIds.remove(id) else pressedKeyIds[id] = curr - 1
        }
    }

    fun isPressed(id: String): Boolean = (pressedKeyIds[id] ?: 0) > 0
}

@Composable
fun rememberKeyboardSurfaceTouchState(): KeyboardSurfaceTouchState =
    remember { KeyboardSurfaceTouchState() }

fun Modifier.keyboardSurfaceTouchDispatcher(
    key: Any?,
    surfaceState: KeyboardSurfaceTouchState,
    coroutineScope: CoroutineScope,
    density: Density,
    isOverlayBlockingTouch: () -> Boolean,
    glideEnabled: () -> Boolean,
    spacebarCursorSwipeEnabled: () -> Boolean,
    spacebarLanguageSwipeEnabled: () -> Boolean,
    onSpaceCursorMove: (Int) -> Unit,
    onSpaceLanguageStart: (Float) -> Unit,
    onSpaceLanguageUpdate: (Float) -> Unit,
    onSpaceLanguageCommit: () -> Unit,
    onSpaceLanguageCancel: () -> Unit,
    onSpaceCursorHoldStateChanged: (Boolean) -> Unit,
    onDockDragRoot: (KeyHitCategory, Offset) -> Unit,
    onDockRelease: (KeyHitCategory) -> Unit,
    onDockCancel: (KeyHitCategory) -> Unit,
    onShiftDragTarget: (String?) -> Unit,
    onShiftDragRelease: () -> Unit,
    onShiftDragCancel: () -> Unit,
    onGlideTrailChanged: (List<Offset>) -> Unit,
    onCommitGlidePath: (List<String>) -> Unit
): Modifier = this.pointerInput(key) {
    val slop = viewConfiguration.touchSlop
    val longPressMs = viewConfiguration.longPressTimeoutMillis
    val releaseSlopPx = maxOf(slop * 2.5f, with(density) { 26.dp.toPx() })
    val glideStartDistPx = maxOf(slop * 3.0f, with(density) { 28.dp.toPx() })
    val swipeActivatePx = maxOf(slop * 2.4f, with(density) { 20.dp.toPx() })
    val verticalJitterPx = maxOf(slop * 2.5f, with(density) { 22.dp.toPx() })
    val cancelYUpPx = with(density) { 110.dp.toPx() }
    val cancelYDownPx = with(density) { 64.dp.toPx() }
    val cursorHoldDelayMs = 340L
    val cursorStepPx = 32f

    val activeSessions = LinkedHashMap<PointerId, ActiveKeyPointerSession>()

    fun cancelExclusiveSessionGestures(excludeId: PointerId?) {
        for ((pid, session) in activeSessions) {
            if (pid == excludeId) continue
            session.longPressJob?.cancel()
            session.cursorHoldJob?.cancel()
            if (session.isGliding) {
                session.isGliding = false
                session.cancelled = true
                onGlideTrailChanged(emptyList())
            }
            if (session.target.category == KeyHitCategory.SPACEBAR) {
                if (session.spaceMode == SpacebarTouchMode.LANGUAGE_SWITCH) {
                    onSpaceLanguageCancel()
                }
                if (session.spaceMode == SpacebarTouchMode.CURSOR_SWIPE) {
                    onSpaceCursorHoldStateChanged(false)
                }
                session.spaceMode = SpacebarTouchMode.CANCELLED
            }
            if (session.shiftDragging) {
                session.shiftDragging = false
                session.cancelled = true
                onShiftDragCancel()
            }
            if (session.dockOpened) {
                session.dockOpened = false
                session.cancelled = true
                onDockCancel(session.target.category)
            }
        }
    }

    fun isStillNearLockedTarget(pos: Offset, target: KeyHitTarget, tolerances: KeyHitToleranceConfig): Boolean {
        val b = target.bounds
        val extraMargin = with(density) { 18.dp.toPx() }
        val dx = max(0f, max(b.left - pos.x, pos.x - b.right))
        val dy = max(0f, max(b.top - pos.y, pos.y - b.bottom))
        return hypot(dx, dy) <= max(extraMargin, tolerances.horizontalMarginPx)
    }

    try {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (isOverlayBlockingTouch() && activeSessions.isEmpty()) {
                continue
            }

            val (targets, medianW) = surfaceState.snapshotTargetsAndMedianWidth()
            val tolerances = KeyHitToleranceConfig.fromDensity(density.density, medianW)

            for (change in event.changes) {
                // 1. Pointer DOWN
                if (change.changedToDownIgnoreConsumed()) {
                    if (isOverlayBlockingTouch()) continue
                    val resolution = KeyHitTestResolver.resolveHit(
                        point = change.position,
                        targets = targets,
                        tolerances = tolerances,
                        phase = "DOWN"
                    )
                    if (resolution == null) {
                        TypingPerformanceTracker.recordMissedTouch()
                        continue
                    }

                    val target = resolution.target
                    val action = surfaceState.actionsById[target.id] ?: continue

                    // If another finger is already down, cancel any exclusive gesture cleanly so roll-over typing stays responsive
                    if (activeSessions.isNotEmpty()) {
                        cancelExclusiveSessionGestures(excludeId = change.id)
                    }

                    val session = ActiveKeyPointerSession(
                        pointerId = change.id,
                        target = target,
                        downPos = change.position
                    )
                    activeSessions[change.id] = session
                    change.consume()

                    // Dispatch Backspace input first so deletion never waits behind visual work
                    if (target.category == KeyHitCategory.BACKSPACE) {
                        action.onTap()
                        session.repeatJob = coroutineScope.launch {
                            delay(380L)
                            while (isActive && !session.backspaceSwiped && !session.cancelled) {
                                surfaceState.actionsById[target.id]?.onTap?.invoke()
                                delay(55L)
                            }
                        }
                    }

                    surfaceState.setPressed(target.id, true)
                    surfaceState.triggerLiquidDrop(
                        keyId = target.id,
                        keyBounds = target.bounds,
                        rawTouchPoint = change.position
                    )

                    when (target.category) {
                        KeyHitCategory.BACKSPACE -> Unit

                        KeyHitCategory.LETTER, KeyHitCategory.NUMBER_OR_SYMBOL -> {
                            if (action.onLongPress != null) {
                                session.longPressJob = coroutineScope.launch {
                                    delay(longPressMs)
                                    if (!session.movedBeyondSlop && !session.cancelled && !session.isGliding) {
                                        session.longPressTriggered = true
                                        surfaceState.actionsById[target.id]?.onLongPress?.invoke()
                                    }
                                }
                            }
                            if (glideEnabled() && target.isGlideEligible && activeSessions.size == 1) {
                                session.glideVisited.add(action.letterLookupKey ?: target.id)
                                session.glidePoints.add(change.position)
                            }
                        }

                        KeyHitCategory.EMOJI, KeyHitCategory.SYMBOL_MODE -> {
                            if (action.onLongPress != null) {
                                session.longPressJob = coroutineScope.launch {
                                    delay(longPressMs)
                                    if (!session.movedBeyondSlop && !session.cancelled) {
                                        session.dockOpened = true
                                        surfaceState.actionsById[target.id]?.onLongPress?.invoke()
                                    }
                                }
                            }
                        }

                        KeyHitCategory.SPACEBAR -> {
                            if (spacebarCursorSwipeEnabled() && activeSessions.size == 1) {
                                session.cursorHoldJob = coroutineScope.launch {
                                    delay(cursorHoldDelayMs)
                                    if (session.spaceMode == SpacebarTouchMode.NONE && !session.cancelled) {
                                        session.spaceMode = SpacebarTouchMode.CURSOR_SWIPE
                                        onSpaceCursorHoldStateChanged(true)
                                    }
                                }
                            }
                        }

                        KeyHitCategory.SHIFT, KeyHitCategory.LANGUAGE, KeyHitCategory.ENTER -> Unit
                    }
                    continue
                }

                val session = activeSessions[change.id] ?: continue
                val target = session.target
                val action = surfaceState.actionsById[target.id]

                // 2. Pointer UP or Cancel
                if (change.changedToUpIgnoreConsumed() || !change.pressed) {
                    session.longPressJob?.cancel()
                    session.repeatJob?.cancel()
                    session.cursorHoldJob?.cancel()
                    activeSessions.remove(change.id)
                    if (!session.isGliding) {
                        surfaceState.setPressed(target.id, false)
                    }

                    val distFromDown = (change.position - session.downPos).getDistance()

                    when (target.category) {
                        KeyHitCategory.BACKSPACE -> {
                            // Already deleted immediately on DOWN
                            change.consume()
                        }

                        KeyHitCategory.LETTER, KeyHitCategory.NUMBER_OR_SYMBOL -> {
                            if (session.isGliding) {
                                change.consume()
                                onCommitGlidePath(session.glideVisited.toList())
                                onGlideTrailChanged(emptyList())
                            } else if (!session.longPressTriggered && !session.cancelled) {
                                val withinRelease = distFromDown <= releaseSlopPx ||
                                    isStillNearLockedTarget(change.position, target, tolerances)
                                if (withinRelease) {
                                    action?.onTap?.invoke()
                                    change.consume()
                                } else {
                                    TypingPerformanceTracker.recordMissedTouch()
                                }
                            }
                        }

                        KeyHitCategory.SHIFT -> {
                            if (session.shiftDragging) {
                                onShiftDragRelease()
                                change.consume()
                            } else if (!session.cancelled &&
                                (distFromDown <= releaseSlopPx || isStillNearLockedTarget(change.position, target, tolerances))
                            ) {
                                action?.onTap?.invoke()
                                change.consume()
                            }
                        }

                        KeyHitCategory.EMOJI, KeyHitCategory.SYMBOL_MODE -> {
                            if (session.dockOpened) {
                                onDockRelease(target.category)
                                change.consume()
                            } else if (!session.cancelled &&
                                (distFromDown <= releaseSlopPx || isStillNearLockedTarget(change.position, target, tolerances))
                            ) {
                                action?.onTap?.invoke()
                                change.consume()
                            }
                        }

                        KeyHitCategory.SPACEBAR -> {
                            when (session.spaceMode) {
                                SpacebarTouchMode.NONE -> {
                                    if (!session.cancelled &&
                                        (distFromDown <= releaseSlopPx || isStillNearLockedTarget(change.position, target, tolerances))
                                    ) {
                                        action?.onTap?.invoke()
                                        change.consume()
                                    }
                                }
                                SpacebarTouchMode.LANGUAGE_SWITCH -> {
                                    onSpaceLanguageCommit()
                                    change.consume()
                                }
                                SpacebarTouchMode.CURSOR_SWIPE -> {
                                    onSpaceCursorHoldStateChanged(false)
                                    change.consume()
                                }
                                SpacebarTouchMode.CANCELLED -> Unit
                            }
                        }

                        KeyHitCategory.LANGUAGE, KeyHitCategory.ENTER -> {
                            if (!session.cancelled &&
                                (distFromDown <= releaseSlopPx || isStillNearLockedTarget(change.position, target, tolerances))
                            ) {
                                action?.onTap?.invoke()
                                change.consume()
                            }
                        }
                    }
                    continue
                }

                // 3. Pointer MOVE while pressed
                val distFromDown = (change.position - session.downPos).getDistance()
                when (target.category) {
                    KeyHitCategory.BACKSPACE -> {
                        val dx = change.position.x - change.previousPosition.x
                        session.backspaceDragX += dx
                        if (session.backspaceDragX < -45f && !session.backspaceSwiped && action?.onSwipeLeft != null) {
                            session.backspaceSwiped = true
                            session.repeatJob?.cancel()
                            action.onSwipeLeft.invoke()
                            session.backspaceDragX = 0f
                            change.consume()
                        }
                    }

                    KeyHitCategory.SHIFT -> {
                        if (!session.shiftDragging && distFromDown > slop && activeSessions.size == 1) {
                            session.shiftDragging = true
                        }
                        if (session.shiftDragging) {
                            change.consume()
                            val hit = KeyHitTestResolver.resolveHit(
                                point = change.position,
                                targets = targets,
                                tolerances = tolerances,
                                onlyCategories = setOf(KeyHitCategory.LETTER),
                                phase = "SHIFT_DRAG"
                            )
                            val lookup = hit?.let { surfaceState.actionsById[it.target.id]?.letterLookupKey ?: it.target.id }
                            onShiftDragTarget(lookup)
                        }
                    }

                    KeyHitCategory.EMOJI, KeyHitCategory.SYMBOL_MODE -> {
                        if (!session.dockOpened) {
                            if (distFromDown > slop) {
                                session.movedBeyondSlop = true
                                session.longPressJob?.cancel()
                            }
                        } else {
                            change.consume()
                            val surface = surfaceState.surfaceCoords
                            if (surface != null && surface.isAttached) {
                                val rootPos = surface.localToRoot(change.position)
                                onDockDragRoot(target.category, rootPos)
                            }
                        }
                    }

                    KeyHitCategory.SPACEBAR -> {
                        val dx = change.position.x - change.previousPosition.x
                        val netDx = change.position.x - session.downPos.x
                        val netDy = change.position.y - session.downPos.y

                        when (session.spaceMode) {
                            SpacebarTouchMode.NONE -> {
                                if (abs(netDx) > slop || abs(netDy) > slop) {
                                    session.cursorHoldJob?.cancel()
                                }
                                if (abs(netDy) > abs(netDx) * 1.6f && abs(netDy) > verticalJitterPx) {
                                    session.spaceMode = SpacebarTouchMode.CANCELLED
                                } else if (spacebarLanguageSwipeEnabled() && abs(netDx) > swipeActivatePx) {
                                    session.spaceMode = SpacebarTouchMode.LANGUAGE_SWITCH
                                    change.consume()
                                    onSpaceLanguageStart(netDx)
                                } else if (!spacebarLanguageSwipeEnabled() && spacebarCursorSwipeEnabled() && abs(netDx) > swipeActivatePx) {
                                    session.spaceMode = SpacebarTouchMode.CURSOR_SWIPE
                                    change.consume()
                                    session.accumulatedCursorDrag += dx
                                }
                            }
                            SpacebarTouchMode.LANGUAGE_SWITCH -> {
                                change.consume()
                                if (netDy < -cancelYUpPx || netDy > cancelYDownPx) {
                                    onSpaceLanguageCancel()
                                    session.spaceMode = SpacebarTouchMode.CANCELLED
                                } else {
                                    onSpaceLanguageUpdate(netDx)
                                }
                            }
                            SpacebarTouchMode.CURSOR_SWIPE -> {
                                change.consume()
                                session.accumulatedCursorDrag += dx
                                while (abs(session.accumulatedCursorDrag) >= cursorStepPx) {
                                    val dir = if (session.accumulatedCursorDrag > 0f) 1 else -1
                                    onSpaceCursorMove(dir)
                                    session.accumulatedCursorDrag -= dir * cursorStepPx
                                }
                            }
                            SpacebarTouchMode.CANCELLED -> {
                                change.consume()
                            }
                        }
                    }

                    KeyHitCategory.LETTER, KeyHitCategory.NUMBER_OR_SYMBOL -> {
                        if (!session.movedBeyondSlop && distFromDown > slop * 1.4f) {
                            session.movedBeyondSlop = true
                            session.longPressJob?.cancel()
                        }
                        if (glideEnabled() && target.isGlideEligible && !session.longPressTriggered && activeSessions.size == 1 && !session.cancelled) {
                            val hit = KeyHitTestResolver.resolveHit(
                                point = change.position,
                                targets = targets,
                                tolerances = tolerances,
                                onlyCategories = setOf(KeyHitCategory.LETTER),
                                phase = "GLIDE"
                            )
                            val hitKey = hit?.let { surfaceState.actionsById[it.target.id]?.letterLookupKey ?: it.target.id }
                            if (hitKey != null && session.glideVisited.lastOrNull() != hitKey) {
                                session.glideVisited.add(hitKey)
                            }
                            if (!session.isGliding && distFromDown > glideStartDistPx && session.glideVisited.size >= 2) {
                                session.isGliding = true
                                surfaceState.setPressed(target.id, false)
                            }
                            if (session.isGliding) {
                                change.consume()
                                session.glidePoints.add(change.position)
                                if (session.glidePoints.size > 48) session.glidePoints.removeAt(0)
                                onGlideTrailChanged(session.glidePoints.toList())
                            }
                        }
                    }

                    KeyHitCategory.LANGUAGE, KeyHitCategory.ENTER -> Unit
                }
            }
        }
    }
    } finally {
        for (session in activeSessions.values) {
            session.longPressJob?.cancel()
            session.repeatJob?.cancel()
            session.cursorHoldJob?.cancel()
            session.cancelled = true
            surfaceState.setPressed(session.target.id, false)
        }
        activeSessions.clear()
        surfaceState.resetTransientState()
    }
}
