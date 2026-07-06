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

@OptIn(KoinExperimentalAPI::class)
val uiModule = module {
    viewModelOf(::MeterViewModel)
    viewModelOf(::UsageViewModel)

    single {
        HttpClient {
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
        MeterRoute(navigate = { meter -> backstack.add(Route.MeterEditor(meter)) })
    }

    navigation<Route.Usage> {
        UsageRoute()
    }

    navigation<Route.MeterEditor> { route ->
        val backstack = LocalBackstack.current
        MeterEditorRoute(meter = route.meter, done = { backstack.removeLastOrNull() })
    }
}
