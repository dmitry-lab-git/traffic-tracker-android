package com.dmitrylab.traffictracker.data.stats

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.RemoteException

class TrafficCollector(private val context: Context) {

    private val statsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

    fun getTrafficForPeriod(startTime: Long, endTime: Long): Pair<Long, Long> {
        var totalUp = 0L
        var totalDown = 0L

        // обильный
        try {
            val mobileStats = statsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE, null, startTime, endTime
            )
            totalUp += mobileStats.txBytes
            totalDown += mobileStats.rxBytes
        } catch (e: Exception) { e.printStackTrace() }

        // Wi-Fi
        try {
            val wifiStats = statsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_WIFI, null, startTime, endTime
            )
            totalUp += wifiStats.txBytes
            totalDown += wifiStats.rxBytes
        } catch (e: Exception) { e.printStackTrace() }

        return Pair(totalUp, totalDown)
    }
}
