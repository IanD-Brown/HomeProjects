package io.github.iandbrown.reconciler.ui

import io.github.iandbrown.reconciler.database.Rule
import io.github.iandbrown.reconciler.database.Transaction
import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionViewTest {

    @Test
    fun testAsDouble() {
        assertEquals(1.23, asDouble(1.23))
        assertEquals(0.0, asDouble("not a double"))
        assertEquals(0.0, asDouble(null))
    }

    @Test
    fun testDescription() {
        assertEquals("test", description("test"))
        assertEquals("Unknown", description(123))
        assertEquals("Unknown", description(null))
    }

    @Test
    fun testTransactionsByAmount_FiltersByDate() {
        val transactions = listOf(
            Transaction(1, 0, 100, "t1", 10.0),
            Transaction(1, 1, 200, "t2", 10.0)
        )
        val result = transactionsByAmount(emptyList(), transactions, 150)
        assertEquals(1, result.size)
        assertEquals(1, result[10.0]?.size)
        assertEquals("t2", result[10.0]?.first()?.description)
    }

    @Test
    fun testTransactionsByAmount_FiltersNoiseAndIncome() {
        val rules = listOf(
            Rule(0, "noise", RuleType.NOISE.ordinal),
            Rule(0, "income", RuleType.INCOME.ordinal)
        )
        val transactions = listOf(
            Transaction(1, 0, 100, "noise transaction", 10.0),
            Transaction(1, 1, 100, "income transaction", 10.0),
            Transaction(1, 2, 100, "other", 10.0)
        )
        val result = transactionsByAmount(rules, transactions, 0)
        assertEquals(1, result[10.0]?.size)
        assertEquals("other", result[10.0]?.first()?.description)
    }

    @Test
    fun testTransactionsByAmount_RemovesZeroSumGroups() {
        val transactions = listOf(
            Transaction(1, 0, 100, "t1", 10.0),
            Transaction(1, 1, 100, "t2", -10.0),
            Transaction(1, 2, 100, "t3", 5.0)
        )
        val result = transactionsByAmount(emptyList(), transactions, 0)
        assertEquals(1, result.size)
        assertEquals(1, result[5.0]?.size)
    }

    @Test
    fun testFilterTransactions_FiltersOtherRules() {
        val rules = listOf(
            Rule(0, "other", RuleType.OTHER.ordinal)
        )
        val transactions = listOf(
            Transaction(1, 0, 100, "other transaction", 10.0),
            Transaction(1, 1, 100, "valid", 10.0)
        )
        val result = filterTransactions(0, transactions, rules)
        assertEquals(1, result.size)
        assertEquals("valid", result.first().description)
    }
}
