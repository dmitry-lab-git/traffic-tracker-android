package com.dmitrylab.traffictracker.data.stats

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log

internal class AppTrafficBreakdownProvider(
    private val context: Context,
    private val statsManager: NetworkStatsManager?
) {
    @Suppress("DEPRECATION")
    fun getAppTrafficBreakdown(
        startTime: Long,
        endTime: Long,
        subscriberId: String?
    ): List<AppTrafficItem> {
        require(endTime >= startTime) { "endTime ($endTime) must be >= startTime ($startTime)" }

        data class MutableUidTraffic(
            var up: Long = 0L,
            var down: Long = 0L
        )

        val byUid = mutableMapOf<Int, MutableUidTraffic>()

        fun accumulate(networkType: Int, subId: String?) {
            val manager = statsManager ?: return
            var stats: NetworkStats? = null
            try {
                stats = manager.querySummary(networkType, subId, startTime, endTime)
                val bucket = NetworkStats.Bucket()
                while (stats != null && stats.hasNextBucket()) {
                    stats.getNextBucket(bucket)
                    val up = bucket.txBytes.coerceAtLeast(0L)
                    val down = bucket.rxBytes.coerceAtLeast(0L)
                    if (up > 0L || down > 0L) {
                        val entry = byUid.getOrPut(bucket.uid) { MutableUidTraffic() }
                        entry.up += up
                        entry.down += down
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Missing PACKAGE_USAGE_STATS for app breakdown type=$networkType", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed app breakdown for type=$networkType", e)
            } finally {
                try {
                    stats?.close()
                } catch (_: Exception) {
                }
            }
        }

        accumulate(ConnectivityManager.TYPE_MOBILE, subscriberId)
        accumulate(ConnectivityManager.TYPE_WIFI, null)

        return byUid.map { (uid, traffic) ->
            val (label, pkg) = resolveUidLabel(uid)
            AppTrafficItem(
                uid = uid,
                label = label,
                packageName = pkg,
                upBytes = traffic.up,
                downBytes = traffic.down
            )
        }.sortedByDescending { it.totalBytes }
    }

    private fun resolveUidLabel(uid: Int): Pair<String, String?> {
        return when (uid) {
            UID_TETHERING -> "аздача / Tethering (UID -5)" to null
            -4 -> "Удалённые приложения (UID -4)" to null
            0 -> "Система / Root (UID 0)" to "android"
            1000 -> "Система Android (UID 1000)" to "android"
            else -> {
                val packages = try {
                    context.packageManager.getPackagesForUid(uid)
                } catch (_: Exception) {
                    null
                }
                val firstPkg = packages?.firstOrNull()
                if (firstPkg != null) {
                    val appName = try {
                        val appInfo = context.packageManager.getApplicationInfo(firstPkg, 0)
                        context.packageManager.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        firstPkg
                    }
                    appName to firstPkg
                } else {
                    "UID $uid" to null
                }
            }
        }
    }

    companion object {
        private const val TAG = "AppTrafficBreakdown"
        private const val UID_TETHERING = -5
    }
}
