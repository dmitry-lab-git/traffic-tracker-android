package com.dmitrylab.traffictracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dmitrylab.traffictracker.data.db.AppDatabase
import com.dmitrylab.traffictracker.data.stats.TrafficCollector
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val collector = TrafficCollector(application)
    private val db = AppDatabase.getInstance(application)

    private val _totalTraffic = MutableStateFlow<Pair<Long, Long>>(Pair(0L, 0L))
    val totalTraffic: StateFlow<Pair<Long, Long>> = _totalTraffic

    init {
        viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                // ерем трафик за последние 24 часа
                val stats = collector.getTrafficForPeriod(now - 86400000, now)
                _totalTraffic.value = stats
                delay(5000) // бновляем раз в 5 секунд
            }
        }
    }
}
