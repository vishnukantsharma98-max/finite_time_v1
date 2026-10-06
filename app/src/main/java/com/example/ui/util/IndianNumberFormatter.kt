package com.example.ui.util

/**
 * Reusable Indian number grouping formatter:
 * - < 1,000: no grouping (e.g. 633 -> "633", 70 -> "70")
 * - 1,000 to 99,999: last 3 digits grouped (e.g. 1000 -> "1,000", 2450 -> "2,450", 10000 -> "10,000")
 * - >= 1,00,000: first group of 3 from right, then groups of 2 (e.g. 100000 -> "1,00,000", 1222333 -> "12,22,333", 12345678 -> "1,23,45,678")
 */
object IndianNumberFormatter {

  fun format(value: Long): String {
    if (value < 0) return "-" + format(-value)
    val s = value.toString()
    if (s.length <= 3) return s
    val lastThree = s.substring(s.length - 3)
    val rest = s.substring(0, s.length - 3)
    val sb = StringBuilder()
    var i = rest.length
    while (i > 0) {
      val start = (i - 2).coerceAtLeast(0)
      if (sb.isNotEmpty()) sb.insert(0, ",")
      sb.insert(0, rest.substring(start, i))
      i -= 2
    }
    return "$sb,$lastThree"
  }

  fun format(value: Int): String = format(value.toLong())
}
