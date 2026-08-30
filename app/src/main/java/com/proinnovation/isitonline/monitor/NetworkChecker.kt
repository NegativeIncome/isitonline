package com.proinnovation.isitonline.monitor

import android.content.Context
import android.net.Uri
import com.proinnovation.isitonline.data.db.Site
import com.proinnovation.isitonline.data.db.SiteCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class NetworkChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun checkSite(context: Context, site: Site): List<SiteCheckResult> = withContext(Dispatchers.IO) {
        // One network-state snapshot per check cycle, shared by both sub-checks.
        val snapshot = NetworkDiagnostics.snapshot(context.applicationContext)
        listOf(checkHttps(site, snapshot), checkPing(site, snapshot))
    }

    private fun checkHttps(site: Site, snapshot: String): SiteCheckResult {
        val start = System.currentTimeMillis()
        val host = Uri.parse(site.url).host ?: site.url
        // Resolve independently of OkHttp so the log distinguishes a DNS failure
        // from a connection failure regardless of how the HTTP call ends up.
        val diagnostics = "$snapshot | ${NetworkDiagnostics.resolve(host)}"
        return try {
            val headRequest = Request.Builder().url(site.url).head().build()
            val response = client.newCall(headRequest).execute()
            val code = response.code
            response.close()

            if (code == 405) {
                // Server rejects HEAD — fall back to GET
                val getRequest = Request.Builder().url(site.url).get().build()
                val getResponse = client.newCall(getRequest).execute()
                val getCode = getResponse.code
                getResponse.body?.close()
                val latency = System.currentTimeMillis() - start
                val success = getCode in 200..399
                SiteCheckResult(
                    siteId = site.id, checkType = "HTTPS", checkedAt = start,
                    success = success, responseCode = getCode, latencyMs = latency,
                    errorMessage = if (success) null else "HTTP $getCode",
                    diagnostics = diagnostics
                )
            } else {
                val latency = System.currentTimeMillis() - start
                val success = code in 200..399
                SiteCheckResult(
                    siteId = site.id, checkType = "HTTPS", checkedAt = start,
                    success = success, responseCode = code, latencyMs = latency,
                    errorMessage = if (success) null else "HTTP $code",
                    diagnostics = diagnostics
                )
            }
        } catch (e: Exception) {
            SiteCheckResult(
                siteId = site.id, checkType = "HTTPS", checkedAt = start,
                success = false, responseCode = null, latencyMs = null,
                errorMessage = "${e.javaClass.simpleName}: ${e.message ?: "Unknown error"}",
                diagnostics = diagnostics
            )
        }
    }

    private fun checkPing(site: Site, snapshot: String): SiteCheckResult {
        val start = System.currentTimeMillis()
        return try {
            val host = Uri.parse(site.url).host ?: site.url
            val proc = Runtime.getRuntime().exec(arrayOf("ping", "-c", "1", "-W", "3", host))
            val finished = proc.waitFor(5, TimeUnit.SECONDS)
            if (!finished) {
                proc.destroy()
                return SiteCheckResult(
                    siteId = site.id, checkType = "PING", checkedAt = start,
                    success = false, responseCode = null, latencyMs = null,
                    errorMessage = "Ping timed out",
                    diagnostics = snapshot
                )
            }
            val exitCode = proc.exitValue()
            val latency = System.currentTimeMillis() - start
            val success = exitCode == 0
            SiteCheckResult(
                siteId = site.id, checkType = "PING", checkedAt = start,
                success = success, responseCode = null,
                latencyMs = if (success) latency else null,
                errorMessage = if (success) null else "Ping failed (exit $exitCode)",
                diagnostics = snapshot
            )
        } catch (e: Exception) {
            SiteCheckResult(
                siteId = site.id, checkType = "PING", checkedAt = start,
                success = false, responseCode = null, latencyMs = null,
                errorMessage = "${e.javaClass.simpleName}: ${e.message ?: "Unknown error"}",
                diagnostics = snapshot
            )
        }
    }
}
