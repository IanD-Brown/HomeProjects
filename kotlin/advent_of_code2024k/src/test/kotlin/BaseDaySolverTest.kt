import org.idb.DaySolver
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class BaseDaySolverTest<P1, P2>(
    val solver: DaySolver<P1, P2>,
    val testData1: P1,
    val resourceData1: P1,
    val testData2: P2,
    val resourceData2: P2
) {
    @Test
    fun testPart1_whenTestDataLoaded() {
        loadTestData()
        assertEquals(testData1, solver.calcPart1())
    }

    @Test
    fun testPart1_whenResourceDataLoaded() {
        solver.loadFromResource()

        assertEquals(resourceData1, solver.calcPart1())
    }

    @Test
    fun testPart2_whenTestDataLoaded() {
        loadTestData()
        assertEquals(testData2, solver.calcPart2())
    }

    @Test
    fun testPart2_whenResourceDataLoaded() {
        solver.loadFromResource()

        assertEquals(resourceData2, solver.calcPart2())
    }

    abstract fun loadTestData()
}