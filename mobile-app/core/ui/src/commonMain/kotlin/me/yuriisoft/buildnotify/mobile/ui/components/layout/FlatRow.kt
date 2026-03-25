package me.yuriisoft.buildnotify.mobile.ui.components.layout

import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Determines how multiple children within a single [FlatRowSlot] are arranged relative to each other.
 *
 *
 * Vertical:          Horizontal:
 * ┌───────────┐      ┌───────────────────┐
 * │  Child A  │      │ Child A │ Child B │
 * │───────────│      └───────────────────┘
 * │  Child B  │
 * └───────────┘
 *
 */
enum class SlotArrangement {
    /** Children are stacked top-to-bottom, separated by the row's `verticalSpacing`. */
    Vertical,

    /** Children are placed side-by-side left-to-right, each consuming remaining width in the slot. */
    Horizontal,
}

/**
 * Defines a single column (slot) within a [FlatRow].
 *
 * Each slot occupies a proportional width of the row determined by [weight] relative to the
 * sum of all slot weights. Multiple children assigned to the same slot are arranged according
 * to [arrangement].
 *
 * @property weight Proportional column width. For example, given slots with weights `0.55f`,
 *   `0.15f`, and `0.30f`, the first slot receives 55% of the available width (after spacing).
 *   Must be greater than zero.
 * @property arrangement How children within this slot are laid out when there are multiple.
 *   [SlotArrangement.Vertical] stacks them top-to-bottom; [SlotArrangement.Horizontal] places
 *   them side-by-side. Defaults to [SlotArrangement.Vertical].
 * @property horizontalAlignment How the content group is positioned horizontally within the
 *   column's width. Defaults to [Alignment.Start].
 * @property verticalAlignment How the content group is positioned vertically within the row's
 *   height. Defaults to [Alignment.CenterVertically].
 */
@Immutable
data class FlatRowSlot(
    val weight: Float,
    val arrangement: SlotArrangement = SlotArrangement.Vertical,
    val horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    val verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
) {
    init {
        require(weight > 0f) { "Slot weight must be greater than zero, was $weight" }
    }
}

/**
 * Scope for the content of a [FlatRow]. Provides the [slot] modifier that assigns a child
 * composable to a specific column index.
 *
 * Children without a [slot] modifier are silently dropped during measurement.
 * Multiple children can share the same slot index — they will be arranged according to
 * the slot's [FlatRowSlot.arrangement].
 */
@Immutable
@LayoutScopeMarker
interface FlatRowScope {

    /**
     * Assigns this child to the slot at [index].
     *
     * The index must be within `0 ..< slots.size` passed to the parent [FlatRow].
     * Multiple children may share the same index; they are arranged per the slot's
     * [FlatRowSlot.arrangement] setting.
     *
     * @param index Zero-based slot index.
     */
    @Stable
    fun Modifier.slot(index: Int): Modifier

    @Immutable
    object Instance : FlatRowScope {
        @Stable
        override fun Modifier.slot(index: Int): Modifier {
            require(index >= 0) { "Slot index must be non-negative, was $index" }
            return this.then(FlatRowSlotElement(index))
        }
    }
}

/**
 * [ModifierNodeElement] that stores the slot index assigned to a child of [FlatRow].
 * Creates and updates [FlatRowSlotNode] which provides parent data during measurement.
 */
internal class FlatRowSlotElement(
    private val slotIndex: Int,
) : ModifierNodeElement<FlatRowSlotNode>() {

    override fun create(): FlatRowSlotNode = FlatRowSlotNode(slotIndex)

    override fun update(node: FlatRowSlotNode) {
        node.slotIndex = slotIndex
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "FlatRowSlot"
        value = slotIndex
        properties["slotIndex"] = slotIndex
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FlatRowSlotElement) return false
        return slotIndex == other.slotIndex
    }

    override fun hashCode(): Int = slotIndex.hashCode()
}

/**
 * Parent data attached to each child of [FlatRow], carrying the slot index.
 */
internal data class FlatRowSlotParentData(var slotIndex: Int = 0)

/**
 * [ParentDataModifierNode] that provides [FlatRowSlotParentData] to the parent [FlatRow] layout.
 */
internal class FlatRowSlotNode(
    var slotIndex: Int,
) : Modifier.Node(), ParentDataModifierNode {

    override fun Density.modifyParentData(parentData: Any?): Any {
        val data = (parentData as? FlatRowSlotParentData) ?: FlatRowSlotParentData()
        return data.also { it.slotIndex = slotIndex }
    }
}

private val IntrinsicMeasurable.flatRowSlotParentData: FlatRowSlotParentData?
    get() = parentData as? FlatRowSlotParentData

private val Measurable.slotIndex: Int?
    get() = flatRowSlotParentData?.slotIndex

/**
 * [MeasurePolicy] for [FlatRow]. Implements weighted column layout with per-slot arrangement.
 *
 * Algorithm:
 * 1. **Group** measurables by their `slotIndex` parent data.
 * 2. **Calculate column widths** — each slot gets `availableWidth * (slot.weight / totalWeight)`.
 *    Available width is `constraints.maxWidth − horizontalSpacing * (slots.size − 1)`.
 * 3. **Measure children** within each slot:
 *    - [SlotArrangement.Vertical]: each child is measured with `maxWidth = columnWidth`;
 *      children are stacked top-to-bottom separated by [verticalSpacingDp].
 *    - [SlotArrangement.Horizontal]: children are measured sequentially, each getting the
 *      remaining width after the previous child.
 * 4. **Determine layout height** — the maximum group height across all slots.
 * 5. **Place** each slot group at its fixed X position, respecting [FlatRowSlot.horizontalAlignment]
 *    and [FlatRowSlot.verticalAlignment].
 *
 * Edge cases:
 * - Empty slots (no children assigned) still occupy their weighted width — this is the key
 *   property that prevents layout shifts when children are conditionally shown/hidden.
 * - Children without a `.slot()` modifier are ignored.
 * - A single child in a slot behaves identically to a group of one.
 */
internal class FlatRowMeasurePolicy(
    private val slots: List<FlatRowSlot>,
    private val horizontalSpacingDp: Dp,
    private val verticalSpacingDp: Dp,
) : MeasurePolicy {

    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        if (measurables.isEmpty()) {
            return layout(constraints.minWidth, constraints.minHeight) {}
        }

        val hSpacingPx = horizontalSpacingDp.roundToPx()
        val vSpacingPx = verticalSpacingDp.roundToPx()

        val grouped: Map<Int, List<Measurable>> = measurables
            .filter { it.slotIndex != null }
            .groupBy { it.slotIndex!! }

        val totalWeight = slots.sumOf { it.weight.toDouble() }.toFloat()
        val totalHSpacing = hSpacingPx * (slots.size - 1).coerceAtLeast(0)
        val availableWidth = (constraints.maxWidth - totalHSpacing).coerceAtLeast(0)

        val columnWidths = slots.map { slot ->
            (availableWidth * slot.weight / totalWeight).roundToInt()
        }

        data class SlotResult(
            val placeables: List<Placeable>,
            val groupWidth: Int,
            val groupHeight: Int,
        )

        val slotResults = slots.mapIndexed { index, slot ->
            val children = grouped[index].orEmpty()
            val colWidth = columnWidths[index]

            if (children.isEmpty()) {
                return@mapIndexed SlotResult(emptyList(), 0, 0)
            }

            when (slot.arrangement) {
                SlotArrangement.Vertical -> {
                    val placeables = children.map { measurable ->
                        measurable.measure(
                            Constraints(maxWidth = colWidth, maxHeight = constraints.maxHeight)
                        )
                    }
                    val groupHeight = placeables.sumOf { it.height } +
                            vSpacingPx * (placeables.size - 1).coerceAtLeast(0)
                    val groupWidth = placeables.maxOf { it.width }
                    SlotResult(placeables, groupWidth, groupHeight)
                }

                SlotArrangement.Horizontal -> {
                    var remainingWidth = colWidth
                    val placeables = children.map { measurable ->
                        val placeable = measurable.measure(
                            Constraints(
                                maxWidth = remainingWidth.coerceAtLeast(0),
                                maxHeight = constraints.maxHeight,
                            )
                        )
                        remainingWidth -= placeable.width
                        placeable
                    }
                    val groupWidth = placeables.sumOf { it.width }
                    val groupHeight = placeables.maxOfOrNull { it.height } ?: 0
                    SlotResult(placeables, groupWidth, groupHeight)
                }
            }
        }

        val layoutHeight = max(
            slotResults.maxOfOrNull { it.groupHeight } ?: 0,
            constraints.minHeight,
        )

        return layout(constraints.maxWidth, layoutHeight) {
            var xOffset = 0

            slotResults.forEachIndexed { index, result ->
                val slot = slots[index]
                val colWidth = columnWidths[index]

                val alignedX = xOffset + slot.horizontalAlignment.align(
                    size = result.groupWidth,
                    space = colWidth,
                    layoutDirection = layoutDirection,
                )

                val alignedY = slot.verticalAlignment.align(
                    size = result.groupHeight,
                    space = layoutHeight,
                )

                when (slot.arrangement) {
                    SlotArrangement.Vertical -> {
                        var y = alignedY
                        result.placeables.forEach { placeable ->
                            placeable.placeRelative(alignedX, y)
                            y += placeable.height + vSpacingPx
                        }
                    }

                    SlotArrangement.Horizontal -> {
                        var x = alignedX
                        result.placeables.forEach { placeable ->
                            val childY = slot.verticalAlignment.align(
                                size = placeable.height,
                                space = layoutHeight,
                            )
                            placeable.placeRelative(x, childY)
                            x += placeable.width
                        }
                    }
                }

                xOffset += colWidth + hSpacingPx
            }
        }
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int {
        val hSpacingPx = horizontalSpacingDp.roundToPx()
        val grouped = measurables
            .filter { it.flatRowSlotParentData != null }
            .groupBy { it.flatRowSlotParentData!!.slotIndex }

        val childrenWidth = slots.indices.sumOf { index ->
            grouped[index]?.maxOfOrNull { it.minIntrinsicWidth(height) } ?: 0
        }
        return childrenWidth + hSpacingPx * (slots.size - 1).coerceAtLeast(0)
    }

    override fun IntrinsicMeasureScope.minIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int {
        val vSpacingPx = verticalSpacingDp.roundToPx()
        val grouped = measurables
            .filter { it.flatRowSlotParentData != null }
            .groupBy { it.flatRowSlotParentData!!.slotIndex }

        return slots.indices.maxOfOrNull { index ->
            val slot = slots[index]
            val children = grouped[index].orEmpty()
            when (slot.arrangement) {
                SlotArrangement.Vertical ->
                    children.sumOf { it.minIntrinsicHeight(width) } +
                            vSpacingPx * (children.size - 1).coerceAtLeast(0)

                SlotArrangement.Horizontal ->
                    children.maxOfOrNull { it.minIntrinsicHeight(width) } ?: 0
            }
        } ?: 0
    }

    override fun IntrinsicMeasureScope.maxIntrinsicWidth(
        measurables: List<IntrinsicMeasurable>,
        height: Int,
    ): Int = minIntrinsicWidth(measurables, height)

    override fun IntrinsicMeasureScope.maxIntrinsicHeight(
        measurables: List<IntrinsicMeasurable>,
        width: Int,
    ): Int = minIntrinsicHeight(measurables, width)
}

/**
 * A depth-1 custom layout that arranges children into pre-defined weighted columns (slots).
 *
 * Unlike a `Row` + `Column` composition, `FlatRow` keeps the component tree completely flat:
 * every child is a direct descendant of the layout, assigned to a logical column via
 * [FlatRowScope.slot]. Column widths are fixed by [FlatRowSlot.weight], so conditional
 * children (e.g. a severity icon that only appears for warnings) do **not** shift the positions
 * of other columns.
 *
 * ### Slot model
 *
 * Slots are defined up-front via `slots: List<FlatRowSlot>`. Each slot has:
 * - A **weight** that determines proportional width.
 * - An **arrangement** — [SlotArrangement.Vertical] to stack children top-to-bottom, or
 *   [SlotArrangement.Horizontal] to place them side-by-side.
 * - **Alignment** controls for both axes.
 *
 * Children declare their slot with `Modifier.slot(index)`. Multiple children can share the
 * same index; empty slots still reserve their weighted width.
 *
 * ### Examples
 *
 * A two-column row with title/subtitle on the left and an action button on the right:
 *
 * FlatRow(
 *     slots = listOf(
 *         FlatRowSlot(weight = 0.7f),
 *         FlatRowSlot(weight = 0.3f, horizontalAlignment = Alignment.End),
 *     ),
 * ) {
 *     Text("Title", Modifier.slot(0))
 *     Text("Subtitle", Modifier.slot(0))
 *     IconButton(onClick = {}, Modifier.slot(1)) { Icon(...) }
 * }
 *
 *
 * A three-column row with name/manufacturer, node label, and action buttons:
 *
[24.03.2026 11:26] Yurii Surzhykov: * FlatRow(
 *     slots = listOf(
 *         FlatRowSlot(weight = 0.55f),
 *         FlatRowSlot(weight = 0.15f),
 *         FlatRowSlot(weight = 0.30f, arrangement = SlotArrangement.Horizontal, horizontalAlignment = Alignment.End),
 *     ),
 *     horizontalSpacing = 8.dp,
 *     verticalSpacing = 4.dp,
 * ) {
 *     Text(deviceName, Modifier.slot(0))
 *     Text(manufacturer, Modifier.slot(0))
 *     Text(nodeLabel, Modifier.slot(1))
 *     if (hasSeverityIcon) Icon(severity, Modifier.slot(2))
 *     IconButton(edit, Modifier.slot(2))
 * }
 *
 *
 * @param slots Slot definitions that partition the row's width. Order determines left-to-right
 *   column position. Must not be empty.
 * @param modifier Modifier applied to the layout itself.
 * @param horizontalSpacing Gap between adjacent columns.
 * @param verticalSpacing Gap between stacked children within a [SlotArrangement.Vertical] slot.
 * @param content Composable content declared inside a [FlatRowScope] — use `Modifier.slot(index)`
 *   to assign children to columns.
 */
@Composable
fun FlatRow(
    slots: List<FlatRowSlot>,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 0.dp,
    verticalSpacing: Dp = 4.dp,
    content: @Composable FlatRowScope.() -> Unit,
) {
    require(slots.isNotEmpty()) { "FlatRow requires at least one slot" }

    val measurePolicy = remember(slots, horizontalSpacing, verticalSpacing) {
        FlatRowMeasurePolicy(
            slots = slots,
            horizontalSpacingDp = horizontalSpacing,
            verticalSpacingDp = verticalSpacing,
        )
    }

    Layout(
        content = { FlatRowScope.Instance.content() },
        measurePolicy = measurePolicy,
        modifier = modifier,
    )
}