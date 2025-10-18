package com.example.daily_text

import android.app.AlarmManager
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
        private const val ACTION_UPDATE_DAILY = "com.example.daily_text.ACTION_UPDATE_DAILY"
        private const val TAG = "DailyTextWidget"

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
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
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
                val inputStream: InputStream = assetManager.open("daily_verses.json")
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

        /**
         * 다음 자정에 위젯을 업데이트하도록 알람 설정
         */
        fun scheduleMidnightUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager not available")
                return
            }

            val intent = Intent(context, DateChangeBroadcastReceiver::class.java).apply {
                action = ACTION_UPDATE_DAILY
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // 다음 자정 시간 계산
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val triggerTime = calendar.timeInMillis
            Log.d(TAG, "Scheduling midnight update at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(triggerTime))}")

            // Android 6.0 이상에서도 정확한 시간에 실행되도록 설정
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to schedule exact alarm", e)
                // 정확한 알람 설정 실패 시 일반 알람으로 fallback
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }

        /**
         * 알람 취소
         */
        fun cancelMidnightUpdate(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val intent = Intent(context, DateChangeBroadcastReceiver::class.java).apply {
                action = ACTION_UPDATE_DAILY
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager?.cancel(pendingIntent)
            Log.d(TAG, "Midnight update alarm cancelled")
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
            val dateLabel = getDateLabel(dateStr)
            val views = RemoteViews(context.packageName, R.layout.daily_text_widget)
            // body 내 (성구) 부분만 이탤릭+어두운 노랑(#FFB300) 처리
            val bodyWithItalic = verseBodyRaw.replace(Regex("\\([^\\)]+\\)")) {
                "<i><font color=\"#FFB300\">${it.value}</font></i>"
            }
            views.setTextViewText(R.id.widget_title, titleLine)
            views.setTextViewText(R.id.widget_body, android.text.Html.fromHtml(bodyWithItalic))
            views.setTextViewText(R.id.widget_date, dateLabel)
            val prevIntent = Intent(context, DailyTextWidgetProvider::class.java).apply {
                action = ACTION_PREV
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val prevPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId * 10 + 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_prev_btn, prevPendingIntent)
            val nextIntent = Intent(context, DailyTextWidgetProvider::class.java).apply {
                action = ACTION_NEXT
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId * 10 + 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_next_btn, nextPendingIntent)
            val todayIntent = Intent(context, DailyTextWidgetProvider::class.java).apply {
                action = ACTION_TODAY
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val todayPendingIntent = PendingIntent.getBroadcast(
                context, appWidgetId * 10 + 5, todayIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_date, todayPendingIntent)
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = dateStr.substring(0,2).toInt()
            val day = dateStr.substring(3,5).toInt()
            val linkUrl = "https://wol.jw.org/ko/wol/h/r8/lp-ko/$year/$month/$day"
            val jwIntent = Intent(Intent.ACTION_VIEW).apply { data = android.net.Uri.parse(linkUrl) }
            val jwPendingIntent = PendingIntent.getActivity(
                context, appWidgetId * 10 + 6, jwIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, jwPendingIntent)
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
        
        // 자정 업데이트 알람 설정
        scheduleMidnightUpdate(context)
    }

    override fun onEnabled(context: Context) {
        // 첫 번째 위젯이 생성될 때 호출
        super.onEnabled(context)
        Log.d(TAG, "Widget enabled - scheduling midnight updates")
        scheduleMidnightUpdate(context)
    }

    override fun onDisabled(context: Context) {
        // 마지막 위젯이 제거될 때 호출
        super.onDisabled(context)
        Log.d(TAG, "Widget disabled - cancelling midnight updates")
        cancelMidnightUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        if (appWidgetId == -1) return
        when (intent.action) {
            ACTION_PREV -> {
                val dateList = getDateList(context)
                val currentDate = getSavedDate(context, appWidgetId) ?: getTodayDate()
                val currentIdx = dateList.indexOf(currentDate).takeIf { it >= 0 } ?: 0
                val prevDate = if (currentIdx > 0) dateList[currentIdx - 1] else currentDate
                updateAppWidget(context, appWidgetManager, appWidgetId, prevDate)
            }
            ACTION_NEXT -> {
                val dateList = getDateList(context)
                val currentDate = getSavedDate(context, appWidgetId) ?: getTodayDate()
                val currentIdx = dateList.indexOf(currentDate).takeIf { it >= 0 } ?: 0
                val nextDate = if (currentIdx < dateList.size - 1) dateList[currentIdx + 1] else currentDate
                updateAppWidget(context, appWidgetManager, appWidgetId, nextDate)
            }
            ACTION_TODAY -> {
                val today = getTodayDate()
                updateAppWidget(context, appWidgetManager, appWidgetId, today)
            }
            else -> {
                super.onReceive(context, intent)
            }
        }
    }
} 