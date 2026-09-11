package com.mouredev.pokemonjetpackcompose

import com.mouredev.pokemonjetpackcompose.util.AppConnectivityManager
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConnectivityManagerTest {

    @Test
    fun networkFailuresAreDiagnosed() {
        assertTrue(AppConnectivityManager.isNetworkFailure(UnknownHostException("pokeapi.co")))
        assertTrue(AppConnectivityManager.isNetworkFailure(SocketTimeoutException("timeout")))
        assertTrue(
            AppConnectivityManager.isNetworkFailure(
                IllegalStateException("wrapper", UnknownHostException("dns"))
            )
        )
    }

    @Test
    fun nonNetworkFailuresAreNotDiagnosed() {
        assertFalse(AppConnectivityManager.isNetworkFailure(IllegalArgumentException("bad data")))
    }

    @Test
    fun remoteRequestsRequireAConnectedPhysicalNetwork() {
        assertTrue(AppConnectivityManager.canStartRemoteRequest(true))
        assertFalse(AppConnectivityManager.canStartRemoteRequest(false))
    }
}
