package com.codingtools.app.tools

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object Dt {
    fun parse(s: String): Instant {
        val t = s.trim()
        if (t.isEmpty() || t.equals("now", true)) return Instant.now()
        t.toLongOrNull()?.let { n -> val a = Math.abs(n); return when { a < 100_000_000_000L -> Instant.ofEpochSecond(n); a < 100_000_000_000_000L -> Instant.ofEpochMilli(n); a < 100_000_000_000_000_000L -> Instant.ofEpochSecond(n / 1_000_000, (n % 1_000_000) * 1000); else -> Instant.ofEpochSecond(n / 1_000_000_000, n % 1_000_000_000) } }
        t.toDoubleOrNull()?.let { return Instant.ofEpochMilli((it * 1000).toLong()) }
        runCatching { return Instant.parse(t) }; runCatching { return OffsetDateTime.parse(t).toInstant() }; runCatching { return ZonedDateTime.parse(t).toInstant() }
        runCatching { return LocalDateTime.parse(t.replace(' ', 'T')).toInstant(ZoneOffset.UTC) }; runCatching { return LocalDate.parse(t).atStartOfDay().toInstant(ZoneOffset.UTC) }
        runCatching { return ZonedDateTime.parse(t, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() }
        throw IllegalArgumentException("Unrecognised date. Use epoch, ISO-8601 (2026-10-13T14:30:00Z) or 'yyyy-MM-dd HH:mm'")
    }
    fun info(s: String): String {
        val i = parse(s); val z = ZoneId.systemDefault(); val now = Instant.now(); val diff = Duration.between(now, i)
        val rel = if (diff.isNegative) Conv.duration(-diff.seconds) + " ago" else "in " + Conv.duration(diff.seconds)
        return "Epoch s : ${i.epochSecond}\nEpoch ms: ${i.toEpochMilli()}\nEpoch µs: ${i.epochSecond * 1_000_000 + i.nano / 1000}\nEpoch ns: ${i.epochSecond * 1_000_000_000 + i.nano}\n\nUTC     : ${DateTimeFormatter.ISO_INSTANT.format(i)}\nLocal   : ${i.atZone(z).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)} ($z)\nRFC 1123: ${DateTimeFormatter.RFC_1123_DATE_TIME.format(i.atZone(ZoneOffset.UTC))}\nWeekday : ${i.atZone(z).dayOfWeek}   Day of year: ${i.atZone(z).dayOfYear}   ISO week: ${i.atZone(z).get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)}\nRelative: $rel"
    }
    fun tz(s: String, from: String, to: String): String {
        val f = ZoneId.of(from.ifBlank { "UTC" }); val t = ZoneId.of(to.ifBlank { ZoneId.systemDefault().id })
        val z = if (s.isBlank()) Instant.now().atZone(f) else runCatching { LocalDateTime.parse(s.trim().replace(' ', 'T')).atZone(f) }.getOrElse { parse(s).atZone(f) }
        return "${z.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z (xxx)"))}\n→\n${z.withZoneSameInstant(t).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z (xxx)"))}"
    }
    fun world(): String { val n = Instant.now(); return listOf("UTC", "Africa/Accra", "Africa/Lagos", "Africa/Nairobi", "Europe/London", "Europe/Berlin", "Asia/Dubai", "Asia/Kolkata", "Asia/Singapore", "Asia/Tokyo", "Australia/Sydney", "America/New_York", "America/Chicago", "America/Los_Angeles", "America/Sao_Paulo").joinToString("\n") { "${it.padEnd(20)} ${n.atZone(ZoneId.of(it)).format(DateTimeFormatter.ofPattern("EEE yyyy-MM-dd HH:mm xxx"))}" } }
    fun diff(a: String, b: String): String {
        val x = parse(a).atZone(ZoneOffset.UTC); val y = parse(b).atZone(ZoneOffset.UTC); val p = Period.between(x.toLocalDate(), y.toLocalDate())
        return "Calendar: ${p.years}y ${p.months}m ${p.days}d\nDays: ${ChronoUnit.DAYS.between(x, y)}\nWeeks: ${ChronoUnit.WEEKS.between(x, y)}\nHours: ${ChronoUnit.HOURS.between(x, y)}\nMinutes: ${ChronoUnit.MINUTES.between(x, y)}\nSeconds: ${ChronoUnit.SECONDS.between(x, y)}"
    }
    fun math(s: String, expr: String): String {
        var z = parse(s).atZone(ZoneOffset.UTC)
        Regex("([+-]?\\d+)\\s*(y|M|w|d|h|m|s)").findAll(expr).forEach { val n = it.groupValues[1].toLong(); z = when (it.groupValues[2]) { "y" -> z.plusYears(n); "M" -> z.plusMonths(n); "w" -> z.plusWeeks(n); "d" -> z.plusDays(n); "h" -> z.plusHours(n); "m" -> z.plusMinutes(n); else -> z.plusSeconds(n) } }
        return "${DateTimeFormatter.ISO_INSTANT.format(z)}\nepoch ${z.toEpochSecond()}\n${z.dayOfWeek}"
    }
    fun iso(s: String): String { val z = parse(s).atZone(ZoneId.systemDefault()); val u = z.withZoneSameInstant(ZoneOffset.UTC)
        return "ISO 8601 UTC   : ${DateTimeFormatter.ISO_INSTANT.format(u)}\nISO 8601 local : ${z.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}\nRFC 3339       : ${u.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))}\nRFC 2822       : ${DateTimeFormatter.RFC_1123_DATE_TIME.format(u)}\nDate only      : ${u.toLocalDate()}\nBasic ISO      : ${DateTimeFormatter.BASIC_ISO_DATE.format(u)}\nWeek date      : ${u.format(DateTimeFormatter.ISO_WEEK_DATE)}\nOrdinal date   : ${u.format(DateTimeFormatter.ISO_ORDINAL_DATE)}" }
    fun strftime(fmt: String, z: ZonedDateTime): String {
        val sb = StringBuilder(); var i = 0
        while (i < fmt.length) { val c = fmt[i]; if (c != '%' || i + 1 >= fmt.length) { sb.append(c); i++; continue }
            val f = fmt[i + 1]; i += 2
            sb.append(when (f) { 'Y' -> z.format(DateTimeFormatter.ofPattern("yyyy")); 'y' -> z.format(DateTimeFormatter.ofPattern("yy")); 'm' -> z.format(DateTimeFormatter.ofPattern("MM")); 'd' -> z.format(DateTimeFormatter.ofPattern("dd")); 'e' -> z.dayOfMonth.toString().padStart(2)
                'H' -> z.format(DateTimeFormatter.ofPattern("HH")); 'I' -> z.format(DateTimeFormatter.ofPattern("hh")); 'M' -> z.format(DateTimeFormatter.ofPattern("mm")); 'S' -> z.format(DateTimeFormatter.ofPattern("ss")); 'p' -> z.format(DateTimeFormatter.ofPattern("a"))
                'b' -> z.format(DateTimeFormatter.ofPattern("MMM")); 'B' -> z.format(DateTimeFormatter.ofPattern("MMMM")); 'a' -> z.format(DateTimeFormatter.ofPattern("EEE")); 'A' -> z.format(DateTimeFormatter.ofPattern("EEEE"))
                'j' -> z.dayOfYear.toString().padStart(3, '0'); 'Z' -> z.format(DateTimeFormatter.ofPattern("z")); 'z' -> z.format(DateTimeFormatter.ofPattern("xx")); 'f' -> z.format(DateTimeFormatter.ofPattern("SSSSSS"))
                's' -> z.toEpochSecond().toString(); 'u' -> z.dayOfWeek.value.toString(); 'w' -> (z.dayOfWeek.value % 7).toString(); 'F' -> z.toLocalDate().toString(); 'T' -> z.format(DateTimeFormatter.ofPattern("HH:mm:ss")); '%' -> "%"; else -> "%$f" }) }
        return sb.toString()
    }
    fun goLayout(l: String): String = l.replace("2006", "yyyy").replace("January", "MMMM").replace("Jan", "MMM").replace("Monday", "EEEE").replace("Mon", "EEE").replace("01", "MM").replace("02", "dd").replace("15", "HH").replace("03", "hh").replace("04", "mm").replace("05", "ss").replace("PM", "a").replace("MST", "z").replace("-0700", "xx").replace("-07:00", "xxx").replace("Z07:00", "XXX").replace(".000", ".SSS")
}

object Cron {
    private val months = listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"); private val dows = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")
    private fun field(f0: String, lo: Int, hi: Int): Set<Int> {
        var f = f0.uppercase(); months.forEachIndexed { i, n -> f = f.replace(n, (i + 1).toString()) }; dows.forEachIndexed { i, n -> f = f.replace(n, i.toString()) }
        val out = HashSet<Int>()
        for (part in f.split(',')) { val (rng, step) = if ('/' in part) part.split('/').let { it[0] to it[1].toInt() } else part to 1
            val (a, b) = when { rng == "*" || rng == "?" -> lo to hi; '-' in rng -> rng.split('-').let { it[0].toInt() to it[1].toInt() }; else -> rng.toInt().let { it to (if (step > 1 || '/' in part) hi else it) } }
            need(a >= lo && b <= hi + (if (hi == 6) 1 else 0), "Value out of range ($lo–$hi): $part"); var v = a; while (v <= b) { out.add(if (hi == 6 && v == 7) 0 else v); v += step } }
        return out
    }
    fun next(expr: String, n: Int, from: LocalDateTime = LocalDateTime.now()): List<LocalDateTime> {
        val p = expr.trim().split(Regex("\\s+")); need(p.size == 5, "Cron needs 5 fields: minute hour day-of-month month day-of-week")
        val mi = field(p[0], 0, 59); val h = field(p[1], 0, 23); val dm = field(p[2], 1, 31); val mo = field(p[3], 1, 12); val dw = field(p[4], 0, 6)
        val domAny = p[2] == "*" || p[2] == "?"; val dowAny = p[4] == "*" || p[4] == "?"
        val out = ArrayList<LocalDateTime>(); var t = from.plusMinutes(1).truncatedTo(ChronoUnit.MINUTES); var guard = 0
        while (out.size < n && guard++ < 1_100_000) {
            val dayOk = when { domAny && dowAny -> true; domAny -> t.dayOfWeek.value % 7 in dw; dowAny -> t.dayOfMonth in dm; else -> t.dayOfMonth in dm || t.dayOfWeek.value % 7 in dw }
            if (t.monthValue in mo && dayOk && t.hour in h && t.minute in mi) out.add(t)
            t = if (t.monthValue !in mo) t.plusMonths(1).withDayOfMonth(1).withHour(0).withMinute(0) else if (!dayOk) t.plusDays(1).withHour(0).withMinute(0) else if (t.hour !in h) t.plusHours(1).withMinute(0) else t.plusMinutes(1)
        }
        return out
    }
    fun explain(expr: String): String {
        val p = expr.trim().split(Regex("\\s+")); need(p.size == 5, "Cron needs 5 fields")
        fun d(f: String, unit: String, all: String) = when { f == "*" -> all; f.startsWith("*/") -> "every ${f.drop(2)} $unit"; else -> "$unit $f" }
        return "minute       : ${d(p[0], "minute", "every minute")}\nhour         : ${d(p[1], "hour", "every hour")}\nday of month : ${d(p[2], "day", "every day")}\nmonth        : ${d(p[3], "month", "every month")}\nday of week  : ${d(p[4], "weekday (0=Sun)", "every weekday")}"
    }
}

val dateTools: List<Tool> = listOf(
    tool("timestamp", "Unix Timestamp ⇄ Date", Category.DATETIME, "Seconds / ms / µs / ns auto-detected. Empty input = now",
        m("Convert") { Dt.info(it) }, m("ISO / RFC formats") { Dt.iso(it) }, m("Now (epoch)") { val n = Instant.now(); "s: ${n.epochSecond}\nms: ${n.toEpochMilli()}" },
        hint = "Epoch or date", sample = "1760000000", kw = "epoch unix time"),
    tool("timezone", "Time Zone Converter", Category.DATETIME, "Convert a date-time between IANA zones",
        mp("Convert") { s, p -> Dt.tz(s, p.getOrElse(0) { "" }, p.getOrElse(1) { "" }) }, m("World clock") { Dt.world() },
        hint = "yyyy-MM-dd HH:mm (blank = now)", params = listOf("From zone (e.g. Africa/Accra)", "To zone (e.g. Asia/Tokyo)"), sample = "2026-10-13 14:30", psample = listOf("Africa/Accra", "Asia/Tokyo")),
    tool("date-diff", "Date Difference & Date Math", Category.DATETIME, "Days between dates, add/subtract durations",
        mp("Difference") { a, p -> Dt.diff(a, p.getOrElse(0) { "" }) }, mp("Add / subtract") { a, p -> Dt.math(a, p.getOrElse(0) { "" }) },
        hint = "Date A (blank = now)", params = listOf("Date B  /  or  expression like +3d -2h +1M"), sample = "2026-01-01", psample = listOf("2026-12-25")),
    tool("date-format", "Date Format Tester", Category.DATETIME, "Java/Kotlin, strftime (C/Python/PHP/Ruby) and Go layouts",
        mp("Java pattern") { s, p -> Instant.parse(Instant.now().toString()).let { _ -> val z = Dt.parse(p.getOrElse(0) { "" }).atZone(ZoneId.systemDefault()); z.format(DateTimeFormatter.ofPattern(s)) } },
        mp("strftime") { s, p -> Dt.strftime(s, Dt.parse(p.getOrElse(0) { "" }).atZone(ZoneId.systemDefault())) },
        mp("Go layout") { s, p -> Dt.parse(p.getOrElse(0) { "" }).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(Dt.goLayout(s))) },
        hint = "Format, e.g. yyyy-MM-dd HH:mm | %Y-%m-%d | 2006-01-02 15:04", params = listOf("Date (blank = now)"), sample = "yyyy-MM-dd'T'HH:mm:ssXXX", psample = listOf("")),
    tool("cron", "Cron Explainer & Next Runs", Category.DATETIME, "5-field cron: explain and list upcoming runs",
        m("Explain + next 8 runs") { s -> Cron.explain(s) + "\n\nNext runs (device time):\n" + Cron.next(s, 8).joinToString("\n") { it.format(DateTimeFormatter.ofPattern("EEE yyyy-MM-dd HH:mm")) } },
        m("Common examples") { "*/5 * * * *      every 5 minutes\n0 * * * *        hourly\n0 9 * * 1-5     weekdays 09:00\n0 0 * * *        daily midnight\n30 2 1 * *      02:30 on the 1st\n0 0 * * 0        weekly Sunday\n0 8,18 * * *     08:00 and 18:00\n0 0 1 1 *        yearly" },
        hint = "Cron expression", sample = "*/15 9-17 * * MON-FRI", kw = "crontab schedule"),
)
