package com.dmitrylab.traffictracker.ui.monitor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dmitrylab.traffictracker.data.stats.TrafficUiState
import com.dmitrylab.traffictracker.ui.components.TrafficSectionCard
import com.dmitrylab.traffictracker.ui.util.formatBytes
import com.dmitrylab.traffictracker.ui.util.formatTime

@Composable
internal fun MonitorContent(
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
            text = "Updates every 5s • Last sample: $updatedText",
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
                        text = "Permission check required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (!state.hasPermission) {
                            "Usage stats access is not granted. Open Settings & permissions to enable it."
                        } else {
                            "Phone permission is not granted. Measurements may be incomplete - open Settings & permissions to grant it."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = onOpenSettingsScreen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open settings & permissions")
                    }
                }
            }
        }

        TrafficSectionCard(
            title = "Phone",
            wifi = state.phoneWifi,
            mobile = state.phoneMobile,
            totalBytes = state.phoneTotalBytes
        )

        TrafficSectionCard(
            title = "Tethering",
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
                    text = "Grand total",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Total: ${formatBytes(state.grandTotalBytes)} (↑ ${formatBytes(state.grandTotalUp)} / ↓ ${formatBytes(state.grandTotalDown)})",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
