import re

def localize_summary_screen():
    path = 'app/src/main/java/com/example/ui/screens/SummaryScreen.kt'
    with open(path, 'r', encoding='utf-8') as f:
        code = f.read()

    replacements = [
        ('text = "Kalori Detayı"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.calorie_detail_title)'),
        ('text = "Günlük kalori dengesi ve öğün dağılımı"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.calorie_detail_sub)'),
        ('text = "Bu gün için kayıt yok."', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.no_records_for_day)'),
        ('Text(text = "Alınan", fontSize = 11.sp, color = SubLabelColor)', 'Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.calories_consumed), fontSize = 11.sp, color = SubLabelColor)'),
        ('Text(text = "kcal aşıldı", fontSize = 10.sp, color = BentoOverRed)', 'Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.kcal_exceeded_format), fontSize = 10.sp, color = BentoOverRed)'),
        ('"Bugün hedefinize dengeli bir tempoyla yaklaşıyorsunuz. Akşam veya ara öğünleriniz için $caloriesRemaining kcal payınız var."', 'androidx.compose.ui.res.stringResource(com.example.R.string.calorie_status_balanced_format, caloriesRemaining)'),
        ('"Bugünkü kalori miktarı hedefin ${-caloriesRemaining} kcal üzerinde seyrediyor. Enerji dengesi haftalık süreçte şekillenir, yarın olağan ritminize devam edebilirsiniz."', 'androidx.compose.ui.res.stringResource(com.example.R.string.calorie_status_over_format, -caloriesRemaining)'),
        ('text = "Öğün Bazlı Kalori Dağılımı"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.meal_calorie_distribution)'),
        ('text = "Son 7 Günlük Ortalama"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.last_7_days_avg)'),
        ('text = "Haftalık tüketim eğilimi"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.weekly_trend)'),
        ('text = "En Yüksek Kalorili Yiyecek"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.highest_calorie_food)'),
        ('text = "Günlük\'ü Aç"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.open_journal)'),
        ('text = "$macroName Detayı"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.macro_detail_title_format, macroName)'),
        ('text = "Günlük $macroName dengesi ve katkı yapan kaynaklar"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.macro_detail_subtitle_format, macroName)'),
        ('Text(text = "günlük hedef", fontSize = 10.sp, color = SubLabelColor)', 'Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.daily_target_sub), fontSize = 10.sp, color = SubLabelColor)'),
        ('Text(text = "Ulaşıldı", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)', 'Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.target_reached), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)'),
        ('Text(text = "hedef tamamlandı", fontSize = 10.sp, color = SubLabelColor)', 'Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.target_completed), fontSize = 10.sp, color = SubLabelColor)'),
        ('text = "Hedef İlerlemesi"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.target_progress)'),
        ('"Günlük $macroName ihtiyacınızın %$percentage kadarı tamamlandı, hedefinize ${remaining.toInt()}g kaldı."', 'androidx.compose.ui.res.stringResource(com.example.R.string.macro_remaining_desc_format, macroName, percentage, remaining.toInt())'),
        ('"Bugünkü $macroName hedefiniz dengeli bir şekilde tamamlandı."', 'androidx.compose.ui.res.stringResource(com.example.R.string.macro_completed_desc_format, macroName)'),
        ('text = "Bugün En Çok Katkı Yapan Yiyecekler"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.top_contributing_foods)'),
        ('text = "Su Tüketimi Detayı"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.water_detail_title)'),
        ('text = "Bugünkü hidrasyon seviyesi ve su kayıtları"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.today_hydration_sub)'),
        ('text = "Bugün İçilen"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.today_drank)'),
        ('text = "$glassCount / $targetGlasses bardak (%$pct tamamlandı)"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.glasses_completed_format, glassCount, targetGlasses, pct)'),
        ('text = if (remaining > 0) "Hedefe $remaining ml kaldı" else "Tebrikler, günlük hedef tamamlandı!"', 'text = if (remaining > 0) androidx.compose.ui.res.stringResource(com.example.R.string.water_remaining_format, remaining) else androidx.compose.ui.res.stringResource(com.example.R.string.congrats_daily_target_done)'),
        ('text = "Hızlı Su Ekle"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.quick_add_water)'),
        ('text = "Özel"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.custom_amount_short)'),
        ('text = "Son 7 Günün Su Grafiği"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.water_chart_7days)'),
        ('text = "Haftalık Alım"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.weekly_intake)'),
        ('text = "Bugünkü Su Kayıtları (${todayWaterLogs.size})"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.today_water_logs_format, todayWaterLogs.size)'),
        ('text = "Su kaydı"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.water_log_item)'),
        ('contentDescription = "Kaydı sil"', 'contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.delete_log)'),
        ('title = { Text("Özel Su Miktarı", fontWeight = FontWeight.Bold) }', 'title = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.custom_water_amount_title), fontWeight = FontWeight.Bold) }'),
        ('Text("İçtiğiniz su miktarını ml olarak girin:")', 'Text(androidx.compose.ui.res.stringResource(com.example.R.string.enter_water_ml_short))'),
        ('placeholder = { Text("ör. 250") }', 'placeholder = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.placeholder_water_amount)) }'),
        ('Text("Vazgeç")', 'Text(androidx.compose.ui.res.stringResource(com.example.R.string.cancel))'),
        ('text = "Kilo & Gelişim Analizi"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.weight_and_progress_analysis)'),
        ('text = "Ağırlık geçmişi ve hedef ilerlemesi"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.weight_history_and_progress)'),
        ('Text(text = "Başlangıç", fontSize = 11.sp, color = SubLabelColor)', 'Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.starting_label), fontSize = 11.sp, color = SubLabelColor)'),
        ('Text(text = if (selectedLog != null) "Kayıtlı Kilo" else "Şu Anki", fontSize = 11.sp, color = SubLabelColor)', 'Text(text = if (selectedLog != null) androidx.compose.ui.res.stringResource(com.example.R.string.registered_weight) else androidx.compose.ui.res.stringResource(com.example.R.string.current_label), fontSize = 11.sp, color = SubLabelColor)'),
        ('Text(text = "Önceki kayda göre", fontSize = 12.sp, color = SubLabelColor)', 'Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.compared_to_prev), fontSize = 12.sp, color = SubLabelColor)'),
        ('Text(text = "Başlangıca göre", fontSize = 12.sp, color = SubLabelColor)', 'Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.compared_to_start), fontSize = 12.sp, color = SubLabelColor)'),
        ('Text(text = "Vücut Kitle İndeksi (BMI)", fontSize = 12.sp, color = SubLabelColor)', 'Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.bmi_label), fontSize = 12.sp, color = SubLabelColor)'),
        ('text = "Son 30 Günün Çizgi Grafiği"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.last_30_days_chart)'),
        ('text = "Henüz kilo kaydı bulunmuyor. Yeni kilonuzu aşağıdan ekleyin."', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.no_weight_logs_yet)'),
        ('text = "Hızlı Tartı Kaydı"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.quick_weight_log)'),
        ('text = "Yeni tartı değerinizi girin (Özet ve kalori hedefiniz anında güncellenir):"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.enter_new_weight_prompt)'),
        ('text = "Kilo başarıyla kaydedildi! Özet ve kalori hedefiniz güncellendi."', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.weight_saved_success)'),
        ('text = "İlerleme\'yi Aç"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.open_progress)'),
        ('text = "$currentType Öğünü"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.meal_detail_title_format, currentType)'),
        ('text = "Eklenen yiyecekler ve besin değerleri"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.meal_detail_sub)'),
        ('Text(text = "Öğün Toplamı", fontSize = 12.sp, color = SubLabelColor)', 'Text(text = androidx.compose.ui.res.stringResource(com.example.R.string.meal_total), fontSize = 12.sp, color = SubLabelColor)'),
        ('contentDescription = "Düzenle"', 'contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.edit)'),
        ('text = "Bu öğün için henüz yiyecek eklenmedi."', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.no_foods_in_meal)'),
        ('Text("Günlük Sayfasında Görüntüle", fontSize = 13.sp)', 'Text(androidx.compose.ui.res.stringResource(com.example.R.string.view_in_journal), fontSize = 13.sp)'),
        ('text = "Kayıt Serisi & Rozetler"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.streak_badges_detail_title)'),
        ('text = "İstikrar takibi ve kazanılan başarılar"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.streak_badges_detail_sub)'),
        ('text = "$streakCount Gün"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.days_count_format, streakCount)'),
        ('text = "$maxStreak Gün"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.days_count_format, maxStreak)'),
        ('text = "Bu Haftanın Takibi"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.this_week_tracking)'),
        ('val days = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")', 'val days = listOf(androidx.compose.ui.res.stringResource(com.example.R.string.day_mon), androidx.compose.ui.res.stringResource(com.example.R.string.day_tue), androidx.compose.ui.res.stringResource(com.example.R.string.day_wed), androidx.compose.ui.res.stringResource(com.example.R.string.day_thu), androidx.compose.ui.res.stringResource(com.example.R.string.day_fri), androidx.compose.ui.res.stringResource(com.example.R.string.day_sat), androidx.compose.ui.res.stringResource(com.example.R.string.day_sun))'),
        ('contentDescription = "Kayıt var"', 'contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.has_log)'),
        ('text = "Sıradaki Rozet Hedefi"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.next_badge_goal)'),
        ('text = "${nextMilestone.first} günlük kesintisiz kayıt rozeti"', 'text = androidx.compose.ui.res.stringResource(com.example.R.string.streak_badge_desc_format, nextMilestone.first)'),
        ('text = if (daysRemainingForNext > 0) "$daysRemainingForNext gün kaldı" else "Kazanıldı!"', 'text = if (daysRemainingForNext > 0) androidx.compose.ui.res.stringResource(com.example.R.string.days_remaining_format, daysRemainingForNext) else androidx.compose.ui.res.stringResource(com.example.R.string.achieved)'),
        ('"Her gün su veya öğün kaydetmeye devam ederek serini koru, ${daysRemainingForNext} gün sonra \\"${nextMilestone.second}\\" rozetini kazan!"', 'androidx.compose.ui.res.stringResource(com.example.R.string.badge_streak_tip_format, daysRemainingForNext, nextMilestone.second)'),
        ('"Harikasın! Bu rozeti başarıyla kazandın, istikrarını devam ettirerek yeni zirvelere ulaşabilirsin."', 'androidx.compose.ui.res.stringResource(com.example.R.string.badge_streak_congrats)'),
    ]

    count = 0
    for old, new in replacements:
        if old in code:
            code = code.replace(old, new)
            count += 1
        else:
            print(f"Not found in SummaryScreen: {old[:40]}")

    with open(path, 'w', encoding='utf-8') as f:
        f.write(code)
    print(f"Localized SummaryScreen: {count} replacements done.")

localize_summary_screen()
