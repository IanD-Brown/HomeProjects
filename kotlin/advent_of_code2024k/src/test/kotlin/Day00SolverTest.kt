import org.idb.Day00Solver

class Day00SolverTest : BaseDaySolverTest<Int, Int>(Day00Solver(), 143, 6951, 123, 4121) {
    override fun loadTestData() {
        solver.loadData(
            sequenceOf(
            )
        )
    }
}