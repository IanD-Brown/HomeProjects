import org.idb.Day6Solver

class Day6SolverTest : BaseDaySolverTest<Int, Int>(Day6Solver(), 41, 5177, 6, 1686) {
    override fun loadTestData() {
        solver.loadData(
            sequenceOf("....#.....",
                ".........#",
                "..........",
                "..#.......",
                ".......#..",
                "..........",
                ".#..^.....",
                "........#.",
                "#.........",
                "......#..."

            )
        )
    }

}