package com.codingtools.app.tools

import org.junit.Assert.assertEquals
import org.junit.Test

class TokensTest {
    private fun show(text: String, kind: OutKind) = tokenize(text, kind).map { "${it.type}:${text.substring(it.start, it.end)}" }

    @Test fun json_tokens() {
        val j = "{\"a\": [1, -2.5e3, true, null], \"b\": \"x\\\"y\"}"
        assertEquals(listOf("KEY:\"a\"", "NUMBER:1", "NUMBER:-2.5e3", "LITERAL:true", "LITERAL:null", "KEY:\"b\"", "STRING:\"x\\\"y\""), show(j, OutKind.JSON))
    }

    @Test fun sql_tokens_respect_word_boundaries_comments_and_escaped_quotes() {
        val s = "select id, 'a''b' FROM t -- note\nWHERE x > 10"
        assertEquals(listOf("KEYWORD:select", "STRING:'a''b'", "KEYWORD:FROM", "COMMENT:-- note", "KEYWORD:WHERE", "NUMBER:10"), show(s, OutKind.SQL))
        assertEquals(listOf("KEYWORD:SELECT", "KEYWORD:FROM"), show("SELECT selection, ending FROM t", OutKind.SQL))
    }

    @Test fun kind_detection() {
        assertEquals(OutKind.JSON, detectKind("json", "JSON Formatter", "{\"a\":1}"))
        assertEquals(OutKind.SQL, detectKind("sql-format", "SQL Formatter", "SELECT 1"))
        assertEquals(OutKind.PLAIN, detectKind("sqlite", "SQLite Playground", "SELECT 1"))   // tabular playground output
        assertEquals(OutKind.PLAIN, detectKind("base64", "Base64", "aGVsbG8="))
    }
}
