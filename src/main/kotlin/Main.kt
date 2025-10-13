package org.idb

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val solvers = listOf(Day1Solver(),
        Day2Solver(),
        Day3Solver(),
        Day4Solver(),
        Day5Solver(),
        Day6Solver(),
        Day7Solver(),
        Day8Solver())


    for (daySolver in solvers) {
        daySolver.loadFromResource()
        println("Day ${daySolver.day} part1=${daySolver.calcPart1()} part2=${daySolver.calcPart2()}")
        daySolver.clear()
    }
}