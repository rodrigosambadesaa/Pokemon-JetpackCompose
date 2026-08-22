package com.mouredev.pokemonjetpackcompose.util

import android.content.Context

/**
 * Manages tiered connectivity checks for the Pokemon app.
 *
 * Tier 1 (Primary): Probes the app's backend domains (pokeapi.co, raw.githubusercontent.com).
 * Tier 2 (Extreme / Diagnostic Fallback): Probes public DNS resolvers and default global hosts
 * to determine whether an issue is specific to the PokeAPI backend or a general Internet outage.
 */
object AppConnectivityManager {

    val APP_ENDPOINTS = listOf(
        "https://pokeapi.co/api/v2/pokemon",
        "https://pokeapi.co/",
        "https://raw.githubusercontent.com/"
    )

    private val appConnectivity = ConnectivityAndInternetAccess.Builder()
        .setHosts(APP_ENDPOINTS)
        .setDnsResolvers(emptyList()) // DNS phase disabled for pure app HTTP/HTTPS reachability
        .build()

    private val fallbackConnectivity = ConnectivityAndInternetAccess.Builder().build()

    fun checkAppEndpointsAsync(
        context: Context,
        onResult: (result: ConnectivityAndInternetAccess.InternetResult) -> Unit
    ): ConnectivityAndInternetAccess.Request {
        return appConnectivity.checkInternetAsync(context) { result ->
            onResult(result)
        }
    }

    fun checkExtremeFallbackAsync(
        context: Context,
        onResult: (result: ConnectivityAndInternetAccess.InternetResult) -> Unit
    ): ConnectivityAndInternetAccess.Request {
        return fallbackConnectivity.checkInternetAsync(context) { result ->
            onResult(result)
        }
    }
}
