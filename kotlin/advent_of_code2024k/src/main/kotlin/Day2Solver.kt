package org.idb

import kotlin.math.abs

class Day2Solver : DaySolver<Int, Int>(2) {
    var data : List<List<String>> = listOf()

    override fun loadData(lines : Sequence<String>) {
        data = lines.map { it.split(" ") }.toList()
    }

    override fun calcPart1() : Int {
        return data.count { list -> validate(list) }
    }

    private fun validate(list: List<String>): Boolean {
        var ascending = false
        var prior = 0
        for (i in list.indices) {
            val current = list[i].toInt()
            when (i) {
                0 -> prior = current
                1 -> {
                    if (prior == current || abs(prior - current) > 3) {
                        return false
                    }
                    ascending = current > prior
                    prior = current
                }

                else -> {
                    if (ascending) {
                        if (current <= prior || current > prior + 3) {
                            return false
                        }
                    } else if (current >= prior || current < prior - 3) {
                        return false
                    }
                    prior = current
                }
            }
        }
        return true
    }

    fun validateSubList(list: List<String>) : Boolean {
        for (i in list.indices) {
            if (validate(list.filterIndexed{ index, _ -> index != i })) {
                return true
            }
        }
        return false
    }

    override fun calcPart2(): Int {
        return data.count { list -> validate(list) || validateSubList(list) }
    }

    override fun clear() {
        data = listOf()
    }
}
