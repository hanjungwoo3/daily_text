package com.example.daily_text

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.RemoteViews
import org.json.JSONArray
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 매일의 성경 구절 위젯을 구현하는 클래스
 */
class DailyTextWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val PREFS_NAME = "DailyTextWidgetPrefs"
        private const val PREF_PREFIX_KEY = "date_"
        private const val ACTION_PREV = "com.example.daily_text.ACTION_PREV"
        private const val ACTION_NEXT = "com.example.daily_text.ACTION_NEXT"
        private const val ACTION_TODAY = "com.example.daily_text.ACTION_TODAY"

        private fun getSavedDate(context: Context, appWidgetId: Int): String? {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getString(PREF_PREFIX_KEY + appWidgetId, null)
        }

        private fun saveDate(context: Context, appWidgetId: Int, date: String) {
            val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, 0)
            prefs.edit().putString(PREF_PREFIX_KEY + appWidgetId, date).apply()
        }

        private fun getTodayDate(): String {
            val calendar = Calendar.getInstance()
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            return String.format("%02d-%02d", month, day)
        }

        private fun getDateLabel(dateStr: String): String {
            // dateStr: MM-DD
            val year = 2025 // 고정 연도
            val month = dateStr.substring(0, 2).toInt()
            val day = dateStr.substring(3, 5).toInt()
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, day)
            val sdf = SimpleDateFormat("yyyy년 M월 d일 E요일", Locale.KOREAN)
            return sdf.format(cal.time)
        }

        private fun getJsonArray(context: Context): JSONArray? {
            return try {
                val assetManager = context.assets
                val inputStream: InputStream = assetManager.open("daily_verses_2025.json")
                val jsonStr = inputStream.bufferedReader().use { it.readText() }
                JSONArray(jsonStr)
            } catch (e: Exception) {
                Log.e("Widget", "JSON 파싱 오류", e)
                null
            }
        }

        private fun getDateList(context: Context): List<String> {
            val arr = getJsonArray(context) ?: return emptyList()
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(obj.getString("date"))
            }
            return list
        }

        private fun getVerseForDate(context: Context, dateStr: String): Triple<String, String, String> {
            val arr = getJsonArray(context) ?: return Triple("일용할 성구를 불러올 수 없습니다.", "", "앱을 다시 실행해 주세요.")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("date") == dateStr) {
                    val title = obj.getString("title")
                    val reference = if (obj.has("reference")) obj.getString("reference") else ""
                    val body = obj.getString("body")
                    return Triple(title, reference, body)
                }
            }
            return Triple("일용할 성구를 불러올 수 없습니다.", "", "앱을 다시 실행해 주세요.")
        }

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            dateStrParam: String? = null
        ) {
            val dateList = getDateList(context)
            val today = getTodayDate()
            val savedDate = dateStrParam ?: getSavedDate(context, appWidgetId) ?: today
            val currentIdx = dateList.indexOf(savedDate).takeIf { it >= 0 } ?: dateList.indexOf(today).takeIf { it >= 0 } ?: 0
            val dateStr = dateList.getOrNull(currentIdx) ?: today
            saveDate(context, appWidgetId, dateStr)

            val (verseTitleRaw, verseReference, verseBodyRaw) = getVerseForDate(context, dateStr)
            val titleLine = if (verseReference.isNotBlank()) "$verseTitleRaw $verseReference" else verseTitleRaw
            val bodyWithLineBreaks = verseBodyRaw.replace("\r\n", "\n").replace("\r", "\n")
            val mergedText = titleLine + "\n" + bodyWithLineBreaks
            val dateLabel = getDateLabel(dateStr)

            val views = RemoteViews(context.packageName, R.layout.clock_widget)
            views.setTextViewText(R.id.widget_body, mergedText)
            views.setTextViewText(R.id.widget_date, dateLabel)

            // < 버튼
            val prevIntent = Intent(context, DailyTextWidgetProvider::class.java).apply {
                action = ACTION_PREV
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId * 10 + 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_prev_btn, prevPendingIntent)

            // > 버튼
            val nextIntent = Intent(context, DailyTextWidgetProvider::class.java).apply {
                action = ACTION_NEXT
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId * 10 + 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_next_btn, nextPendingIntent)

            // 날짜(TextView) 클릭 시 오늘 날짜로 이동
            val todayIntent = Intent(context, DailyTextWidgetProvider::class.java).apply {
                action = ACTION_TODAY
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val todayPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId * 10 + 5, todayIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_date, todayPendingIntent)

            // 링크 텍스트 및 URL 생성
            val (year, month, day) = listOf(2025, dateStr.substring(0,2).toInt(), dateStr.substring(3,5).toInt())
            val linkUrl = "https://wol.jw.org/ko/wol/h/r8/lp-ko/$year/$month/$day"
            val linkText = "날마다 성경을 검토함—2025"
            views.setTextViewText(R.id.widget_link, linkText)
            // 링크 클릭 시 브라우저로 이동
            val linkIntent = Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.parse(linkUrl) }
            val linkPendingIntent = PendingIntent.getActivity(
                context, appWidgetId * 10 + 6, linkIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_link, linkPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 각 위젯 인스턴스를 업데이트
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // 첫 번째 위젯이 생성될 때 호출
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        // 마지막 위젯이 제거될 때 호출
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        if (appWidgetId == -1) return
        val dateList = getDateList(context)
        val currentDate = getSavedDate(context, appWidgetId) ?: getTodayDate()
        val currentIdx = dateList.indexOf(currentDate).takeIf { it >= 0 } ?: 0
        val (verseTitleRaw, verseReference, verseBodyRaw) = getVerseForDate(context, currentDate)
        val titleLine = if (verseReference.isNotBlank()) "$verseTitleRaw $verseReference" else verseTitleRaw
        val bodyWithLineBreaks = verseBodyRaw.replace("\r\n", "\n").replace("\r", "\n")
        val mergedText = titleLine + "\n" + bodyWithLineBreaks
        when (intent.action) {
            ACTION_PREV -> {
                val prevIdx = if (currentIdx > 0) currentIdx - 1 else 0
                val prevDate = dateList.getOrNull(prevIdx) ?: currentDate
                updateAppWidget(context, appWidgetManager, appWidgetId, prevDate)
            }
            ACTION_NEXT -> {
                val nextIdx = if (currentIdx < dateList.size - 1) currentIdx + 1 else currentIdx
                val nextDate = dateList.getOrNull(nextIdx) ?: currentDate
                updateAppWidget(context, appWidgetManager, appWidgetId, nextDate)
            }
            ACTION_TODAY -> {
                val today = getTodayDate()
                updateAppWidget(context, appWidgetManager, appWidgetId, today)
            }
        }
    }
} 