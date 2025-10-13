package org.idb

class Grid {
    var rowCount = 0
    var colCount = 0

    fun withinBounds(row : Int, col : Int) : Boolean =
    row >= 0 && col >= 0 && row < rowCount && col < colCount
}

class Day8Solver : DaySolver<Int, Int>(8) {
    val grid = Grid()
    val locations = mutableMapOf<Char, MutableSet<Pair<Int, Int>>>()

    override fun loadData(lines : Sequence<String>) {
        lines.forEach { it -> run {
            if (grid.colCount == 0) {
                grid.colCount = it.length
            }
            for (i in it.indices) {
                when (val ch = it[i]) {
                    '.' -> {}
                    else -> {
                        val location = Pair(grid.rowCount, i)
                        locations.getOrPut(ch) {mutableSetOf()}.add(location)
                    }
                }
            }
            ++grid.rowCount
        } }
    }

    /*
    * Say outer is at 3/7
    *     inner might be at 5/4
    * need to set 1/10
    * i.e.
    * rowDiff = -2 and colDiff = 3
    */
    override fun calcPart1() : Int =
        extendLocations()

    private fun extendLocations(count: Int = 1): Int {
        val visited = mutableSetOf<Pair<Int, Int>>()
        for (freqLocations in locations.values) {
            for (outer in freqLocations) {
                for (inner in freqLocations) {
                    if (inner != outer) {
                        val rowDiff = outer.r() - inner.r()
                        val colDiff = outer.c() - inner.c()
                        for (i in 1.. count) {
                            if (grid.withinBounds(outer.r() + (i * rowDiff), outer.c() + (i * colDiff))) {
                                val a = Pair(outer.r() + (i * rowDiff), outer.c() + (i * colDiff))
                                visited.add(a)
                            } else {
                                break
                            }
                        }
                    }
                }
            }
            if (count > 1 && freqLocations.size > 1) {
                visited.addAll(freqLocations)
            }
        }
        return visited.size
    }

    override fun calcPart2(): Int =
        extendLocations(grid.rowCount)

    override fun clear() {
        locations.clear()
    }
}
