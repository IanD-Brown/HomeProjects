package io.github.iandbrown.home_energy.ui

import io.github.iandbrown.home_energy.database.MeterTariff
import io.github.iandbrown.home_energy.database.RawUsage
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class MonthlyStatisticsTest {

    private val csvFileName = "RawUsages_202607151651.csv"

    @Test
    fun testBuildUsageFromCsv() {
        val rawUsages = parseCsv()
        assertTrue(rawUsages.isNotEmpty(), "Raw usages should not be empty")

        val tariffs = listOf(
            MeterTariff(meterId = 1, fromHour = 0, fromPeriod = 0, toHour = 24, toPeriod = 0, tariff = 0.20, id = 1),
            MeterTariff(meterId = 2, fromHour = 0, fromPeriod = 0, toHour = 24, toPeriod = 0, tariff = 0.15, id = 2),
            MeterTariff(meterId = 3, fromHour = 0, fromPeriod = 0, toHour = 24, toPeriod = 0, tariff = 0.25, id = 3)
        )

        val stats = MonthlyStatistics(rawUsages, tariffs)

        // Verify some results
        assertTrue(stats.monthlyBill.isNotEmpty(), "Monthly bill should not be empty")
        assertTrue(stats.monthlyKWh.isNotEmpty(), "Monthly kWh should not be empty")

        for (expected in expectedkWh()) {
            val meterMonth = MeterMonth(expected.first.first, expected.first.second)
            val actual = stats.monthlyKWh[meterMonth] ?: 0.0
            val tolerance = 5 + expected.second * 0.8 // TODO, make this more accurate
            val minExpected = expected.second - tolerance
            val maxException = expected.second + tolerance
            assertTrue(  minExpected < actual && actual < maxException, "Monthly kWh for ${expected.first} should be $minExpected to $maxException actual $actual" )
        }
    }

    private fun expectedkWh() : List<Pair<Pair<Short, Short>, Double>> {
        return listOf(
            Pair(Pair(1, 1), 289.98),
            Pair(Pair(1, 2), 216.64),
            Pair(Pair(1, 3), 245.24),
            Pair(Pair(1, 4), 115.79),
            Pair(Pair(1, 5), 98.41),
            Pair(Pair(1, 6), 114.79),
            Pair(Pair(1, 7), 92.36),
            Pair(Pair(1, 8), 147.17),
            Pair(Pair(1, 9), 182.74),
            Pair(Pair(1, 10), 230.94),
            Pair(Pair(1, 11), 295.58),
            Pair(Pair(1, 12), 358.39),
            Pair(Pair(2, 1), 40.90),
            Pair(Pair(2, 2), 71.71),
            Pair(Pair(2, 3), 268.81),
            Pair(Pair(2, 4), 398.84),
            Pair(Pair(2, 5), 403.29),
            Pair(Pair(2, 6), 351.58),
            Pair(Pair(2, 7), 236.73),
            Pair(Pair(2, 8), 263.19),
            Pair(Pair(2, 9), 220.02),
            Pair(Pair(2, 10), 127.80),
            Pair(Pair(2, 11), 44.94),
            Pair(Pair(2, 12), 17.92),
            Pair(Pair(3, 1), 3027.15),
            Pair(Pair(3, 2), 2315.23),
            Pair(Pair(3, 3), 1591.30),
            Pair(Pair(3, 4), 533.46),
            Pair(Pair(3, 5), 215.77),
            Pair(Pair(3, 6), 21.14),
            Pair(Pair(3, 8), 0.86),
            Pair(Pair(3, 9), 232.93),
            Pair(Pair(3, 10), 836.91),
            Pair(Pair(3, 11), 1701.27),
            Pair(Pair(3, 12), 2550.79),
        )
    }

    private fun parseCsv(): List<RawUsage> {
        val inputStream = this::class.java.classLoader.getResourceAsStream(csvFileName)
            ?: return emptyList()

        val lines = inputStream.bufferedReader().readLines()
        if (lines.isEmpty()) return emptyList()

        // Skip header
        return lines.drop(1).mapNotNull { line ->
            val parts = line.split(",")
            if (parts.size >= 6) {
                try {
                    RawUsage(
                        year = parts[0].toShort(),
                        month = parts[1].toShort(),
                        day = parts[2].toShort(),
                        period = parts[3].toShort(),
                        meterId = parts[4].toInt(),
                        averageConsumption = parts[5].toDouble()
                    )
                } catch (_: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
}
