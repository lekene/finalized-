package com.example.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Ortak Seri Hesaplayıcı:
 * - Bir günde en az bir öğün kaydı (hızlı kalori dahil) varsa o gün kayıt günüdür.
 * - Seri, bugünden geriye doğru kesintisiz kayıt günü sayısıdır.
 * - Bugün henüz kayıt yoksa seri dünden devam eder ve sıfırlanmaz.
 * - Eğer ne bugün ne de dün kayıt yoksa seri 0'dır.
 * - Özet rozeti, İlerleme kartı, seri detay penceresi, rozetler ve widget'lar bu ortak fonksiyonu kullanır.
 */
object StreakCalculator {
  private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

  fun calculateStreak(
    mealDates: Set<String>,
    today: LocalDate = LocalDate.now()
  ): Int {
    if (mealDates.isEmpty()) return 0
    val normalizedDates = mealDates.mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } }.toSet()
    if (normalizedDates.isEmpty()) return 0

    val todayStr = today.format(DATE_FORMATTER)
    val hasToday = normalizedDates.contains(todayStr)

    val startDate = if (hasToday) {
      today
    } else {
      val yesterday = today.minusDays(1)
      val yesterdayStr = yesterday.format(DATE_FORMATTER)
      if (normalizedDates.contains(yesterdayStr)) {
        yesterday
      } else {
        return 0
      }
    }

    var streak = 0
    var checkDate = startDate
    while (normalizedDates.contains(checkDate.format(DATE_FORMATTER))) {
      streak++
      checkDate = checkDate.minusDays(1)
    }

    return streak
  }

  fun calculateStreakFromMeals(
    meals: List<Meal>,
    today: LocalDate = LocalDate.now()
  ): Int {
    val dates = meals.mapNotNull { it.date.trim().takeIf { s -> s.isNotEmpty() } }.toSet()
    return calculateStreak(dates, today)
  }
}
