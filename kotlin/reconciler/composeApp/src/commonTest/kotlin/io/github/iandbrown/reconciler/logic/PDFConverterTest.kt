package io.github.iandbrown.reconciler.logic

import kotlin.test.Test
import kotlin.test.assertEquals

class PDFConverterTest {
    @Test
    fun testCalcRows() {
        val textByArea = getTextByArea()
        val rows = calcRows(getSortedItems(textByArea))

        assertEquals(2, rows.size)
        assertEquals(1427.1011F, rows[0].from)
        assertEquals(1432.884F, rows[0].to)
        assertEquals(1436.6279F, rows[1].from)
        assertEquals(1450.4607F, rows[1].to)
    }

    @Test
    fun testRowContents() {
        val textByArea = getTextByArea()
        val sortedItems = getSortedItems(textByArea)
        val rowRanges = calcRows(sortedItems)

        val rowContents = rowContent(rowRanges, sortedItems) { it.isNotEmpty() }

        assertEquals(2, rowContents.size)
        assertEquals(4, rowContents[0].size)
        assert("9th Mar" in rowContents[0].values)
        assert("FASTER PAYMENTS RECEIPT REF.U13G vMancESFA0802 FROM Leeds Schools' Foo" in rowContents[0].values)
        assert("324.00" in rowContents[0].values)
        assert("1,060.07" in rowContents[0].values)
        assertEquals(4, rowContents[1].size)
        assert("9th Mar" in rowContents[1].values)
        assert("BILL PAYMENT VIA FASTER PAYMENT TO LEEDS SCHOOLS FO REFERENCE repay 240725 woodk ,MANDATE NO 0115" in rowContents[1].values)
        assert("528.00" in rowContents[1].values)
        assert("532.07" in rowContents[1].values)
    }

    private fun getTextByArea(): Map<RectArea, String> = mapOf(
        Pair(RectArea(54.074097F, 77.80386F, 1427.1011F, 1432.884F), "9th Mar"),
        Pair(RectArea(115.99988F, 394.70233F, 1427.1011F, 1432.884F), "FASTER PAYMENTS RECEIPT REF.U13G vMancESFA0802 FROM Leeds Schools' Foo"),
        Pair(RectArea(434.2948F, 455.70062F, 1427.1011F, 1432.884F), "324.00"),
        Pair(RectArea(521.75305F, 548.9968F, 1427.1011F, 1432.884F), "1,060.07"),
        Pair(RectArea(115.999954F, 445.10193F, 1436.6279F, 1442.4109F), "BILL PAYMENT VIA FASTER PAYMENT TO LEEDS SCHOOLS FO REFERENCE repay 240725 woodk ,"),
        Pair(RectArea(54.074127F, 77.803894F, 1440.6528F, 1446.4358F), "9th Mar"),
        Pair(RectArea(480.96454F, 502.37036F, 1440.6528F, 1446.4358F), "528.00"),
        Pair(RectArea(527.59375F, 548.9996F, 1440.6528F, 1446.4358F), "532.07"),
        Pair(RectArea(115.999954F, 180.18239F, 1444.6777F, 1450.4607F), "MANDATE NO 0115"),
    )
}
