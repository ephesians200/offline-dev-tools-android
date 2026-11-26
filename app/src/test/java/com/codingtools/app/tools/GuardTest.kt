package com.codingtools.app.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GuardTest {
    /** (.*a){45} on 40 a's has ~2^40 paths: far too slow to finish, so the 3 s deadline must fire. */
    @Test fun runaway_pattern_is_aborted_at_the_deadline() {
        val t0 = System.nanoTime()
        val r = runCatching { Regex("(.*a){45}").containsMatchIn(guarded("a".repeat(40))) }
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertTrue("expected RegexTimeout but got ${r.exceptionOrNull()}", r.exceptionOrNull() is RegexTimeout)
        assertTrue("fired too late: $ms ms", ms < 6_000)
    }

    @Test fun normal_patterns_behave_exactly_like_unguarded_ones() {
        assertEquals("Smith John, Doe Jane", Regex("(\\w+) (\\w+)").replace(guarded("John Smith, Jane Doe"), "\$2 \$1"))
        assertEquals(3, Regex("a").findAll(guarded("banana")).count())
        assertEquals(listOf("a", "b", "c"), Regex(",\\s*").split(guarded("a, b,c")))
        val m = java.util.regex.Pattern.compile("(\\d+)-(\\d+)").matcher(guarded("10-20 and 30-40"))
        val found = ArrayList<String>(); while (m.find()) found += "${m.start()}:${m.group(1)}-${m.group(2)}"
        assertEquals(listOf("0:10-20", "10:30-40"), found)
    }
}
