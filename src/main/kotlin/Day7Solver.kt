package org.idb

class Day7Solver : DaySolver<Long, Long>(7) {
    private enum class Operator {
        MULTIPLY, ADD, CONCATENATE;

        fun process(first : Long, second : Long) : Long =
            when (this) {
                MULTIPLY -> first * second
                ADD -> first + second
                CONCATENATE -> (first.toString() + second.toString()).toLong()
            }
    }

    private class Equation {
        val total : Long
        val numbers : List<Long>

        constructor(line : String) {
            val pos = line.indexOf(':')
            total = line.take(pos).toLong()
            numbers = line.substring(pos + 2).splitToSequence(" ").map { it.toLong() }.toList()
        }

        private fun calibrate(runningTotal: Long, index: Int, operators: List<Operator>) : Long {
            for (operator in operators) {
                val newRunning = operator.process(runningTotal, numbers[index])
                val lastIndex = index == numbers.size - 1

                if (lastIndex) {
                    if (newRunning == total) {
                        return total
                    }
                    if (operator == operators[operators.lastIndex]) {
                        return -1
                    }
                } else if (newRunning <= total && calibrate(newRunning, index + 1, operators) == total) {
                    return total
                }
            }
            return -1
        }

        fun calibrate(operators: List<Operator>): Long =
            if (calibrate(numbers[0], 1, operators) == total) {
                total
            } else {
                0
            }
    }
    var data = listOf<String>()

    override fun loadData(lines : Sequence<String>) {
        data = lines.toList()
    }

    override fun calcPart1() : Long
        = data.asSequence()
            .map{ Equation(it) }
            .map { it.calibrate(Operator.entries.asSequence().filter { item -> item != Operator.CONCATENATE }.toList()) }
            .sum()

    override fun calcPart2(): Long
        = data.asSequence()
            .map{ Equation(it) }
            .map { it.calibrate(Operator.entries.toList()) }
            .sum()
}
