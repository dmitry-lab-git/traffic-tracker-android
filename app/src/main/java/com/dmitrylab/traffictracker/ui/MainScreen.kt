package com.dmitrylab.traffictracker.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitrylab.traffictracker.ui.apps.AppsBreakdownContent
import com.dmitrylab.traffictracker.ui.monitor.MonitorContent
import com.dmitrylab.traffictracker.ui.settings.PermissionsSettingsContent

private enum class AppScreen {
    MONITOR,
    APPS,
    SETTINGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentScreen by remember { mutableStateOf(AppScreen.MONITOR) }
    var menuExpanded by remember { mutableStateOf(false) }

    var hasPhoneStatePermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val refreshPhonePermission = {
        hasPhoneStatePermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPhonePermission()
                viewModel.onScreenResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            AppScreen.MONITOR -> "Traffic Tracker"
                            AppScreen.APPS -> "By app"
                            AppScreen.SETTINGS -> "Settings & permissions"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Text(text = "⋮", style = MaterialTheme.typography.titleLarge)
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Traffic monitor") },
                            onClick = { menuExpanded = false; currentScreen = AppScreen.MONITOR }
                        )
                        DropdownMenuItem(
                            text = { Text("By app") },
                            onClick = { menuExpanded = false; currentScreen = AppScreen.APPS }
                        )
                        DropdownMenuItem(
                            text = { Text("Settings & permissions") },
                            onClick = { menuExpanded = false; refreshPhonePermission(); currentScreen = AppScreen.SETTINGS }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when (currentScreen) {
            AppScreen.MONITOR -> MonitorContent(
                state = state,
                hasPhoneStatePermission = hasPhoneStatePermission,
                onOpenSettingsScreen = { refreshPhonePermission(); currentScreen = AppScreen.SETTINGS },
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
            AppScreen.APPS -> AppsBreakdownContent(
                apps = state.appUsageList,
                onBackToMonitor = { currentScreen = AppScreen.MONITOR },
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
            AppScreen.SETTINGS -> PermissionsSettingsContent(
                hasUsageStats = state.hasPermission,
                hasPhoneState = hasPhoneStatePermission,
                onOpenUsageAccessSettings = { context.startActivity(viewModel.createPermissionSettingsIntent()) },
                onRequestPhonePermission = {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                    val activity = context as? androidx.activity.ComponentActivity
                    activity?.requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), 1001)
                },
                onBackToMonitor = { currentScreen = AppScreen.MONITOR },
                modifier = Modifier.fillMaxSize().padding(innerPadding)
            )
        }
    }
}
