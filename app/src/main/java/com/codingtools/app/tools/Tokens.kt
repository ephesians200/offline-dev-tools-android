package com.codingtools.app.tools

/** Pure (UI free) tokenizer used to colour JSON / SQL output. See ui/OutputText.kt for the colours. */
enum class OutKind { JSON, SQL, PLAIN }
enum class Tok { KEY, STRING, NUMBER, LITERAL, KEYWORD, COMMENT }
class TokSpan(val start: Int, val end: Int, val type: Tok)

private val sqlStarts = setOf("select", "insert", "update", "delete", "create", "alter", "drop", "with", "pragma", "explain")

fun detectKind(toolId: String, title: String, text: String): OutKind {
    val t = text.trim()
    if (t.isEmpty()) return OutKind.PLAIN
    if ((t.first() == '{' && t.last() == '}') || (t.first() == '[' && t.last() == ']')) return OutKind.JSON
    val sqlish = (toolId.contains("sql", true) || title.contains("sql", true)) && !toolId.startsWith("sqlite")
    if (sqlish && t.takeWhile { it.isLetter() }.lowercase() in sqlStarts) return OutKind.SQL
    return OutKind.PLAIN
}

private val jsonRe = Regex("""("(?:\\.|[^"\\\n])*")(\s*:)?|(-?\b\d+(?:\.\d+)?(?:[eE][+-]?\d+)?\b)|\b(true|false|null)\b""")

private const val SQL_KW = "SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|ALTER|DROP|TABLE|INDEX|VIEW|JOIN|LEFT|RIGHT|INNER|OUTER|FULL|CROSS|ON|USING|GROUP|BY|ORDER|HAVING|LIMIT|OFFSET|UNION|ALL|DISTINCT|AS|AND|OR|NOT|NULL|IS|IN|LIKE|BETWEEN|EXISTS|CASE|WHEN|THEN|ELSE|END|PRIMARY|KEY|FOREIGN|REFERENCES|DEFAULT|UNIQUE|CHECK|CONSTRAINT|WITH|ASC|DESC|INTEGER|INT|TEXT|REAL|BLOB|VARCHAR|BOOLEAN|DATE|TIMESTAMP|PRAGMA|EXPLAIN|IF|BEGIN|COMMIT|ROLLBACK"
private val sqlRe = Regex("""(--[^\n]*|/\*.*?\*/)|('(?:''|[^'])*')|(\b\d+(?:\.\d+)?\b)|\b($SQL_KW)\b""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

fun tokenize(text: String, kind: OutKind): List<TokSpan> {
    val out = ArrayList<TokSpan>()
    when (kind) {
        OutKind.PLAIN -> {}
        OutKind.JSON -> for (m in jsonRe.findAll(text)) {
            val g = m.groups
            when {
                g[1] != null -> out += TokSpan(g[1]!!.range.first, g[1]!!.range.last + 1, if (g[2] != null) Tok.KEY else Tok.STRING)
                g[3] != null -> out += TokSpan(g[3]!!.range.first, g[3]!!.range.last + 1, Tok.NUMBER)
                g[4] != null -> out += TokSpan(g[4]!!.range.first, g[4]!!.range.last + 1, Tok.LITERAL)
            }
        }
        OutKind.SQL -> for (m in sqlRe.findAll(text)) {
            val g = m.groups
            val (idx, type) = when {
                g[1] != null -> 1 to Tok.COMMENT
                g[2] != null -> 2 to Tok.STRING
                g[3] != null -> 3 to Tok.NUMBER
                else -> 4 to Tok.KEYWORD
            }
            out += TokSpan(g[idx]!!.range.first, g[idx]!!.range.last + 1, type)
        }
    }
    return out
}
