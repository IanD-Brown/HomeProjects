import org.idb.Day3Solver

class Day3SolverTest : BaseDaySolverTest<Int, Int>(Day3Solver(), 161, 170807108, 48, 74838033) {

    override fun loadTestData() {
        solver.loadData(
            sequenceOf(
                "xmul(2,4)&mul[3,7]!^don't()_mul(5,5)+mul(32,64](mul(11,8)undo()?mul(8,5))")
        )
    }
}