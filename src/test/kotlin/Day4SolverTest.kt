import org.idb.Day4Solver

class Day4SolverTest : BaseDaySolverTest<Int, Int>(Day4Solver(), 18, 2571, 9, 1992) {
    override fun loadTestData() {
        solver.loadData(
            sequenceOf(
                "MMMSXXMASM",
                "MSAMXMSMSA",
                "AMXSXMAAMM",
                "MSAMASMSMX",
                "XMASAMXAMM",
                "XXAMMXXAMA",
                "SMSMSASXSS",
                "SAXAMASAAA",
                "MAMMMXMMMM",
                "MXMXAXMASX"
            )
        )
    }
}