package com.dmitrylab.traffictracker.data.stats

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager

class TrafficCollector(private val context: Context) {

    private val statsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager

    fun getTrafficForPeriod(startTime: Long, endTime: Long): Pair<Long, Long> {
        var totalUp = 0L
        var totalDown = 0L

        // 1. обильный трафик
        try {
            // а Android < 10 нужен subscriberId для TYPE_MOBILE
            val subscriberId = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                tm.subscriberId
            } else null

            val mobileStats = statsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                subscriberId,
                startTime,
                endTime
            )
            totalUp += mobileStats.txBytes
            totalDown += mobileStats.rxBytes
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Wi-Fi трафик
        try {
            val wifiStats = statsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_WIFI,
                null,
                startTime,
                endTime
            )
            totalUp += wifiStats.txBytes
            totalDown += wifiStats.rxBytes
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return Pair(totalUp, totalDown)
    }
}
