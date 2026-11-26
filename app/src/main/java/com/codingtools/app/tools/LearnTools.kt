package com.codingtools.app.tools

import java.util.PriorityQueue

object Learn {
    class BN(val v: Int) { var l: BN? = null; var r: BN? = null }
    private fun ins(n: BN?, v: Int): BN { if (n == null) return BN(v); if (v < n.v) n.l = ins(n.l, v) else if (v > n.v) n.r = ins(n.r, v); return n }
    private fun show(n: BN?, d: Int, sb: StringBuilder) { if (n == null) return; show(n.r, d + 1, sb); sb.append("    ".repeat(d)).append(n.v).append('\n'); show(n.l, d + 1, sb) }
    private fun height(n: BN?): Int = if (n == null) 0 else 1 + maxOf(height(n.l), height(n.r))
    fun bst(s: String): String { var root: BN? = null; Regex("-?\\d+").findAll(s).forEach { root = ins(root, it.value.toInt()) }
        val inO = ArrayList<Int>(); val pre = ArrayList<Int>(); val post = ArrayList<Int>()
        fun go(n: BN?) { if (n == null) return; pre.add(n.v); go(n.l); inO.add(n.v); go(n.r); post.add(n.v) }; go(root)
        val sb = StringBuilder("Tree (rotated: right branch on top)\n"); show(root, 0, sb)
        return "$sb\nHeight: ${height(root)}   Nodes: ${inO.size}\nIn-order  : $inO\nPre-order : $pre\nPost-order: $post" }
    fun heap(s: String): String { val a = ArrayList<Int>(); val steps = StringBuilder()
        Regex("-?\\d+").findAll(s).forEach { m -> a.add(m.value.toInt()); var i = a.size - 1; while (i > 0 && a[(i - 1) / 2] > a[i]) { val p = (i - 1) / 2; val t = a[p]; a[p] = a[i]; a[i] = t; i = p }; steps.append("insert ${m.value}  →  $a\n") }
        val pq = PriorityQueue(a); val out = ArrayList<Int>(); while (pq.isNotEmpty()) out.add(pq.poll())
        return "Min-heap array (index i → children 2i+1, 2i+2)\n$steps\nPop order (sorted): $out" }
    private class Tn { val k = java.util.TreeMap<Char, Tn>(); var end = false }
    fun trie(words: String, prefix: String): String { val root = Tn(); val ws = words.split(Regex("[\\s,]+")).filter { it.isNotEmpty() }
        ws.forEach { w -> var n = root; w.forEach { c -> n = n.k.getOrPut(c) { Tn() } }; n.end = true }
        val sb = StringBuilder(); fun p(n: Tn, ind: String) { n.k.forEach { (c, ch) -> sb.append(ind).append(c).append(if (ch.end) " ✔" else "").append('\n'); p(ch, "$ind  ") } }; p(root, "")
        val hit = if (prefix.isBlank()) "" else "\nWords starting with '$prefix': ${ws.filter { it.startsWith(prefix) }.sorted()}"
        return sb.toString() + "\n${ws.size} words, ${ws.sumOf { it.length }} chars stored$hit" }
    fun hashmap(keys: String, size: Int): String { val b = Array(size) { ArrayList<String>() }; keys.split(Regex("[\\s,]+")).filter { it.isNotEmpty() }.forEach { b[Math.floorMod(it.hashCode(), size)].add(it) }
        val load = b.sumOf { it.size }.toDouble() / size; return b.mapIndexed { i, l -> "[$i] " + l.joinToString(" → ").ifEmpty { "·" } }.joinToString("\n") + "\n\nLoad factor ${"%.2f".format(load)}  Longest chain ${b.maxOf { it.size }}  Collisions ${b.sumOf { maxOf(0, it.size - 1) }}" }
    fun lru(seq: String, cap: Int): String { val c = LinkedHashMap<String, Boolean>(); var hits = 0; var miss = 0; val sb = StringBuilder()
        seq.split(Regex("[\\s,]+")).filter { it.isNotEmpty() }.forEach { k -> if (c.containsKey(k)) { hits++; c.remove(k); c[k] = true; sb.append("access $k  HIT   → ${c.keys}\n") } else { miss++; var ev = ""; if (c.size >= cap) { ev = c.keys.first(); c.remove(ev) }; c[k] = true; sb.append("access $k  MISS  → ${c.keys}${if (ev.isNotEmpty()) "   (evicted $ev)" else ""}\n") } }
        return sb.toString() + "\nHits $hits · Misses $miss · Hit rate ${if (hits + miss == 0) 0 else hits * 100 / (hits + miss)}%\n(left = least recently used)" }
    fun stackQueue(cmds: String, stack: Boolean): String { val d = ArrayDeque<String>(); val sb = StringBuilder(); val t = cmds.split(Regex("\\s+")).filter { it.isNotEmpty() }; var i = 0
        while (i < t.size) { when (t[i].lowercase()) { "push", "enqueue" -> { d.addLast(t[++i]); sb.append("${t[i - 1]} ${t[i]}  → $d\n") }; "pop", "dequeue" -> { val v = if (d.isEmpty()) null else if (stack) d.removeLast() else d.removeFirst(); sb.append("${t[i]} → ${v ?: "EMPTY!"}   state $d\n") }; "peek" -> sb.append("peek → ${(if (stack) d.lastOrNull() else d.firstOrNull()) ?: "EMPTY"}\n") }; i++ }
        return (if (stack) "Stack (top = right) – LIFO\n" else "Queue (front = left) – FIFO\n") + sb }
    fun path(grid: String, astar: Boolean): String { val g = lines(grid).filter { it.isNotEmpty() }.map { it.toCharArray() }; var s: Pair<Int, Int>? = null; var e: Pair<Int, Int>? = null
        g.forEachIndexed { r, row -> row.forEachIndexed { c, ch -> if (ch == 'S') s = r to c; if (ch == 'E') e = r to c } }; need(s != null && e != null, "Mark start with S and end with E (# = wall)")
        val h = { p: Pair<Int, Int> -> if (astar) Math.abs(p.first - e!!.first) + Math.abs(p.second - e!!.second) else 0 }
        val dist = HashMap<Pair<Int, Int>, Int>(); val prev = HashMap<Pair<Int, Int>, Pair<Int, Int>>(); val pq = PriorityQueue<Pair<Int, Pair<Int, Int>>>(compareBy { it.first }); dist[s!!] = 0; pq.add(h(s!!) to s!!); var visited = 0
        while (pq.isNotEmpty()) { val (_, cur) = pq.poll(); visited++; if (cur == e) break
            for ((dr, dc) in listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)) { val n = cur.first + dr to cur.second + dc; if (n.first !in g.indices || n.second !in g[n.first].indices || g[n.first][n.second] == '#') continue; val nd = dist.getValue(cur) + 1; if (nd < (dist[n] ?: Int.MAX_VALUE)) { dist[n] = nd; prev[n] = cur; pq.add(nd + h(n) to n) } } }
        if (!dist.containsKey(e!!)) return "No path exists."; var c = prev[e!!]; while (c != null && c != s) { g[c.first][c.second] = '*'; c = prev[c] }
        return g.joinToString("\n") { String(it) } + "\n\nShortest path: ${dist[e!!]} steps · nodes expanded: $visited (${if (astar) "A* Manhattan" else "Dijkstra/BFS"})" }
    fun dfa(trans: String, start: String, accept: String, test: String): String { val t = HashMap<Pair<String, String>, String>(); lines(trans).map { it.trim().split(Regex("\\s+")) }.filter { it.size == 3 }.forEach { t[it[0] to it[1]] = it[2] }
        val acc = accept.split(',').map { it.trim() }.toSet(); var s = start.trim(); val sb = StringBuilder("$s"); for (ch in test) { val n = t[s to ch.toString()]; if (n == null) { sb.append(" --$ch--> ✘ (no transition)"); return sb.toString() + "\nREJECTED" }; sb.append(" --$ch--> $n"); s = n }
        return sb.toString() + "\n" + if (s in acc) "ACCEPTED (ended in $s)" else "REJECTED (ended in $s, not accepting)" }
    fun fibTree(n: Int): String { need(n in 0..8, "Use n between 0 and 8"); var calls = 0; val sb = StringBuilder(); fun f(k: Int, ind: String): Int { calls++; sb.append(ind).append("fib($k)\n"); return if (k < 2) k else f(k - 1, "$ind│ ") + f(k - 2, "$ind│ ") }; val r = f(n, ""); return "$sb\nfib($n) = $r computed with $calls calls (exponential – use memoisation!)" }
    fun master(s: String): String { val (a, b, d) = s.trim().split(Regex("[\\s,]+")).map { it.toDouble() }; val c = Math.log(a) / Math.log(b)
        val res = when { Math.abs(d - c) < 1e-9 -> "Θ(n^${fmt(d)} · log n)"; d < c -> "Θ(n^${fmt(c)})   (≈ n^log_b a)"; else -> "Θ(n^${fmt(d)})" }
        return "T(n) = ${fmt(a)}·T(n/${fmt(b)}) + n^${fmt(d)}\nlog_b a = ${"%.3f".format(c)}\nCase: ${if (Math.abs(d - c) < 1e-9) "2 (equal)" else if (d < c) "1 (leaves dominate)" else "3 (root dominates)"}\nResult: $res\n\nExamples: merge sort (2 2 1) → Θ(n log n) · binary search (1 2 0) → Θ(log n) · Karatsuba (3 2 1) → Θ(n^1.585)" }
    private val tips = listOf("Name things for what they mean, not how they are implemented.", "Make it work, make it right, make it fast – in that order.", "Read the error message fully. Then read it again.", "A bug you can reproduce is a bug half-fixed.", "Commit small, commit often, write messages for humans.", "Premature optimisation is the root of much evil – measure first.", "Delete code you don't need; it can't have bugs.", "Prefer composition over inheritance.", "Write the test that fails first.", "Rubber-duck it: explaining the problem often reveals the answer.", "Use immutable data where you can.", "Fail fast: validate inputs at the boundary.", "Never trust user input. Never log secrets.", "A good abstraction hides complexity; a bad one hides bugs.", "Learn your debugger and your profiler.", "Take breaks – the answer often arrives when you stop staring.", "Automate anything you do more than three times.", "Code is read far more than it is written.", "Time zones, encodings and floating point: assume trouble.", "Keep functions small and focused on one thing.", "Pin your dependency versions; read changelogs before upgrading.", "Idempotent operations make retries safe.", "Cache invalidation and naming are the hard problems.", "Leave the campsite cleaner than you found it.", "Estimate, then double it, then add the integration time.", "Use feature flags to decouple deploy from release.", "Logs without context are noise: include IDs and timestamps.", "Backups you've never restored are hopes, not backups.", "The best code review is a kind one.", "Learn one new thing about your tools every week.")
    private val quotes = listOf("“Programs must be written for people to read, and only incidentally for machines to execute.” – Abelson & Sussman", "“Simplicity is prerequisite for reliability.” – Edsger Dijkstra", "“Talk is cheap. Show me the code.” – Linus Torvalds", "“The most important property of a program is whether it accomplishes the intention of its user.” – C.A.R. Hoare", "“First, solve the problem. Then, write the code.” – John Johnson", "“Debugging is twice as hard as writing the code in the first place.” – Brian Kernighan", "“Make it work, make it right, make it fast.” – Kent Beck", "“Any fool can write code that a computer can understand. Good programmers write code that humans can understand.” – Martin Fowler")
    private val trivia = listOf("The first computer bug was a real moth found in Harvard's Mark II relay in 1947 (Grace Hopper's team).", "Ada Lovelace published what is considered the first algorithm for a machine in 1843.", "The first domain name ever registered was symbolics.com (1985).", "Python is named after Monty Python, not the snake.", "The '@' in email addresses was chosen by Ray Tomlinson in 1971.", "JavaScript was created in 10 days by Brendan Eich in 1995.", "Linux started as a hobby project announced by Linus Torvalds in 1991.", "The Apollo 11 guidance computer had about 4 KB of RAM.", "UTF-8 was designed on a placemat in a New Jersey diner in 1992.", "The Unix epoch (1 Jan 1970) overflows 32-bit time_t on 19 Jan 2038.", "Git was written in about two weeks in 2005 after BitKeeper's licence was withdrawn.", "The term 'bit' was coined by John Tukey in 1947.")
    fun daily(kind: String): String { val d = java.time.LocalDate.now().dayOfYear; return when (kind) { "tip" -> "💡 " + tips[d % tips.size]; "quote" -> quotes[d % quotes.size]; else -> "🎓 " + trivia[d % trivia.size] } }
}

object Fun {
    private val font = "A=010101111101101,B=110101110101110,C=011100100100011,D=110101101101110,E=111100110100111,F=111100110100100,G=011100101101011,H=101101111101101,I=111010010010111,J=001001001101010,K=101101110101101,L=100100100100111,M=101111111101101,N=110101101101101,O=010101101101010,P=110101110100100,Q=010101101110011,R=110101110101101,S=011100010001110,T=111010010010010,U=101101101101111,V=101101101101010,W=101101111111101,X=101101010101101,Y=101101010010010,Z=111001010100111,0=111101101101111,1=010110010010111,2=110001010100111,3=110001010001110,4=101101111001001,5=111100110001110,6=011100111101111,7=111001010010010,8=111101111101111,9=111101111001110,!=010010010000010,?=110001010000010,-=000000111000000,.=000000000000010".split(",").associate { it[0] to it.substring(2) }
    fun banner(s: String, on: String = "█"): String { val chars = s.uppercase().filter { it == ' ' || font.containsKey(it) }; return (0 until 5).joinToString("\n") { row -> chars.toList().joinToString(" ") { c -> if (c == ' ') "   " else font.getValue(c).substring(row * 3, row * 3 + 3).map { if (it == '1') on else " " }.joinToString("") } }.trimEnd() }
    fun box(s: String): String { val ls = lines(s); val w = ls.maxOf { it.length }; return "┌" + "─".repeat(w + 2) + "┐\n" + ls.joinToString("\n") { "│ ${it.padEnd(w)} │" } + "\n└" + "─".repeat(w + 2) + "┘" }
    fun emoji(s: String): String = s.codePoints().toArray().joinToString("\n") { cp -> val ch = String(Character.toChars(cp)); "$ch  U+%04X  &#$cp;  \\u{%X}  ${if (cp > 0xFFFF) "\\u%04X\\u%04X".format(ch[0].code, ch[1].code) else "\\u%04X".format(cp)}".format(cp, cp) }
}

val learnTools: List<Tool> = listOf(
    tool("ds-playground", "Data Structure Playground", Category.LEARN, "BST, heap, trie, hash map, LRU cache, stack & queue – step by step",
        m("Binary search tree") { Learn.bst(it) }, m("Min-heap") { Learn.heap(it) }, mp("Trie") { s, p -> Learn.trie(s, p.getOrElse(0) { "" }) },
        mp("Hash map buckets") { s, p -> Learn.hashmap(s, p.getOrElse(0) { "" }.toIntOrNull() ?: 8) }, mp("LRU cache") { s, p -> Learn.lru(s, p.getOrElse(0) { "" }.toIntOrNull() ?: 3) },
        m("Stack ops") { Learn.stackQueue(it, true) }, m("Queue ops") { Learn.stackQueue(it, false) },
        hint = "Numbers / words / keys / commands (push 1 push 2 pop)", params = listOf("Prefix · buckets · capacity (depends on mode)"), sample = "50 30 70 20 40 60 80"),
    tool("pathfinder", "Pathfinding Visualizer (BFS / A*)", Category.LEARN, "S = start, E = end, # = wall",
        m("Dijkstra / BFS") { Learn.path(it, false) }, m("A* (Manhattan)") { Learn.path(it, true) }, hint = "Grid", sample = "S..#......\n.#.#.####.\n.#...#....\n.#####.##.\n.......#.E"),
    tool("dfa", "DFA Simulator", Category.LEARN, "Transitions as 'state symbol next' lines", mp("Run") { t, p -> Learn.dfa(t, p.getOrElse(0) { "q0" }, p.getOrElse(1) { "q1" }, p.getOrElse(2) { "" }) },
        hint = "q0 a q1\nq0 b q0\nq1 a q1\nq1 b q0", params = listOf("Start state", "Accepting states (comma)", "Input string"), sample = "q0 a q1\nq0 b q0\nq1 a q1\nq1 b q0", psample = listOf("q0", "q1", "abab")),
    tool("recursion", "Recursion Tree Visualizer", Category.LEARN, "See why naive Fibonacci explodes", m("fib(n) call tree") { Learn.fibTree(it.trim().toInt()) }, hint = "n (0–8)", sample = "5"),
    tool("master", "Recurrence Solver (Master Theorem)", Category.LEARN, "T(n) = a·T(n/b) + n^d", m("Solve") { Learn.master(it) }, hint = "a b d", sample = "2 2 1"),
    tool("daily", "Tip · Quote · Trivia of the Day", Category.LEARN, "A fresh developer thought, bundled offline", m("Tip of the day") { Learn.daily("tip") }, m("Quote") { Learn.daily("quote") }, m("Trivia") { Learn.daily("trivia") }, hint = "(no input needed)", auto = true),
    custom("sort-vis", "Sorting Algorithm Visualizer", Category.LEARN, "Watch bubble, selection, insertion, merge and quick sort animate", "algorithm animation"),
    custom("flashcards", "Flashcards (spaced repetition)", Category.LEARN, "Leitner-box deck of syntax, concepts and CS fundamentals", "quiz study interview"),
)

val productivityTools: List<Tool> = listOf(
    custom("pomodoro", "Pomodoro & Project Time Tracker", Category.PRODUCTIVITY, "Focus timer that logs minutes per project", "stopwatch timer focus"),
    custom("pipeline", "Pipeline Builder (chain tools)", Category.PRODUCTIVITY, "Chain transforms, e.g. trim → Base64 → copy", "clipboard transform chain custom tool"),
    tool("cmd-vars", "Command Templates with Variables", Category.PRODUCTIVITY, "Use {{name}} placeholders; fill them from JSON",
        mp("Render") { s, p -> SqlTools.mustache(s, parseAny(p.getOrElse(0) { "{}" })) }, hint = "ssh {{user}}@{{host}} -p {{port}}", params = listOf("Variables as JSON"), sample = "ssh {{user}}@{{host}} -p {{port}}", psample = listOf("{\"user\":\"dev\",\"host\":\"10.0.0.5\",\"port\":22}")),
    tool("banner", "ASCII Banner & Box Text", Category.PRODUCTIVITY, "Big block letters and boxed notes for READMEs and logs", m("Banner") { Fun.banner(it) }, m("Banner (#)") { Fun.banner(it, "#") }, m("Boxed text") { Fun.box(it) }, hint = "Text", sample = "HELLO"),
    tool("emoji-unicode", "Emoji ⇄ Unicode", Category.PRODUCTIVITY, "Code points, HTML entities and escapes for any emoji", m("Inspect") { Fun.emoji(it) }, hint = "Paste emoji", sample = "🚀✨"),
    tool("rubber-duck", "Rubber Duck Debugging Notepad", Category.PRODUCTIVITY, "Structured questions that usually reveal the bug",
        m("Ask me") { s -> "🦆 Quack! Let's debug${if (s.isNotBlank()) ": \"${s.trim().take(80)}\"" else ""}\n\n1. What did you expect to happen?\n2. What actually happened (exact error text)?\n3. When did it last work? What changed since?\n4. Can you reproduce it every time? Smallest steps?\n5. What are you ASSUMING that you haven't verified?\n6. Have you printed / logged the inputs at the failing line?\n7. Does it fail with simpler data?\n8. What would you tell a teammate to check first?\n\nWrite your answers below and read them out loud." },
        hint = "Describe the bug", sample = "App crashes when I rotate the screen"),
)
