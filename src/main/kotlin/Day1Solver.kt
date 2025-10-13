package org.idb

import kotlin.math.abs

class Day1Solver : DaySolver<Long, Long>(1) {
    val left = mutableListOf<Long>()
    val right = mutableListOf<Long>()

    override fun loadData(lines : Sequence<String>) {
        val regex = """(\d+)( +)(\d+)""".toRegex()
        lines.forEach { s ->
            run {
                val matchResult = regex.find(s)
                if (matchResult != null && !matchResult.groupValues.isEmpty()) {
                    left += matchResult.groupValues[1].toLong()
                    right += matchResult.groupValues[3].toLong()
                }
            }
        }
    }

    override fun calcPart1() : Long {
        var result = 0L
        left.sort()
        right.sort()
        for (i in left.indices) {
            result += abs(right[i] - left[i])
        }
        return result
    }

    override fun calcPart2(): Long {
        var result = 0L
        val similarity = mutableMapOf<Long, Long>()

        left.forEach {
            l -> if (!similarity.containsKey(l)) {
                similarity[l] = l * right.count { r -> r == l}
            }
            result += similarity.getOrDefault(l, 0)
        }
        return result
    }

    override fun clear() {
        left.clear()
        right.clear()
    }
}
