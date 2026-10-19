package com.manga.reader.data.network

import okhttp3.Dns
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap

object MangaDns : Dns {
    private val mangaDexIps = listOf(
        InetAddress.getByAddress("api.mangadex.org", byteArrayOf(45.toByte(), 129.toByte(), 229.toByte(), 1.toByte())),
        InetAddress.getByAddress("api.mangadex.org", byteArrayOf(45.toByte(), 129.toByte(), 229.toByte(), 2.toByte()))
    )

    private val staticHosts: Map<String, List<InetAddress>> = mapOf(
        "api.mangadex.org" to mangaDexIps,
        "uploads.mangadex.org" to listOf(
            InetAddress.getByAddress("uploads.mangadex.org", byteArrayOf(45.toByte(), 129.toByte(), 229.toByte(), 1.toByte())),
            InetAddress.getByAddress("uploads.mangadex.org", byteArrayOf(45.toByte(), 129.toByte(), 229.toByte(), 2.toByte()))
        ),
        "mangadex.org" to listOf(
            InetAddress.getByAddress("mangadex.org", byteArrayOf(45.toByte(), 129.toByte(), 229.toByte(), 1.toByte())),
            InetAddress.getByAddress("mangadex.org", byteArrayOf(45.toByte(), 129.toByte(), 229.toByte(), 2.toByte()))
        ),
        "dxlb.mangadex.org" to listOf(
            InetAddress.getByAddress("dxlb.mangadex.org", byteArrayOf(45.toByte(), 129.toByte(), 229.toByte(), 1.toByte())),
            InetAddress.getByAddress("dxlb.mangadex.org", byteArrayOf(45.toByte(), 129.toByte(), 229.toByte(), 2.toByte()))
        )
    )

    private val dohCache = ConcurrentHashMap<String, List<InetAddress>>()

    override fun lookup(hostname: String): List<InetAddress> {
        val lower = hostname.lowercase()

        staticHosts[lower]?.let { return it }

        dohCache[lower]?.let { return it }

        try {
            val systemAddrs = Dns.SYSTEM.lookup(hostname)
            val isPoisoned = systemAddrs.any { addr ->
                val ip = addr.hostAddress ?: ""
                ip.startsWith("158.140.") ||
                ip.startsWith("118.97.") ||
                ip.startsWith("36.86.") ||
                ip == "10.10.10.10" ||
                ip == "127.0.0.1"
            }
            if (systemAddrs.isNotEmpty() && !isPoisoned) {
                return systemAddrs
            }
        } catch (_: Exception) {}

        val dohAddrs = resolveViaDoH(lower)
        if (dohAddrs.isNotEmpty()) {
            dohCache[lower] = dohAddrs
            return dohAddrs
        }

        throw UnknownHostException("Unable to resolve host: $hostname")
    }

    private fun resolveViaDoH(hostname: String): List<InetAddress> {
        try {
            val url = URL("https://1.1.1.1/dns-query?name=$hostname&type=A")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.setRequestProperty("Accept", "application/dns-json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val json = reader.readText()
                reader.close()
                val root = JSONObject(json)
                val answers = root.optJSONArray("Answer")
                if (answers != null && answers.length() > 0) {
                    val list = mutableListOf<InetAddress>()
                    for (i in 0 until answers.length()) {
                        val item = answers.optJSONObject(i) ?: continue
                        val type = item.optInt("type", 0)
                        if (type == 1) {
                            val ip = item.optString("data")
                            if (ip.isNotBlank()) {
                                list.add(InetAddress.getByName(ip))
                            }
                        }
                    }
                    if (list.isNotEmpty()) return list
                }
            }
        } catch (_: Exception) {}

        try {
            val url = URL("https://dns.google/resolve?name=$hostname&type=A")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val json = reader.readText()
                reader.close()
                val root = JSONObject(json)
                val answers = root.optJSONArray("Answer")
                if (answers != null && answers.length() > 0) {
                    val list = mutableListOf<InetAddress>()
                    for (i in 0 until answers.length()) {
                        val item = answers.optJSONObject(i) ?: continue
                        val type = item.optInt("type", 0)
                        if (type == 1) {
                            val ip = item.optString("data")
                            if (ip.isNotBlank()) {
                                list.add(InetAddress.getByName(ip))
                            }
                        }
                    }
                    if (list.isNotEmpty()) return list
                }
            }
        } catch (_: Exception) {}

        return emptyList()
    }
}
