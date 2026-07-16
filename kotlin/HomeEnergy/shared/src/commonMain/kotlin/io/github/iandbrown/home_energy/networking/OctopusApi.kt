package io.github.iandbrown.home_energy.networking

import io.github.iandbrown.home_energy.database.Meter
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OctopusConsumptionResponse(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<ConsumptionDto>
)

@Serializable
data class ConsumptionDto(
    val consumption: Double,
    @SerialName("interval_start") val intervalStart: String,
    @SerialName("interval_end") val intervalEnd: String
)

class OctopusApi(private val client: HttpClient) {
    suspend fun getConsumption(meter: Meter, url: String? = null, year: Short = 2023): OctopusConsumptionResponse {
        val requestUrl = url ?: "https://api.octopus.energy/v1/${meter.urlPart()}/?period_from=$year-01-01T00:00:00"
        return client.get(requestUrl).body()
    }

    private fun Meter.type() = if (electric) "electricity" else "gas"
    private fun Meter.urlPart() = "${type()}-meter-points/${meterPointAdminNumber}/meters/${serial}/consumption"
}
