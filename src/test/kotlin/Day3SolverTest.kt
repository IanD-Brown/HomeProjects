import org.idb.Day3Solver
import kotlin.test.Test
import kotlin.test.assertEquals

class Day3SolverTest {
    @Test
    fun testPart1_whenTestDataLoaded() {
        val it = Day3Solver()

        loadTestData(it)
        assertEquals(161, it.calcPart1())
    }

    @Test
    fun testPart1_whenResourceDataLoaded() {
        val it = Day3Solver()

        it.loadFromResource()

        assertEquals(170807108, it.calcPart1())
    }

    @Test
    fun testPart2_whenTestDataLoaded() {
        val it = Day3Solver()

        loadTestData(it)
        assertEquals(48, it.calcPart2())
    }

    @Test
    fun testPart2_whenResourceDataLoaded() {
        val it = Day3Solver()

        it.loadFromResource()

        assertEquals(74838033, it.calcPart2())
    }

    private fun loadTestData(it: Day3Solver) {
        it.loadData(
            sequenceOf(
                "xmul(2,4)&mul[3,7]!^don't()_mul(5,5)+mul(32,64](mul(11,8)undo()?mul(8,5))")
        )
    }
}