package org.idb

class Day9Solver : DaySolver<Long, Long>(9) {
    private data class Block(val fileId : Int, val count : Int, var start : Int) {
        fun checksum() : Long {
            return (0..<count).sumOf { (it + start.toLong()) * fileId }
        }
    }
    private val sectorUse = mutableListOf<Int>()
    private val blocks = mutableListOf<Block>()

    override fun loadData(lines : Sequence<String>) {
        clear()
        var fileId = 0
        var freeSpace = false
        lines.take(1).forEach { it -> run {
            it.forEach { char ->
                run {
                    val number = char.digitToInt()
                    val currentFile = if (freeSpace) -1 else fileId++
                    blocks += Block(currentFile, number, sectorUse.size)
                    sectorUse.addAll(List(number) { currentFile })

                    freeSpace = !freeSpace

                }
            }
        } }
    }

    override fun calcPart1() : Long {
        var checksum = 0L
        var lastIndex = sectorUse.lastIndex
        for (i in sectorUse.indices) {
            if (i >= lastIndex) {
                break
            }
            if (sectorUse[i] == -1) {
                while (true) {
                    val moved = sectorUse[lastIndex]
                    if (moved >= 0) {
                        sectorUse[lastIndex] = -1
                        sectorUse[i] = moved
                        break
                    }
                    --lastIndex

                }
             }
            if (sectorUse[i] >= 0) {
                checksum += i * sectorUse[i]
            }
        }
        return checksum
    }

    override fun calcPart2(): Long {
        val freeBlocks = blocks.filter { it.fileId == -1 }.toMutableList()
        val usedBlocks = blocks.filter { it.fileId >= 0 }.toMutableList()
        for (i in usedBlocks.indices.reversed()) {
            for (j in freeBlocks.indices) {
                if (freeBlocks[j].start < usedBlocks[i].start && freeBlocks[j].count >= usedBlocks[i].count) {
                    usedBlocks[i].start = freeBlocks[j].start

                    if (freeBlocks[j].count > usedBlocks[i].count) {
                        freeBlocks[j] = Block(-1, freeBlocks[j].count - usedBlocks[i].count, freeBlocks[j].start + usedBlocks[i].count)
                    } else {
                        freeBlocks.removeAt(j)
                    }
                    break
                }
            }
        }
        return usedBlocks.sumOf { it.checksum() }
    }

    override fun clear() {
        sectorUse.clear()
    }
}
