package org.idb

import kotlin.time.measureTime

fun main() {
    val solvers = listOf(Day1Solver(),
        Day2Solver(),
        Day3Solver(),
        Day4Solver(),
        Day5Solver(),
        Day6Solver(),
        Day7Solver(),
        Day8Solver(),
        Day9Solver())


    for (daySolver in solvers) {
        val elapsed = measureTime {
            daySolver.loadFromResource()
            print("Day ${daySolver.day} part1=${daySolver.calcPart1()} part2=${daySolver.calcPart2()}")
            daySolver.clear()
        }
        println(": ${elapsed.inWholeMilliseconds} ms")
    }
}