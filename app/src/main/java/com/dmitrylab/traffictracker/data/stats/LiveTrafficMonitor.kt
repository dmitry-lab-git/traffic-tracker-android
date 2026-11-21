package com.dmitrylab.traffictracker.data.stats

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class BreakdownItemUi(
    val upBytes: Long,
    val downBytes: Long
) {
    val totalBytes: Long get() = upBytes + downBytes
}

data class TrafficUiState(
    val hasPermission: Boolean = false,
    val isPolling: Boolean = false,
    val lastUpdatedAtMs: Long? = null,
    val phoneWifi: BreakdownItemUi = BreakdownItemUi(0L, 0L),
    val phoneMobile: BreakdownItemUi = BreakdownItemUi(0L, 0L),
    val tetherWifi: BreakdownItemUi = BreakdownItemUi(0L, 0L),
    val tetherMobile: BreakdownItemUi = BreakdownItemUi(0L, 0L),
    val appUsageList: List<AppTrafficItem> = emptyList()
) {
    val phoneTotalUp: Long get() = phoneWifi.upBytes + phoneMobile.upBytes
    val phoneTotalDown: Long get() = phoneWifi.downBytes + phoneMobile.downBytes
    val phoneTotalBytes: Long get() = phoneTotalUp + phoneTotalDown

    val tetherTotalUp: Long get() = tetherWifi.upBytes + tetherMobile.upBytes
    val tetherTotalDown: Long get() = tetherWifi.downBytes + tetherMobile.downBytes
    val tetherTotalBytes: Long get() = tetherTotalUp + tetherTotalDown

    val grandTotalUp: Long get() = phoneTotalUp + tetherTotalUp
    val grandTotalDown: Long get() = phoneTotalDown + tetherTotalDown
    val grandTotalBytes: Long get() = grandTotalUp + grandTotalDown
}

class LiveTrafficMonitor(
    context: Context,
    private val pollIntervalMs: Long = 5_000L
) {
    private val collector = TrafficCollector(context.applicationContext)
    private var pollJob: Job? = null
    private var sessionStartMs: Long = System.currentTimeMillis()

    private val _uiState = MutableStateFlow(
        TrafficUiState(hasPermission = collector.hasUsageStatsPermission())
    )
    val uiState: StateFlow<TrafficUiState> = _uiState.asStateFlow()

    fun createPermissionSettingsIntent(): Intent =
        collector.createUsageAccessSettingsIntent()

    fun refreshPermissionStatus() {
        val granted = collector.hasUsageStatsPermission()
        _uiState.value = _uiState.value.copy(hasPermission = granted)
    }

    fun resetBaseline() {
        sessionStartMs = System.currentTimeMillis()
        sampleOnce()
    }

    fun start(scope: CoroutineScope) {
        if (pollJob?.isActive == true) return
        sessionStartMs = System.currentTimeMillis()
        _uiState.value = _uiState.value.copy(
            hasPermission = collector.hasUsageStatsPermission(),
            isPolling = true
        )

        pollJob = scope.launch {
            while (isActive) {
                sampleOnce()
                delay(pollIntervalMs.milliseconds)
            }
        }
    }

    fun stop() {
        pollJob?.cancel()
        pollJob = null
        _uiState.value = _uiState.value.copy(isPolling = false)
    }

    fun sampleOnce() {
        val granted = collector.hasUsageStatsPermission()
        if (!granted) {
            _uiState.value = _uiState.value.copy(
                hasPermission = false,
                lastUpdatedAtMs = System.currentTimeMillis()
            )
            return
        }

        val now = System.currentTimeMillis()
        val snapshot = collector.getTrafficSnapshot(startTime = sessionStartMs, endTime = now)
        val apps = collector.getAppTrafficBreakdown(startTime = sessionStartMs, endTime = now)

        _uiState.value = TrafficUiState(
            hasPermission = true,
            isPolling = pollJob?.isActive == true,
            lastUpdatedAtMs = now,
            phoneWifi = BreakdownItemUi(snapshot.phoneWifi.upBytes, snapshot.phoneWifi.downBytes),
            phoneMobile = BreakdownItemUi(snapshot.phoneMobile.upBytes, snapshot.phoneMobile.downBytes),
            tetherWifi = BreakdownItemUi(snapshot.tetherWifi.upBytes, snapshot.tetherWifi.downBytes),
            tetherMobile = BreakdownItemUi(snapshot.tetherMobile.upBytes, snapshot.tetherMobile.downBytes),
            appUsageList = apps
        )
    }
}