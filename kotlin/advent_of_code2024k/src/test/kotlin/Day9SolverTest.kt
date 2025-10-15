import org.idb.Day9Solver

class Day9SolverTest : BaseDaySolverTest<Long, Long>(Day9Solver(), 1928, 6320029754031L, 2858, 6347435485773L) {
    override fun loadTestData() {
        solver.loadData(
            sequenceOf("2333133121414131402")
        )
    }
}