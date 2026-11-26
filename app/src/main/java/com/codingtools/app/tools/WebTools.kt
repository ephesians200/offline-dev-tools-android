package com.codingtools.app.tools

import kotlin.math.*

object Color {
    private val names = mapOf("black" to "#000000", "white" to "#ffffff", "red" to "#ff0000", "green" to "#008000", "blue" to "#0000ff", "yellow" to "#ffff00", "orange" to "#ffa500", "purple" to "#800080", "pink" to "#ffc0cb", "gray" to "#808080", "grey" to "#808080", "cyan" to "#00ffff", "magenta" to "#ff00ff", "teal" to "#008080", "navy" to "#000080", "brown" to "#a52a2a", "lime" to "#00ff00", "gold" to "#ffd700", "indigo" to "#4b0082", "tomato" to "#ff6347")
    fun parse(s0: String): IntArray {
        val s = s0.trim().lowercase(); names[s]?.let { return parse(it) }
        if (s.startsWith("#") || Regex("[0-9a-f]{3,8}").matches(s)) {
            var h = s.removePrefix("#"); if (h.length == 3 || h.length == 4) h = h.map { "$it$it" }.joinToString("")
            need(h.length == 6 || h.length == 8, "Bad hex colour"); return intArrayOf(h.substring(0, 2).toInt(16), h.substring(2, 4).toInt(16), h.substring(4, 6).toInt(16))
        }
        val nums = Regex("-?[\\d.]+").findAll(s).map { it.value.toDouble() }.toList()
        return when {
            s.startsWith("rgb") -> intArrayOf(nums[0].roundToInt().coerceIn(0, 255), nums[1].roundToInt().coerceIn(0, 255), nums[2].roundToInt().coerceIn(0, 255))
            s.startsWith("hsl") -> hslToRgb(nums[0], nums[1] / 100, nums[2] / 100)
            else -> throw IllegalArgumentException("Use #hex, rgb(r,g,b), hsl(h,s%,l%) or a name")
        }
    }
    fun hex(c: IntArray) = "#%02x%02x%02x".format(c[0], c[1], c[2])
    fun hslToRgb(h: Double, s: Double, l: Double): IntArray {
        val c = (1 - abs(2 * l - 1)) * s; val hh = ((h % 360) + 360) % 360 / 60; val x = c * (1 - abs(hh % 2 - 1)); val m = l - c / 2
        val (r, g, b) = when { hh < 1 -> Triple(c, x, 0.0); hh < 2 -> Triple(x, c, 0.0); hh < 3 -> Triple(0.0, c, x); hh < 4 -> Triple(0.0, x, c); hh < 5 -> Triple(x, 0.0, c); else -> Triple(c, 0.0, x) }
        return intArrayOf(((r + m) * 255).roundToInt(), ((g + m) * 255).roundToInt(), ((b + m) * 255).roundToInt())
    }
    fun hsl(c: IntArray): DoubleArray {
        val r = c[0] / 255.0; val g = c[1] / 255.0; val b = c[2] / 255.0; val mx = max(r, max(g, b)); val mn = min(r, min(g, b)); val d = mx - mn; val l = (mx + mn) / 2
        val s = if (d == 0.0) 0.0 else d / (1 - abs(2 * l - 1))
        val h = when { d == 0.0 -> 0.0; mx == r -> 60 * (((g - b) / d) % 6); mx == g -> 60 * ((b - r) / d + 2); else -> 60 * ((r - g) / d + 4) }
        return doubleArrayOf((h + 360) % 360, s * 100, l * 100)
    }
    private fun lin(v: Int): Double { val c = v / 255.0; return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4) }
    fun luminance(c: IntArray) = 0.2126 * lin(c[0]) + 0.7152 * lin(c[1]) + 0.0722 * lin(c[2])
    fun contrast(a: IntArray, b: IntArray): Double { val l1 = luminance(a); val l2 = luminance(b); return (max(l1, l2) + 0.05) / (min(l1, l2) + 0.05) }
    fun all(s: String): String {
        val c = parse(s); val h = hsl(c); val r = c[0] / 255.0; val g = c[1] / 255.0; val b = c[2] / 255.0
        val mx = max(r, max(g, b)); val mn = min(r, min(g, b)); val k = 1 - mx
        val cy = if (k == 1.0) 0.0 else (1 - r - k) / (1 - k); val ma = if (k == 1.0) 0.0 else (1 - g - k) / (1 - k); val ye = if (k == 1.0) 0.0 else (1 - b - k) / (1 - k)
        val lr = lin(c[0]); val lg = lin(c[1]); val lb = lin(c[2])
        val l_ = Math.cbrt(0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb); val m_ = Math.cbrt(0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb); val s_ = Math.cbrt(0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb)
        val ol = 0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_; val oa = 1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_; val ob = 0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_
        val x = 0.4124564 * lr + 0.3575761 * lg + 0.1804375 * lb; val y = 0.2126729 * lr + 0.7151522 * lg + 0.0721750 * lb; val z = 0.0193339 * lr + 0.1191920 * lg + 0.9503041 * lb
        fun f(t: Double) = if (t > 0.008856) Math.cbrt(t) else 7.787 * t + 16.0 / 116
        val L = 116 * f(y) - 16; val A = 500 * (f(x / 0.95047) - f(y)); val B = 200 * (f(y) - f(z / 1.08883))
        return "HEX   ${hex(c)}\nRGB   rgb(${c[0]}, ${c[1]}, ${c[2]})\nHSL   hsl(${h[0].roundToInt()}, ${h[1].roundToInt()}%, ${h[2].roundToInt()}%)\nHSV   ${"%.0f".format(h[0])}°, ${"%.0f".format(if (mx == 0.0) 0.0 else (mx - mn) / mx * 100)}%, ${"%.0f".format(mx * 100)}%\nCMYK  ${"%.0f".format(cy * 100)}%, ${"%.0f".format(ma * 100)}%, ${"%.0f".format(ye * 100)}%, ${"%.0f".format(k * 100)}%\nOKLCH oklch(${"%.3f".format(ol)} ${"%.3f".format(hypot(oa, ob))} ${"%.1f".format((Math.toDegrees(atan2(ob, oa)) + 360) % 360)})\nLAB   ${"%.1f".format(L)}, ${"%.1f".format(A)}, ${"%.1f".format(B)}\nXYZ   ${"%.3f".format(x)}, ${"%.3f".format(y)}, ${"%.3f".format(z)}\nAndroid 0xFF${hex(c).drop(1).uppercase()}\n\nContrast vs white: ${"%.2f".format(contrast(c, intArrayOf(255, 255, 255)))}:1\nContrast vs black: ${"%.2f".format(contrast(c, intArrayOf(0, 0, 0)))}:1"
    }
    fun wcag(fg: String, bg: String): String {
        val r = contrast(parse(fg), parse(bg)); fun ok(b: Boolean) = if (b) "PASS" else "FAIL"
        return "Contrast ratio: ${"%.2f".format(r)}:1\n\nNormal text  AA: ${ok(r >= 4.5)}   AAA: ${ok(r >= 7)}\nLarge text   AA: ${ok(r >= 3)}   AAA: ${ok(r >= 4.5)}\nUI / graphics AA: ${ok(r >= 3)}"
    }
    fun palette(s: String): String {
        val h = hsl(parse(s)); fun hx(dh: Double, l: Double = h[2]) = hex(hslToRgb(h[0] + dh, h[1] / 100, l / 100))
        return "Shades (light → dark)\n" + (95 downTo 5 step 10).joinToString("\n") { "  L$it  ${hx(0.0, it.toDouble())}" } + "\n\nComplementary  ${hx(0.0)}  ${hx(180.0)}\nAnalogous      ${hx(-30.0)}  ${hx(0.0)}  ${hx(30.0)}\nTriadic        ${hx(0.0)}  ${hx(120.0)}  ${hx(240.0)}\nTetradic       ${hx(0.0)}  ${hx(90.0)}  ${hx(180.0)}  ${hx(270.0)}\nSplit-comp.    ${hx(0.0)}  ${hx(150.0)}  ${hx(210.0)}"
    }
}

object Web {
    fun specificity(sel: String): String = sel.split(',').joinToString("\n") { s0 ->
        val s = s0.trim(); val noStr = s.replace(Regex("\"[^\"]*\"|'[^']*'"), ""); val a = Regex("#[\\w-]+").findAll(noStr).count()
        val pseudoEl = Regex("::[\\w-]+").findAll(noStr).count(); val b = Regex("\\.[\\w-]+|\\[[^]]+]|:(?!:)(?!not|is|where)[\\w-]+").findAll(noStr).count()
        val c = Regex("(^|[\\s>+~(])[a-zA-Z][\\w-]*").findAll(noStr.replace(Regex("[#.:\\[][\\w-]+"), " ")).count() + pseudoEl
        "$s → ($a, $b, $c)  ${a * 100 + b * 10 + c}"
    }
    fun svgMin(s: String) = s.replace(Regex("<\\?xml.*?\\?>", RegexOption.DOT_MATCHES_ALL), "").replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "").replace(Regex("<metadata.*?</metadata>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("\\s(inkscape|sodipodi):[\\w-]+=\"[^\"]*\""), "").replace(Regex("\\s+"), " ").replace(Regex(">\\s+<"), "><").replace(Regex("(\\d+\\.\\d{3})\\d+")) { it.groupValues[1] }.trim()
    fun cache(h: String): String {
        val d = h.lowercase().removePrefix("cache-control:").split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return d.joinToString("\n") { x -> val k = x.substringBefore('='); val v = x.substringAfter('=', "")
            "$x → " + when (k) { "max-age" -> "fresh for $v s (${Conv.duration(v.toLong())})"; "s-maxage" -> "shared caches (CDN) fresh for $v s"; "no-cache" -> "must revalidate before reuse"; "no-store" -> "never store"; "public" -> "any cache may store"; "private" -> "only the browser may store"
                "immutable" -> "will never change while fresh"; "must-revalidate" -> "no stale use after expiry"; "stale-while-revalidate" -> "serve stale for $v s while refreshing"; "stale-if-error" -> "serve stale on error for $v s"; else -> "?" } }
    }
    fun meta(i: String): String { val k = kv(i); val t = k.getOr("title", "Page title"); val d = k.getOr("description", "Page description"); val u = k.getOr("url", "https://example.com"); val img = k.getOr("image", "$u/og.png")
        return "<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n<title>$t</title>\n<meta name=\"description\" content=\"$d\">\n<link rel=\"canonical\" href=\"$u\">\n\n<!-- Open Graph -->\n<meta property=\"og:type\" content=\"website\">\n<meta property=\"og:title\" content=\"$t\">\n<meta property=\"og:description\" content=\"$d\">\n<meta property=\"og:url\" content=\"$u\">\n<meta property=\"og:image\" content=\"$img\">\n\n<!-- Twitter -->\n<meta name=\"twitter:card\" content=\"summary_large_image\">\n<meta name=\"twitter:title\" content=\"$t\">\n<meta name=\"twitter:description\" content=\"$d\">\n<meta name=\"twitter:image\" content=\"$img\">" }
    fun jsonLd(type: String, i: String): String { val k = kv(i); val m = LinkedHashMap<String, Any?>(); m["@context"] = "https://schema.org"; m["@type"] = type; k.forEach { (a, b) -> m[a] = b }; return "<script type=\"application/ld+json\">\n" + Json.write(m) + "\n</script>" }
    fun robots(i: String): String { val k = kv(i); return "User-agent: *\nAllow: /\nDisallow: ${k.getOr("disallow", "/admin/")}\n\nSitemap: ${k.getOr("sitemap", "https://example.com/sitemap.xml")}" }
    fun sitemap(i: String) = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n" + lines(i).filter { it.isNotBlank() }.joinToString("\n") { "  <url>\n    <loc>${it.trim()}</loc>\n    <lastmod>${java.time.LocalDate.now()}</lastmod>\n  </url>" } + "\n</urlset>"
    fun manifest(i: String): String { val k = kv(i); return Json.write(linkedMapOf("name" to k.getOr("name", "My App"), "short_name" to k.getOr("short_name", "App"), "start_url" to "/", "display" to "standalone", "background_color" to k.getOr("background", "#ffffff"), "theme_color" to k.getOr("theme", "#0b3d91"), "icons" to listOf(linkedMapOf("src" to "/icon-192.png", "sizes" to "192x192", "type" to "image/png"), linkedMapOf("src" to "/icon-512.png", "sizes" to "512x512", "type" to "image/png")))) }
    val sw = "const CACHE = 'v1';\nconst ASSETS = ['/', '/index.html', '/app.js', '/style.css'];\n\nself.addEventListener('install', e => {\n  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ASSETS)));\n  self.skipWaiting();\n});\nself.addEventListener('activate', e => {\n  e.waitUntil(caches.keys().then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))));\n});\nself.addEventListener('fetch', e => {\n  e.respondWith(caches.match(e.request).then(r => r || fetch(e.request)));\n});\n\n// register: navigator.serviceWorker.register('/sw.js');"
    fun css(kind: String, i: String): String { val p = i.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when (kind) {
            "shadow" -> "box-shadow: ${p.getOrElse(0) { "0" }}px ${p.getOrElse(1) { "4" }}px ${p.getOrElse(2) { "12" }}px ${p.getOrElse(3) { "0" }}px ${p.getOrElse(4) { "rgba(0,0,0,.25)" }};"
            "radius" -> "border-radius: ${p.joinToString(" ") { if (it.endsWith("%") || it.endsWith("px") || it.endsWith("rem")) it else it + "px" }.ifBlank { "12px" }};"
            "flex" -> ".center {\n  display: flex;\n  justify-content: center;\n  align-items: center;\n}\n\n.row { display: flex; gap: 1rem; flex-wrap: wrap; }\n.col { display: flex; flex-direction: column; gap: 1rem; }\n.space-between { display: flex; justify-content: space-between; align-items: center; }"
            "grid" -> ".grid {\n  display: grid;\n  grid-template-columns: repeat(auto-fit, minmax(${p.getOrElse(0) { "240" }}px, 1fr));\n  gap: 1rem;\n}\n\n.layout {\n  display: grid;\n  grid-template-areas: \"header header\" \"sidebar main\" \"footer footer\";\n  grid-template-columns: 240px 1fr;\n  grid-template-rows: auto 1fr auto;\n  min-height: 100vh;\n}"
            "anim" -> "@keyframes ${p.getOrElse(0) { "fade-in" }} {\n  from { opacity: 0; transform: translateY(8px); }\n  to   { opacity: 1; transform: translateY(0); }\n}\n\n.animated {\n  animation: ${p.getOrElse(0) { "fade-in" }} ${p.getOrElse(1) { "0.4" }}s ease-out both;\n}"
            "clip" -> "triangle:  clip-path: polygon(50% 0%, 0% 100%, 100% 0%, 100% 100%);\nhexagon:   clip-path: polygon(25% 0%, 75% 0%, 100% 50%, 75% 100%, 25% 100%, 0% 50%);\ncircle:    clip-path: circle(50% at 50% 50%);\nstar:      clip-path: polygon(50% 0%, 61% 35%, 98% 35%, 68% 57%, 79% 91%, 50% 70%, 21% 91%, 32% 57%, 2% 35%, 39% 35%);\nbadge:     clip-path: polygon(0 0, 100% 0, 100% 75%, 50% 100%, 0 75%);"
            "media" -> "/* mobile first */\n@media (min-width: 480px)  { /* large phones */ }\n@media (min-width: 768px)  { /* tablets */ }\n@media (min-width: 1024px) { /* laptops */ }\n@media (min-width: 1280px) { /* desktops */ }\n@media (prefers-color-scheme: dark) { }\n@media (prefers-reduced-motion: reduce) { }\n@media (hover: hover) { }"
            else -> "" } }
    fun gradient(i: String): String { val c = i.trim().split(Regex("[\\s,]+")).filter { it.isNotEmpty() }.ifEmpty { listOf("#0b3d91", "#4f9cff") }; val l = c.joinToString(", ")
        return "background: linear-gradient(135deg, $l);\nbackground: radial-gradient(circle at 30% 30%, $l);\nbackground: conic-gradient(from 0deg, $l, ${c.first()});\nbackground: repeating-linear-gradient(45deg, ${c.first()} 0 10px, ${c.last()} 10px 20px);" }
    fun headers(kind: String, i: String): String { val k = kv(i); return when (kind) {
        "csp" -> "Content-Security-Policy: default-src 'self'; script-src 'self' ${k.getOr("script", "")}; style-src 'self' 'unsafe-inline'; img-src 'self' data: ${k.getOr("img", "")}; connect-src 'self' ${k.getOr("connect", "")}; frame-ancestors 'none'; base-uri 'self'; form-action 'self'".replace(Regex("\\s+"), " ").replace(" ;", ";")
        "cors" -> "Access-Control-Allow-Origin: ${k.getOr("origin", "https://example.com")}\nAccess-Control-Allow-Methods: ${k.getOr("methods", "GET, POST, PUT, DELETE, OPTIONS")}\nAccess-Control-Allow-Headers: ${k.getOr("headers", "Content-Type, Authorization")}\nAccess-Control-Max-Age: 86400\nVary: Origin"
        "cookie" -> "Set-Cookie: ${k.getOr("name", "session")}=${k.getOr("value", "abc123")}; Path=/; Max-Age=${k.getOr("maxage", "3600")}; HttpOnly; Secure; SameSite=${k.getOr("samesite", "Lax")}"
        else -> "Strict-Transport-Security: max-age=63072000; includeSubDomains; preload\nX-Content-Type-Options: nosniff\nX-Frame-Options: DENY\nReferrer-Policy: strict-origin-when-cross-origin\nPermissions-Policy: camera=(), microphone=(), geolocation=()\nCross-Origin-Opener-Policy: same-origin" } }
}

val webTools: List<Tool> = listOf(
    tool("color", "Color Converter", Category.WEB, "HEX, RGB, HSL, HSV, CMYK, OKLCH, LAB, XYZ",
        m("Convert all") { Color.all(it) }, m("Palette & harmonies") { Color.palette(it) }, hint = "#0b3d91 | rgb(11,61,145) | hsl(220,86%,30%) | name", sample = "#0b3d91"),
    tool("contrast", "WCAG Contrast Checker", Category.WEB, "AA / AAA for text and UI",
        mp("Check") { fg, p -> Color.wcag(fg, p.getOrElse(0) { "#ffffff" }.ifBlank { "#ffffff" }) }, hint = "Foreground colour", params = listOf("Background colour (default white)"), sample = "#4f9cff", psample = listOf("#ffffff")),
    tool("gradient", "Gradient Generator", Category.WEB, "Linear, radial, conic CSS gradients", m("Generate") { Web.gradient(it) }, hint = "Colours, space or comma separated", sample = "#0b3d91 #4f9cff #ffffff"),
    tool("css-gen", "CSS Generators", Category.WEB, "Box-shadow, radius, flex, grid, animation, clip-path, media queries",
        m("Box shadow (x y blur spread colour)") { Web.css("shadow", it) }, m("Border radius") { Web.css("radius", it) }, m("Flexbox snippets") { Web.css("flex", it) },
        m("Grid (min column px)") { Web.css("grid", it) }, m("Keyframes (name seconds)") { Web.css("anim", it) }, m("Clip-path shapes") { Web.css("clip", it) }, m("Media queries") { Web.css("media", it) },
        hint = "Values", sample = "0 8 24 0 rgba(0,0,0,.3)"),
    tool("specificity", "CSS Selector Specificity", Category.WEB, "Calculate (id, class, element) weights", m("Calculate") { Web.specificity(it) }, hint = "Selector(s)", sample = "#nav ul li.active a:hover, .card > p::first-line"),
    tool("svg-min", "SVG Optimizer", Category.WEB, "Strip comments, metadata, editor attrs and extra precision", m("Minify") { Web.svgMin(it) }, hint = "SVG markup"),
    tool("meta-gen", "Meta / Open Graph Generator", Category.WEB, "key: value lines → head tags",
        m("Meta + OG + Twitter") { Web.meta(it) }, hint = "title:\ndescription:\nurl:\nimage:", sample = "title: My App\ndescription: Great tools\nurl: https://example.com"),
    tool("jsonld", "JSON-LD (schema.org) Generator", Category.WEB, "key: value lines",
        *listOf("Article", "Person", "Organization", "Product", "SoftwareApplication", "Event").map { t -> m(t) { Web.jsonLd(t, it) } }.toTypedArray(), hint = "name: ...\nurl: ...", sample = "name: Ada\nurl: https://example.com"),
    tool("robots", "robots.txt & sitemap.xml", Category.WEB, "Generate crawler files",
        m("robots.txt") { Web.robots(it) }, m("sitemap.xml (one URL per line)") { Web.sitemap(it) }, hint = "disallow: /admin\nsitemap: https://…", sample = "https://example.com/\nhttps://example.com/about"),
    tool("pwa", "Web Manifest & Service Worker", Category.WEB, "PWA boilerplate", m("manifest.json") { Web.manifest(it) }, m("sw.js") { Web.sw }, hint = "name: My App\nshort_name: App\ntheme: #0b3d91", sample = "name: My App\nshort_name: App"),
    tool("sec-headers", "CSP · CORS · Cookie · Security Headers", Category.WEB, "Build HTTP header values",
        m("Content-Security-Policy") { Web.headers("csp", it) }, m("CORS") { Web.headers("cors", it) }, m("Set-Cookie") { Web.headers("cookie", it) }, m("Security header set") { Web.headers("sec", it) },
        hint = "origin: https://app.com\nname: session", sample = "origin: https://app.example.com"),
    tool("cache-explain", "HTTP Cache-Control Explainer", Category.WEB, "Understand caching directives", m("Explain") { Web.cache(it) }, hint = "Cache-Control value", sample = "public, max-age=31536000, immutable"),
)
