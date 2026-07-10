package com.codershubinc.aaxion_mob

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.codershubinc.aaxion_mob.ui.theme.AaxionMobTheme
import com.codershubinc.aaxion_mob.utils.DataStoreManager
import com.codershubinc.aaxion_mob.utils.ServerDiscovery

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AaxionMobTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }

    val savedIp by dataStoreManager.serverIp.collectAsState(initial = null)
    val savedPort by dataStoreManager.serverPort.collectAsState(initial = null)

    var currentStatus by remember { mutableStateOf("Searching for server...") }

    DisposableEffect(Unit) {
        val discovery = ServerDiscovery(context) { ip, port ->
            currentStatus = "Found Server: $ip:$port"
        }
        discovery.startDiscovery()
        onDispose {
            discovery.stopDiscovery()
        }
    }

    androidx.compose.foundation.layout.Column(modifier = modifier.padding(16.dp).background(Color.Black)) {
        Text(text = "Hello $name!")
        androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(text = "Status: $currentStatus")
        if (savedIp != null) {
            Text(text = "Last Saved: $savedIp:$savedPort")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AaxionMobTheme {
        Greeting("Android")
    }
}