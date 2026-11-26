package com.codingtools.app.tools

const val REGEX_TIMEOUT_MS = 3000L

/** Thrown when a user supplied regular expression runs for too long (catastrophic backtracking). */
class RegexTimeout : RuntimeException(
    "Regex timed out after ${REGEX_TIMEOUT_MS / 1000}s – the pattern probably has catastrophic backtracking (nested quantifiers such as (a+)+)."
)

/**
 * A CharSequence that aborts regex matching once a deadline has passed. OpenJDK's java.util.regex
 * reads its input through charAt(), so checking the clock there is the only way to stop a runaway
 * match: coroutine cancellation and withTimeout cannot interrupt blocking code.
 *
 * Verified on the JVM (a 2^40-path pattern is aborted at the 3 s deadline). Android's Matcher is
 * backed by the native ICU engine and may copy the input up front, in which case this wrapper is
 * simply a no-op there. ToolScreen therefore ALSO enforces its own timeout and runs tools on
 * low-priority daemon threads, so a runaway pattern can never freeze the UI either way.
 */
class TimedText(private val s: CharSequence, private val deadlineNanos: Long) : CharSequence {
    private var reads = 0
    override val length: Int get() = s.length
    override fun get(index: Int): Char {
        if ((++reads and 0x3FF) == 0 && System.nanoTime() > deadlineNanos) throw RegexTimeout()
        return s[index]
    }
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = TimedText(s.subSequence(startIndex, endIndex), deadlineNanos)
    override fun toString(): String = s.toString()
}

/** Wrap text before handing it to a user supplied pattern. */
fun guarded(s: CharSequence, timeoutMs: Long = REGEX_TIMEOUT_MS): CharSequence = TimedText(s, System.nanoTime() + timeoutMs * 1_000_000L)
