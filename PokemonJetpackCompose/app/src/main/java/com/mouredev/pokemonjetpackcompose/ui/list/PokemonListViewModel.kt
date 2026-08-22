package com.mouredev.pokemonjetpackcompose.ui.list

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mouredev.pokemonjetpackcompose.api.PokemonAPI
import com.mouredev.pokemonjetpackcompose.model.Pokemon
import com.mouredev.pokemonjetpackcompose.util.AppConnectivityManager
import com.mouredev.pokemonjetpackcompose.util.ConnectivityAndInternetAccess

/**
 * Created by MoureDev by Brais Moure on 28/10/22.
 * Modernized with comprehensive connectivity observation & tiered active checks.
 */
class PokemonListViewModel : ViewModel() {

    var pokemonList: List<Pokemon> by mutableStateOf(listOf())
    var isLoadingData: Boolean by mutableStateOf(false)
    var errorLoadingData: Boolean by mutableStateOf(false)

    // Passive network state
    var networkState: ConnectivityAndInternetAccess.NetworkState? by mutableStateOf(null)

    // Active reachability states
    var isAppBackendReachable: Boolean? by mutableStateOf(null)
    var isFallbackInternetReachable: Boolean? by mutableStateOf(null)
    var isCheckingConnectivity: Boolean by mutableStateOf(false)
    var diagnosticSummary: String by mutableStateOf("")

    private var networkObserver: ConnectivityAndInternetAccess.NetworkObserver? = null
    private var activeRequest: ConnectivityAndInternetAccess.Request? = null

    init {
        loadData()
    }

    fun startObservingNetwork(context: Context) {
        if (networkObserver != null) return

        networkObserver = ConnectivityAndInternetAccess.observeNetwork(context) { state ->
            networkState = state
            if (state.connected) {
                // Perform active check with app endpoints first when connected
                performConnectivityCheck(context)
            } else {
                isAppBackendReachable = false
                isFallbackInternetReachable = false
                diagnosticSummary = "Sin conexión de red."
            }
        }
    }

    fun stopObservingNetwork() {
        networkObserver?.close()
        networkObserver = null
        activeRequest?.cancel()
        activeRequest = null
    }

    fun performConnectivityCheck(context: Context, runExtremeFallbackAlways: Boolean = false) {
        isCheckingConnectivity = true
        activeRequest?.cancel()

        // Tier 1: Check primary app endpoints first
        activeRequest = AppConnectivityManager.checkAppEndpointsAsync(context) { appResult ->
            if (appResult.reachable) {
                isAppBackendReachable = true
                isFallbackInternetReachable = true
                isCheckingConnectivity = false
                diagnosticSummary = "Conectado al servidor de la app via: ${appResult.reachedHost} (${appResult.elapsedMilliseconds} ms)"
                if (pokemonList.isEmpty() || errorLoadingData) {
                    loadData()
                }
            } else {
                isAppBackendReachable = false
                diagnosticSummary = "Endpoint de la app inalcanzable. Iniciando diagnóstico extremo (DNS público/dominios por defecto)..."

                // Tier 2 (Extreme cases only): Fallback to public DNS & default domains
                activeRequest = AppConnectivityManager.checkExtremeFallbackAsync(context) { fallbackResult ->
                    isCheckingConnectivity = false
                    if (fallbackResult.reachable) {
                        isFallbackInternetReachable = true
                        diagnosticSummary = "Internet general disponible via ${fallbackResult.reachedHost}, pero los servidores de Pokémon están caídos o inaccesibles."
                    } else {
                        isFallbackInternetReachable = false
                        diagnosticSummary = "Sin acceso a Internet. Falló el diagnóstico DNS y dominios por defecto (${fallbackResult.attemptedHosts.joinToString()})."
                    }
                }
            }
        }
    }

    fun loadData() {
        isLoadingData = true
        errorLoadingData = false

        PokemonAPI.loadPokemon({ pokemon ->
            pokemonList = pokemon
            isLoadingData = false
            errorLoadingData = false
        }, {
            isLoadingData = false
            errorLoadingData = true
        })
    }

    override fun onCleared() {
        stopObservingNetwork()
        super.onCleared()
    }
}