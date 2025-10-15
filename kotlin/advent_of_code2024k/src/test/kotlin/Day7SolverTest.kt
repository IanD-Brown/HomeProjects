import org.idb.Day7Solver

class Day7SolverTest : BaseDaySolverTest<Long, Long>(Day7Solver(), 3749, 303766880536L, 11387, 337041851384440L) {
    override fun loadTestData() {
        solver.loadData(
            sequenceOf(
                "190: 10 19",
                "3267: 81 40 27",
                "83: 17 5",
                "156: 15 6",
                "7290: 6 8 6 15",
                "161011: 16 10 13",
                "192: 17 8 14",
                "21037: 9 7 18 13",
                "292: 11 6 16 20"
            )
        )
    }
}