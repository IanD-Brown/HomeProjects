import org.idb.Day2Solver
import kotlin.test.Test
import kotlin.test.assertEquals

class Day2SolverTest {
    @Test
    fun testPart1_whenTestDataLoaded() {
        val it = Day2Solver()

        loadTestData(it)
        assertEquals(2, it.calcPart1())
    }

    @Test
    fun testPart1_whenResourceDataLoaded() {
        val it = Day2Solver()

        it.loadFromResource()

        assertEquals(490, it.calcPart1())
    }

    @Test
    fun testPart2_whenTestDataLoaded() {
        val it = Day2Solver()

        loadTestData(it)
        assertEquals(4, it.calcPart2())
    }

    @Test
    fun testPart2_whenResourceDataLoaded() {
        val it = Day2Solver()

        it.loadFromResource()

        assertEquals(536, it.calcPart2())
    }

    private fun loadTestData(it: Day2Solver) {
        it.loadData(
            sequenceOf(
                "7 6 4 2 1",
                "1 2 7 8 9",
                "9 7 6 2 1",
                "1 3 2 4 5",
                "8 6 4 4 1",
                "1 3 6 7 9")
        )
    }
}