package com.dmitrylab.traffictracker.data.stats

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

data class AppTrafficItem(
    val uid: Int,
    val label: String,
    val packageName: String?,
    val upBytes: Long,
    val downBytes: Long
) {
    val totalBytes: Long get() = upBytes + downBytes
}
