import org.idb.Day1Solver
import kotlin.test.Test
import kotlin.test.assertEquals

class Day1SolverTest {
    @Test
    fun whenSingleLineThenResultIsDiff() {
        val it = Day1Solver()

        it.loadData(sequenceOf("3   9"))

        assertEquals(6, it.calcPart1())
    }

    @Test
    fun whenMultipleLinesThenSortedResultsAreDiff() {
        val it = Day1Solver()

        loadTestData(it)
        assertEquals(11L, it.calcPart1())
    }

    private fun loadTestData(it: Day1Solver) {
        it.loadData(
            sequenceOf(
                "3   4",
                "4   3",
                "2   5",
                "1   3",
                "3   9",
                "3   3"
            )
        )
    }

    @Test
    fun whenPart1IsCalculatedFromFullData() {
        val it = Day1Solver()

        it.loadFromResource()

        assertEquals(1590491L, it.calcPart1())
    }

    @Test
    fun whenMultipleLinesThenPart2ResultIsSimilarity() {
        val it = Day1Solver()

        loadTestData(it)
        assertEquals(31L, it.calcPart2())
    }

    @Test
    fun whenPart2IsCalculatedFromFullData() {
        val it = Day1Solver()

        it.loadFromResource()

        assertEquals(22588371L, it.calcPart2())
    }
}