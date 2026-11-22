package com.dmitrylab.traffictracker.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dmitrylab.traffictracker.data.db.AppDatabase
import com.dmitrylab.traffictracker.data.stats.LiveTrafficMonitor
import com.dmitrylab.traffictracker.data.stats.TrafficUiState
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val monitor = LiveTrafficMonitor(application, pollIntervalMs = 5_000L)

    val uiState: StateFlow<TrafficUiState> = monitor.uiState

    init {
        monitor.start(viewModelScope)
    }

    fun onScreenResumed() {
        monitor.refreshPermissionStatus()
        monitor.sampleOnce()
    }

    fun createPermissionSettingsIntent(): Intent =
        monitor.createPermissionSettingsIntent()

    fun resetMeasurement() {
        monitor.resetBaseline()
    }

    override fun onCleared() {
        monitor.stop()
        super.onCleared()
    }
}
