package io.github.iandbrown.home_energy.ui

import io.github.iandbrown.home_energy.database.MeterTariff
import io.github.iandbrown.home_energy.database.RawUsage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonthlyStatisticsTest {

    private val csvFileName = "RawUsages_202607151651.csv"

    @Test
    fun testSingleDayUsageIsMonthFigure() {
        val rawUsages = listOf(RawUsage(2025, 6, 3, 1, 3, 20.0))
        val tariffs = listOf(MeterTariff(3, fromHour = 0, 0, 24, 0, 0.25))
        val stats = MonthlyStatistics(rawUsages, tariffs)

        assertEquals(
            20.0,
            stats.getMonthlyKWh(MeterMonth(3, 6)),
            "When not all days have values, the usage is the sum"
        )

        assertEquals(
            5.0,
            stats.getMonthlyBill(MeterMonth(3, 6), 2025, 0.0),
            "When not all days have values, the bill is correct"
        )
    }

    @Test
    fun testWhenAllDaysInMonthHaveValuesThenUsageIsTotal() {
        val rawUsages = listOf(
            RawUsage(2025, 6, 1, 1, 3, 30.0),
            RawUsage(2025, 6, 2, 1, 3, 30.0),
            RawUsage(2025, 6, 3, 1, 3, 30.0),
            RawUsage(2025, 6, 4, 1, 3, 30.0),
            RawUsage(2025, 6, 5, 1, 3, 30.0),
            RawUsage(2025, 6, 6, 1, 3, 30.0),
            RawUsage(2025, 6, 7, 1, 3, 30.0),
            RawUsage(2025, 6, 8, 1, 3, 30.0),
            RawUsage(2025, 6, 9, 1, 3, 30.0),
            RawUsage(2025, 6, 10, 1, 3, 30.0),
            RawUsage(2025, 6, 11, 1, 3, 30.0),
            RawUsage(2025, 6, 12, 1, 3, 30.0),
            RawUsage(2025, 6, 13, 1, 3, 30.0),
            RawUsage(2025, 6, 14, 1, 3, 30.0),
            RawUsage(2025, 6, 15, 1, 3, 30.0),
            RawUsage(2025, 6, 16, 1, 3, 30.0),
            RawUsage(2025, 6, 17, 1, 3, 30.0),
            RawUsage(2025, 6, 18, 1, 3, 30.0),
            RawUsage(2025, 6, 19, 1, 3, 30.0),
            RawUsage(2025, 6, 20, 1, 3, 30.0),
            RawUsage(2025, 6, 21, 1, 3, 30.0),
            RawUsage(2025, 6, 22, 1, 3, 30.0),
            RawUsage(2025, 6, 23, 1, 3, 30.0),
            RawUsage(2025, 6, 24, 1, 3, 30.0),
            RawUsage(2025, 6, 25, 1, 3, 30.0),
            RawUsage(2025, 6, 26, 1, 3, 30.0),
            RawUsage(2025, 6, 27, 1, 3, 30.0),
            RawUsage(2025, 6, 28, 1, 3, 30.0),
            RawUsage(2025, 6, 29, 1, 3, 30.0),
            RawUsage(2025, 6, 30, 1, 3, 30.0),
            )
        val stats = MonthlyStatistics(rawUsages, emptyList())

        assertEquals(
            900.0,
            stats.getMonthlyKWh(MeterMonth(3, 6)),
            "When not all days have values, the usage is averaged across the month"
        )
    }

    @Test
    fun testUsageInMultipleYearsForSameMonth() {
        val rawUsages = listOf(
            RawUsage(2023, 6, 3, 1, 3, 15.0),
            RawUsage(2024, 6, 3, 1, 3, 12.0),
            RawUsage(2024, 6, 3, 2, 3, 38.0),
            RawUsage(2025, 6, 3, 1, 3, 25.0)
        )
        val stats = MonthlyStatistics(rawUsages, emptyList())

        assertEquals(
            30.0,
            stats.getMonthlyKWh(MeterMonth(3, 6)),
            "When not all days have values, the usage is averaged across the month"
        )

    }

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

        for ((meterMonth, value) in expectedKWh()) {
            val actual = stats.getMonthlyKWh(meterMonth)
            val tolerance = 0.01
            val minExpected = value  - tolerance
            val maxException = value + tolerance
            assertTrue(  minExpected < actual && actual < maxException, "Monthly kWh for $meterMonth should be $minExpected to $maxException actual $actual" )
        }
    }

    private fun expectedKWh() : Map<MeterMonth, Double> {
        return mapOf(
            MeterMonth(1, 1) to 286.53,
            MeterMonth(1, 2) to 213.29,
            MeterMonth(1, 3) to 238.21,
            MeterMonth(1, 4) to 112.34,
            MeterMonth(1, 5) to 96.60,
            MeterMonth(1, 6) to 111.56,
            MeterMonth(1, 7) to 107.33,
            MeterMonth(1, 8) to 85.20,
            MeterMonth(1, 9) to 179.98,
            MeterMonth(1, 10) to 228.53,
            MeterMonth(1, 11) to 289.63,
            MeterMonth(1, 12) to 350.27,
            MeterMonth(2, 1) to 40.15,
            MeterMonth(2, 2) to 70.74,
            MeterMonth(2, 3) to 267.66,
            MeterMonth(2, 4) to 396.77,
            MeterMonth(2, 5) to 400.60,
            MeterMonth(2, 6) to 349.95,
            MeterMonth(2, 7) to 400.87,
            MeterMonth(2, 8) to 167.95,
            MeterMonth(2, 9) to 219.17,
            MeterMonth(2, 10) to 126.84,
            MeterMonth(2, 11) to 43.62,
            MeterMonth(2, 12) to 16.94,
            MeterMonth(3, 1) to 2627.50,
            MeterMonth(3, 2) to 1919.30,
            MeterMonth(3, 3) to 1218.21,
            MeterMonth(3, 4) to 375.88,
            MeterMonth(3, 5) to 141.82,
            MeterMonth(3, 6) to 12.6,
            MeterMonth(3, 7) to 2.7,
            MeterMonth(3, 8) to 0.5,
            MeterMonth(3, 9) to 166.72,
            MeterMonth(3, 10) to 625.52,
            MeterMonth(3, 11) to 1394.42,
            MeterMonth(3, 12) to 2109.49,
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
