import org.idb.Day1Solver

class Day1SolverTest : BaseDaySolverTest<Long, Long>(Day1Solver(), 11, 1590491L, 31, 22588371L) {
    override fun loadTestData() {
        solver.loadData(
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
}