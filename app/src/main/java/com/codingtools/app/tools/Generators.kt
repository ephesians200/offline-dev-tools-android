package com.codingtools.app.tools

import java.security.MessageDigest
import java.util.UUID

object Gen {
    val r = java.security.SecureRandom()
    private fun bytesToUuid(b: ByteArray): String { val h = b.hex(); return "${h.substring(0, 8)}-${h.substring(8, 12)}-${h.substring(12, 16)}-${h.substring(16, 20)}-${h.substring(20, 32)}" }
    fun v1(): String { val t = (System.currentTimeMillis() + 12219292800000L) * 10000; val b = ByteArray(16).also { r.nextBytes(it) }
        val tl = (t and 0xffffffffL); val tm = (t shr 32) and 0xffff; val th = ((t shr 48) and 0x0fff) or 0x1000
        return "%08x-%04x-%04x-%02x%02x-%s".format(tl, tm, th, (b[0].toInt() and 0x3f) or 0x80, b[1].toInt() and 0xff, b.copyOfRange(2, 8).hex()) }
    fun v4() = UUID.randomUUID().toString()
    fun v5(ns: String, name: String): String {
        val nsu = when (ns.lowercase()) { "dns", "" -> UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8"); "url" -> UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8"); else -> UUID.fromString(ns) }
        val nb = java.nio.ByteBuffer.allocate(16).putLong(nsu.mostSignificantBits).putLong(nsu.leastSignificantBits).array()
        val h = MessageDigest.getInstance("SHA-1").digest(nb + name.toByteArray()).copyOf(16)
        h[6] = ((h[6].toInt() and 0x0f) or 0x50).toByte(); h[8] = ((h[8].toInt() and 0x3f) or 0x80).toByte(); return bytesToUuid(h)
    }
    fun v7(): String { val b = ByteArray(16).also { r.nextBytes(it) }; val t = System.currentTimeMillis()
        for (i in 0..5) b[i] = (t shr (8 * (5 - i))).toByte(); b[6] = ((b[6].toInt() and 0x0f) or 0x70).toByte(); b[8] = ((b[8].toInt() and 0x3f) or 0x80).toByte(); return bytesToUuid(b) }
    private const val CROCK = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    fun ulid(): String { var t = System.currentTimeMillis(); val sb = StringBuilder(); repeat(10) { sb.append(CROCK[(t % 32).toInt()]); t /= 32 }; sb.reverse(); repeat(16) { sb.append(CROCK[r.nextInt(32)]) }; return sb.toString() }
    fun nano(n: Int = 21): String { val a = "_-0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"; return (1..n).map { a[r.nextInt(64)] }.joinToString("") }
    fun cuid(): String = "c" + java.lang.Long.toString(System.currentTimeMillis(), 36) + (1..4).map { "0123456789abcdefghijklmnopqrstuvwxyz"[r.nextInt(36)] }.joinToString("") + (1..12).map { "0123456789abcdefghijklmnopqrstuvwxyz"[r.nextInt(36)] }.joinToString("")
    fun many(n: String, f: () -> String) = (1..(n.trim().toIntOrNull() ?: 5).coerceIn(1, 500)).joinToString("\n") { f() }

    private val first = listOf("Ada", "Linus", "Grace", "Dennis", "Margaret", "Alan", "Kwame", "Ama", "Yuki", "Chen", "Ines", "Omar", "Priya", "Sven", "Lena", "Tariq", "Noor", "Mateo", "Zoe", "Ivan")
    private val last = listOf("Lovelace", "Torvalds", "Hopper", "Ritchie", "Hamilton", "Turing", "Mensah", "Boateng", "Tanaka", "Wei", "Silva", "Haddad", "Patel", "Larsen", "Meyer", "Khan", "Ali", "Garcia", "Brown", "Petrov")
    private val cities = listOf("Accra", "Lagos", "Nairobi", "Cairo", "Lisbon", "Berlin", "Tokyo", "Austin", "Toronto", "Mumbai", "Sydney", "Oslo")
    private val lorem = "lorem ipsum dolor sit amet consectetur adipiscing elit sed do eiusmod tempor incididunt ut labore et dolore magna aliqua enim ad minim veniam quis nostrud exercitation ullamco laboris nisi aliquip ex ea commodo consequat duis aute irure in reprehenderit voluptate velit esse cillum fugiat nulla pariatur".split(" ")
    fun lorem(paras: Int): String = (1..paras).joinToString("\n\n") { p -> (1..4).joinToString(" ") { (1..(8 + r.nextInt(8))).joinToString(" ") { lorem[r.nextInt(lorem.size)] }.replaceFirstChar { c -> c.uppercase() } + "." }.let { if (p == 1) "Lorem ipsum dolor sit amet. $it" else it } }
    fun person(i: Int): Map<String, Any?> { val f = first[r.nextInt(first.size)]; val l = last[r.nextInt(last.size)]
        return linkedMapOf("id" to i, "name" to "$f $l", "email" to "${f.lowercase()}.${l.lowercase()}@example.com", "age" to 18 + r.nextInt(50), "city" to cities[r.nextInt(cities.size)], "active" to r.nextBoolean(), "uuid" to v4()) }
    fun fake(n: Int, fmt: String): String {
        val rows = (1..n).map { person(it) }
        return when (fmt) {
            "json" -> Json.write(rows)
            "csv" -> Json.write(rows).let { Conv.jsonToCsv(it) }
            else -> rows.joinToString("\n") { p -> "INSERT INTO users (${p.keys.joinToString(", ")}) VALUES (${p.values.joinToString(", ") { v -> if (v is String) "'" + v.replace("'", "''") + "'" else v.toString() }});" }
        }
    }
    fun mock(schema: String, n: Int): String {
        val s = Json.parse(schema) as Map<*, *>
        fun gen(t: String, i: Int): Any? = when (t.lowercase()) {
            "name" -> first[r.nextInt(first.size)] + " " + last[r.nextInt(last.size)]; "first" -> first[r.nextInt(first.size)]; "last" -> last[r.nextInt(last.size)]
            "email" -> "user${r.nextInt(9999)}@example.com"; "int", "number" -> r.nextInt(1000); "float" -> Math.round(r.nextDouble() * 10000) / 100.0
            "uuid" -> v4(); "bool", "boolean" -> r.nextBoolean(); "city" -> cities[r.nextInt(cities.size)]; "id" -> i
            "date" -> java.time.LocalDate.now().minusDays(r.nextInt(1000).toLong()).toString(); "word" -> lorem[r.nextInt(lorem.size)]
            "sentence" -> (1..6).joinToString(" ") { lorem[r.nextInt(lorem.size)] }; "url" -> "https://example.com/" + nano(6); "ip" -> "${r.nextInt(223) + 1}.${r.nextInt(256)}.${r.nextInt(256)}.${r.nextInt(254) + 1}"
            else -> t
        }
        return Json.write((1..n).map { i -> LinkedHashMap<String, Any?>().also { o -> s.forEach { (k, v) -> o[k.toString()] = gen(v.toString(), i) } } })
    }
    fun license(kind: String, holder: String): String {
        val y = java.time.Year.now().value
        return when (kind) {
            "MIT" -> "MIT License\n\nCopyright (c) $y $holder\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\nThe above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\nTHE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE."
            "Apache-2.0" -> "Copyright $y $holder\n\nLicensed under the Apache License, Version 2.0 (the \"License\");\nyou may not use this file except in compliance with the License.\nYou may obtain a copy of the License at\n\n    http://www.apache.org/licenses/LICENSE-2.0\n\nUnless required by applicable law or agreed to in writing, software\ndistributed under the License is distributed on an \"AS IS\" BASIS,\nWITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\nSee the License for the specific language governing permissions and\nlimitations under the License."
            "GPL-3.0 header" -> "Copyright (C) $y $holder\n\nThis program is free software: you can redistribute it and/or modify\nit under the terms of the GNU General Public License as published by\nthe Free Software Foundation, either version 3 of the License, or\n(at your option) any later version.\n\nThis program is distributed in the hope that it will be useful,\nbut WITHOUT ANY WARRANTY; without even the implied warranty of\nMERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\nGNU General Public License for more details.\n\nYou should have received a copy of the GNU General Public License\nalong with this program.  If not, see <https://www.gnu.org/licenses/>."
            "ISC" -> "ISC License\n\nCopyright (c) $y $holder\n\nPermission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby granted, provided that the above copyright notice and this permission notice appear in all copies.\n\nTHE SOFTWARE IS PROVIDED \"AS IS\" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE."
            else -> "BSD 2-Clause License\n\nCopyright (c) $y, $holder\nAll rights reserved.\n\nRedistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:\n\n1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.\n\n2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.\n\nTHIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND ANY EXPRESS OR IMPLIED WARRANTIES ARE DISCLAIMED."
        }
    }
    fun gitignore(lang: String): String {
        val common = "# OS / IDE\n.DS_Store\nThumbs.db\n.idea/\n.vscode/\n*.swp\n*.log\n.env\n"
        return common + "\n" + when (lang.lowercase().trim()) {
            "android", "kotlin" -> "# Android / Kotlin\n*.iml\n.gradle/\n/local.properties\n/build/\n/app/build/\n/captures/\n.externalNativeBuild/\n.cxx/\n*.apk\n*.aab\n*.keystore\n*.jks"
            "java", "maven" -> "# Java\n*.class\n*.jar\n*.war\ntarget/\nbuild/\nout/\nhs_err_pid*"
            "python" -> "# Python\n__pycache__/\n*.py[cod]\n.venv/\nvenv/\n.pytest_cache/\n.mypy_cache/\ndist/\n*.egg-info/\n.ipynb_checkpoints/"
            "node", "js", "javascript", "typescript", "ts" -> "# Node\nnode_modules/\ndist/\nbuild/\ncoverage/\nnpm-debug.log*\n.next/\n.cache/"
            "go" -> "# Go\n*.exe\n*.test\n*.out\nvendor/\nbin/"
            "rust" -> "# Rust\n/target\nCargo.lock\n**/*.rs.bk"
            "c", "cpp", "c++", "cmake" -> "# C / C++\n*.o\n*.obj\n*.a\n*.so\n*.dll\n*.exe\nbuild/\nCMakeCache.txt\nCMakeFiles/\ncmake-build-*/"
            "swift", "ios" -> "# Swift / Xcode\nxcuserdata/\n*.xcworkspace/xcuserdata\nDerivedData/\n.build/\nPods/\n*.ipa"
            "dart", "flutter" -> "# Flutter / Dart\n.dart_tool/\n.packages\nbuild/\n.flutter-plugins\n.flutter-plugins-dependencies\npubspec.lock"
            "php" -> "# PHP\nvendor/\ncomposer.phar\n.phpunit.result.cache"
            "ruby" -> "# Ruby\n*.gem\n.bundle/\nvendor/bundle\nlog/\ntmp/"
            "csharp", "c#", "dotnet", ".net" -> "# .NET\nbin/\nobj/\n*.user\n*.suo\n.vs/\npackages/"
            else -> "# (add language-specific patterns)\nbuild/\ndist/"
        }
    }
    fun dockerfile(lang: String): String = when (lang.lowercase().trim()) {
        "node", "js", "ts" -> "FROM node:20-alpine AS build\nWORKDIR /app\nCOPY package*.json ./\nRUN npm ci\nCOPY . .\nRUN npm run build\n\nFROM node:20-alpine\nWORKDIR /app\nCOPY --from=build /app/dist ./dist\nCOPY package*.json ./\nRUN npm ci --omit=dev\nEXPOSE 3000\nCMD [\"node\", \"dist/index.js\"]"
        "python", "py" -> "FROM python:3.12-slim\nWORKDIR /app\nCOPY requirements.txt .\nRUN pip install --no-cache-dir -r requirements.txt\nCOPY . .\nEXPOSE 8000\nCMD [\"python\", \"main.py\"]"
        "java", "kotlin", "spring" -> "FROM eclipse-temurin:21-jdk AS build\nWORKDIR /src\nCOPY . .\nRUN ./gradlew bootJar --no-daemon\n\nFROM eclipse-temurin:21-jre\nCOPY --from=build /src/build/libs/*.jar /app.jar\nEXPOSE 8080\nENTRYPOINT [\"java\", \"-jar\", \"/app.jar\"]"
        "go" -> "FROM golang:1.22 AS build\nWORKDIR /src\nCOPY go.* ./\nRUN go mod download\nCOPY . .\nRUN CGO_ENABLED=0 go build -o /app ./...\n\nFROM gcr.io/distroless/static\nCOPY --from=build /app /app\nEXPOSE 8080\nENTRYPOINT [\"/app\"]"
        "rust" -> "FROM rust:1.78 AS build\nWORKDIR /src\nCOPY . .\nRUN cargo build --release\n\nFROM debian:bookworm-slim\nCOPY --from=build /src/target/release/app /usr/local/bin/app\nCMD [\"app\"]"
        "php" -> "FROM php:8.3-apache\nCOPY . /var/www/html/\nEXPOSE 80"
        "dotnet", "c#" -> "FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build\nWORKDIR /src\nCOPY . .\nRUN dotnet publish -c Release -o /out\n\nFROM mcr.microsoft.com/dotnet/aspnet:8.0\nWORKDIR /app\nCOPY --from=build /out .\nEXPOSE 8080\nENTRYPOINT [\"dotnet\", \"App.dll\"]"
        else -> "FROM ubuntu:24.04\nWORKDIR /app\nCOPY . .\nCMD [\"./run.sh\"]"
    }
    fun boiler(kind: String, name: String): String { val n = name.ifBlank { "my-project" }; return when (kind) {
        "README" -> "# $n\n\nShort description of what $n does.\n\n## Features\n- Feature one\n- Feature two\n\n## Getting started\n```bash\ngit clone https://github.com/you/$n.git\ncd $n\n```\n\n## Usage\n\n## Contributing\nPull requests are welcome.\n\n## License\nMIT"
        "package.json" -> Json.write(linkedMapOf("name" to kebab(n), "version" to "1.0.0", "description" to "", "main" to "index.js", "type" to "module", "scripts" to linkedMapOf("start" to "node index.js", "test" to "echo \"no tests\""), "license" to "MIT"))
        "tsconfig.json" -> "{\n  \"compilerOptions\": {\n    \"target\": \"ES2022\",\n    \"module\": \"ESNext\",\n    \"moduleResolution\": \"Bundler\",\n    \"strict\": true,\n    \"esModuleInterop\": true,\n    \"skipLibCheck\": true,\n    \"outDir\": \"dist\"\n  },\n  \"include\": [\"src\"]\n}"
        "pom.xml" -> "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">\n  <modelVersion>4.0.0</modelVersion>\n  <groupId>com.example</groupId>\n  <artifactId>${kebab(n)}</artifactId>\n  <version>1.0.0</version>\n  <properties>\n    <maven.compiler.release>21</maven.compiler.release>\n  </properties>\n</project>"
        "Makefile" -> ".PHONY: build run test clean\n\nbuild:\n\t@echo building $n\n\nrun: build\n\t./bin/$n\n\ntest:\n\t@echo testing\n\nclean:\n\trm -rf bin build"
        "CMakeLists.txt" -> "cmake_minimum_required(VERSION 3.20)\nproject(${pascal(n)} LANGUAGES CXX)\nset(CMAKE_CXX_STANDARD 20)\nset(CMAKE_CXX_STANDARD_REQUIRED ON)\nadd_executable(${kebab(n)} src/main.cpp)"
        "build.gradle.kts" -> "plugins {\n    kotlin(\"jvm\") version \"2.0.21\"\n    application\n}\n\nrepositories { mavenCentral() }\n\ndependencies {\n    testImplementation(kotlin(\"test\"))\n}\n\napplication { mainClass.set(\"MainKt\") }\nkotlin { jvmToolchain(21) }"
        "pyproject.toml" -> "[project]\nname = \"${kebab(n)}\"\nversion = \"0.1.0\"\nrequires-python = \">=3.10\"\ndependencies = []\n\n[build-system]\nrequires = [\"setuptools>=68\"]\nbuild-backend = \"setuptools.build_meta\""
        "Cargo.toml" -> "[package]\nname = \"${kebab(n)}\"\nversion = \"0.1.0\"\nedition = \"2021\"\n\n[dependencies]\n"
        "go.mod" -> "module github.com/you/${kebab(n)}\n\ngo 1.22\n"
        "composer.json" -> Json.write(linkedMapOf("name" to "you/${kebab(n)}", "type" to "project", "require" to linkedMapOf("php" to ">=8.2"), "autoload" to linkedMapOf("psr-4" to linkedMapOf("App\\" to "src/"))))
        "Gemfile" -> "source \"https://rubygems.org\"\n\nruby \">= 3.2\"\n\ngem \"rake\"\ngroup :test do\n  gem \"rspec\"\nend"
        "pubspec.yaml" -> "name: ${snake(n)}\ndescription: A new Dart project.\nversion: 1.0.0\n\nenvironment:\n  sdk: ^3.4.0\n\ndependencies:\n\ndev_dependencies:\n  lints: ^4.0.0\n  test: ^1.25.0"
        "Package.swift" -> "// swift-tools-version:5.9\nimport PackageDescription\n\nlet package = Package(\n    name: \"${pascal(n)}\",\n    targets: [\n        .executableTarget(name: \"${pascal(n)}\")\n    ]\n)"
        ".csproj" -> "<Project Sdk=\"Microsoft.NET.Sdk\">\n  <PropertyGroup>\n    <OutputType>Exe</OutputType>\n    <TargetFramework>net8.0</TargetFramework>\n    <Nullable>enable</Nullable>\n    <ImplicitUsings>enable</ImplicitUsings>\n  </PropertyGroup>\n</Project>"
        ".eslintrc / prettierrc" -> ".eslintrc.json\n{\n  \"root\": true,\n  \"extends\": [\"eslint:recommended\"],\n  \"env\": { \"es2022\": true, \"node\": true }\n}\n\n.prettierrc\n{\n  \"semi\": true,\n  \"singleQuote\": true,\n  \"printWidth\": 100,\n  \"trailingComma\": \"all\"\n}"
        else -> "" } }
}

val generatorTools: List<Tool> = listOf(
    tool("uuid", "UUID / ULID / NanoID / CUID", Category.GENERATE, "v1, v4, v5, v7 UUIDs, ULID, NanoID. Input = how many",
        m("UUID v4") { Gen.many(it) { Gen.v4() } }, m("UUID v7") { Gen.many(it) { Gen.v7() } }, m("UUID v1") { Gen.many(it) { Gen.v1() } },
        mp("UUID v5") { s, p -> Gen.v5(p.getOrElse(0) { "" }, s) }, m("ULID") { Gen.many(it) { Gen.ulid() } }, m("NanoID") { Gen.many(it) { Gen.nano() } }, m("CUID-style") { Gen.many(it) { Gen.cuid() } },
        hint = "Count (or name for v5)", params = listOf("v5 namespace: dns | url | <uuid>"), sample = "5", kw = "guid identifier"),
    tool("lorem", "Lorem Ipsum", Category.GENERATE, "Placeholder text. Input = paragraphs",
        m("Paragraphs") { Gen.lorem((it.trim().toIntOrNull() ?: 3).coerceIn(1, 30)) }, hint = "Paragraphs", sample = "3"),
    tool("fake-data", "Fake Data (JSON / CSV / SQL)", Category.GENERATE, "Realistic test rows. Input = row count",
        m("JSON") { Gen.fake((it.trim().toIntOrNull() ?: 5).coerceIn(1, 500), "json") }, m("CSV") { Gen.fake((it.trim().toIntOrNull() ?: 5).coerceIn(1, 500), "csv") },
        m("SQL INSERT") { Gen.fake((it.trim().toIntOrNull() ?: 5).coerceIn(1, 500), "sql") }, hint = "Row count", sample = "5"),
    tool("random", "Random Numbers & Strings", Category.GENERATE, "Dice, ranges, coin flip, shuffles",
        mp("Integer in range") { s, p -> val lo = p.getOrElse(0) { "" }.toLongOrNull() ?: 1; val hi = p.getOrElse(1) { "" }.toLongOrNull() ?: 100; (1..(s.trim().toIntOrNull() ?: 5)).joinToString("\n") { (lo + (Gen.r.nextDouble() * (hi - lo + 1)).toLong()).toString() } },
        m("Coin flip") { if (Gen.r.nextBoolean()) "Heads" else "Tails" }, m("Roll d6") { "🎲 ${Gen.r.nextInt(6) + 1}" }, m("Roll d20") { "🎲 ${Gen.r.nextInt(20) + 1}" },
        m("Shuffle lines") { lines(it).shuffled(Gen.r).joinToString("\n") }, m("Pick one line") { lines(it).filter { l -> l.isNotBlank() }.random(Gen.r.asKotlinRandom()) },
        hint = "Count / lines", params = listOf("Min (1)", "Max (100)"), sample = "5"),
    tool("mock-json", "Mock JSON from Template", Category.GENERATE, "Template maps field → type: name, email, int, float, uuid, bool, date, city, word, sentence, url, ip, id",
        mp("Generate") { s, p -> Gen.mock(s, (p.getOrElse(0) { "" }.toIntOrNull() ?: 5).coerceIn(1, 200)) }, hint = "Template JSON", params = listOf("Rows (5)"),
        sample = "{\"id\":\"id\",\"user\":\"name\",\"mail\":\"email\",\"score\":\"float\",\"joined\":\"date\"}"),
    tool("license", "License Generator", Category.GENERATE, "MIT, Apache-2.0, GPL-3.0 header, ISC, BSD-2",
        *listOf("MIT", "Apache-2.0", "GPL-3.0 header", "ISC", "BSD-2").map { k -> m(k) { Gen.license(k, it.ifBlank { "Your Name" }) } }.toTypedArray(), hint = "Copyright holder", sample = "Your Name"),
    tool("gitignore", ".gitignore Generator", Category.GENERATE, "Per-language ignore files",
        m("Generate") { Gen.gitignore(it) }, hint = "Language (android, java, python, node, go, rust, cpp, swift, flutter, php, ruby, dotnet)", sample = "android"),
    tool("dockerfile", "Dockerfile Generator", Category.GENERATE, "Multi-stage Dockerfiles",
        m("Generate") { Gen.dockerfile(it) }, hint = "Stack (node, python, java, go, rust, php, dotnet)", sample = "node"),
    tool("boilerplate", "Project File Scaffolds", Category.GENERATE, "README, package.json, tsconfig, pom.xml, Gradle, Makefile, CMake, pyproject, Cargo, go.mod, Composer, Gemfile, pubspec, Package.swift, .csproj, ESLint/Prettier",
        *listOf("README", "package.json", "tsconfig.json", "pom.xml", "build.gradle.kts", "Makefile", "CMakeLists.txt", "pyproject.toml", "Cargo.toml", "go.mod", "composer.json", "Gemfile", "pubspec.yaml", "Package.swift", ".csproj", ".eslintrc / prettierrc").map { k -> m(k) { Gen.boiler(k, it) } }.toTypedArray(),
        hint = "Project name", sample = "my-awesome-app"),
)

private fun java.security.SecureRandom.asKotlinRandom(): kotlin.random.Random = kotlin.random.Random(this.nextLong())
