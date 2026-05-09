package io.github.iandbrown.reconciler.ui

import io.github.iandbrown.reconciler.logic.Range
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportDefinitionViewTest {

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

    @Test
    fun testGetByRange() {
        val row = mapOf(
            Pair(Range(54.074127F, 81.695854F), "16th Mar"),
            Pair(Range(115.99975F, 295.72314F), "BANK GIRO CREDIT REF WYPF 871625, WYPF 871625"),
            Pair(Range(428.4578F, 455.70157F), "1,158.61"),
            Pair(Range(521.75366F, 548.99744F), "1,627.15"),
        )
        assertEquals("16th Mar", getByRange(Range(54.074097F, 68.857956F) , row))
        assertEquals("BANK GIRO CREDIT REF WYPF 871625, WYPF 871625", getByRange(Range(115.999954F, 151.00664F) , row))
        assertEquals("1,158.61", getByRange(Range(427.30438F, 455.70312F) , row))
    }
}
