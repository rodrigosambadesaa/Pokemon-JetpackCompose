package com.mouredev.pokemonjetpackcompose.util

import android.content.Context

/**
 * Shared connectivity policy for the Pokemon app.
 *
 * The normal request path is deliberately cheap: callers first use [isConnected] and then
 * perform the real operation with its own timeout/error handling. The active diagnostic is
 * reserved for transport failures or an explicit user request.
 */
object AppConnectivityManager {

    private val generalConnectivity by lazy {
        ConnectivityAndInternetAccess.Builder().build()
    }

    fun isConnected(context: Context): Boolean =
        ConnectivityAndInternetAccess.isConnected(context)

    fun hasPhysicalNetwork(context: Context): Boolean =
        ConnectivityAndInternetAccess.hasPhysicalNetwork(context)

    fun canStartRemoteRequest(context: Context): Boolean =
        canStartRemoteRequest(isConnected(context), hasPhysicalNetwork(context))

    internal fun canStartRemoteRequest(
        isConnected: Boolean,
        hasPhysicalNetwork: Boolean
    ): Boolean = isConnected && hasPhysicalNetwork

    fun diagnoseGeneralInternetAsync(
        context: Context,
        onResult: (result: ConnectivityAndInternetAccess.InternetResult) -> Unit
    ): ConnectivityAndInternetAccess.Request {
        return generalConnectivity.checkInternetAsync(context) { result ->
            onResult(result)
        }
    }

    fun isNetworkFailure(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is java.net.UnknownHostException ||
                current is java.net.ConnectException ||
                current is java.net.SocketTimeoutException ||
                current is javax.net.ssl.SSLException ||
                current is java.io.IOException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
