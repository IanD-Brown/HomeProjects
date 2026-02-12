package io.github.iandbrown.sportplanner.ui

import androidx.compose.material3.AssistChipDefaults.IconSize
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.shouldBe

class ViewSharedTest : ShouldSpec({
    val density = Density(1f)
    val iconSizePx = with(density) { (IconSize + 16.dp).roundToPx() }

    context("WeightedIconGridCells") {
        val availableSize = 500
        val spacing = 10
        should("calculate cell sizes correctly") {
            with(WeightedIconGridCells(4, 3, 2)) {
                val sizes = density.calculateCrossAxisCellSizes(availableSize, spacing)
                val gapCount = iconCount + weights.size - 1
                val remainingSpace = availableSize - (gapCount * spacing + iconCount * iconSizePx)
                val perWeight = remainingSpace / weights.sum()
                sizes shouldBe listOf(perWeight * weights[0], perWeight * weights[1], iconSizePx, iconSizePx, iconSizePx, iconSizePx)
                sizes.sum() shouldBeLessThanOrEqual availableSize - spacing * gapCount
            }
        }
        should("calculate cell sizes correctly with zero icons") {
            with(WeightedIconGridCells(0, 3, 2)) {
                val sizes = density.calculateCrossAxisCellSizes(availableSize, spacing)
                val gapCount = iconCount + weights.size - 1
                val remainingSpace = availableSize - (gapCount * spacing + iconCount * iconSizePx)
                val perWeight = remainingSpace / weights.sum()
                sizes shouldBe listOf(perWeight * weights[0], perWeight * weights[1])
            }
        }
        should("calculate cell sizes correctly with zero columns") {
            with(WeightedIconGridCells(4)) {
                density.calculateCrossAxisCellSizes(availableSize, spacing) shouldBe listOf(iconSizePx, iconSizePx, iconSizePx, iconSizePx)
            }
        }
        should("calculate cell sizes correctly with zero columns and icons") {
            with(WeightedIconGridCells(0)) {
                density.calculateCrossAxisCellSizes(availableSize, spacing) shouldBe emptyList()
            }
        }
    }

    context("TrailingIconGridCells") {
        val availableSize = 500
        val spacing = 11
        should("calculate cell sizes correctly") {
            with(TrailingIconGridCells(2, 1)) {
                val sizes = density.calculateCrossAxisCellSizes(availableSize, spacing)
                val gapCount = trailingIconCount + dataColumnCount - 1
                val usableWidth = availableSize - (trailingIconCount * iconSizePx + gapCount * spacing)
                val dataWidth = usableWidth / dataColumnCount
                sizes shouldBe listOf(dataWidth, dataWidth, iconSizePx)
                sizes.sum() shouldBeLessThanOrEqual availableSize - spacing * gapCount
            }
        }
        should("calculate cell sizes correctly with zero columns") {
            with(TrailingIconGridCells(0, 1)) {
                density.calculateCrossAxisCellSizes(availableSize, spacing) shouldBe listOf(iconSizePx)
            }
        }
        should("calculate cell sizes correctly with zero trailing icons") {
            with(TrailingIconGridCells(2, 0)) {
                val sizes = density.calculateCrossAxisCellSizes(availableSize, spacing)
                val gapCount = trailingIconCount + dataColumnCount - 1
                val usableWidth = availableSize - (trailingIconCount * iconSizePx + gapCount * spacing)
                val dataWidth = usableWidth / dataColumnCount
                sizes shouldBe listOf(dataWidth, dataWidth)
            }
        }
        should("calculate cell sizes correctly with zero columns and icons") {
            with(TrailingIconGridCells(0, 0)) {
                density.calculateCrossAxisCellSizes(availableSize, spacing) shouldBe emptyList()
            }
        }
    }

    context("DoubleFirstGridCells") {
        val availableSize = 234
        val spacing = 11
        should("calculate cell sizes correctly") {
            with(DoubleFirstGridCells(2)) {
                val sizes = density.calculateCrossAxisCellSizes(availableSize, spacing)
                val usableWidth = availableSize - spacing
                val laterColumnWidth = (usableWidth * (columns - 1)) / (columns + 1)
                sizes shouldBe listOf(usableWidth - laterColumnWidth, laterColumnWidth)
            }
        }
        should("calculate cell sizes correctly for a single column") {
            with(DoubleFirstGridCells(1)) {
                val sizes = density.calculateCrossAxisCellSizes(availableSize, spacing)
                sizes shouldBe listOf(availableSize)
            }
        }
        should("calculate cell sizes correctly for zero columns") {
            with(DoubleFirstGridCells(0)) {
                val sizes = density.calculateCrossAxisCellSizes(availableSize, spacing)
                sizes shouldBe emptyList()
            }
        }
    }
})
