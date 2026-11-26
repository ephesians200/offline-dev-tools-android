package com.codingtools.app.tools

import org.w3c.dom.Element
import org.w3c.dom.Node

object Mob {
    @Suppress("UNCHECKED_CAST")
    private fun plistVal(e: Element): Any? = when (e.nodeName) {
        "dict" -> LinkedHashMap<String, Any?>().also { m -> var k = ""; for (i in 0 until e.childNodes.length) { val n = e.childNodes.item(i); if (n.nodeType != Node.ELEMENT_NODE) continue; if (n.nodeName == "key") k = n.textContent else m[k] = plistVal(n as Element) } }
        "array" -> (0 until e.childNodes.length).map { e.childNodes.item(it) }.filter { it.nodeType == Node.ELEMENT_NODE }.map { plistVal(it as Element) }
        "string", "date", "data" -> e.textContent; "integer" -> e.textContent.toLongOrNull() ?: 0L; "real" -> e.textContent.toDouble(); "true" -> true; "false" -> false; else -> null
    }
    fun plistToJson(s: String): String { val d = Xml.parseDom(s).documentElement; val root = (0 until d.childNodes.length).map { d.childNodes.item(it) }.first { it.nodeType == Node.ELEMENT_NODE } as Element; return Json.write(plistVal(root)) }
    private fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    private fun pv(v: Any?, ind: String, sb: StringBuilder) { when (v) { is Map<*, *> -> { sb.append("$ind<dict>\n"); v.forEach { (k, x) -> sb.append("$ind\t<key>${esc(k.toString())}</key>\n"); pv(x, "$ind\t", sb) }; sb.append("$ind</dict>\n") }; is List<*> -> { sb.append("$ind<array>\n"); v.forEach { pv(it, "$ind\t", sb) }; sb.append("$ind</array>\n") }; is Boolean -> sb.append("$ind<$v/>\n"); is Long, is Int -> sb.append("$ind<integer>$v</integer>\n"); is Double -> sb.append("$ind<real>$v</real>\n"); else -> sb.append("$ind<string>${esc(v?.toString() ?: "")}</string>\n") } }
    fun jsonToPlist(s: String): String { val sb = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n<plist version=\"1.0\">\n"); pv(Json.parse(s), "", sb); return sb.append("</plist>").toString() }
    private fun androidStrings(s: String): Map<String, String> { val d = Xml.parseDom(s).documentElement; val m = LinkedHashMap<String, String>(); val ns = d.getElementsByTagName("string"); for (i in 0 until ns.length) { val e = ns.item(i) as Element; m[e.getAttribute("name")] = e.textContent.replace("\\'", "'").replace("\\n", "\n") }; return m }
    private fun iosStrings(s: String) = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"\\s*=\\s*\"((?:[^\"\\\\]|\\\\.)*)\"\\s*;").findAll(s).associate { it.groupValues[1] to it.groupValues[2].replace("\\n", "\n").replace("\\\"", "\"") }
    private fun toAndroid(m: Map<String, String>) = "<resources>\n" + m.entries.joinToString("\n") { "    <string name=\"${it.key}\">${esc(it.value).replace("'", "\\'").replace("\n", "\\n")}</string>" } + "\n</resources>"
    private fun toIos(m: Map<String, String>) = m.entries.joinToString("\n") { "\"${it.key}\" = \"${it.value.replace("\"", "\\\"").replace("\n", "\\n")}\";" }
    fun loc(s: String, from: String, to: String): String { val m: Map<String, String> = when (from) { "android" -> androidStrings(s); "ios" -> iosStrings(s); else -> (Json.parse(s) as Map<*, *>).entries.associate { it.key.toString() to it.value.toString() } }
        return when (to) { "android" -> toAndroid(m); "ios" -> toIos(m); else -> Json.write(m) } }
    fun locDiff(a: String, b: String): String { val x = (Json.parse(a) as Map<*, *>).keys.map { it.toString() }.toSet(); val y = (Json.parse(b) as Map<*, *>).keys.map { it.toString() }.toSet()
        return "Missing in B (${(x - y).size}):\n" + (x - y).sorted().joinToString("\n") { "  - $it" } + "\n\nExtra in B (${(y - x).size}):\n" + (y - x).sorted().joinToString("\n") { "  + $it" } }
    fun deeplink(i: String): String { val k = kv(i); val scheme = k.getOr("scheme", "myapp"); val host = k.getOr("host", "open"); val path = k.getOr("path", "/item/42"); val pkg = k.getOr("package", "com.example.app")
        return "URL            : $scheme://$host$path\nWeb link       : https://${k.getOr("domain", "example.com")}$path\n\nadb (Android)  :\nadb shell am start -a android.intent.action.VIEW -d \"$scheme://$host$path\" $pkg\n\nxcrun (iOS sim):\nxcrun simctl openurl booted \"$scheme://$host$path\"\n\nAndroidManifest.xml:\n<intent-filter>\n    <action android:name=\"android.intent.action.VIEW\" />\n    <category android:name=\"android.intent.category.DEFAULT\" />\n    <category android:name=\"android.intent.category.BROWSABLE\" />\n    <data android:scheme=\"$scheme\" android:host=\"$host\" />\n</intent-filter>\n\nInfo.plist:\n<key>CFBundleURLTypes</key>\n<array><dict><key>CFBundleURLSchemes</key><array><string>$scheme</string></array></dict></array>\n\nKotlin: startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(\"$scheme://$host$path\")))\nSwift : UIApplication.shared.open(URL(string: \"$scheme://$host$path\")!)" }
    fun format(fmt: String, args: String): String { val a: List<Any?> = Regex("\"[^\"]*\"|[^,]+").findAll(args).map { it.value.trim() }.filter { it.isNotEmpty() }.map { v -> if (v.startsWith("\"")) v.trim('"') else v.toLongOrNull() ?: v.toDoubleOrNull() ?: (if (v == "true" || v == "false") v.toBoolean() else v) }.toList(); return String.format(fmt, *a.toTypedArray()) }
    fun gradle(lib: String): String = when (lib.lowercase().trim()) {
        "compose" -> "val composeBom = platform(\"androidx.compose:compose-bom:2024.12.01\")\nimplementation(composeBom)\nimplementation(\"androidx.compose.material3:material3\")\nimplementation(\"androidx.compose.ui:ui-tooling-preview\")\nimplementation(\"androidx.activity:activity-compose:1.9.3\")"
        "retrofit" -> "implementation(\"com.squareup.retrofit2:retrofit:2.11.0\")\nimplementation(\"com.squareup.retrofit2:converter-gson:2.11.0\")\nimplementation(\"com.squareup.okhttp3:logging-interceptor:4.12.0\")"
        "room" -> "val room = \"2.6.1\"\nimplementation(\"androidx.room:room-runtime:\$room\")\nimplementation(\"androidx.room:room-ktx:\$room\")\nksp(\"androidx.room:room-compiler:\$room\")"
        "hilt" -> "implementation(\"com.google.dagger:hilt-android:2.51.1\")\nksp(\"com.google.dagger:hilt-android-compiler:2.51.1\")\nimplementation(\"androidx.hilt:hilt-navigation-compose:1.2.0\")"
        "coroutines" -> "implementation(\"org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0\")"
        "coil" -> "implementation(\"io.coil-kt:coil-compose:2.7.0\")"
        "navigation" -> "implementation(\"androidx.navigation:navigation-compose:2.8.5\")"
        "datastore" -> "implementation(\"androidx.datastore:datastore-preferences:1.1.1\")"
        "serialization" -> "implementation(\"org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3\")"
        "okhttp" -> "implementation(\"com.squareup.okhttp3:okhttp:4.12.0\")"
        "junit" -> "testImplementation(\"junit:junit:4.13.2\")\nandroidTestImplementation(\"androidx.test.ext:junit:1.2.1\")\nandroidTestImplementation(\"androidx.test.espresso:espresso-core:3.6.1\")"
        else -> "Known keys: compose, retrofit, room, hilt, coroutines, coil, navigation, datastore, serialization, okhttp, junit\n(Check Maven / Google Maven for the newest versions.)" }
}

object Ops {
    class V(val maj: Int, val min: Int, val pat: Int, val pre: String)
    fun semver(s: String): V { val m = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)(?:-([\\w.]+))?").find(s.trim()) ?: throw IllegalArgumentException("Not a semantic version: $s"); return V(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt(), m.groupValues[4]) }
    fun cmp(a: V, b: V): Int = compareValuesBy(a, b, { it.maj }, { it.min }, { it.pat }).let { if (it != 0) it else if (a.pre.isEmpty() && b.pre.isNotEmpty()) 1 else if (a.pre.isNotEmpty() && b.pre.isEmpty()) -1 else a.pre.compareTo(b.pre) }
    fun satisfies(v: V, range: String): Boolean = range.split("||").any { part -> part.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.all { c ->
        when { c.startsWith("^") -> semver(c.drop(1)).let { b -> cmp(v, b) >= 0 && (if (b.maj > 0) v.maj == b.maj else if (b.min > 0) v.maj == 0 && v.min == b.min else v.maj == 0 && v.min == 0 && v.pat == b.pat) }
            c.startsWith("~") -> semver(c.drop(1)).let { b -> cmp(v, b) >= 0 && v.maj == b.maj && v.min == b.min }
            c.startsWith(">=") -> cmp(v, semver(c.drop(2))) >= 0; c.startsWith("<=") -> cmp(v, semver(c.drop(2))) <= 0; c.startsWith(">") -> cmp(v, semver(c.drop(1))) > 0; c.startsWith("<") -> cmp(v, semver(c.drop(1))) < 0
            c == "*" || c == "x" -> true; else -> cmp(v, semver(c.removePrefix("="))) == 0 } } }
    fun commitLint(s: String): String { val re = Regex("^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\\([\\w./-]+\\))?(!)?: .{1,}"); val first = lines(s).first(); val probs = ArrayList<String>()
        if (!re.containsMatchIn(first)) probs.add("Header must look like  type(scope)!: description  (types: feat fix docs style refactor perf test build ci chore revert)"); if (first.length > 72) probs.add("Header is ${first.length} chars (keep ≤ 72)"); if (first.endsWith(".")) probs.add("Don't end the header with a period"); if (first.substringAfter(": ", "").firstOrNull()?.isUpperCase() == true) probs.add("Start the description in lower case")
        if (lines(s).size > 1 && lines(s)[1].isNotBlank()) probs.add("Leave a blank line after the header")
        return if (probs.isEmpty()) "✔ Valid Conventional Commit" else probs.joinToString("\n") { "✘ $it" } }
    fun changelog(s: String): String { val groups = linkedMapOf("feat" to "### ✨ Features", "fix" to "### 🐛 Bug Fixes", "perf" to "### ⚡ Performance", "refactor" to "### ♻️ Refactoring", "docs" to "### 📚 Docs", "chore" to "### 🧹 Chores"); val bucket = HashMap<String, MutableList<String>>(); val other = ArrayList<String>()
        lines(s).map { it.trim().replace(Regex("^[0-9a-f]{7,40}\\s+"), "").removePrefix("- ").removePrefix("* ") }.filter { it.isNotEmpty() }.forEach { l -> val m = Regex("^(\\w+)(?:\\(([^)]+)\\))?(!)?:\\s*(.*)").find(l); if (m != null && m.groupValues[1] in groups) bucket.getOrPut(m.groupValues[1]) { ArrayList() }.add((if (m.groupValues[2].isNotEmpty()) "**${m.groupValues[2]}:** " else "") + m.groupValues[4]) else other.add(l) }
        return "## [Unreleased] - ${java.time.LocalDate.now()}\n\n" + groups.entries.filter { bucket.containsKey(it.key) }.joinToString("\n\n") { "${it.value}\n" + bucket[it.key]!!.joinToString("\n") { x -> "- $x" } } + (if (other.isNotEmpty()) "\n\n### Other\n" + other.joinToString("\n") { "- $it" } else "") }
    fun compose(i: String): String { val k = kv(i); val svc = k.getOr("service", "app"); val img = k.getOr("image", "myorg/app:latest"); val port = k.getOr("port", "8080"); val db = k.getOr("db", "postgres")
        return "services:\n  $svc:\n    image: $img\n    ports:\n      - \"$port:$port\"\n    environment:\n      DATABASE_URL: ${if (db == "mysql") "mysql://user:pass@db:3306/app" else "postgres://user:pass@db:5432/app"}\n    depends_on:\n      - db\n    restart: unless-stopped\n\n  db:\n    image: ${if (db == "mysql") "mysql:8" else "postgres:16"}\n    environment:\n      ${if (db == "mysql") "MYSQL_ROOT_PASSWORD: pass\n      MYSQL_DATABASE: app" else "POSTGRES_USER: user\n      POSTGRES_PASSWORD: pass\n      POSTGRES_DB: app"}\n    volumes:\n      - db-data:/var/lib/${if (db == "mysql") "mysql" else "postgresql/data"}\n\nvolumes:\n  db-data:" }
    fun k8s(i: String, kind: String): String { val k = kv(i); val n = k.getOr("name", "my-app"); val img = k.getOr("image", "myorg/app:1.0"); val port = k.getOr("port", "8080"); val rep = k.getOr("replicas", "2")
        return when (kind) {
            "deploy" -> "apiVersion: apps/v1\nkind: Deployment\nmetadata:\n  name: $n\nspec:\n  replicas: $rep\n  selector:\n    matchLabels:\n      app: $n\n  template:\n    metadata:\n      labels:\n        app: $n\n    spec:\n      containers:\n        - name: $n\n          image: $img\n          ports:\n            - containerPort: $port\n          resources:\n            requests: { cpu: 100m, memory: 128Mi }\n            limits: { cpu: 500m, memory: 512Mi }\n          readinessProbe:\n            httpGet: { path: /health, port: $port }"
            "service" -> "apiVersion: v1\nkind: Service\nmetadata:\n  name: $n\nspec:\n  selector:\n    app: $n\n  ports:\n    - port: 80\n      targetPort: $port\n  type: ClusterIP"
            else -> "apiVersion: networking.k8s.io/v1\nkind: Ingress\nmetadata:\n  name: $n\nspec:\n  rules:\n    - host: ${k.getOr("host", "app.example.com")}\n      http:\n        paths:\n          - path: /\n            pathType: Prefix\n            backend:\n              service:\n                name: $n\n                port:\n                  number: 80" } }
    fun gha(lang: String): String = when (lang.lowercase().trim()) {
        "android", "kotlin" -> "name: Android CI\non: [push, pull_request]\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-java@v4\n        with: { distribution: temurin, java-version: 17 }\n      - uses: gradle/actions/setup-gradle@v4\n      - run: ./gradlew test assembleDebug"
        "node", "js", "ts" -> "name: Node CI\non: [push, pull_request]\njobs:\n  test:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-node@v4\n        with: { node-version: 20, cache: npm }\n      - run: npm ci\n      - run: npm test --if-present\n      - run: npm run build --if-present"
        "python" -> "name: Python CI\non: [push, pull_request]\njobs:\n  test:\n    runs-on: ubuntu-latest\n    strategy:\n      matrix:\n        python: [\"3.10\", \"3.12\"]\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-python@v5\n        with: { python-version: \"\${{ matrix.python }}\" }\n      - run: pip install -r requirements.txt pytest\n      - run: pytest"
        "go" -> "name: Go CI\non: [push, pull_request]\njobs:\n  test:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-go@v5\n        with: { go-version: stable }\n      - run: go vet ./...\n      - run: go test ./..."
        "rust" -> "name: Rust CI\non: [push, pull_request]\njobs:\n  test:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: dtolnay/rust-toolchain@stable\n      - run: cargo clippy -- -D warnings\n      - run: cargo test"
        "java", "maven" -> "name: Java CI\non: [push, pull_request]\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - uses: actions/setup-java@v4\n        with: { distribution: temurin, java-version: 21, cache: maven }\n      - run: mvn -B verify"
        else -> "name: CI\non: [push]\njobs:\n  build:\n    runs-on: ubuntu-latest\n    steps:\n      - uses: actions/checkout@v4\n      - run: echo \"build here\"" }
    fun env(s: String, mode: String): String { val rows = lines(s).map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.map { it.removePrefix("export ").let { l -> l.substringBefore('=').trim() to l.substringAfter('=', "").trim().trim('"', '\'') } }
        return when (mode) { "json" -> Json.write(rows.toMap()); "export" -> rows.joinToString("\n") { "export ${it.first}=\"${it.second}\"" }; "docker" -> rows.joinToString(" ") { "-e ${it.first}=\"${it.second}\"" }; "k8s" -> "env:\n" + rows.joinToString("\n") { "  - name: ${it.first}\n    value: \"${it.second}\"" }
            "example" -> rows.joinToString("\n") { "${it.first}=" }; else -> { val dup = rows.groupBy { it.first }.filter { it.value.size > 1 }.keys; val empty = rows.filter { it.second.isEmpty() }.map { it.first }; "${rows.size} variables\nDuplicates: ${dup.ifEmpty { setOf("none") }.joinToString()}\nEmpty: ${empty.ifEmpty { listOf("none") }.joinToString()}\nSecret-like names: ${rows.map { it.first }.filter { Regex("KEY|SECRET|TOKEN|PASS", RegexOption.IGNORE_CASE).containsMatchIn(it) }.ifEmpty { listOf("none") }.joinToString()}" } } }
    fun pert(s: String): String { val rows = lines(s).filter { it.isNotBlank() }.map { l -> l.trim().split(Regex("[\\s,]+")).map { it.toDouble() } }; var tot = 0.0; var vr = 0.0
        val out = rows.mapIndexed { i, (o, m, p) -> val e = (o + 4 * m + p) / 6; val sd = (p - o) / 6; tot += e; vr += sd * sd; "Task ${i + 1}: expected ${fmt(e)}  (±${"%.2f".format(sd)})" }
        return out.joinToString("\n") + "\n\nTotal expected: ${fmt(tot)}\nStd dev: ${"%.2f".format(Math.sqrt(vr))}\n68% range: ${"%.1f".format(tot - Math.sqrt(vr))} – ${"%.1f".format(tot + Math.sqrt(vr))}\n95% range: ${"%.1f".format(tot - 2 * Math.sqrt(vr))} – ${"%.1f".format(tot + 2 * Math.sqrt(vr))}" }
}

private fun gen(name: String, text: String) = m(name) { text.replace("{{input}}", it.ifBlank { "…" }) }

val mobileTools: List<Tool> = listOf(
    tool("plist", "plist ⇄ JSON", Category.MOBILE, "iOS property lists (XML)", m("plist → JSON") { Mob.plistToJson(it) }, m("JSON → plist") { Mob.jsonToPlist(it) }, hint = "plist XML or JSON"),
    tool("loc-convert", "Localization Converter", Category.MOBILE, "Android strings.xml ⇄ iOS .strings ⇄ JSON",
        m("Android → iOS") { Mob.loc(it, "android", "ios") }, m("iOS → Android") { Mob.loc(it, "ios", "android") }, m("Android → JSON") { Mob.loc(it, "android", "json") }, m("iOS → JSON") { Mob.loc(it, "ios", "json") }, m("JSON → Android") { Mob.loc(it, "json", "android") }, m("JSON → iOS") { Mob.loc(it, "json", "ios") },
        hint = "Source file", sample = "<resources>\n  <string name=\"app_name\">My App</string>\n  <string name=\"hello\">Hello, %s!</string>\n</resources>"),
    tool("loc-diff", "Localization Key Diff", Category.MOBILE, "Find missing translations between two JSON files", mp("Diff") { a, p -> Mob.locDiff(a, p.getOrElse(0) { "{}" }) }, hint = "Base language JSON", params = listOf("Translated JSON"), psample = listOf("{\"a\":\"1\"}")),
    tool("deeplink", "Deep Link Builder", Category.MOBILE, "Android intent-filter, iOS URL scheme, adb and xcrun test commands", m("Build") { Mob.deeplink(it) }, hint = "scheme:\nhost:\npath:\npackage:\ndomain:", sample = "scheme: myapp\nhost: open\npath: /item/42\npackage: com.example.app"),
    tool("fmt-tester", "String.format / printf Tester", Category.MOBILE, "Java/Kotlin format specifiers (≈ C printf)", mp("Format") { s, p -> Mob.format(s, p.getOrElse(0) { "" }) }, hint = "Format string, e.g. %5.2f | %-10s | %08X", params = listOf("Arguments, comma separated (strings in \"quotes\")"), sample = "%-8s|%6.2f|%05d|%x", psample = listOf("\"ada\", 3.14159, 42, 255")),
    tool("gradle-deps", "Gradle Dependency Snippets", Category.MOBILE, "Common Android libraries", m("Snippet") { Mob.gradle(it) }, hint = "compose, retrofit, room, hilt, coroutines, coil, navigation, datastore, serialization, okhttp, junit", sample = "retrofit"),
)

val devopsTools: List<Tool> = listOf(
    tool("semver", "Semver Calculator & Range Checker", Category.DEVOPS, "Bump versions and test ^ ~ >= ranges",
        m("Bump major") { s -> Ops.semver(s).let { "${it.maj + 1}.0.0" } }, m("Bump minor") { s -> Ops.semver(s).let { "${it.maj}.${it.min + 1}.0" } }, m("Bump patch") { s -> Ops.semver(s).let { "${it.maj}.${it.min}.${it.pat + 1}" } },
        mp("Satisfies range?") { s, p -> if (Ops.satisfies(Ops.semver(s), p.getOrElse(0) { "*" })) "✔ ${s.trim()} satisfies ${p[0]}" else "✘ ${s.trim()} does NOT satisfy ${p.getOrElse(0) { "*" }}" },
        mp("Compare") { s, p -> Ops.cmp(Ops.semver(s), Ops.semver(p.getOrElse(0) { "0.0.0" })).let { if (it > 0) "$s > ${p[0]}" else if (it < 0) "$s < ${p[0]}" else "equal" } }, hint = "Version", params = listOf("Range (^1.2.0, ~2.1, >=1 <3) or other version"), sample = "1.4.2", psample = listOf("^1.2.0")),
    tool("commit-lint", "Conventional Commit Linter", Category.DEVOPS, "Validate commit messages", m("Lint") { Ops.commitLint(it) }, hint = "Commit message", sample = "feat(auth): add biometric login"),
    tool("changelog", "Changelog Generator", Category.DEVOPS, "Group Conventional Commit lines into release notes", m("Generate") { Ops.changelog(it) }, hint = "git log --oneline output", sample = "a1b2c3d feat(ui): dark mode\n9f8e7d6 fix: crash on rotate\nc0ffee1 docs: update readme"),
    tool("compose-gen", "docker-compose Generator", Category.DEVOPS, "App + database stack", m("Generate") { Ops.compose(it) }, hint = "service:\nimage:\nport:\ndb: postgres|mysql", sample = "service: api\nimage: myorg/api:1.0\nport: 8080\ndb: postgres"),
    tool("k8s-gen", "Kubernetes YAML Helper", Category.DEVOPS, "Deployment, Service, Ingress", m("Deployment") { Ops.k8s(it, "deploy") }, m("Service") { Ops.k8s(it, "service") }, m("Ingress") { Ops.k8s(it, "ingress") }, hint = "name:\nimage:\nport:\nreplicas:\nhost:", sample = "name: web\nimage: nginx:1.27\nport: 80"),
    tool("gha", "GitHub Actions Templates", Category.DEVOPS, "CI workflows", m("Workflow") { Ops.gha(it) }, hint = "android, node, python, go, rust, java", sample = "android"),
    tool("env-manager", ".env Manager", Category.DEVOPS, "Audit, convert and template environment files",
        m("Audit") { Ops.env(it, "audit") }, m("→ JSON") { Ops.env(it, "json") }, m("→ export") { Ops.env(it, "export") }, m("→ docker -e") { Ops.env(it, "docker") }, m("→ k8s env") { Ops.env(it, "k8s") }, m("→ .env.example") { Ops.env(it, "example") }, hint = ".env contents", sample = "API_KEY=abc123\nDEBUG=true\nDEBUG=false\nDB_PASSWORD="),
    tool("estimate", "Estimation Calculator (PERT)", Category.DEVOPS, "One task per line: optimistic likely pessimistic", m("Estimate") { Ops.pert(it) }, hint = "2 4 8\n1 2 3", sample = "2 4 8\n1 2 3\n5 8 14"),
    tool("templates", "Bug · PR · Standup Templates", Category.DEVOPS, "Fill-in-the-blank engineering documents",
        gen("Bug report", "## Bug: {{input}}\n\n**Environment**\n- App version:\n- Device / OS:\n\n**Steps to reproduce**\n1. \n2. \n3. \n\n**Expected**\n\n**Actual**\n\n**Logs / screenshots**\n"),
        gen("Pull request", "## What\n{{input}}\n\n## Why\n\n## How\n\n## Testing\n- [ ] Unit tests\n- [ ] Manual test on device\n\n## Screenshots\n\n## Checklist\n- [ ] Docs updated\n- [ ] No new warnings"),
        gen("Daily standup", "**Yesterday**\n- {{input}}\n\n**Today**\n- \n\n**Blockers**\n- None"),
        gen("Decision record (ADR)", "# ADR: {{input}}\n\n- Status: proposed\n- Date: ${java.time.LocalDate.now()}\n\n## Context\n\n## Decision\n\n## Consequences\n"),
        gen("Meeting notes", "# {{input}}  –  ${java.time.LocalDate.now()}\n\n**Attendees:**\n\n**Agenda**\n1. \n\n**Decisions**\n- \n\n**Action items**\n- [ ] "),
        hint = "Title / subject", sample = "Crash when rotating screen"),
)
