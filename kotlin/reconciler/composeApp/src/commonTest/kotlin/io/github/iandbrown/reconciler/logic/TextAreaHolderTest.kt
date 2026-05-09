package io.github.iandbrown.reconciler.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TextAreaHolderTest {

    @Test
    fun testClear() {
        val holder = TextAreaHolder()
        holder.stringAt("test", sequenceOf(RectArea(0f, 10f, 0f, 10f)))
        holder.clear(5f)

        assertEquals(5f, holder.dropThreshold)
        assertTrue(holder.items.isEmpty())
        assertNull(holder.currentText)
        assertTrue(holder.positions.isEmpty())
    }

    @Test
    fun testStartPageAndOffset() {
        val holder = TextAreaHolder()

        // Page 1
        holder.startPage(10f, 20f, 100f)
        assertEquals(10f, holder.pageLowerLeftX)
        assertEquals(20f, holder.pageLowerLeftY)
        assertEquals(100f, holder.pageHeight)
        assertEquals(0f, holder.pageOffset)

        // Page 2
        holder.startPage(15f, 25f, 200f)
        assertEquals(15f, holder.pageLowerLeftX)
        assertEquals(25f, holder.pageLowerLeftY)
        assertEquals(200f, holder.pageHeight)
        assertEquals(100f, holder.pageOffset)
    }

    @Test
    fun testStringAtAndSaveCurrent() {
        val holder = TextAreaHolder()
        holder.clear(2f)
        holder.startPage(10f, 20f, 100f)

        // RectArea(left, right, top, bottom)
        // mergePositions uses:
        // x = it.left + pageLowerLeftX!!
        // y = pageOffset + it.top + pageLowerLeftY!!
        // left = min(x)
        // right = max(x + (it.right - it.left)) = max(it.right + pageLowerLeftX)
        // top = min(y)
        // bottom = max(y + (it.bottom - it.top)) + dropThreshold = max(it.bottom + pageLowerLeftY + pageOffset) + dropThreshold

        val rect1 = RectArea(5f, 15f, 30f, 40f)
        // x1 = 5 + 10 = 15
        // y1 = 0 + 30 + 20 = 50
        // w1 = 15 - 5 = 10. x1 + w1 = 25
        // h1 = 40 - 30 = 10. y1 + h1 = 60

        holder.stringAt("Hello", sequenceOf(rect1))

        val rect2 = RectArea(20f, 30f, 30f, 45f)
        // x2 = 20 + 10 = 30
        // y2 = 0 + 30 + 20 = 50
        // w2 = 30 - 20 = 10. x2 + w2 = 40
        // h2 = 45 - 30 = 15. y2 + h2 = 65

        holder.stringAt(" World", sequenceOf(rect2))

        holder.saveCurrent()

        assertEquals(1, holder.items.size)
        val mergedArea = holder.items.keys.first()
        assertEquals("Hello World", holder.items[mergedArea])

        // Merged area calculation:
        // left: min(15, 30) = 15
        // right: max(25, 40) = 40
        // top: min(50, 50) = 50
        // bottom: max(60, 65) + dropThreshold(2) = 67

        assertEquals(15f, mergedArea.left)
        assertEquals(40f, mergedArea.right)
        assertEquals(50f, mergedArea.top)
        assertEquals(67f, mergedArea.bottom)
    }

    @Test
    fun testMultiPageCollection() {
        val holder = TextAreaHolder()
        holder.clear(0f)

        // Page 1 (height 100, LL at 0,0)
        holder.startPage(0f, 0f, 100f)
        holder.stringAt("P1", sequenceOf(RectArea(0f, 10f, 10f, 20f)))
        holder.saveCurrent()

        // Page 2 (height 100, LL at 0,0)
        holder.startPage(0f, 0f, 100f)
        // pageOffset should be 100 now
        holder.stringAt("P2", sequenceOf(RectArea(0f, 10f, 10f, 20f)))
        holder.saveCurrent()

        assertEquals(2, holder.items.size)
        val sortedItems = holder.items.toList().sortedBy { it.first.top }

        assertEquals("P1", sortedItems[0].second)
        assertEquals(10f, sortedItems[0].first.top)
        assertEquals(20f, sortedItems[0].first.bottom)

        assertEquals("P2", sortedItems[1].second)
        assertEquals(110f, sortedItems[1].first.top)
        assertEquals(120f, sortedItems[1].first.bottom)
    }
}
