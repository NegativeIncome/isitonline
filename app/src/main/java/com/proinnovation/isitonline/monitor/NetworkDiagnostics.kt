package com.proinnovation.isitonline.monitor

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.os.Build
import android.os.PowerManager
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * Compact snapshot of the device's network state at check time, plus a standalone
 * DNS-resolution probe. Stored in [com.proinnovation.isitonline.data.db.SiteCheckResult.diagnostics]
 * so the history log shows what the network looked like when a check failed.
 *
 * Added 2026-08-30 to chase an intermittent UnknownHostException for www.gljc.org
 * on a Pixel that a Samsung on the same accounts never hit. The two signals that
 * matter here: which transport was active (WIFI/CELL/VPN) and whether Private DNS
 * (DNS-over-TLS) was in strict mode, since an opportunistic-DoT stall resolves as
 * "host not found" even when plain DNS would have worked.
 */
object NetworkDiagnostics {

    /** One-line "key=val key=val" summary of the active network. Never throws. */
    fun snapshot(context: Context): String = try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val net = cm.activeNetwork
        val caps = net?.let { cm.getNetworkCapabilities(it) }
        val lp = net?.let { cm.getLinkProperties(it) }

        val parts = mutableListOf<String>()
        parts += "net=" + transportName(caps)
        parts += "validated=" + bit(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        parts += "inet=" + bit(caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
        parts += "pdns=" + privateDns(lp)
        parts += "dns=" + (lp?.dnsServers
            ?.joinToString(",") { it.hostAddress ?: it.toString().removePrefix("/") }
            ?.ifEmpty { "none" } ?: "?")
        parts += "idle=" + bit(pm.isDeviceIdleMode)
        parts += "psave=" + bit(pm.isPowerSaveMode)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            parts += "bgrestrict=" + bit(am.isBackgroundRestricted)
        }
        parts += "restrictBg=" + cm.restrictBackgroundStatus
        parts.joinToString(" ")
    } catch (e: Exception) {
        "snapshot-failed:${e.javaClass.simpleName}"
    }

    /**
     * Resolve [host] directly, independent of OkHttp, so the log can tell a DNS
     * failure apart from a connection failure. Returns e.g.
     * "resolve=93.184.216.34,2606:2800:220:1:248:1893:25c8:1946 (12ms)" or
     * "resolve=FAIL/UnknownHostException (34ms)".
     */
    fun resolve(host: String): String {
        val start = System.currentTimeMillis()
        return try {
            val addrs = InetAddress.getAllByName(host)
            val ms = System.currentTimeMillis() - start
            "resolve=" + addrs.joinToString(",") { it.hostAddress ?: it.toString().removePrefix("/") } + " (${ms}ms)"
        } catch (e: UnknownHostException) {
            "resolve=FAIL/UnknownHostException (${System.currentTimeMillis() - start}ms)"
        } catch (e: Exception) {
            "resolve=FAIL/${e.javaClass.simpleName} (${System.currentTimeMillis() - start}ms)"
        }
    }

    private fun transportName(caps: NetworkCapabilities?): String = when {
        caps == null -> "NONE"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELL"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETH"
        else -> "OTHER"
    }

    private fun privateDns(lp: LinkProperties?): String = when {
        lp == null -> "?"
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> "n/a"
        lp.isPrivateDnsActive && lp.privateDnsServerName != null -> "strict(${lp.privateDnsServerName})"
        lp.isPrivateDnsActive -> "opportunistic"
        else -> "off"
    }

    private fun bit(b: Boolean?): String = when (b) {
        true -> "1"
        false -> "0"
        null -> "?"
    }
}
