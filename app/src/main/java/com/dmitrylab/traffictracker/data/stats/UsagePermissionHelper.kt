package com.dmitrylab.traffictracker.data.stats

import android.app.AppOpsManager
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log

internal class UsagePermissionHelper(private val context: Context) {
    val statsManager: NetworkStatsManager? =
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
    fun getMobileSubscriberId(): String? {
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

    companion object {
        private const val TAG = "UsagePermissionHelper"
    }
}
