package com.codingtools.app.tools

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.*

class BigCalc(private val s: String) {
    private var i = 0
    fun eval(): BigInteger { val v = or(); ws(); need(i == s.length, "Unexpected '${s.getOrNull(i)}' at $i"); return v }
    private fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
    private fun peek(op: String): Boolean { ws(); return s.startsWith(op, i) }
    private fun eat(op: String): Boolean { if (peek(op)) { i += op.length; return true }; return false }
    private fun or(): BigInteger { var v = xor(); while (peek("|")) { i++; v = v.or(xor()) }; return v }
    private fun xor(): BigInteger { var v = and(); while (eat("^")) v = v.xor(and()); return v }
    private fun and(): BigInteger { var v = shift(); while (peek("&")) { i++; v = v.and(shift()) }; return v }
    private fun shift(): BigInteger { var v = add(); while (true) { if (eat("<<")) v = v.shiftLeft(add().toInt()) else if (eat(">>")) v = v.shiftRight(add().toInt()) else break }; return v }
    private fun add(): BigInteger { var v = mul(); while (true) { if (eat("+")) v = v.add(mul()) else if (eat("-")) v = v.subtract(mul()) else break }; return v }
    private fun mul(): BigInteger { var v = pow(); while (true) { if (peek("*") && !peek("**")) { i++; v = v.multiply(pow()) } else if (eat("/")) { val d = pow(); need(d.signum() != 0, "Division by zero"); v = v.divide(d) } else if (eat("%")) { val d = pow(); need(d.signum() != 0, "Division by zero"); v = v.remainder(d) } else break }; return v }
    private fun pow(): BigInteger { val b = unary(); return if (eat("**")) b.pow(pow().toInt()) else b }
    private fun unary(): BigInteger = when { eat("-") -> unary().negate(); eat("~") -> unary().not(); eat("+") -> unary(); else -> post() }
    private fun post(): BigInteger { var v = prim(); while (peek("!") && !peek("!=")) { i++; var f = BigInteger.ONE; for (k in 2..v.toInt()) f = f.multiply(BigInteger.valueOf(k.toLong())); v = f }; return v }
    private fun prim(): BigInteger { ws(); if (eat("(")) { val v = or(); need(eat(")"), "Missing )"); return v }
        val st = i; val radix = when { s.startsWith("0x", i, true) -> { i += 2; 16 }; s.startsWith("0b", i, true) -> { i += 2; 2 }; s.startsWith("0o", i, true) -> { i += 2; 8 }; else -> 10 }
        val ds = i; while (i < s.length && (s[i].isLetterOrDigit() || s[i] == '_')) i++; need(i > ds, "Number expected at $st")
        return BigInteger(s.substring(ds, i).replace("_", ""), radix) }
}

class DCalc(private val s: String) {
    private var i = 0
    fun eval(): Double { val v = add(); ws(); need(i == s.length, "Unexpected '${s.getOrNull(i)}' at $i"); return v }
    private fun ws() { while (i < s.length && s[i].isWhitespace()) i++ }
    private fun eat(c: Char): Boolean { ws(); if (i < s.length && s[i] == c) { i++; return true }; return false }
    private fun add(): Double { var v = mul(); while (true) { if (eat('+')) v += mul() else if (eat('-')) v -= mul() else break }; return v }
    private fun mul(): Double { var v = pw(); while (true) { if (eat('*')) v *= pw() else if (eat('/')) v /= pw() else if (eat('%')) v %= pw() else break }; return v }
    private fun pw(): Double { val b = un(); return if (eat('^')) b.pow(pw()) else b }
    private fun un(): Double = if (eat('-')) -un() else if (eat('+')) un() else prim()
    private fun prim(): Double { ws()
        if (eat('(')) { val v = add(); need(eat(')'), "Missing )"); return v }
        if (i < s.length && (s[i].isDigit() || s[i] == '.')) { val st = i; while (i < s.length && (s[i].isDigit() || s[i] == '.' || s[i] == 'e' && i + 1 < s.length && (s[i + 1].isDigit() || s[i + 1] == '-'))) { if (s[i] == 'e') i++; i++ }; return s.substring(st, i).toDouble() }
        val st = i; while (i < s.length && s[i].isLetter()) i++; val n = s.substring(st, i); need(n.isNotEmpty(), "Unexpected '${s.getOrNull(i)}'")
        when (n) { "pi" -> return PI; "e" -> return E }
        need(eat('('), "Expected ( after $n"); val args = ArrayList<Double>(); if (!eat(')')) { do { args.add(add()) } while (eat(',')); need(eat(')'), "Missing )") }
        val a = args.getOrElse(0) { 0.0 }
        return when (n) { "sin" -> sin(a); "cos" -> cos(a); "tan" -> tan(a); "asin" -> asin(a); "acos" -> acos(a); "atan" -> atan(a); "sqrt" -> sqrt(a); "cbrt" -> Math.cbrt(a); "ln" -> ln(a); "log" -> log10(a); "log2" -> log2(a); "abs" -> abs(a); "exp" -> exp(a); "floor" -> floor(a); "ceil" -> ceil(a); "round" -> a.roundToLong().toDouble()
            "pow" -> a.pow(args[1]); "min" -> args.min(); "max" -> args.max(); "deg" -> Math.toDegrees(a); "rad" -> Math.toRadians(a); else -> throw IllegalArgumentException("Unknown function $n") }
    }
}

object MathX {
    fun bits(n: BigInteger, w: Int): String { val u = n.mod(BigInteger.ONE.shiftLeft(w)); val b = u.toString(2).padStart(w, '0'); return b.chunked(8).joinToString(" ") }
    fun bitInfo(s: String, w: Int): String { val n = Conv.parseNum(s, null); val u = n.mod(BigInteger.ONE.shiftLeft(w))
        return "Value  : $n  (0x${u.toString(16).uppercase()})\nBits   : ${bits(n, w)}\nIndex  : ${(w - 1 downTo 0).joinToString("") { (it % 10).toString() }.chunked(8).joinToString(" ")}\nSet bits (popcount): ${u.bitCount()}\nLeading zeros: ${w - u.bitLength()}   Trailing zeros: ${if (u.signum() == 0) w else u.lowestSetBit}\nPower of two: ${u.signum() > 0 && u.bitCount() == 1}\nSigned (${w}-bit): ${if (u.testBit(w - 1)) u - BigInteger.ONE.shiftLeft(w) else u}" }
    fun bitOp(a: String, b: String, op: String, w: Int): String { val x = Conv.parseNum(a, null); val y = Conv.parseNum(b.ifBlank { "0" }, null)
        val r = when (op) { "and" -> x.and(y); "or" -> x.or(y); "xor" -> x.xor(y); "shl" -> x.shiftLeft(y.toInt()); "shr" -> x.shiftRight(y.toInt()); else -> x.not() }
        return "A    : ${bits(x, w)}\n${if (op == "not") "" else "B    : ${bits(y, w)}\n"}Result: ${bits(r, w)}\n\n= $r  (0x${r.mod(BigInteger.ONE.shiftLeft(w)).toString(16).uppercase()})" }
    fun ieee(s: String): String {
        val d: Double; val t = s.trim()
        d = if (t.startsWith("0x", true)) { val h = t.substring(2); if (h.length <= 8) java.lang.Float.intBitsToFloat(h.toLong(16).toInt()).toDouble() else java.lang.Double.longBitsToDouble(java.lang.Long.parseUnsignedLong(h, 16)) } else t.toDouble()
        val f = d.toFloat(); val fb = java.lang.Float.floatToIntBits(f); val db = java.lang.Double.doubleToLongBits(d)
        fun part(bits: String, e: Int) = "${bits.substring(0, 1)} ${bits.substring(1, 1 + e)} ${bits.substring(1 + e)}"
        return "float32 : ${f}\n  hex   0x${"%08X".format(fb)}\n  bits  ${part(Integer.toBinaryString(fb).padStart(32, '0'), 8)}  (sign | exponent | mantissa)\n  exact ${BigDecimal(f.toDouble()).toPlainString()}\n\nfloat64 : $d\n  hex   0x${"%016X".format(db)}\n  bits  ${part(java.lang.Long.toBinaryString(db).padStart(64, '0'), 11)}\n  exact ${BigDecimal(d).toPlainString()}\n\n0.1 + 0.2 = ${0.1 + 0.2}   (floating-point is binary, not decimal)\nULP at this magnitude: ${Math.ulp(d)}\nNaN: ${d.isNaN()}  Infinite: ${d.isInfinite()}" }
    fun truth(expr: String): String {
        val toks = Regex("->|<->|&&|\\|\\||[A-Za-z_]\\w*|[()!~&|^]").findAll(expr).map { it.value }.toList(); var p = 0
        val vars = toks.filter { it[0].isLetter() && it.lowercase() !in setOf("and", "or", "not", "xor") }.distinct()
        need(vars.size in 1..8, "Use 1–8 variables")
        fun parse(): (Map<String, Boolean>) -> Boolean {
            fun peek() = toks.getOrNull(p)
            lateinit var imp: () -> (Map<String, Boolean>) -> Boolean
            fun prim(): (Map<String, Boolean>) -> Boolean { val t = toks.getOrNull(p++) ?: throw IllegalArgumentException("Unexpected end")
                return when { t == "(" -> { val e = imp(); need(peek() == ")", "Missing )"); p++; e }; t == "!" || t == "~" || t.lowercase() == "not" -> { val e = prim(); { m -> !e(m) } }; t.lowercase() == "true" -> { _ -> true }; t.lowercase() == "false" -> { _ -> false }; t[0].isLetter() -> { m -> m.getValue(t) }; else -> throw IllegalArgumentException("Unexpected '$t'") } }
            fun and(): (Map<String, Boolean>) -> Boolean { var l = prim(); while (peek() in setOf("&", "&&") || peek()?.lowercase() == "and") { p++; val a = l; val r = prim(); l = { m -> a(m) && r(m) } }; return l }
            fun xor(): (Map<String, Boolean>) -> Boolean { var l = and(); while (peek() == "^" || peek()?.lowercase() == "xor") { p++; val a = l; val r = and(); l = { m -> a(m) != r(m) } }; return l }
            fun or(): (Map<String, Boolean>) -> Boolean { var l = xor(); while (peek() in setOf("|", "||") || peek()?.lowercase() == "or") { p++; val a = l; val r = xor(); l = { m -> a(m) || r(m) } }; return l }
            imp = { var l = or(); while (peek() == "->" || peek() == "<->") { val op = toks[p++]; val a = l; val r = or(); l = if (op == "->") { m -> !a(m) || r(m) } else { m -> a(m) == r(m) } }; l }
            val e = imp(); need(p == toks.size, "Unexpected '${peek()}'"); return e
        }
        val f = parse(); val rows = ArrayList<List<String>>(); rows.add(vars + "RESULT"); var taut = true; var contra = true
        for (mask in (1 shl vars.size) - 1 downTo 0) { val m = vars.withIndex().associate { (i, v) -> v to ((mask shr (vars.size - 1 - i)) and 1 == 1) }; val r = f(m); if (r) contra = false else taut = false; rows.add(vars.map { if (m.getValue(it)) "1" else "0" } + (if (r) "1" else "0")) }
        return table(rows) + "\n\n" + (if (taut) "Tautology" else if (contra) "Contradiction" else "Contingent")
    }
    fun nums(s: String) = s.split(Regex("[\\s,;]+")).filter { it.isNotEmpty() }.map { it.toDouble() }
    fun stats(s: String): String { val v = nums(s).sorted(); need(v.isNotEmpty(), "Enter numbers"); val n = v.size; val mean = v.average(); val med = if (n % 2 == 1) v[n / 2] else (v[n / 2 - 1] + v[n / 2]) / 2
        val varP = v.sumOf { (it - mean).pow(2) } / n; val varS = if (n > 1) v.sumOf { (it - mean).pow(2) } / (n - 1) else 0.0; val mode = v.groupingBy { it }.eachCount().maxByOrNull { it.value }
        fun pct(p: Double) = v[((p / 100) * (n - 1)).roundToInt()]
        return "Count ${n}\nSum ${fmt(v.sum())}\nMean ${fmt(mean)}\nMedian ${fmt(med)}\nMode ${fmt(mode!!.key)} (×${mode.value})\nMin ${fmt(v.first())}   Max ${fmt(v.last())}   Range ${fmt(v.last() - v.first())}\nVariance (pop) ${fmt(varP)}   (sample) ${fmt(varS)}\nStd dev (pop) ${fmt(sqrt(varP))}   (sample) ${fmt(sqrt(varS))}\nP25 ${fmt(pct(25.0))}  P75 ${fmt(pct(75.0))}  P90 ${fmt(pct(90.0))}  P99 ${fmt(pct(99.0))}" }
    fun mat(s: String) = lines(s).filter { it.isNotBlank() }.map { r -> r.trim().split(Regex("[\\s,;]+")).map { it.toDouble() }.toDoubleArray() }.toTypedArray()
    fun show(m: Array<DoubleArray>) = m.joinToString("\n") { r -> r.joinToString("  ") { fmt(it).padStart(9) } }
    fun det(m: Array<DoubleArray>): Double { val n = m.size; need(m.all { it.size == n }, "Matrix must be square"); val a = m.map { it.clone() }.toTypedArray(); var d = 1.0
        for (c in 0 until n) { var p = c; for (r in c + 1 until n) if (abs(a[r][c]) > abs(a[p][c])) p = r; if (abs(a[p][c]) < 1e-12) return 0.0; if (p != c) { val t = a[p]; a[p] = a[c]; a[c] = t; d = -d }; d *= a[c][c]; for (r in c + 1 until n) { val f = a[r][c] / a[c][c]; for (k in c until n) a[r][k] -= f * a[c][k] } }; return d }
    fun inv(m: Array<DoubleArray>): Array<DoubleArray> { val n = m.size; val a = Array(n) { r -> DoubleArray(2 * n) { c -> if (c < n) m[r][c] else if (c - n == r) 1.0 else 0.0 } }
        for (c in 0 until n) { var p = c; for (r in c + 1 until n) if (abs(a[r][c]) > abs(a[p][c])) p = r; need(abs(a[p][c]) > 1e-12, "Matrix is singular"); val t = a[p]; a[p] = a[c]; a[c] = t; val d = a[c][c]; for (k in 0 until 2 * n) a[c][k] /= d; for (r in 0 until n) if (r != c) { val f = a[r][c]; for (k in 0 until 2 * n) a[r][k] -= f * a[c][k] } }
        return Array(n) { r -> DoubleArray(n) { c -> a[r][n + c] } } }
    fun mulM(a: Array<DoubleArray>, b: Array<DoubleArray>): Array<DoubleArray> { need(a[0].size == b.size, "Columns of A must equal rows of B"); return Array(a.size) { i -> DoubleArray(b[0].size) { j -> a[i].indices.sumOf { k -> a[i][k] * b[k][j] } } } }
    fun big(s: String) = s.trim().split(Regex("[\\s,]+")).map { BigInteger(it.trim()) }
    fun factor(n0: Long): String { var n = n0; val f = ArrayList<String>(); var p = 2L; while (p * p <= n) { var c = 0; while (n % p == 0L) { n /= p; c++ }; if (c > 0) f.add(if (c > 1) "$p^$c" else "$p"); p += if (p == 2L) 1 else 2 }; if (n > 1) f.add("$n"); return "$n0 = ${f.joinToString(" × ")}" }
    fun sieve(n: Int): String { need(n in 2..200000, "Up to 200000"); val c = BooleanArray(n + 1); val out = ArrayList<Int>(); for (i in 2..n) { if (!c[i]) { out.add(i); var j = i.toLong() * i; while (j <= n) { c[j.toInt()] = true; j += i } } }; return "${out.size} primes ≤ $n\n" + out.joinToString(" ") }
    fun binom(n: Int, k: Int): BigInteger { var r = BigInteger.ONE; for (i in 1..k) r = r.multiply(BigInteger.valueOf((n - k + i).toLong())).divide(BigInteger.valueOf(i.toLong())); return r }
    fun fib(n: Int): BigInteger { var a = BigInteger.ZERO; var b = BigInteger.ONE; repeat(n) { val t = a.add(b); a = b; b = t }; return a }
    fun overflow(s: String): String { val v = Conv.parseNum(s, null)
        return listOf(8, 16, 32, 64).joinToString("\n") { w -> val mod = BigInteger.ONE.shiftLeft(w); val u = v.mod(mod); val sg = if (u.testBit(w - 1)) u - mod else u; val smin = BigInteger.ONE.shiftLeft(w - 1).negate(); val smax = BigInteger.ONE.shiftLeft(w - 1).subtract(BigInteger.ONE)
            "int$w : ${if (v in smin..smax) "fits" else "OVERFLOW → wraps to $sg"}    uint$w : ${if (v.signum() >= 0 && v < mod) "fits" else "OVERFLOW → wraps to $u"}" } }
    fun qfmt(s: String, q: String, toRaw: Boolean): String { val (m, n) = q.trim().ifBlank { "16.16" }.split('.').let { it[0].toInt() to it[1].toInt() }; val sc = 2.0.pow(n)
        return if (toRaw) { val raw = (s.trim().toDouble() * sc).roundToLong(); "Q$m.$n raw = $raw (0x${raw.toString(16)})\nError: ${raw / sc - s.trim().toDouble()}\nRange: ${-(2.0.pow(m - 1))} … ${2.0.pow(m - 1) - 1 / sc}   Resolution: ${1 / sc}" } else "${Conv.parseNum(s, null).toLong() / sc}" }
    private const val B32 = "0123456789bcdefghjkmnpqrstuvwxyz"
    fun geoEnc(lat: Double, lng: Double, prec: Int): String { var latR = -90.0 to 90.0; var lngR = -180.0 to 180.0; val sb = StringBuilder(); var bit = 0; var ch = 0; var even = true
        while (sb.length < prec) { if (even) { val mid = (lngR.first + lngR.second) / 2; if (lng >= mid) { ch = ch * 2 + 1; lngR = mid to lngR.second } else { ch *= 2; lngR = lngR.first to mid } } else { val mid = (latR.first + latR.second) / 2; if (lat >= mid) { ch = ch * 2 + 1; latR = mid to latR.second } else { ch *= 2; latR = latR.first to mid } }
            even = !even; if (++bit == 5) { sb.append(B32[ch]); bit = 0; ch = 0 } }
        return sb.toString() }
    fun geoDec(h: String): String { var latR = -90.0 to 90.0; var lngR = -180.0 to 180.0; var even = true
        for (c in h.lowercase()) { val v = B32.indexOf(c); need(v >= 0, "Bad geohash char"); for (b in 4 downTo 0) { val bit = (v shr b) and 1; if (even) { val mid = (lngR.first + lngR.second) / 2; lngR = if (bit == 1) mid to lngR.second else lngR.first to mid } else { val mid = (latR.first + latR.second) / 2; latR = if (bit == 1) mid to latR.second else latR.first to mid }; even = !even } }
        return "lat ${(latR.first + latR.second) / 2}  (±${(latR.second - latR.first) / 2})\nlng ${(lngR.first + lngR.second) / 2}  (±${(lngR.second - lngR.first) / 2})" }
    fun luhn(s: String): Boolean { val d = s.filter { it.isDigit() }.reversed().map { it - '0' }; return d.isNotEmpty() && d.mapIndexed { i, x -> if (i % 2 == 1) (x * 2).let { if (it > 9) it - 9 else it } else x }.sum() % 10 == 0 }
    fun crc16(b: ByteArray): Int { var crc = 0xFFFF; for (x in b) { crc = crc xor ((x.toInt() and 0xff) shl 8); repeat(8) { crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1; crc = crc and 0xFFFF } }; return crc }
    fun histogram(kind: String, n: Int): String { val r = java.util.Random(); val bins = IntArray(20)
        repeat(n) { val v = if (kind == "normal") ((r.nextGaussian() * 1.5 + 5) / 10).coerceIn(0.0, 0.9999) else r.nextDouble(); bins[(v * 20).toInt()]++ }
        val mx = bins.max().coerceAtLeast(1); return "$kind distribution, $n samples\n\n" + bins.mapIndexed { i, c -> "%4.2f │%s %d".format(i / 20.0, "█".repeat(c * 40 / mx), c) }.joinToString("\n") }
}

val mathTools: List<Tool> = listOf(
    tool("prog-calc", "Programmer Calculator", Category.MATH, "Big-integer math with hex/bin/oct literals, bit ops, shifts, two's complement. Operators: + - * / % ** << >> & | ^ ~ !",
        m("Evaluate") { Conv.bases(BigCalc(it).eval()) }, hint = "Expression", sample = "(0xFF << 4) | 0b1010 ^ 12", kw = "hex bitwise bignum arbitrary precision"),
    tool("sci-calc", "Scientific Calculator", Category.MATH, "sin cos tan sqrt ln log exp pow min max abs floor ceil round, pi, e, ^",
        m("Evaluate") { fmt(DCalc(it).eval()).let { r -> if (r.length > 14 && DCalc(it).eval().let { d -> d != floor(d) }) DCalc(it).eval().toString() else r } }, hint = "Expression", sample = "sqrt(2) * sin(pi/4) + 2^10"),
    tool("bits", "Bitwise Visualizer", Category.MATH, "Bit grid, popcount, and AND/OR/XOR/NOT/shift operations",
        mp("Bits (32)") { s, _ -> MathX.bitInfo(s, 32) }, mp("Bits (64)") { s, _ -> MathX.bitInfo(s, 64) }, mp("Bits (8)") { s, _ -> MathX.bitInfo(s, 8) },
        mp("A AND B") { s, p -> MathX.bitOp(s, p.getOrElse(0) { "" }, "and", 32) }, mp("A OR B") { s, p -> MathX.bitOp(s, p.getOrElse(0) { "" }, "or", 32) }, mp("A XOR B") { s, p -> MathX.bitOp(s, p.getOrElse(0) { "" }, "xor", 32) },
        mp("A << B") { s, p -> MathX.bitOp(s, p.getOrElse(0) { "" }, "shl", 32) }, mp("A >> B") { s, p -> MathX.bitOp(s, p.getOrElse(0) { "" }, "shr", 32) }, mp("NOT A") { s, p -> MathX.bitOp(s, p.getOrElse(0) { "" }, "not", 32) },
        hint = "Number A (dec, 0x, 0b)", params = listOf("Number B"), sample = "0xB6"),
    tool("ieee754", "IEEE 754 Float Inspector", Category.MATH, "Sign / exponent / mantissa, exact decimal value, rounding surprises", m("Inspect") { MathX.ieee(it) }, hint = "Number, or 0x40490fdb (float) / 0x400921FB54442D18 (double)", sample = "0.1", kw = "float double precision rounding"),
    tool("truth", "Boolean Algebra & Truth Table", Category.MATH, "Operators: & | ^ ! ~ -> <-> and or not xor, parentheses", m("Truth table") { MathX.truth(it) }, hint = "Expression", sample = "(A & B) | !C -> D"),
    tool("stats", "Statistics", Category.MATH, "Mean, median, mode, variance, std-dev, percentiles", m("Analyse") { MathX.stats(it) }, hint = "Numbers (space / comma / newline)", sample = "4 8 15 16 23 42"),
    tool("matrix", "Matrix Calculator", Category.MATH, "Determinant, inverse, transpose, multiply (rows on separate lines)",
        m("Determinant") { MathX.det(MathX.mat(it)).let { d -> fmt(d) } }, m("Inverse") { MathX.show(MathX.inv(MathX.mat(it))) },
        m("Transpose") { s -> val a = MathX.mat(s); MathX.show(Array(a[0].size) { c -> DoubleArray(a.size) { r -> a[r][c] } }) }, mp("Multiply A × B") { s, p -> MathX.show(MathX.mulM(MathX.mat(s), MathX.mat(p.getOrElse(0) { "" }.replace(";", "\n")))) },
        hint = "Matrix A", params = listOf("Matrix B (rows separated by ; )"), sample = "2 1\n1 3"),
    tool("number-theory", "Number Theory", Category.MATH, "GCD/LCM, primes, factorisation, modular arithmetic",
        m("GCD") { MathX.big(it).reduce { a, b -> a.gcd(b) }.toString() }, m("LCM") { MathX.big(it).reduce { a, b -> a.multiply(b).abs().divide(a.gcd(b)) }.toString() },
        m("Is prime?") { s -> val n = BigInteger(s.trim()); "$n is ${if (n.isProbablePrime(40)) "" else "NOT "}prime" }, m("Factorise") { MathX.factor(it.trim().toLong()) },
        m("Primes ≤ n") { MathX.sieve(it.trim().toInt()) }, m("modPow  a b m") { s -> val (a, b, c) = MathX.big(s); a.modPow(b, c).toString() },
        m("modInverse  a m") { s -> val (a, c) = MathX.big(s); a.modInverse(c).toString() }, m("Euler φ(n)") { s -> var n = s.trim().toLong(); var r = n; var p = 2L; while (p * p <= n) { if (n % p == 0L) { while (n % p == 0L) n /= p; r -= r / p }; p++ }; if (n > 1) r -= r / n; "φ = $r" },
        hint = "Numbers (space separated)", sample = "84 36"),
    tool("combinatorics", "Combinatorics & Sequences", Category.MATH, "n!, nCr, nPr, Fibonacci",
        m("Factorial n!") { s -> var f = BigInteger.ONE; for (k in 2..s.trim().toInt().also { need(it <= 2000, "n ≤ 2000") }) f = f.multiply(BigInteger.valueOf(k.toLong())); f.toString() },
        m("nCr  (n r)") { s -> val (n, r) = s.trim().split(Regex("\\s+")).map { it.toInt() }; MathX.binom(n, r).toString() },
        m("nPr  (n r)") { s -> val (n, r) = s.trim().split(Regex("\\s+")).map { it.toInt() }; (MathX.binom(n, r) * (1..r).fold(BigInteger.ONE) { a, b -> a.multiply(BigInteger.valueOf(b.toLong())) }).toString() },
        m("Fibonacci F(n)") { MathX.fib(it.trim().toInt().also { n -> need(n <= 20000, "n ≤ 20000") }).toString() }, hint = "n  or  n r", sample = "10 3"),
    tool("overflow", "Integer Overflow Checker", Category.MATH, "Does a value fit int8…int64 / uint8…uint64?", m("Check") { MathX.overflow(it) }, hint = "Value", sample = "300"),
    tool("qformat", "Fixed-Point (Q-format)", Category.MATH, "Real ⇄ Qm.n raw integers",
        mp("Real → raw") { s, p -> MathX.qfmt(s, p.getOrElse(0) { "" }, true) }, mp("Raw → real") { s, p -> MathX.qfmt(s, p.getOrElse(0) { "" }, false) }, hint = "Value", params = listOf("Format m.n (default 16.16)"), sample = "3.14159"),
    tool("geohash", "Geohash ⇄ Lat/Long", Category.MATH, "Encode and decode",
        mp("Encode lat,lng") { s, p -> val (a, b) = s.split(',').map { it.trim().toDouble() }; MathX.geoEnc(a, b, p.getOrElse(0) { "" }.toIntOrNull() ?: 9) }, m("Decode") { MathX.geoDec(it.trim()) }, hint = "5.6037, -0.1870  or geohash", params = listOf("Precision (9)"), sample = "5.6037, -0.1870"),
    tool("checksum", "Checksums & Parity", Category.MATH, "Luhn, CRC-16, XOR/sum8, parity, ISBN",
        m("Luhn validate") { if (MathX.luhn(it)) "✔ Valid Luhn" else "✘ Invalid" },
        m("Luhn check digit") { s -> val d = s.filter { c -> c.isDigit() }; (0..9).first { MathX.luhn(d + it) }.toString() },
        m("CRC-16/CCITT") { "0x%04X".format(MathX.crc16(it.toByteArray())) }, m("XOR / sum8") { s -> val b = s.toByteArray(); "XOR 0x%02X   SUM8 0x%02X".format(b.fold(0) { a, x -> a xor (x.toInt() and 0xff) }, b.sumOf { x -> x.toInt() and 0xff } and 0xff) },
        m("Parity bits") { s -> s.toByteArray().joinToString("\n") { b -> val v = b.toInt() and 0xff; "'${v.toChar()}' ${Integer.toBinaryString(v).padStart(8, '0')}  ones=${Integer.bitCount(v)}  even-parity bit=${Integer.bitCount(v) % 2}" } },
        hint = "Text / digits", sample = "4539578763621486"),
    tool("distribution", "Random Distribution Visualizer", Category.MATH, "ASCII histogram. Input = samples",
        m("Uniform") { MathX.histogram("uniform", it.trim().toIntOrNull() ?: 2000) }, m("Normal") { MathX.histogram("normal", it.trim().toIntOrNull() ?: 2000) }, hint = "Samples", sample = "2000"),
)
