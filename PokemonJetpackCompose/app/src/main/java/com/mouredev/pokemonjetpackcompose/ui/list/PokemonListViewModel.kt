package com.mouredev.pokemonjetpackcompose.ui.list

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mouredev.pokemonjetpackcompose.api.PokemonAPI
import com.mouredev.pokemonjetpackcompose.model.Pokemon
import com.mouredev.pokemonjetpackcompose.R
import com.mouredev.pokemonjetpackcompose.util.AppConnectivityManager
import com.mouredev.pokemonjetpackcompose.util.ConnectivityAndInternetAccess
import retrofit2.HttpException
import retrofit2.Call

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
    var toastMessage: String? by mutableStateOf(null)

    private var networkObserver: ConnectivityAndInternetAccess.NetworkObserver? = null
    private var activeRequest: ConnectivityAndInternetAccess.Request? = null
    private var activeApiCall: Call<*>? = null
    private var lastToastMessage: String? = null
    private var wasConnected: Boolean? = null

    fun startObservingNetwork(context: Context) {
        if (networkObserver != null) return

        networkObserver = ConnectivityAndInternetAccess.observeNetwork(context) { state ->
            networkState = state
            if (state.connected) {
                if (wasConnected == false) emitToast(context, R.string.connection_recovered)
                if (state.captivePortalDetected) {
                    emitToast(context, R.string.captive_portal)
                }
                wasConnected = true
                diagnosticSummary = "Red disponible. La app realizará la petición real con sus propios timeouts."
                if (pokemonList.isEmpty() && !isLoadingData) {
                    loadData(context)
                }
            } else {
                if (wasConnected != false) emitToast(context, R.string.no_network)
                wasConnected = false
                markOffline()
            }
        }
    }

    fun stopObservingNetwork() {
        networkObserver?.close()
        networkObserver = null
        activeRequest?.cancel()
        activeRequest = null
        activeApiCall?.cancel()
        activeApiCall = null
    }

    fun performConnectivityCheck(context: Context) {
        if (!AppConnectivityManager.canStartRemoteRequest(context)) {
            emitToast(context, R.string.no_network)
            markOffline()
            return
        }

        isCheckingConnectivity = true
        activeRequest?.cancel()
        diagnosticSummary = "Ejecutando diagnóstico general de Internet..."
        activeRequest = AppConnectivityManager.diagnoseGeneralInternetAsync(context) { result ->
            isCheckingConnectivity = false
            isFallbackInternetReachable = result.reachable
            diagnosticSummary = if (result.reachable) {
                "Internet general disponible vía ${result.reachedHost}. El fallo puede ser específico de PokéAPI."
            } else {
                "Sin acceso general a Internet (${result.attemptedHosts.joinToString()})."
            }
        }
    }

    fun loadData(context: Context) {
        if (!AppConnectivityManager.canStartRemoteRequest(context)) {
            markOffline()
            return
        }

        isLoadingData = true
        errorLoadingData = false
        isAppBackendReachable = null

        activeApiCall = PokemonAPI.loadPokemon({ pokemon ->
            activeApiCall = null
            isAppBackendReachable = true
            isFallbackInternetReachable = true
            pokemonList = pokemon
            isLoadingData = false
            errorLoadingData = false
            diagnosticSummary = "PokéAPI disponible (${pokemon.size} Pokémon cargados)."
        }, {
            error ->
            activeApiCall = null
            isLoadingData = false
            errorLoadingData = true
            isAppBackendReachable = false
            if (error !is HttpException && AppConnectivityManager.isNetworkFailure(error)) {
                diagnosticSummary = "Falló la conexión con PokéAPI. Ejecutando diagnóstico general..."
                runGeneralDiagnosis(context)
            } else {
                emitToast(context, R.string.service_unavailable)
                diagnosticSummary = "PokéAPI respondió con un error (${(error as? HttpException)?.code() ?: "desconocido"})."
            }
        })
    }

    private fun runGeneralDiagnosis(context: Context) {
        if (!AppConnectivityManager.canStartRemoteRequest(context)) {
            markOffline()
            return
        }
        isCheckingConnectivity = true
        activeRequest?.cancel()
        activeRequest = AppConnectivityManager.diagnoseGeneralInternetAsync(context) { result ->
            isCheckingConnectivity = false
            isFallbackInternetReachable = result.reachable
            diagnosticSummary = if (result.reachable) {
                emitToast(context, R.string.service_unavailable)
                "Internet general disponible, pero PokéAPI no responde."
            } else {
                emitToast(context, R.string.no_internet)
                "Sin acceso general a Internet."
            }
        }
    }

    private fun markOffline() {
        isAppBackendReachable = false
        isFallbackInternetReachable = false
        isCheckingConnectivity = false
        isLoadingData = false
        diagnosticSummary = "Sin conexión de red. La operación se ha pospuesto."
    }

    private fun emitToast(context: Context, @StringRes messageRes: Int) {
        val message = context.getString(messageRes)
        if (message != lastToastMessage) {
            lastToastMessage = message
            toastMessage = message
        }
    }

    fun consumeToast() {
        toastMessage = null
    }

    override fun onCleared() {
        stopObservingNetwork()
        super.onCleared()
    }
}
