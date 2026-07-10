package com.codershubinc.aaxion_mob

import android.os.Bundle
import android.util.Log
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.codershubinc.aaxion_mob.auth.Auth
import com.codershubinc.aaxion_mob.ui.FileExplorerScreen
import com.codershubinc.aaxion_mob.ui.LoginScreen
import com.codershubinc.aaxion_mob.ui.MeScreen
import com.codershubinc.aaxion_mob.ui.theme.AaxionMobTheme
import com.codershubinc.aaxion_mob.utils.DataStoreManager
import com.codershubinc.aaxion_mob.utils.ServerDiscovery
import com.codershubinc.aaxion_mob.viewmodels.FileViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AaxionMobTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    
    val token by dataStoreManager.token.collectAsState(initial = null)
    var serverUri by remember { mutableStateOf("") }
    var currentStatus by remember { mutableStateOf("Searching for server...") }

    DisposableEffect(Unit) {
        val discovery = ServerDiscovery(context) { ip, port ->
            currentStatus = "Found Server: $ip:$port"
            serverUri = "http://$ip:$port"
        }
        discovery.startDiscovery()
        onDispose {
            discovery.stopDiscovery()
        }
    }

    val startDestination = if (token == null) "login" else "me"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            if (serverUri.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF080808)),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    com.codershubinc.aaxion_mob.ui.components.BackgroundGlows()
                    Column(
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = currentStatus,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }
            } else {
                LoginScreen(serverUri = serverUri) {
                    navController.navigate("me") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            }
        }
        composable("me") {
            MeScreen(
                serverUri = serverUri,
                token = token ?: "",
                onExploreFiles = { navController.navigate("explorer") },
                onLogout = {
                    scope.launch {
                        dataStoreManager.clearAll()
                        navController.navigate("login") {
                            popUpTo("me") { inclusive = true }
                        }
                    }
                }
            )
        }
        composable("explorer") {
            val fileViewModel: FileViewModel = viewModel()
            
            LaunchedEffect(serverUri, token) {
                if (serverUri.isNotEmpty() && token != null) {
                    fileViewModel.fetchFiles(serverUri, token!!)
                }
            }

            FileExplorerScreen(
                files = fileViewModel.files,
                currentPath = fileViewModel.currentPath,
                baseUrl = serverUri,
                token = token ?: "",
                onFileClick = { file ->
                    if (file.isDir) {
                        fileViewModel.fetchFiles(serverUri, token!!, file.rawPath)
                    }
                },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
