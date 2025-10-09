package org.idb

import kotlin.Int
import kotlin.Pair
import kotlin.collections.getOrPut
import kotlin.collections.mutableMapOf

private enum class Day6Direction(val rAdjust: Int, val cAdjust : Int) {
    UP(-1, 0),
    DOWN(1, 0),
    LEFT(0, -1),
    RIGHT(0, 1);

    fun adjust(position : Pair<Int, Int>) : Pair<Int, Int> = Pair(position.first + rAdjust, position.second+cAdjust)
    fun turn() : Day6Direction =
        when(this) {
            UP -> RIGHT
            DOWN -> LEFT
            RIGHT -> DOWN
            LEFT -> UP
        }
}

private fun Pair<Int, Int>.r() : Int = this.first
private fun Pair<Int, Int>.c() : Int = this.second

private fun Pair<Int, Int>.isOutOfBounds(data : List<String>) : Boolean =
    r() < 0 || c() < 0 || r() >= data.size || c() >= data[r()].length

class Day6Solver : DaySolver<Int, Int>(6) {
    private var data = listOf<String>()
    private var addedBlock : Pair<Int, Int>? = null

    override fun loadData(lines : Sequence<String>) {
        data = lines.toList()
    }

    private fun findStartPosition(): Pair<Int, Int> {
        for (r in data.indices) {
            val pos = data[r].indexOf('^')

            if (pos >= 0) {
                return Pair(r, pos)
            }
        }
        error("Start location not found")
    }

    private fun isBlocked(position : Pair<Int, Int>) : Boolean {
        return data[position.r()][position.c()] == '#' || position == addedBlock
    }

    private fun guardMoves() : Map<Pair<Int, Int>, Set<Day6Direction>> {
        val visited = mutableMapOf<Pair<Int, Int>, MutableSet<Day6Direction>>()
        moveGuard(visited)

        return visited
    }

    private fun moveGuard(visited : MutableMap<Pair<Int, Int>, MutableSet<Day6Direction>>) : Boolean {
        var position = findStartPosition()

        var heading = Day6Direction.UP
        while (!position.isOutOfBounds(data)) {
            if (!visited.getOrPut(position) { mutableSetOf() }.add(heading)) {
                return false
            }
            var newPosition = heading.adjust(position)
            if (newPosition.isOutOfBounds(data)) {
                return true
            }
            if (isBlocked(newPosition)) {
                do {
                    heading = heading.turn()
                    newPosition = heading.adjust(position)
                } while (isBlocked(newPosition))
            }
            position = newPosition
        }

        return true
    }

    override fun calcPart1() : Int = guardMoves().size

    override fun calcPart2(): Int {
        val result = guardMoves().keys.count {
            addedBlock = it
            !moveGuard(mutableMapOf())
        }
        addedBlock = null
        return result
    }
}
