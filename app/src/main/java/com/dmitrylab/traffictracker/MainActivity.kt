package com.dmitrylab.traffictracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dmitrylab.traffictracker.data.stats.AppTrafficItem
import com.dmitrylab.traffictracker.data.stats.BreakdownItemUi
import com.dmitrylab.traffictracker.data.stats.TrafficUiState
import com.dmitrylab.traffictracker.ui.MainViewModel
import com.dmitrylab.traffictracker.ui.theme.TrafficTrackerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AppScreen {
    MONITOR,
    APPS,
    SETTINGS
}

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrafficTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
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

    val phonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPhoneStatePermission = granted
        viewModel.onScreenResumed()
    }

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshPhonePermission()
                viewModel.onScreenResumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            AppScreen.MONITOR -> "Traffic Tracker"
                            AppScreen.APPS -> "По приложениям"
                            AppScreen.SETTINGS -> "Настройки и права"
                        },
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Text(
                            text = "⋮",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Монитор трафика") },
                            onClick = {
                                menuExpanded = false
                                currentScreen = AppScreen.MONITOR
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("По приложениям") },
                            onClick = {
                                menuExpanded = false
                                currentScreen = AppScreen.APPS
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Настройки и права") },
                            onClick = {
                                menuExpanded = false
                                refreshPhonePermission()
                                currentScreen = AppScreen.SETTINGS
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when (currentScreen) {
            AppScreen.MONITOR -> {
                MonitorContent(
                    state = state,
                    hasPhoneStatePermission = hasPhoneStatePermission,
                    onOpenSettingsScreen = {
                        refreshPhonePermission()
                        currentScreen = AppScreen.SETTINGS
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            AppScreen.APPS -> {
                AppsBreakdownContent(
                    apps = state.appUsageList,
                    onBackToMonitor = { currentScreen = AppScreen.MONITOR },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            AppScreen.SETTINGS -> {
                PermissionsSettingsContent(
                    hasUsageStats = state.hasPermission,
                    hasPhoneState = hasPhoneStatePermission,
                    onOpenUsageAccessSettings = {
                        context.startActivity(viewModel.createPermissionSettingsIntent())
                    },
                    onRequestPhonePermission = {
                        phonePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                    },
                    onBackToMonitor = {
                        currentScreen = AppScreen.MONITOR
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun MonitorContent(
    state: TrafficUiState,
    hasPhoneStatePermission: Boolean,
    onOpenSettingsScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val updatedText = state.lastUpdatedAtMs?.let { formatTime(it) } ?: "—"
        Text(
            text = "Обновление каждые 5 сек. • Последний замер: $updatedText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )

        if (!state.hasPermission || !hasPhoneStatePermission) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Требуется проверка прав доступа",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (!state.hasPermission) {
                            "Не выдан доступ к данным об использовании. Откройте «Настройки и права», чтобы включить доступ."
                        } else {
                            "Не выдано разрешение «Телефон». Замер может работать неполно — откройте «Настройки и права» для выдачи."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = onOpenSettingsScreen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Открыть настройки и права")
                    }
                }
            }
        }

        TrafficSectionCard(
            title = "Телефон",
            wifi = state.phoneWifi,
            mobile = state.phoneMobile,
            totalBytes = state.phoneTotalBytes
        )

        TrafficSectionCard(
            title = "Раздача",
            wifi = state.tetherWifi,
            mobile = state.tetherMobile,
            totalBytes = state.tetherTotalBytes
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Общий итог",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Всего: ${formatBytes(state.grandTotalBytes)} (↑ ${formatBytes(state.grandTotalUp)} / ↓ ${formatBytes(state.grandTotalDown)})",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AppsBreakdownContent(
    apps: List<AppTrafficItem>,
    onBackToMonitor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onBackToMonitor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Вернуться к монитору")
        }

        if (apps.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Данных по приложениям за текущий интервал пока нет.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            apps.forEach { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = formatBytes(item.totalBytes),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (item.packageName != null) {
                            Text(
                                text = item.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = "↑ ${formatBytes(item.upBytes)}   •   ↓ ${formatBytes(item.downBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsSettingsContent(
    hasUsageStats: Boolean,
    hasPhoneState: Boolean,
    onOpenUsageAccessSettings: () -> Unit,
    onRequestPhonePermission: () -> Unit,
    onBackToMonitor: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Проверка требований приложения",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        PermissionStatusCard(
            title = "Доступ к данным об использовании",
            description = "Нужен для чтения системной статистики трафика по каналам и UID.",
            granted = hasUsageStats,
            actionLabel = "Открыть настройки доступа",
            onAction = onOpenUsageAccessSettings
        )

        PermissionStatusCard(
            title = "Доступ к состоянию телефона",
            description = "Нужен на этой версии Android для корректного определения счётчиков мобильной сети.",
            granted = hasPhoneState,
            actionLabel = "Запросить доступ к телефону",
            onAction = onRequestPhonePermission
        )

        OutlinedButton(
            onClick = onBackToMonitor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Вернуться к монитору")
        }
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (granted) "Выдано" else "Не выдано",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (granted) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
            if (!granted) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun TrafficSectionCard(
    title: String,
    wifi: BreakdownItemUi,
    mobile: BreakdownItemUi,
    totalBytes: Long
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            ChannelRow(label = "Wi-Fi", item = wifi)
            ChannelRow(label = "Мобильный", item = mobile)

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Итого ($title)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatBytes(totalBytes),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ChannelRow(
    label: String,
    item: BreakdownItemUi
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(text = formatBytes(item.totalBytes), style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text = "↑ ${formatBytes(item.upBytes)}   •   ↓ ${formatBytes(item.downBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

private fun formatBytes(bytes: Long): String {
    val safe = bytes.coerceAtLeast(0L)
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        safe >= gb -> String.format(Locale.getDefault(), "%.2f ГБ", safe / gb)
        safe >= mb -> String.format(Locale.getDefault(), "%.2f МБ", safe / mb)
        safe >= kb -> String.format(Locale.getDefault(), "%.1f КБ", safe / kb)
        else -> "$safe Б"
    }
}

private fun formatTime(timestampMs: Long): String {
    val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(timestampMs))
}