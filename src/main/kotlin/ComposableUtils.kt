import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Maximize
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import java.awt.Cursor
import kotlin.math.max

val MaterialBlue = Color(0xFF1976d2)

@Composable
fun CustomChip(
    category: String,
    modifier: Modifier = Modifier,
    textColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
    onClick: () -> Unit = {}
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = Modifier.then(modifier),
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
    ) {
        Row {
            Text(
                text = category,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                color = textColor,
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.CenterVertically),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    mainAxisSize: SizeMode = SizeMode.Wrap,
    mainAxisAlignment: FlowMainAxisAlignment = FlowMainAxisAlignment.Start,
    mainAxisSpacing: Dp = 0.dp,
    crossAxisAlignment: FlowCrossAxisAlignment = FlowCrossAxisAlignment.Start,
    crossAxisSpacing: Dp = 0.dp,
    lastLineMainAxisAlignment: FlowMainAxisAlignment = mainAxisAlignment,
    content: @Composable () -> Unit
) {
    Flow(
        modifier = modifier,
        orientation = LayoutOrientation.Horizontal,
        mainAxisSize = mainAxisSize,
        mainAxisAlignment = mainAxisAlignment,
        mainAxisSpacing = mainAxisSpacing,
        crossAxisAlignment = crossAxisAlignment,
        crossAxisSpacing = crossAxisSpacing,
        lastLineMainAxisAlignment = lastLineMainAxisAlignment,
        content = content
    )
}

/**
 * Used to specify the alignment of a layout's children, in cross axis direction.
 */
public enum class FlowCrossAxisAlignment {
    /**
     * Place children such that their center is in the middle of the cross axis.
     */
    Center,

    /**
     * Place children such that their start edge is aligned to the start edge of the cross axis.
     */
    Start,

    /**
     * Place children such that their end edge is aligned to the end edge of the cross axis.
     */
    End,
}

public typealias FlowMainAxisAlignment = MainAxisAlignment

/**
 * Layout model that arranges its children in a horizontal or vertical flow.
 */
@Composable
private fun Flow(
    modifier: Modifier,
    orientation: LayoutOrientation,
    mainAxisSize: SizeMode,
    mainAxisAlignment: FlowMainAxisAlignment,
    mainAxisSpacing: Dp,
    crossAxisAlignment: FlowCrossAxisAlignment,
    crossAxisSpacing: Dp,
    lastLineMainAxisAlignment: FlowMainAxisAlignment,
    content: @Composable () -> Unit
) {
    fun Placeable.mainAxisSize() =
        if (orientation == LayoutOrientation.Horizontal) width else height

    fun Placeable.crossAxisSize() =
        if (orientation == LayoutOrientation.Horizontal) height else width

    Layout(content, modifier) { measurables, outerConstraints ->
        val sequences = mutableListOf<List<Placeable>>()
        val crossAxisSizes = mutableListOf<Int>()
        val crossAxisPositions = mutableListOf<Int>()

        var mainAxisSpace = 0
        var crossAxisSpace = 0

        val currentSequence = mutableListOf<Placeable>()
        var currentMainAxisSize = 0
        var currentCrossAxisSize = 0

        val constraints = OrientationIndependentConstraints(outerConstraints, orientation)

        val childConstraints = if (orientation == LayoutOrientation.Horizontal) {
            Constraints(maxWidth = constraints.mainAxisMax)
        } else {
            Constraints(maxHeight = constraints.mainAxisMax)
        }

        // Return whether the placeable can be added to the current sequence.
        fun canAddToCurrentSequence(placeable: Placeable) =
            currentSequence.isEmpty() || currentMainAxisSize + mainAxisSpacing.roundToPx() +
                    placeable.mainAxisSize() <= constraints.mainAxisMax

        // Store current sequence information and start a new sequence.
        fun startNewSequence() {
            if (sequences.isNotEmpty()) {
                crossAxisSpace += crossAxisSpacing.roundToPx()
            }
            sequences += currentSequence.toList()
            crossAxisSizes += currentCrossAxisSize
            crossAxisPositions += crossAxisSpace

            crossAxisSpace += currentCrossAxisSize
            mainAxisSpace = max(mainAxisSpace, currentMainAxisSize)

            currentSequence.clear()
            currentMainAxisSize = 0
            currentCrossAxisSize = 0
        }

        for (measurable in measurables) {
            // Ask the child for its preferred size.
            val placeable = measurable.measure(childConstraints)

            // Start a new sequence if there is not enough space.
            if (!canAddToCurrentSequence(placeable)) startNewSequence()

            // Add the child to the current sequence.
            if (currentSequence.isNotEmpty()) {
                currentMainAxisSize += mainAxisSpacing.roundToPx()
            }
            currentSequence.add(placeable)
            currentMainAxisSize += placeable.mainAxisSize()
            currentCrossAxisSize = max(currentCrossAxisSize, placeable.crossAxisSize())
        }

        if (currentSequence.isNotEmpty()) startNewSequence()

        val mainAxisLayoutSize = if (constraints.mainAxisMax != Constraints.Infinity &&
            mainAxisSize == SizeMode.Expand
        ) {
            constraints.mainAxisMax
        } else {
            max(mainAxisSpace, constraints.mainAxisMin)
        }
        val crossAxisLayoutSize = max(crossAxisSpace, constraints.crossAxisMin)

        val layoutWidth = if (orientation == LayoutOrientation.Horizontal) {
            mainAxisLayoutSize
        } else {
            crossAxisLayoutSize
        }
        val layoutHeight = if (orientation == LayoutOrientation.Horizontal) {
            crossAxisLayoutSize
        } else {
            mainAxisLayoutSize
        }

        layout(layoutWidth, layoutHeight) {
            sequences.forEachIndexed { i, placeables ->
                val childrenMainAxisSizes = IntArray(placeables.size) { j ->
                    placeables[j].mainAxisSize() +
                            if (j < placeables.lastIndex) mainAxisSpacing.roundToPx() else 0
                }
                val arrangement = if (i < sequences.lastIndex) {
                    mainAxisAlignment.arrangement
                } else {
                    lastLineMainAxisAlignment.arrangement
                }
                // TODO(soboleva): rtl support
                // Handle vertical direction
                val mainAxisPositions = IntArray(childrenMainAxisSizes.size) { 0 }
                with(arrangement) {
                    arrange(mainAxisLayoutSize, childrenMainAxisSizes, mainAxisPositions)
                }
                placeables.forEachIndexed { j, placeable ->
                    val crossAxis = when (crossAxisAlignment) {
                        FlowCrossAxisAlignment.Start -> 0
                        FlowCrossAxisAlignment.End ->
                            crossAxisSizes[i] - placeable.crossAxisSize()
                        FlowCrossAxisAlignment.Center ->
                            Alignment.Center.align(
                                IntSize.Zero,
                                IntSize(
                                    width = 0,
                                    height = crossAxisSizes[i] - placeable.crossAxisSize()
                                ),
                                LayoutDirection.Ltr
                            ).y
                    }
                    if (orientation == LayoutOrientation.Horizontal) {
                        placeable.place(
                            x = mainAxisPositions[j],
                            y = crossAxisPositions[i] + crossAxis
                        )
                    } else {
                        placeable.place(
                            x = crossAxisPositions[i] + crossAxis,
                            y = mainAxisPositions[j]
                        )
                    }
                }
            }
        }
    }
}

/**
 * Used to specify how a layout chooses its own size when multiple behaviors are possible.
 */
// TODO(popam): remove this when Flow is reworked
public enum class SizeMode {
    /**
     * Minimize the amount of free space by wrapping the children,
     * subject to the incoming layout constraints.
     */
    Wrap,

    /**
     * Maximize the amount of free space by expanding to fill the available space,
     * subject to the incoming layout constraints.
     */
    Expand
}

/**
 * Used to specify the alignment of a layout's children, in main axis direction.
 */
public enum class MainAxisAlignment(internal val arrangement: Arrangement.Vertical) {
    // TODO(soboleva) support RTl in Flow
    // workaround for now - use Arrangement that equals to previous Arrangement
    /**
     * Place children such that they are as close as possible to the middle of the main axis.
     */
    Center(Arrangement.Center),

    /**
     * Place children such that they are as close as possible to the start of the main axis.
     */
    Start(Arrangement.Top),

    /**
     * Place children such that they are as close as possible to the end of the main axis.
     */
    End(Arrangement.Bottom),

    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child.
     */
    SpaceEvenly(Arrangement.SpaceEvenly),

    /**
     * Place children such that they are spaced evenly across the main axis, without free
     * space before the first child or after the last child.
     */
    SpaceBetween(Arrangement.SpaceBetween),

    /**
     * Place children such that they are spaced evenly across the main axis, including free
     * space before the first child and after the last child, but half the amount of space
     * existing otherwise between two consecutive children.
     */
    SpaceAround(Arrangement.SpaceAround);
}

internal enum class LayoutOrientation {
    Horizontal,
    Vertical
}

internal data class OrientationIndependentConstraints(
    val mainAxisMin: Int,
    val mainAxisMax: Int,
    val crossAxisMin: Int,
    val crossAxisMax: Int
) {
    constructor(c: Constraints, orientation: LayoutOrientation) : this(
        if (orientation === LayoutOrientation.Horizontal) c.minWidth else c.minHeight,
        if (orientation === LayoutOrientation.Horizontal) c.maxWidth else c.maxHeight,
        if (orientation === LayoutOrientation.Horizontal) c.minHeight else c.minWidth,
        if (orientation === LayoutOrientation.Horizontal) c.maxHeight else c.maxWidth
    )
}

@Composable
fun CustomBottomAppBar(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    shape: Shape = RectangleShape,
    elevation: Dp = 4.dp,
    contentPadding: PaddingValues = PaddingValues(4.dp, 4.dp),
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        tonalElevation = elevation,
        shape = shape,
        modifier = modifier
    ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun CustomTooltip(
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    tooltip: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    TooltipArea(
        tooltip = {
            // composable tooltip content
            Surface(
                modifier = Modifier.shadow(4.dp),
                color = backgroundColor,
                contentColor = contentColor,
                shape = RoundedCornerShape(4.dp)
            ) { tooltip() }
        }
    ) { content() }
}

@Composable
@ExperimentalFoundationApi
fun ContextMenuArea(
    items: () -> List<ContextMenuItem>,
    state: ContextMenuState = remember { ContextMenuState() },
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val data = ContextMenuData(items, LocalContextMenuData.current)

    ContextMenuDataProvider(data) {
        Box(Modifier.contextMenuDetector(state, enabled), propagateMinConstraints = true) {
            content()
        }
        (if (darkTheme) DarkDefaultContextMenuRepresentation else LightDefaultContextMenuRepresentation).Representation(state, data)
    }
}

@Composable
@ExperimentalFoundationApi
fun ContextMenuDataProvider(
    items: () -> List<ContextMenuItem>,
    content: @Composable () -> Unit
) {
    ContextMenuDataProvider(
        ContextMenuData(items, LocalContextMenuData.current),
        content
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ContextMenuDataProvider(
    data: ContextMenuData,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalContextMenuData provides data
    ) {
        content()
    }
}

@OptIn(ExperimentalFoundationApi::class)
private val LocalContextMenuData = staticCompositionLocalOf<ContextMenuData?> {
    null
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.contextMenuDetector(
    state: ContextMenuState,
    enabled: Boolean = true
): Modifier {
    return if (
        enabled && state.status == ContextMenuState.Status.Closed
    ) {
        this.pointerInput(state) {
            forEachGesture {
                awaitPointerEventScope {
                    val event = awaitEventFirstDown()
                    if (event.buttons.isSecondaryPressed) {
                        event.changes.forEach { it.consumeDownChange() }
                        state.status =
                            ContextMenuState.Status.Open(Rect(event.changes[0].position, 0f))
                    }
                }
            }
        }
    } else {
        Modifier
    }
}

private suspend fun AwaitPointerEventScope.awaitEventFirstDown(): PointerEvent {
    var event: PointerEvent
    do {
        event = awaitPointerEvent()
    } while (
        !event.changes.all { it.changedToDown() }
    )
    return event
}

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.cursorForSelectable(): Modifier = pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FrameWindowScope.WindowHeader(state: WindowState, title: @Composable () -> Unit, onCloseRequest: () -> Unit) {
    Column {
        WindowDraggableArea(
            modifier = Modifier.combinedClickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = {},
                onDoubleClick = {
                    state.placement = if (state.placement != WindowPlacement.Maximized) {
                        WindowPlacement.Maximized
                    } else {
                        WindowPlacement.Floating
                    }
                }
            )
        ) {
            val hasFocus = LocalWindowInfo.current.isWindowFocused

            SmallTopAppBar(
                title = title,
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = animateColorAsState(
                        if (hasFocus) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                    ).value
                ),
                actions = {
                    IconButton(onClick = onCloseRequest) { Icon(Icons.Default.Close, null) }
                    IconButton(onClick = { state.isMinimized = true }) { Icon(Icons.Default.Minimize, null) }
                    IconButton(
                        onClick = {
                            state.placement = if (state.placement != WindowPlacement.Maximized) {
                                WindowPlacement.Maximized
                            } else {
                                WindowPlacement.Floating
                            }
                        }
                    ) { Icon(Icons.Default.Maximize, null) }
                }
            )
        }
        Divider(color = MaterialTheme.colorScheme.onSurface)
    }
}