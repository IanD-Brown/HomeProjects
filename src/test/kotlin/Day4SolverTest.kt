import org.idb.Day4Solver
import kotlin.test.Test
import kotlin.test.assertEquals

class Day4SolverTest {
    @Test
    fun testPart1_whenTestDataLoaded() {
        val it = Day4Solver()

        loadTestData(it)
        assertEquals(18, it.calcPart1())
    }

    @Test
    fun testPart1_whenResourceDataLoaded() {
        val it = Day4Solver()

        it.loadFromResource()

        assertEquals(2571, it.calcPart1())
    }

    @Test
    fun testPart2_whenTestDataLoaded() {
        val it = Day4Solver()

        loadTestData(it)
        assertEquals(9, it.calcPart2())
    }

    @Test
    fun testPart2_whenResourceDataLoaded() {
        val it = Day4Solver()

        it.loadFromResource()

        assertEquals(1992, it.calcPart2())
    }

    private fun loadTestData(it: Day4Solver) {
        it.loadData(sequenceOf(
                "MMMSXXMASM",
                "MSAMXMSMSA",
                "AMXSXMAAMM",
                "MSAMASMSMX",
                "XMASAMXAMM",
                "XXAMMXXAMA",
                "SMSMSASXSS",
                "SAXAMASAAA",
                "MAMMMXMMMM",
                "MXMXAXMASX"))
    }
}