package com.codershubinc.aaxion_mob.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.codershubinc.aaxion_mob.core.data.TokenManager
import com.codershubinc.aaxion_mob.core.model.StorageInfoResponse
import com.codershubinc.aaxion_mob.core.network.Fetcher
import com.codershubinc.aaxion_mob.feature.home.components.*
import com.codershubinc.aaxion_mob.ui.theme.LocalAccentColor
import java.util.Calendar

@Composable
fun HomeScreen(
    serverUri: String,
    token: String,
    onNavigateToDrive: (path: String?) -> Unit,
    onNavigateToMusic: () -> Unit,
    onNavigateToStorage: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToConnectionManager: () -> Unit,
    isServerOnline: Boolean?,
    serverLatencyMs: Long?,
    isTablet: Boolean
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    val tokenManager = remember { TokenManager.getInstance(context) }

    var storageInfo by remember { mutableStateOf<StorageInfoResponse?>(null) }
    var isLoadingStorage by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    // Fetch storage capacity and volume telemetry
    LaunchedEffect(serverUri, token, refreshTrigger) {
        if (serverUri.isNotBlank() && token.isNotBlank()) {
            isLoadingStorage = true
            try {
                val service = Fetcher.getInstance().getService(serverUri)
                val response = tokenManager.withAuthRetry(serverUri, token) { auth ->
                    service.getStorageInfo("Bearer $auth")
                }
                if (response.isSuccessful) {
                    storageInfo = response.body()
                }
            } catch (_: Exception) {
            } finally {
                isLoadingStorage = false
            }
        }
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Late Night Session"
        }
    }

    val displayHost = remember(serverUri) {
        try {
            serverUri.toUri().host ?: serverUri.ifBlank { "No Host Configured" }
        } catch (_: Exception) {
            serverUri.ifBlank { "No Host Configured" }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(
                start = if (isTablet) 24.dp else 16.dp,
                end = if (isTablet) 24.dp else 16.dp,
                top = 14.dp,
                bottom = 110.dp
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Top Command & Telemetry Header
        HomeHeaderBar(
            greeting = greeting,
            displayHost = displayHost,
            isServerOnline = isServerOnline,
            serverLatencyMs = serverLatencyMs,
            accentColor = accentColor,
            onNavigateToConnectionManager = onNavigateToConnectionManager,
            onNavigateToSettings = onNavigateToSettings,
            onRefresh = { refreshTrigger++ }
        )

        // 2. Hero Storage & Volume Telemetry Card
        StorageHeroCard(
            storageInfo = storageInfo,
            isLoading = isLoadingStorage,
            accentColor = accentColor,
            onOpenStorageHub = onNavigateToStorage,
            onSelectMountPoint = { path -> onNavigateToDrive(path) }
        )

        // 3. Spatial Bento Quick-Access Grid
        BentoWorkspaceGrid(
            accentColor = accentColor,
            displayHost = displayHost,
            isServerOnline = isServerOnline,
            onNavigateToDrive = onNavigateToDrive,
            onNavigateToMusic = onNavigateToMusic,
            onNavigateToStorage = onNavigateToStorage,
            onNavigateToConnectionManager = onNavigateToConnectionManager
        )

        // 4. Marked Media Directories Grid
        MarkedDirectoriesSection(
            serverUri = serverUri,
            token = token,
            isServerOnline = isServerOnline,
            refreshTrigger = refreshTrigger,
            accentColor = accentColor,
            onNavigateToDrive = onNavigateToDrive
        )

        // 5. Continue Playing / Recent Media Stream
        ContinuePlayingSection(
            serverUri = serverUri,
            token = token,
            accentColor = accentColor,
            onNavigateToDrive = onNavigateToDrive
        )
    }
}