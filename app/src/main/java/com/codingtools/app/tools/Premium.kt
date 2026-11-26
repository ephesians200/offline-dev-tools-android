package com.codingtools.app.tools

/** Product ID of the one-time "Pro" in-app product. Create it in Play Console → Monetize → One-time products. */
const val PRO_PRODUCT_ID = "pro_unlock"

/** Shown only until Google Play returns the real, localised price. The real price is set in Play Console. */
const val PRO_FALLBACK_PRICE = "\$4.99"

/**
 * The advanced tools that need Pro. Everything else is free forever.
 * To move a tool between tiers just add or remove its id here (one place, nothing else to change).
 */
val ProToolIds: Set<String> = setOf(
    // Code editing & analysis
    "editor", "secret-scan", "dupes", "complexity", "stacktrace", "logview", "deobfuscate", "deps", "snippets",
    // Crypto & security
    "aes", "keygen", "kdf", "x509", "vault", "totp",
    // Data & databases
    "sqlite", "jq", "xpath", "xsd", "sql-orm", "sql-er", "csv-sql", "sql-index",
    // Binary & low-level
    "protobuf", "msgpack-cbor", "asn1", "compress", "file-inspector", "struct-layout", "struct-pack", "bitfield", "charset", "encoding-algos",
    // Networking & APIs
    "rest", "openapi", "webhook", "net-probe",
    // Git & DevOps
    "compose-gen", "k8s-gen", "gha", "env-manager", "changelog",
    // Converters
    "json-code", "json-schema", "curl", "image-base64",
    // Math
    "matrix", "qformat", "distribution",
    // Generators
    "fake-data", "mock-json", "boilerplate",
    // Productivity
    "pipeline", "cmd-vars",
    // Web
    "svg-min", "jsonld",
    // Text
    "regex-gen",
    // Reference
    "asm", "syscalls", "interview", "challenges", "payloads"
)

val Tool.isPremium: Boolean get() = id in ProToolIds

val proToolCount: Int get() = Registry.all.count { it.isPremium }
val freeToolCount: Int get() = Registry.all.count { !it.isPremium }
