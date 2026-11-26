package com.codingtools.app.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumTest {
    @Test fun every_pro_id_is_a_real_tool() {
        val missing = ProToolIds.filter { it !in Registry.byId }
        assertTrue("Pro ids with no tool: $missing", missing.isEmpty())
    }

    @Test fun pro_and_free_counts_add_up_and_free_tier_stays_generous() {
        assertEquals(Registry.all.size, proToolCount + freeToolCount)
        assertEquals(62, proToolCount)
        assertTrue("free tier should stay the clear majority", freeToolCount > proToolCount * 2)
    }

    @Test fun everyday_tools_stay_free() {
        listOf("json", "regex", "base64", "jwt", "hash", "uuid", "timestamp", "color", "cron", "diff", "url", "case", "password")
            .forEach { assertFalse("$it must stay free", Registry.byId[it]!!.isPremium) }
    }

    @Test fun headline_pro_tools_are_locked() {
        listOf("editor", "sqlite", "rest", "vault", "kdf", "protobuf")
            .forEach { assertTrue("$it should be Pro", Registry.byId[it]!!.isPremium) }
    }
}
