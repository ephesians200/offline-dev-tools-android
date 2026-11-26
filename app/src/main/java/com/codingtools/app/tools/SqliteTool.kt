package com.codingtools.app.tools

import android.database.sqlite.SQLiteDatabase

/** A scratch in-memory SQLite database that survives between runs while the app is open. */
object SqlPlay {
    private var db: SQLiteDatabase = SQLiteDatabase.create(null)
    fun reset() { db.close(); db = SQLiteDatabase.create(null) }
    private fun statements(s: String): List<String> {
        val out = ArrayList<String>(); val cur = StringBuilder(); var q = '\u0000'
        for (c in s) { if (q != '\u0000') { if (c == q) q = '\u0000' } else if (c == '\'' || c == '"') q = c; if (c == ';' && q == '\u0000') { out.add(cur.toString().trim()); cur.clear() } else cur.append(c) }
        if (cur.isNotBlank()) out.add(cur.toString().trim()); return out.filter { it.isNotEmpty() }
    }
    fun run(sql: String, prefix: String = ""): String = statements(sql).joinToString("\n\n") { st0 ->
        val st = prefix + st0; val kw = st.trimStart().takeWhile { it.isLetter() }.lowercase()
        if (kw in setOf("select", "pragma", "with", "explain")) {
            db.rawQuery(st, null).use { c -> val rows = ArrayList<List<String>>(); rows.add(c.columnNames.toList())
                while (c.moveToNext() && rows.size <= 500) rows.add((0 until c.columnCount).map { i -> if (c.isNull(i)) "NULL" else runCatching { c.getString(i) }.getOrDefault("<blob>") })
                table(rows) + "\n(${rows.size - 1} row(s))" }
        } else { db.execSQL(st); "OK  ✔  $kw" }
    }
}

val sqliteTools: List<Tool> = listOf(
    tool("sqlite", "SQLite Playground", Category.DATA, "Run real SQL on-device. The in-memory database persists between runs until reset",
        m("Run SQL") { SqlPlay.run(it) }, m("Explain query plan") { SqlPlay.run(it, "EXPLAIN QUERY PLAN ") }, m("Show schema") { SqlPlay.run("SELECT name, sql FROM sqlite_master WHERE type IN ('table','index','view')") },
        md("Reset database") { SqlPlay.reset(); "Database reset." },
        hint = "SQL statements separated by ;", sample = "CREATE TABLE IF NOT EXISTS t(id INTEGER PRIMARY KEY, name TEXT);\nINSERT INTO t(name) VALUES ('Ada'),('Linus');\nSELECT * FROM t;", kw = "database sql query"),
)
