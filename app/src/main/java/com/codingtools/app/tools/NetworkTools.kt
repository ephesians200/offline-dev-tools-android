package com.codingtools.app.tools

import java.math.BigInteger
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import java.net.URL

object Net {
    private fun ip4(s: String): Long { val p = s.trim().split('.'); need(p.size == 4, "Invalid IPv4"); return p.fold(0L) { a, x -> (a shl 8) or x.toLong().also { v -> need(v in 0..255, "Invalid octet") } } }
    private fun str(v: Long) = "${v shr 24 and 255}.${v shr 16 and 255}.${v shr 8 and 255}.${v and 255}"
    fun subnet(s: String): String {
        val (a, c) = s.trim().split('/').let { it[0] to (it.getOrNull(1) ?: "") }
        if (a.contains(':')) {
            val pl = c.toIntOrNull() ?: 64; val addr = BigInteger(1, InetAddress.getByName(a).address); val mask = BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE).shiftLeft(128 - pl).and(BigInteger.ONE.shiftLeft(128).subtract(BigInteger.ONE))
            fun fmt(b: BigInteger) = InetAddress.getByAddress(b.toByteArray().let { x -> ByteArray(16).also { o -> val s0 = x.size - 16; for (i in 0 until 16) o[i] = if (i + s0 >= 0 && i + s0 < x.size) x[i + s0] else 0 } }).hostAddress
            return "IPv6 /$pl\nNetwork : ${fmt(addr.and(mask))}\nLast    : ${fmt(addr.or(BigInteger.ONE.shiftLeft(128 - pl).subtract(BigInteger.ONE)))}\nAddresses: 2^${128 - pl}"
        }
        val p = c.toIntOrNull() ?: 24; need(p in 0..32, "Prefix must be 0–32")
        val ip = ip4(a); val mask = if (p == 0) 0L else (0xffffffffL shl (32 - p)) and 0xffffffffL; val net = ip and mask; val bc = net or (mask.inv() and 0xffffffffL); val hosts = if (p >= 31) (1L shl (32 - p)) else (1L shl (32 - p)) - 2
        val cls = when { ip shr 24 < 128 -> "A"; ip shr 24 < 192 -> "B"; ip shr 24 < 224 -> "C"; ip shr 24 < 240 -> "D (multicast)"; else -> "E" }
        val priv = (ip shr 24 == 10L) || (ip shr 20 == 0xAC1L) || (ip shr 16 == 0xC0A8L)
        return "CIDR      : ${str(net)}/$p\nNetmask   : ${str(mask)}\nWildcard  : ${str(mask.inv() and 0xffffffffL)}\nNetwork   : ${str(net)}\nBroadcast : ${str(bc)}\nFirst host: ${str(if (p >= 31) net else net + 1)}\nLast host : ${str(if (p >= 31) bc else bc - 1)}\nUsable    : $hosts\nClass     : $cls   Private: $priv\nBinary    : ${java.lang.Long.toBinaryString(ip).padStart(32, '0').chunked(8).joinToString(".")}"
    }
    fun chmod(s: String): String {
        val t = s.trim()
        if (Regex("[0-7]{3,4}").matches(t)) { val d = t.takeLast(3); val sym = d.map { c -> val v = c - '0'; (if (v and 4 != 0) "r" else "-") + (if (v and 2 != 0) "w" else "-") + (if (v and 1 != 0) "x" else "-") }.joinToString(""); return "$t → $sym\nchmod $t file\n\nowner: ${d[0]}  group: ${d[1]}  others: ${d[2]}" }
        need(Regex("[-rwxsStT]{9}").matches(t.takeLast(9)), "Use 755 or rwxr-xr-x"); val x = t.takeLast(9); val o = x.chunked(3).joinToString("") { g -> ((if (g[0] == 'r') 4 else 0) + (if (g[1] == 'w') 2 else 0) + (if (g[2] in "xst") 1 else 0)).toString() }
        return "$x → $o\nchmod $o file"
    }
    fun ua(s: String): String {
        val browser = listOf("Edg/" to "Edge", "OPR/" to "Opera", "Chrome/" to "Chrome", "Firefox/" to "Firefox", "Version/" to "Safari").firstOrNull { s.contains(it.first) }?.let { (k, n) -> "$n ${Regex(Regex.escape(k) + "([\\d.]+)").find(s)?.groupValues?.get(1) ?: ""}" } ?: "Unknown"
        val os = when { s.contains("Android") -> "Android " + (Regex("Android ([\\d.]+)").find(s)?.groupValues?.get(1) ?: ""); s.contains("iPhone") || s.contains("iPad") -> "iOS " + (Regex("OS ([\\d_]+)").find(s)?.groupValues?.get(1)?.replace('_', '.') ?: ""); s.contains("Windows") -> "Windows"; s.contains("Mac OS X") -> "macOS"; s.contains("Linux") -> "Linux"; else -> "Unknown" }
        val dev = if (s.contains("Mobile")) "Mobile" else if (s.contains("Tablet") || s.contains("iPad")) "Tablet" else "Desktop"; val eng = when { s.contains("AppleWebKit") && s.contains("Chrome") -> "Blink"; s.contains("AppleWebKit") -> "WebKit"; s.contains("Gecko/") -> "Gecko"; else -> "?" }
        return "Browser: $browser\nEngine : $eng\nOS     : $os\nDevice : $dev\nBot    : ${Regex("bot|crawl|spider", RegexOption.IGNORE_CASE).containsMatchIn(s)}"
    }
    fun url(s: String): String { val u = URI(s.trim()); val q = u.rawQuery
        return "Scheme  : ${u.scheme}\nHost    : ${u.host}\nPort    : ${if (u.port > 0) u.port else "(default)"}\nUser    : ${u.userInfo ?: "-"}\nPath    : ${u.rawPath}\nQuery   : ${q ?: "-"}\nFragment: ${u.rawFragment ?: "-"}\n" + (q?.split('&')?.joinToString("\n", "\nParameters\n") { p -> "  " + java.net.URLDecoder.decode(p.substringBefore('='), "UTF-8") + " = " + java.net.URLDecoder.decode(p.substringAfter('=', ""), "UTF-8") } ?: "") }
    fun clean(s: String): String { val u = URI(s.trim()); val q = u.rawQuery ?: return s.trim(); val keep = q.split('&').filter { p -> !Regex("^(utm_.*|fbclid|gclid|mc_eid|igshid|ref)$").matches(p.substringBefore('=')) }
        return URI(u.scheme, u.authority, u.path, null, u.fragment).toString().let { base -> base + (if (keep.isNotEmpty()) "?" + keep.joinToString("&") else "") + (u.rawFragment?.let { "#$it" } ?: "") } }
    fun bandwidth(size: String, speed: String): String { val b = Conv.parseSize(size); val m = Regex("([\\d.]+)\\s*([kmg]?)(bps|b/s)?", RegexOption.IGNORE_CASE).matchEntire(speed.trim()) ?: throw IllegalArgumentException("Speed like 50 Mbps")
        val bps = m.groupValues[1].toDouble() * when (m.groupValues[2].lowercase()) { "k" -> 1e3; "m" -> 1e6; "g" -> 1e9; else -> 1.0 }; return "Transfer time: ${Conv.duration((b * 8 / bps).toLong())}  (${"%.1f".format(b * 8 / bps)} s)\nSize: ${b.toLong()} bytes   Speed: ${fmt(bps / 1e6)} Mbit/s (${fmt(bps / 8e6)} MB/s)" }
    fun request(line: String, headers: String, body: String): String {
        val parts = line.trim().split(Regex("\\s+"), 2); val method = if (parts.size == 2) parts[0].uppercase() else "GET"; val u = parts.last()
        need(u.startsWith("http"), "Enter 'GET https://host/path' in the first parameter")
        val t0 = System.currentTimeMillis(); val c = URL(u).openConnection() as HttpURLConnection
        c.requestMethod = method; c.connectTimeout = 15000; c.readTimeout = 20000; c.instanceFollowRedirects = true
        lines(headers).forEach { h -> val i = h.indexOf(':'); if (i > 0) c.setRequestProperty(h.substring(0, i).trim(), h.substring(i + 1).trim()) }
        if (body.isNotBlank() && method != "GET") { c.doOutput = true; c.outputStream.use { it.write(body.toByteArray()) } }
        val code = c.responseCode; val stream = if (code >= 400) c.errorStream else c.inputStream; val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        val pretty = runCatching { Json.write(Json.parse(text)) }.getOrDefault(text)
        return (if (u.startsWith("http://", ignoreCase = true)) "⚠ Plain HTTP request: nothing is encrypted, so anyone on the network can read it.\n\n" else "") + "HTTP $code ${c.responseMessage}   (${System.currentTimeMillis() - t0} ms, ${text.length} chars)\n\n" + c.headerFields.filterKeys { it != null }.entries.joinToString("\n") { "${it.key}: ${it.value.joinToString("; ")}" } + "\n\n" + pretty.take(60_000)
    }
    fun openapi(s: String): String {
        val d = parseAny(s) as Map<*, *>; val info = d["info"] as? Map<*, *>; val sb = StringBuilder("${info?.get("title")} v${info?.get("version")}\n")
        (d["servers"] as? List<*>)?.forEach { sb.append("Server: ${(it as Map<*, *>)["url"]}\n") }; sb.append('\n')
        (d["paths"] as? Map<*, *>)?.forEach { (path, ops) -> (ops as? Map<*, *>)?.forEach { (m, op) -> if (m.toString() in setOf("get", "post", "put", "delete", "patch", "head", "options")) sb.append("${m.toString().uppercase().padEnd(7)} $path   ${(op as? Map<*, *>)?.get("summary") ?: ""}\n") } }
        return sb.toString().trim()
    }
}

val networkTools: List<Tool> = listOf(
    tool("subnet", "IP & Subnet Calculator", Category.NETWORK, "IPv4 / IPv6 CIDR: network, broadcast, hosts, masks", m("Calculate") { Net.subnet(it) }, hint = "192.168.1.10/24 or 2001:db8::1/64", sample = "192.168.1.10/24"),
    tool("chmod", "chmod Permission Calculator", Category.NETWORK, "Numeric ⇄ symbolic Unix permissions", m("Convert") { Net.chmod(it) }, hint = "755 or rwxr-xr-x", sample = "755"),
    tool("url-parse", "URL Parser & Cleaner", Category.NETWORK, "Split URLs, decode query strings, strip tracking params", m("Parse") { Net.url(it) }, m("Remove tracking params") { Net.clean(it) }, hint = "URL", sample = "https://user@example.com:8443/a/b?q=kotlin%20compose&utm_source=x#top"),
    tool("ua", "User-Agent Parser", Category.NETWORK, "Browser, engine, OS and device", m("Parse") { Net.ua(it) }, hint = "User-Agent string", sample = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"),
    tool("bandwidth", "Bandwidth / Transfer Time", Category.NETWORK, "How long will it take?", mp("Calculate") { s, p -> Net.bandwidth(s, p.getOrElse(0) { "" }) }, hint = "File size (e.g. 2.5 GB)", params = listOf("Speed (e.g. 50 Mbps)"), sample = "2.5 GB", psample = listOf("50 Mbps")),
    tool("rest", "REST Client", Category.NETWORK, "Send a request directly from your phone to the host you type (no middle server)",
        mp("Send") { b, p -> Net.request(p.getOrElse(0) { "" }, p.getOrElse(1) { "" }, b) },
        mp("GraphQL query") { q, p -> Net.request("POST " + p.getOrElse(0) { "" }.substringAfter(' '), "Content-Type: application/json\n" + p.getOrElse(1) { "" }, Json.write(mapOf("query" to q), 0)) },
        hint = "Request body (or GraphQL query)", params = listOf("METHOD URL, e.g. GET https://api.github.com/zen", "Headers (Name: value per line)"), sample = "", psample = listOf("GET https://api.github.com/zen", "Accept: application/json")),
    tool("net-probe", "DNS · Ping · Port Check", Category.NETWORK, "Resolve hosts, test reachability and TCP ports",
        m("DNS lookup") { h -> InetAddress.getAllByName(h.trim()).joinToString("\n") { "${if (it.address.size == 4) "A    " else "AAAA "} ${it.hostAddress}" } },
        m("Reachable? (ICMP/TCP-7)") { h -> val t0 = System.currentTimeMillis(); val ok = InetAddress.getByName(h.trim()).isReachable(3000); "${h.trim()} ${if (ok) "is reachable" else "did not answer"} (${System.currentTimeMillis() - t0} ms)" },
        mp("TCP port check") { h, p -> val port = p.getOrElse(0) { "" }.toIntOrNull() ?: 443; val t0 = System.currentTimeMillis(); Socket().use { it.connect(InetSocketAddress(h.trim(), port), 4000) }; "✔ ${h.trim()}:$port open (${System.currentTimeMillis() - t0} ms)" },
        m("Reverse DNS") { h -> InetAddress.getByName(h.trim()).canonicalHostName },
        hint = "Host name or IP", params = listOf("Port (443)"), sample = "example.com"),
    tool("openapi", "OpenAPI / Swagger Viewer", Category.NETWORK, "List endpoints from a JSON or YAML spec", m("List endpoints") { Net.openapi(it) }, hint = "OpenAPI JSON/YAML"),
    tool("webhook", "Webhook / Payload Viewer", Category.NETWORK, "Pretty-print JSON, form-encoded or header dumps locally",
        m("Auto format") { s -> val t = s.trim(); if (t.startsWith("{") || t.startsWith("[")) Json.write(Json.parse(t)) else if (t.contains('=') && !t.contains('\n')) t.split('&').joinToString("\n") { java.net.URLDecoder.decode(it, "UTF-8") } else t }, hint = "Paste payload", sample = "id=1&event=push&repo=my%2Fapp"),
)
