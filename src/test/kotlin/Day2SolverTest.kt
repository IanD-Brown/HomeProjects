import org.idb.Day2Solver

class Day2SolverTest : BaseDaySolverTest<Int, Int>(Day2Solver(), 2, 490, 4, 536) {
    override fun loadTestData() {
        solver.loadData(
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