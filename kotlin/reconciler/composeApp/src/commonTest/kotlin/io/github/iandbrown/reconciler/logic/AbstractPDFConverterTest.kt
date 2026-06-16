package io.github.iandbrown.reconciler.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AbstractPDFConverterTest {

    private class TestPDFConverter(private val items: Map<RectArea, String>) : AbstractPDFConverter() {
        override fun getItems(): Map<RectArea, String> = items
    }

    @Test
    fun testGetDateRange() {
        val items = mapOf(
            RectArea(0f, 0f, 0f, 0f) to "1st Jan 2022 to 31st Dec 2023",
            RectArea(10f, 10f, 10f, 10f) to "Other text"
        )
        val converter = TestPDFConverter(items)
        val range = converter.getDateRange()
        assertEquals(2022, range.first)
        assertEquals(2023, range.second)
    }

    @Test
    fun testCalcRows() {
        val items = listOf(
            RectArea(0f, 100f, 10f, 20f) to "Row1-Col1",
            RectArea(150f, 250f, 12f, 18f) to "Row1-Col2", // Overlaps Row 1
            RectArea(0f, 100f, 30f, 40f) to "Row2-Col1"
        )
        val rows = calcRows(items)
        assertEquals(2, rows.size)
        assertEquals(Range(10f, 20f), rows[0])
        assertEquals(Range(30f, 40f), rows[1])
    }

    @Test
    fun testRowContentFiltering() {
        val items = mapOf(
            RectArea(10f, 50f, 10f, 20f) to "Date",
            RectArea(60f, 100f, 10f, 20f) to "Description",
            RectArea(10f, 50f, 30f, 40f) to "2023-01-01",
            RectArea(60f, 100f, 30f, 40f) to "Lunch"
        )
        val converter = TestPDFConverter(items)

        // Filter for rows containing "Lunch"
        val rows = converter.rowContent { it.contains("Lunch") }
        assertEquals(1, rows.size)
        assertTrue(rows[0].values.contains("Lunch"))
        assertTrue(rows[0].values.contains("2023-01-01"))
    }

    @Test
    fun testMergeOverlapping() {
        // Note: mergeOverlapping is private in AbstractPDFConverter.kt.
        // We test it indirectly through rowContent.

        val sortedItems = listOf(
            RectArea(10f, 50f, 10f, 20f) to "Part 1",
            RectArea(40f, 80f, 10f, 20f) to " and Part 2",
            RectArea(100f, 150f, 10f, 20f) to "Separate"
        )
        val rowRanges = listOf(Range(10f, 20f))

        val result = rowContent(rowRanges, sortedItems) { true }
        assertEquals(1, result.size)
        val row = result[0]

        // Should have 2 entries after merge: "Part 1 and Part 2" and "Separate"
        assertEquals(2, row.size)
        assertTrue(row.values.contains("Part 1 and Part 2"))
        assertTrue(row.values.contains("Separate"))
    }

    @Test
    fun testTextAreaHolderStringAt() {
        val holder = TextAreaHolder()
        holder.stringAt("Hello", sequenceOf(RectArea(10f, 20f, 10f, 20f)))
        assertEquals("Hello", holder.currentText)
        holder.stringAt(" ", sequenceOf(RectArea(20f, 30f, 10f, 20f)))
        assertEquals("Hello ", holder.currentText)
        holder.stringAt(null, sequenceOf(RectArea(30f, 40f, 10f, 20f)))
        assertEquals(null, holder.currentText)
        holder.stringAt("World", sequenceOf(RectArea(30f, 40f, 10f, 20f)))
        assertEquals("World", holder.currentText)
    }

    @Test
    fun testCalcRowsWithLaterRowBelow() {
        val items = mapOf(
            RectArea(10f, 50f, 10f, 19f) to "Date",
            RectArea(60f, 100f, 10f, 22f) to "Description",
            RectArea(10f, 50f, 30f, 40f) to "2023-01-01",
            RectArea(60f, 100f, 30f, 40f) to "Lunch"
        )

        val sortedItems = getSortedItems(items)

        assertEquals("Date", sortedItems[0].second)
        assertEquals("Description", sortedItems[1].second)
        assertEquals("2023-01-01", sortedItems[2].second)
        assertEquals("Lunch", sortedItems[3].second)

        val rows = calcRows(sortedItems)
        assertEquals(2, rows.size)
        assertEquals(Range(10f, 22f), rows[0])
        assertEquals(Range(30f, 40f), rows[1])
    }

    @Test
    fun testCalcRowsWithInvalidTop() {
        val items = mapOf(
            RectArea(10f, 50f, Float.MAX_VALUE, 1f) to "XXX"
        )

        val rows = calcRows(getSortedItems(items))

        assertTrue { rows.isEmpty() }
    }
}
