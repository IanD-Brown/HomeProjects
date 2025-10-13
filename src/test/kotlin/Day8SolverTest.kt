import org.idb.Day8Solver

class Day8SolverTest : BaseDaySolverTest<Int, Int>(Day8Solver(), 14, 311, 34, 1115) {
    override fun loadTestData() {
        solver.loadData(
            sequenceOf("............",
                "........0...",
                ".....0......",
                ".......0....",
                "....0.......",
                "......A.....",
                "............",
                "............",
                "........A...",
                ".........A..",
                "............",
                "............"

            )
        )
    }
}