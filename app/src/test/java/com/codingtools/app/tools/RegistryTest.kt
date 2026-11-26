package com.codingtools.app.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistryTest {
    @Test fun ids_are_unique_and_every_tool_has_something_to_open() {
        val all = Registry.all
        assertEquals(all.size, all.map { it.id }.toSet().size)
        all.forEach { t -> assertTrue("${t.id} has no modes, reference text or custom screen", t.modes.isNotEmpty() || t.reference != null || t.custom != null) }
    }

    @Test fun categories_have_short_names_for_filter_chips() {
        Category.values().forEach {
            assertTrue("${it.name} short name", it.shortName.isNotBlank() && !it.shortName.endsWith(","))
        }
    }

    /** Only the SQLite reset is destructive: it must never auto-run when its chip is tapped. */
    @Test fun sqlite_reset_is_the_only_destructive_mode() {
        val reset = Registry.byId["sqlite"]!!.modes.single { it.name == "Reset database" }
        assertTrue(reset.destructive)
        assertEquals(1, Registry.all.flatMap { it.modes }.count { it.destructive })
    }

    /** Regression: the sample used "\$2 \$1" (escaped), which printed the literal text instead of swapping the groups. */
    @Test fun find_replace_sample_really_swaps_the_groups() {
        val t = Registry.byId["find-replace"]!!
        val out = t.modes.first { it.name == "Regex replace" }.run(t.sample, t.paramSamples)
        assertEquals("Smith John, Doe Jane", out)
    }
}
