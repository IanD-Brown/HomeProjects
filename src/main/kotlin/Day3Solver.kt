package org.idb

class Day3Solver : DaySolver<Int, Int>(3) {
    var data = ""

    override fun loadData(lines : Sequence<String>) {
        data = lines.joinToString(separator = "\n")
    }

    override fun calcPart1() : Int {
        var result = 0
        """mul\((\d+),(\d+)\)"""
            .toRegex()
            .findAll(data)
            .forEach { it -> run {
                result += it.groupValues[1].toInt() * it.groupValues[2].toInt()
            }
        }
        return result
    }

    override fun calcPart2(): Int {
        var result = 0
        var adding = true
        """don't\(\)|do\(\)|mul\((\d+),(\d+)\)"""
            .toRegex()
            .findAll(data)
            .forEach { it ->
                run {
                    when(it.groupValues[0]) {
                        "do()" -> adding = true
                        "don't()" -> adding = false
                        else -> if (adding) {
                            result += it.groupValues[1].toInt() * it.groupValues[2].toInt()
                        }
                    }
                }
            }
        return result
    }
}
