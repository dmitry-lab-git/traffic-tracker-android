package com.dmitrylab.traffictracker.data.stats

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log

data class NetworkTrafficBucket(
    val upBytes: Long,
    val downBytes: Long
) {
    val totalBytes: Long get() = upBytes + downBytes

    operator fun plus(other: NetworkTrafficBucket): NetworkTrafficBucket =
        NetworkTrafficBucket(
            upBytes = upBytes + other.upBytes,
            downBytes = downBytes + other.downBytes
        )

    fun minusClamped(other: NetworkTrafficBucket): NetworkTrafficBucket =
        NetworkTrafficBucket(
            upBytes = (upBytes - other.upBytes).coerceAtLeast(0L),
            downBytes = (downBytes - other.downBytes).coerceAtLeast(0L)
        )

    companion object {
        val ZERO = NetworkTrafficBucket(0L, 0L)
    }
}

data class TrafficSnapshot(
    val startTime: Long,
    val endTime: Long,
    val phoneMobile: NetworkTrafficBucket,
    val phoneWifi: NetworkTrafficBucket,
    val tetherMobile: NetworkTrafficBucket,
    val tetherWifi: NetworkTrafficBucket
) {
    val phoneUp: Long get() = phoneMobile.upBytes + phoneWifi.upBytes
    val phoneDown: Long get() = phoneMobile.downBytes + phoneWifi.downBytes
    val phoneTotal: Long get() = phoneUp + phoneDown

    val tetherUp: Long get() = tetherMobile.upBytes + tetherWifi.upBytes
    val tetherDown: Long get() = tetherMobile.downBytes + tetherWifi.downBytes
    val tetherTotal: Long get() = tetherUp + tetherDown

    val totalUp: Long get() = phoneUp + tetherUp
    val totalDown: Long get() = phoneDown + tetherDown
}

class TrafficCollector(private val context: Context) {

    @Suppress("DEPRECATION")
    fun getAppTrafficBreakdown(startTime: Long, endTime: Long): List<AppTrafficItem> {
        require(endTime >= startTime) { "endTime ($endTime) must be >= startTime ($startTime)" }
        val subscriberId = getMobileSubscriberId()

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
            UID_TETHERING -> "Раздача / Tethering (UID -5)" to null
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
    private val statsManager: NetworkStatsManager? =
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager

    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun createUsageAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    @Suppress("DEPRECATION", "MissingPermission")
    private fun getMobileSubscriberId(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return null
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            tm?.subscriberId
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to read subscriberId (READ_PHONE_STATE missing)", e)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get subscriberId", e)
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun queryDeviceBucket(
        networkType: Int,
        subscriberId: String?,
        startTime: Long,
        endTime: Long
    ): NetworkTrafficBucket {
        val manager = statsManager ?: return NetworkTrafficBucket.ZERO
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
        val manager = statsManager ?: return NetworkTrafficBucket.ZERO
        var stats: NetworkStats? = null
        return try {
            stats = manager.querySummary(networkType, subscriberId, startTime, endTime)
            val bucket = NetworkStats.Bucket()
            var up = 0L
            var down = 0L
            while (stats != null && stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                if (isTetherUid(bucket.uid)) {
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

    private fun isTetherUid(uid: Int): Boolean {
        return uid == UID_TETHERING
    }

    fun getTrafficSnapshot(startTime: Long, endTime: Long): TrafficSnapshot {
        require(endTime >= startTime) { "endTime ($endTime) must be >= startTime ($startTime)" }
        val subscriberId = getMobileSubscriberId()

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

    @Suppress("DEPRECATION")
    private fun logMobileDiagnostic(subscriberId: String?, endTime: Long) {
        val manager = statsManager ?: return
        val dayAgo = endTime - 86_400_000L
        var stats: NetworkStats? = null
        try {
            stats = manager.querySummary(ConnectivityManager.TYPE_MOBILE, subscriberId, dayAgo, endTime)
            val bucket = NetworkStats.Bucket()
            var sumUp = 0L
            var sumDown = 0L
            val nonZeroUids = mutableListOf<String>()
            while (stats != null && stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val up = bucket.txBytes.coerceAtLeast(0L)
                val down = bucket.rxBytes.coerceAtLeast(0L)
                sumUp += up
                sumDown += down
                if (up > 0L || down > 0L) {
                    nonZeroUids.add("uid=${bucket.uid}(up=$up,down=$down)")
                }
            }
            Log.d(
                TAG,
                "Diag24h TYPE_MOBILE sub=${subscriberId != null} totalUp=$sumUp totalDown=$sumDown uids=${nonZeroUids.take(15)}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Diag24h TYPE_MOBILE failed", e)
        } finally {
            try {
                stats?.close()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val TAG = "TrafficCollector"
        private const val UID_TETHERING = -5
    }
}

data class AppTrafficItem(
    val uid: Int,
    val label: String,
    val packageName: String?,
    val upBytes: Long,
    val downBytes: Long
) {
    val totalBytes: Long get() = upBytes + downBytes
}