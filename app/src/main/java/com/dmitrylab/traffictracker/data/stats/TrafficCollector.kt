package com.dmitrylab.traffictracker.data.stats

import android.app.usage.NetworkStats
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log

class TrafficCollector(context: Context) {
    private val permissionHelper = UsagePermissionHelper(context)
    private val breakdownProvider = AppTrafficBreakdownProvider(
        context = context,
        statsManager = permissionHelper.statsManager
    )

    fun hasUsageStatsPermission(): Boolean = permissionHelper.hasUsageStatsPermission()

    fun createUsageAccessSettingsIntent(): Intent = permissionHelper.createUsageAccessSettingsIntent()

    fun getAppTrafficBreakdown(startTime: Long, endTime: Long): List<AppTrafficItem> =
        breakdownProvider.getAppTrafficBreakdown(
            startTime = startTime,
            endTime = endTime,
            subscriberId = permissionHelper.getMobileSubscriberId()
        )

    @Suppress("DEPRECATION")
    private fun queryDeviceBucket(
        networkType: Int,
        subscriberId: String?,
        startTime: Long,
        endTime: Long
    ): NetworkTrafficBucket {
        val manager = permissionHelper.statsManager ?: return NetworkTrafficBucket.ZERO
        return try {
            val stats = manager.querySummaryForDevice(networkType, subscriberId, startTime, endTime)
            NetworkTrafficBucket(
                upBytes = stats?.txBytes?.coerceAtLeast(0L) ?: 0L,
                downBytes = stats?.rxBytes?.coerceAtLeast(0L) ?: 0L
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing PACKAGE_USAGE_STATS permission for type=$networkType", e)
            NetworkTrafficBucket.ZERO
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query device traffic for type=$networkType", e)
            NetworkTrafficBucket.ZERO
        }
    }

    @Suppress("DEPRECATION")
    private fun queryTetherBucketByUid(
        networkType: Int,
        subscriberId: String?,
        startTime: Long,
        endTime: Long
    ): NetworkTrafficBucket {
        val manager = permissionHelper.statsManager ?: return NetworkTrafficBucket.ZERO
        var stats: NetworkStats? = null
        return try {
            stats = manager.querySummary(networkType, subscriberId, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            var up = 0L
            var down = 0L
            while (stats != null && stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                if (bucket.uid == UID_TETHERING) {
                    up += bucket.txBytes.coerceAtLeast(0L)
                    down += bucket.rxBytes.coerceAtLeast(0L)
                }
            }
            NetworkTrafficBucket(upBytes = up, downBytes = down)
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing PACKAGE_USAGE_STATS permission for tether uid type=$networkType", e)
            NetworkTrafficBucket.ZERO
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query tether uid bucket for type=$networkType", e)
            NetworkTrafficBucket.ZERO
        } finally {
            try {
                stats?.close()
            } catch (_: Exception) {
            }
        }
    }

    fun getTrafficSnapshot(startTime: Long, endTime: Long): TrafficSnapshot {
        require(endTime >= startTime) { "endTime ($endTime) must be >= startTime ($startTime)" }
        val subscriberId = permissionHelper.getMobileSubscriberId()

        val deviceMobile = queryDeviceBucket(
            networkType = ConnectivityManager.TYPE_MOBILE,
            subscriberId = subscriberId,
            startTime = startTime,
            endTime = endTime
        )
        val deviceWifi = queryDeviceBucket(
            networkType = ConnectivityManager.TYPE_WIFI,
            subscriberId = null,
            startTime = startTime,
            endTime = endTime
        )

        val tetherMobile = queryTetherBucketByUid(
            networkType = ConnectivityManager.TYPE_MOBILE,
            subscriberId = subscriberId,
            startTime = startTime,
            endTime = endTime
        )
        val tetherWifi = queryTetherBucketByUid(
            networkType = ConnectivityManager.TYPE_WIFI,
            subscriberId = null,
            startTime = startTime,
            endTime = endTime
        )

        val phoneMobile = deviceMobile.minusClamped(tetherMobile)
        val phoneWifi = deviceWifi.minusClamped(tetherWifi)

        val snapshot = TrafficSnapshot(
            startTime = startTime,
            endTime = endTime,
            phoneMobile = phoneMobile,
            phoneWifi = phoneWifi,
            tetherMobile = tetherMobile,
            tetherWifi = tetherWifi
        )

        Log.d(
            TAG,
            "Snapshot [$startTime..$endTime]: phoneWifi=${snapshot.phoneWifi}, " +
                    "phoneMobile=${snapshot.phoneMobile}, tetherWifi=${snapshot.tetherWifi}, " +
                    "tetherMobile=${snapshot.tetherMobile}"
        )

        return snapshot
    }

    fun getTrafficForPeriod(startTime: Long, endTime: Long): Pair<Long, Long> {
        val snapshot = getTrafficSnapshot(startTime, endTime)
        return snapshot.totalUp to snapshot.totalDown
    }

    companion object {
        private const val TAG = "TrafficCollector"
        private const val UID_TETHERING = -5
    }
}
