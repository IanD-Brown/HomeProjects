package org.idb

class Day5Solver : DaySolver<Int, Int>(5) {
    class PageValidator {
        private val rules = mutableSetOf<Pair<Int, Int>>()
        private val pages = mutableListOf<Map<Int, Int>>()

        constructor(lines : Sequence<String>) {
            var ruleRegex = """(\d+)\|(\d+)""".toRegex()

            lines.forEach { it -> run {
                var ruleMatch = ruleRegex.matchEntire(it)

                if (ruleMatch != null) {
                    rules += Pair(ruleMatch.groupValues[1].toInt(), ruleMatch.groupValues[2].toInt())
                } else if (!it.isEmpty()) {
                    var index = 0
                    var mapping = mutableMapOf<Int, Int>()
                    for (s in it.split(',')) {
                        mapping[s.toInt()] = index++
                    }
                    pages.add(mapping)
                }
            } }
        }

        fun sumMidPages(corrected: Boolean = false): Int {
            var sumPages = 0

            for (page in pages) {
                if (validatePage(page)) {
                    if (!corrected) {
                        val mid = page.size / 2
                        for (p in page) {
                            if (p.value == mid) {
                                sumPages += p.key
                                break
                            }
                        }
                    }
                } else if (corrected) {
                    val toOrder = page.keys.toList()
                    val ordered = toOrder.sortedWith { n1, n2 ->
                        when {
                            rules.contains(Pair(n1, n2)) -> -1
                            rules.contains(Pair(n2, n1)) -> 1
                            else -> 0
                        }
                    }
                    val sortedPage = mutableMapOf<Int, Int>()
                    for (i in ordered.indices) {
                        sortedPage[ordered[i]] = i
                    }
                    if (validatePage(sortedPage)) {
                        sumPages += ordered[toOrder.size / 2]
                    }
                }
            }

            return sumPages
        }

        private fun validatePage(page: Map<Int, Int>): Boolean {
            for (rule in rules) {
                val firstPos = page[rule.first]
                val secondPos = page[rule.second]

                if (firstPos != null && secondPos != null && firstPos > secondPos) {
                    return false
                }
            }
            return true
        }
    }

    var pageValidator : PageValidator? = null

    override fun loadData(lines : Sequence<String>) {
        pageValidator = PageValidator(lines)
    }

    override fun calcPart1() : Int {
        return pageValidator!!.sumMidPages()
    }

    override fun calcPart2(): Int {
        return pageValidator!!.sumMidPages(true)
    }

    override fun clear() {
        pageValidator = null
    }
}
