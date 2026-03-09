package io.github.iandbrown.reconciler.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionViewTest {

    @Test
    fun testAsDouble() {
        assertEquals(1.23, asDouble(1.23))
        assertEquals(0.0, asDouble("not a double"))
        assertEquals(0.0, asDouble(null))
    }

    @Test
    fun testDescription() {
        assertEquals("test", description("test"))
        assertEquals("Unknown", description(123))
        assertEquals("Unknown", description(null))
    }

}
