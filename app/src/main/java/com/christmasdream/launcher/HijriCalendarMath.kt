package com.christmasdream.launcher

import kotlin.math.min

/**
 * Offline tabular Islamic (civil) calendar.
 * Dates can differ by one day from local moon-sighting announcements.
 */
object HijriCalendarMath {
    data class Date(val year: Int, val month: Int, val day: Int)
    data class GregorianDate(val year: Int, val month: Int, val day: Int)

    val monthNames = listOf(
        "Мухаррам", "Сафар", "Раби аль-авваль", "Раби ас-сани",
        "Джумада аль-уля", "Джумада ас-сания", "Раджаб", "Шаабан",
        "Рамадан", "Шавваль", "Зуль-каада", "Зуль-хиджа"
    )

    fun fromGregorian(year: Int, month: Int, day: Int): Date {
        val jd = gregorianToJdn(year, month, day)
        val hijriYear = Math.floorDiv(30 * (jd - 1948440) + 10646, 10631)
        var hijriMonth = 1
        while (hijriMonth < 12 && islamicToJdn(hijriYear, hijriMonth + 1, 1) <= jd) {
            hijriMonth++
        }
        val hijriDay = jd - islamicToJdn(hijriYear, hijriMonth, 1) + 1
        return Date(hijriYear, hijriMonth, hijriDay)
    }

    fun monthLength(year: Int, month: Int): Int =
        islamicToJdn(year, month + 1, 1) - islamicToJdn(year, month, 1)

    fun toGregorian(year: Int, month: Int, day: Int, adjustmentDays: Int = 0): GregorianDate =
        jdnToGregorian(islamicToJdn(year, month, day) - adjustmentDays)

    private fun islamicToJdn(year: Int, month: Int, day: Int): Int {
        val elapsedMonths = month - 1
        val daysBeforeMonth = (59 * elapsedMonths + 1) / 2
        return day + daysBeforeMonth + 354 * (year - 1) +
            (3 + 11 * year) / 30 + 1948439
    }

    private fun gregorianToJdn(year: Int, month: Int, day: Int): Int {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    }

    private fun jdnToGregorian(jdn: Int): GregorianDate {
        var l = jdn + 68569
        val n = 4 * l / 146097
        l -= (146097 * n + 3) / 4
        val i = 4000 * (l + 1) / 1461001
        l = l - 1461 * i / 4 + 31
        val j = 80 * l / 2447
        val day = l - 2447 * j / 80
        l = j / 11
        val month = j + 2 - 12 * l
        val year = 100 * (n - 49) + i + l
        return GregorianDate(year, month, day)
    }
}
