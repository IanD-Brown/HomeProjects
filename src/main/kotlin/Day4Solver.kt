package org.idb
const val SEARCH = "XMAS"

private enum class Direction(val rAdjust : Int, val cAdjust : Int) {
    UP(-1, 0),
    DOWN(1, 0),
    LEFT(0, -1),
    RIGHT(0, 1),
    UPLEFT(-1, -1),
    UPRIGHT(-1, 1),
    DOWNLEFT(1, -1),
    DOWNRIGHT(1, 1)
}

/*
 * needs to locate 'star' patterns:
 * M.S    S.S    M.M    S.M
 * .A. or .A. or .A. or .A.
 * M.S    M.M    S.S    S.M
 */
private enum class Star(val tl : Char, val tr : Char, val bl : Char, val br : Char) {
    MM_TOP('M', 'M', 'S', 'S'),
    SS_TOP('S', 'S', 'M', 'M'),
    SM_TOP('S', 'M', 'S', 'M'),
    MS_TOP('M', 'S', 'M', 'S');

    fun match(data : List<String>, r : Int, c : Int) : Boolean {
        return data[r - 1][c - 1] == tl &&
                data[r - 1][c + 1] == tr &&
                data[r + 1][c - 1] == bl &&
                data[r + 1][c + 1] == br
    }
}

class Day4Solver : DaySolver<Int, Int>(4) {
    var data = listOf<String>()

    override fun loadData(lines : Sequence<String>) {
        data = lines.toList()
    }

    fun matchCount(r : Int, c : Int) : Int {
        var count = 0

        if (data[r][c] == SEARCH[0]) {
            for (direction in Direction.entries) {
                var match = true
                for (i in 1..<SEARCH.length) {
                    val adjustedR = r + i * direction.rAdjust
                    val adjustedC = c + i * direction.cAdjust

                    if (adjustedR < 0 || adjustedR >= data.size ||
                        adjustedC < 0 || adjustedC >= data[adjustedR].length ||
                        data[adjustedR][adjustedC] != SEARCH[i]) {
                        match = false
                        break
                    }
                }
                if (match) {
                    ++count
                }
            }
        }
        return count
    }

    override fun calcPart1() : Int {
        var result = 0
        for (r in data.indices) {
            for (c in data[r].indices) {
                result += matchCount(r, c)
            }
        }
        return result
    }

    override fun calcPart2(): Int {
        var result = 0
        for (r in 1..<data.size - 1) {
            for (c in 1..<data[r].length - 1) {
                if (data[r][c] == 'A') {
                    result += Star.entries.count { it.match(data, r, c) }
                }
            }
        }
        return result
    }
}
