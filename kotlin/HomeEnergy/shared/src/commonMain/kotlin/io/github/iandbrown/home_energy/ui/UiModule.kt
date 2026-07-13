package io.github.iandbrown.home_energy.ui

import org.koin.dsl.navigation3.navigation
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.core.annotation.KoinExperimentalAPI
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.github.iandbrown.home_energy.repository.SettingsRepository
import io.ktor.client.plugins.HttpRequestRetry
import org.koin.core.module.dsl.viewModel

@OptIn(KoinExperimentalAPI::class)
val uiModule = module {
    viewModel { MeterTariffViewModel(it.get(), get()) }
    viewModelOf(::MeterTariffsViewModel)
    viewModelOf(::MeterViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::UsageViewModel)

    single {
        HttpClient {
            install(HttpRequestRetry) {
                // Retries on any 5xx response (including 502)
                retryOnServerErrors(maxRetries = 3)

                // Optional: Add exponential backoff delay
                exponentialDelay()
            }
            install(Auth) {
                basic {
                    credentials {
                        val currentSettings = get<SettingsRepository>().settings.value
                        BasicAuthCredentials(
                            username = currentSettings?.apiKey ?: "",
                            password = currentSettings?.apiPassword ?: ""
                        )
                    }
                    sendWithoutRequest { true }
                }
            }
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    navigation<Route.Root> {
        val backstack = LocalBackstack.current
        RouteScreen(onNavigate = { backstack.add(it) })
    }

    navigation<Route.Meters> {
        val backstack = LocalBackstack.current
        MeterRoute(showMeterEditor = { meter -> backstack.add(Route.MeterEditor(meter)) },
            editTariff = { meter -> backstack.add(Route.MeterTariffList(meter!!)) })
    }

    navigation<Route.Usage> {
        UsageList()
    }

    navigation<Route.Settings> {
        val backstack = LocalBackstack.current
        SettingsRoute(navigate = { setting -> backstack.add(Route.SettingEditor(setting)) })
    }

    navigation<Route.SettingEditor> { route ->
        val backstack = LocalBackstack.current
        SettingsEditorRoute(route.setting) { backstack.removeLastOrNull() }
    }

    navigation<Route.MeterEditor> { route ->
        val backstack = LocalBackstack.current
        MeterEditorRoute(meter = route.meter, done = { backstack.removeLastOrNull() })
    }

    navigation<Route.MeterTariffList> { route ->
        val backstack = LocalBackstack.current
        MeterTariffListRoute(meter = route.meter) { meterId, meterTariff ->
            backstack.add(Route.MeterTariffEditor(meterId, meterTariff)) }
    }

    navigation<Route.MeterTariffEditor> {route ->
        val backstack = LocalBackstack.current
        MeterTariffEditorRoute(route.meterId, route.meterTariff) { backstack.removeLastOrNull() }
    }

    navigation<Route.Future> {
        FutureScreen()
    }
}
