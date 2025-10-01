package org.idb

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    val day1Solver = Day1Solver()

    day1Solver.loadFromResource()

    println("result " + day1Solver.calcPart1())
}