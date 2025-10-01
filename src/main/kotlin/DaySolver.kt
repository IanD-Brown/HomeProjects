package org.idb

abstract class DaySolver<P1, P2>(val day : Int) {
    abstract fun loadData(lines : Sequence<String>)
    abstract fun calcPart1() : P1
    abstract fun calcPart2() : P2

    fun loadFromResource() {
        val javaClass = this.javaClass
        val inputStream = javaClass.getResourceAsStream("/day" + day + "data")
        val reader = inputStream?.bufferedReader()
        val lines = reader?.readLines()
        loadData(lines!!.asSequence())
    }
}