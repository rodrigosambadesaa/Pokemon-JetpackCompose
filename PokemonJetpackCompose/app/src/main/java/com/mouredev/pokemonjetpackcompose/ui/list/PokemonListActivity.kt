package com.mouredev.pokemonjetpackcompose.ui.list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mouredev.pokemonjetpackcompose.ui.theme.PokemonJetpackComposeTheme

/**
 * Created by MoureDev by Brais Moure on 28/10/22.
 * www.mouredev.com
 * Modernized with broad tiered connectivity observation and diagnostic UI.
 */
class PokemonListActivity : ComponentActivity() {

    private val viewModel: PokemonListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PokemonJetpackComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    PokemonList(viewModel = viewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.startObservingNetwork(this)
    }

    override fun onStop() {
        viewModel.stopObservingNetwork()
        super.onStop()
    }
}

@Composable
fun PokemonList(viewModel: PokemonListViewModel) {
    val context = LocalContext.current
    var showDiagnosticDetails by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pokémon list") },
                actions = {
                    IconButton(onClick = {
                        viewModel.performConnectivityCheck(context)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Comprobar conectividad"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Connectivity Status Banner
            ConnectivityBanner(
                viewModel = viewModel,
                onDiagnoseClick = {
                    viewModel.performConnectivityCheck(context)
                    showDiagnosticDetails = true
                },
                onToggleDetails = {
                    showDiagnosticDetails = !showDiagnosticDetails
                }
            )

            // Diagnostic details dialog/expandable view
            AnimatedVisibility(visible = showDiagnosticDetails && viewModel.diagnosticSummary.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = 4.dp,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Detalles del Diagnóstico de Red:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.diagnosticSummary,
                            fontSize = 13.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "La petición real mantiene sus timeouts; el diagnóstico general (DNS/TCP/NTP/TLS/HTTPS/ICMP) solo se ejecuta tras un fallo de red o al solicitarlo.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (viewModel.isLoadingData) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (viewModel.errorLoadingData && viewModel.pokemonList.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Error al cargar la lista de Pokémon.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadData(context) }) {
                            Text("Reintentar")
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(viewModel.pokemonList) { pokemon ->
                            PokemonCell(pokemon)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectivityBanner(
    viewModel: PokemonListViewModel,
    onDiagnoseClick: () -> Unit,
    onToggleDetails: () -> Unit
) {
    val state = viewModel.networkState
    val isAppReachable = viewModel.isAppBackendReachable
    val isFallbackReachable = viewModel.isFallbackInternetReachable
    val isChecking = viewModel.isCheckingConnectivity

    val isConnected = state?.connected == true && state.physicalNetworkAvailable

    if (isConnected && isAppReachable != false) {
        // Connected & App Backend is healthy or checking
        if (isChecking) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE3F2FD))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Comprobando conectividad con pokeapi.co...",
                    fontSize = 12.sp,
                    color = Color(0xFF0D47A1)
                )
            }
        }
        return
    }

    // Determine Banner Color & Message
    val (backgroundColor, textColor, icon, message) = when {
        !isConnected -> Quadruple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            Icons.Default.Warning,
            "Sin conexión de red en el dispositivo"
        )
        isAppReachable == false && isFallbackReachable == true -> Quadruple(
            Color(0xFFFFF3E0),
            Color(0xFFE65100),
            Icons.Default.Warning,
            "Internet disponible, pero PokéAPI (pokeapi.co) no responde"
        )
        else -> Quadruple(
            Color(0xFFFFEBEE),
            Color(0xFFC62828),
            Icons.Default.Warning,
            "Fallo de conectividad detectado"
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onToggleDetails() },
        backgroundColor = backgroundColor,
        elevation = 2.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = message,
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Toca para ver detalles o diagnosticar",
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            OutlinedButton(
                onClick = onDiagnoseClick,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(if (isChecking) "Probando..." else "Diagnosticar", fontSize = 11.sp)
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Preview(showSystemUi = true)
@Composable
fun PokemonListDefaultPreview() {
    PokemonJetpackComposeTheme {
        PokemonList(viewModel = PokemonListViewModel())
    }
}
