package com.dmitrylab.traffictracker.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dmitrylab.traffictracker.ui.components.PermissionStatusCard

@Composable
internal fun PermissionsSettingsContent(
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
            text = "App requirements check",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        PermissionStatusCard(
            title = "Usage stats access",
            description = "Required to read system traffic statistics by channel and UID.",
            granted = hasUsageStats,
            actionLabel = "Open usage access settings",
            onAction = onOpenUsageAccessSettings
        )

        PermissionStatusCard(
            title = "Phone state permission",
            description = "Required on this Android version to accurately resolve mobile network counters.",
            granted = hasPhoneState,
            actionLabel = "Request phone permission",
            onAction = onRequestPhonePermission
        )

        OutlinedButton(
            onClick = onBackToMonitor,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to monitor")
        }
    }
}
