package com.codingtools.app.tools

enum class Category(val title: String, val shortName: String) {
    FORMAT("Formatters & Validators", "Format"),
    CONVERT("Converters", "Convert"),
    CODEC("Encoders & Decoders", "Codecs"),
    CRYPTO("Hashing, Crypto & Security", "Security"),
    GENERATE("Generators", "Generate"),
    TEXT("Text, Diff & Regex", "Text"),
    DATETIME("Date, Time & Cron", "Date & time"),
    WEB("Web & Frontend", "Web"),
    NETWORK("Networking & APIs", "Network"),
    DATA("Data & Databases", "Data"),
    MATH("Math & CS Calculators", "Math"),
    CODE("Code Editing & Analysis", "Code"),
    LOWLEVEL("Binary & Low-Level", "Low-level"),
    MOBILE("Mobile & Language Helpers", "Mobile"),
    DEVOPS("Git & DevOps", "DevOps"),
    REFERENCE("Cheat Sheets & Reference", "Cheat sheets"),
    LEARN("Learn & Practice", "Learn"),
    PRODUCTIVITY("Productivity & Fun", "Productivity")
}

/**
 * One action of a tool. [run] receives the main input and the extra parameter fields.
 * [destructive] modes (e.g. "Reset database") never auto-run when their chip is tapped and
 * always ask for confirmation first.
 */
class Mode(val name: String, val destructive: Boolean = false, val run: (String, List<String>) -> String)

/**
 * A tool is pure data. A generic screen renders it (input + parameters + mode chips + output).
 * Tools with [custom] set open a dedicated screen instead (see ui/CustomScreens.kt).
 * Tools with [reference] set are searchable offline reference pages.
 */
class Tool(
    val id: String,
    val title: String,
    val category: Category,
    val description: String,
    val modes: List<Mode> = emptyList(),
    val inputHint: String = "Input",
    val params: List<String> = emptyList(),
    val sample: String = "",
    val autoRun: Boolean = false,
    val reference: String? = null,
    val custom: String? = null,
    val keywords: String = "",
    val paramSamples: List<String> = emptyList()
)

fun m(name: String, f: (String) -> String) = Mode(name) { i, _ -> f(i) }
fun mp(name: String, f: (String, List<String>) -> String) = Mode(name, run = f)
/** Destructive single-input mode: asks for confirmation and never auto-runs. */
fun md(name: String, f: (String) -> String) = Mode(name, destructive = true) { i, _ -> f(i) }

fun tool(
    id: String, title: String, cat: Category, desc: String, vararg modes: Mode,
    hint: String = "Input", params: List<String> = emptyList(), sample: String = "",
    auto: Boolean = false, kw: String = "", psample: List<String> = emptyList()
) = Tool(id, title, cat, desc, modes.toList(), hint, params, sample, auto, null, null, kw, psample)

fun ref(id: String, title: String, cat: Category, desc: String, text: String, kw: String = "") =
    Tool(id, title, cat, desc, emptyList(), "Filter…", emptyList(), "", false, text.trimIndent().replace('§', '$'), null, kw)

fun custom(id: String, title: String, cat: Category, desc: String, kw: String = "") =
    Tool(id, title, cat, desc, custom = id, keywords = kw)

object Registry {
    val all: List<Tool> by lazy {
        (formatTools + convertTools + codecTools + cryptoTools + generatorTools + textTools +
            dateTools + webTools + networkTools + dataTools + mathTools + codeTools +
            lowLevelTools + mobileTools + devopsTools + referenceTools + learnTools + productivityTools)
            .also { list -> require(list.map { it.id }.toSet().size == list.size) { "duplicate tool id" } }
    }
    val byId: Map<String, Tool> by lazy { all.associateBy { it.id } }
    fun byCategory(c: Category) = all.filter { it.category == c }
    fun search(q: String): List<Tool> {
        val s = q.trim().lowercase()
        if (s.isEmpty()) return all
        val terms = s.split(' ').filter { it.isNotBlank() }
        return all.map { t ->
            val hay = (t.title + " " + t.description + " " + t.keywords + " " + t.category.title).lowercase()
            val score = terms.fold(0) { acc, term ->
                acc + when {
                    t.title.lowercase().startsWith(term) -> 4
                    t.title.lowercase().contains(term) -> 3
                    t.keywords.lowercase().contains(term) -> 2
                    hay.contains(term) -> 1
                    else -> -100
                }
            }
            t to score
        }.filter { it.second > 0 }.sortedByDescending { it.second }.map { it.first }
    }
}
