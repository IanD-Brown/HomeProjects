package io.github.iandbrown.home_energy.ui

import io.github.iandbrown.home_energy.database.MeterTariff
import io.github.iandbrown.home_energy.database.RawUsage
import kotlin.test.Test
import kotlin.test.assertTrue

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

        for ((meterMonth, value) in expectedKWh()) {
            val actual = stats.getMonthlyKWh(meterMonth, 2026)
            val tolerance = value + 0.01
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
            MeterMonth(1, 8) to 132.04,
            MeterMonth(1, 9) to 179.98,
            MeterMonth(1, 10) to 228.53,
            MeterMonth(1, 11) to 289.63,
            MeterMonth(1, 12) to 350.27,
            MeterMonth(2, 1) to 40.15,
            MeterMonth(2, 2) to 72.02,
            MeterMonth(2, 3) to 267.66,
            MeterMonth(2, 4) to 396.77,
            MeterMonth(2, 5) to 400.60,
            MeterMonth(2, 6) to 349.95,
            MeterMonth(2, 7) to 400.87,
            MeterMonth(2, 8) to 264.72,
            MeterMonth(2, 9) to 219.17,
            MeterMonth(2, 10) to 126.84,
            MeterMonth(2, 11) to 43.62,
            MeterMonth(2, 12) to 17.22,
            MeterMonth(3, 1) to 2627.50,
            MeterMonth(3, 2) to 1919.30,
            MeterMonth(3, 3) to 1302.22,
            MeterMonth(3, 4) to 563.81,
            MeterMonth(3, 5) to 351.72,
            MeterMonth(3, 6) to 151.21,
            MeterMonth(3, 7) to 0.0,
            MeterMonth(3, 8) to 7.74,
            MeterMonth(3, 9) to 416.79,
            MeterMonth(3, 10) to 745.81,
            MeterMonth(3, 11) to 1494.02,
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
