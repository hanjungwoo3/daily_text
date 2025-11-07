package com.example.daily_text

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

/**
 * ListView 위젯을 위한 RemoteViewsService
 */
class DailyTextWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return DailyTextRemoteViewsFactory(this.applicationContext, intent)
    }
}

/**
 * ListView 항목을 생성하는 Factory
 */
class DailyTextRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId: Int = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    private var dateStr: String = ""
    private var titleLine: String = ""
    private var bodyWithItalic: String = ""
    private var readingDay: String = ""
    private var readingContent: String = ""
    private var jwUrl: String = ""
    private var spreadsheetUrl: String = "https://docs.google.com/spreadsheets/d/1kCUN3Jsh9b1Y1_rGfFsT7vVjj08atzdwfPuQxs08SnI"
    private var readingRangeUrl: String = ""

    companion object {
        private const val PREFS_NAME = "DailyTextWidgetPrefs"
        private const val PREF_PREFIX_KEY = "date_"
        private const val TAG = "DailyTextWidget"

        const val ITEM_TYPE_TITLE = 0
        const val ITEM_TYPE_READING = 1
        const val ITEM_TYPE_BODY = 2
    }

    override fun onCreate() {
        Log.d(TAG, "RemoteViewsFactory onCreate for widget $appWidgetId")
        loadData()
    }

    override fun onDataSetChanged() {
        Log.d(TAG, "RemoteViewsFactory onDataSetChanged for widget $appWidgetId")
        loadData()
    }

    private fun loadData() {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, 0)
        dateStr = prefs.getString(PREF_PREFIX_KEY + appWidgetId, getTodayDate()) ?: getTodayDate()

        val (verseTitleRaw, verseReference, verseBodyRaw) = getVerseForDate(context, dateStr)
        titleLine = if (verseReference.isNotBlank()) "$verseTitleRaw $verseReference" else verseTitleRaw

        // body 내 (성구) 부분만 이탤릭+어두운 노랑(#FFB300) 처리
        bodyWithItalic = verseBodyRaw.replace(Regex("\\([^\\)]+\\)")) {
            "<i><font color=\"#FFB300\">${it.value}</font></i>"
        }

        // 성서 읽기 범위 설정
        val bibleReading = getBibleReadingForDate(context, dateStr)
        readingDay = if (bibleReading != null) "(${bibleReading.first}일차) " else ""
        readingContent = bibleReading?.second ?: ""

        // URL 설정
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = dateStr.substring(0, 2).toInt()
        val day = dateStr.substring(3, 5).toInt()
        jwUrl = "https://wol.jw.org/ko/wol/h/r8/lp-ko/$year/$month/$day"

        val encodedReadingRange = URLEncoder.encode(readingContent, "UTF-8")
        readingRangeUrl = "https://wol.jw.org/ko/wol/l/r8/lp-ko?q=$encodedReadingRange"

        Log.d(TAG, "Loaded data for date: $dateStr")
    }

    override fun onDestroy() {
        // 정리 작업
    }

    override fun getCount(): Int = 3 // 제목, 성서읽기, 본문

    override fun getViewAt(position: Int): RemoteViews {
        return when (position) {
            ITEM_TYPE_TITLE -> {
                val views = RemoteViews(context.packageName, R.layout.widget_list_item_title)
                views.setTextViewText(R.id.widget_title, titleLine)

                // 제목 클릭 시 JW.org 링크
                val fillInIntent = Intent().apply {
                    putExtra("url", jwUrl)
                }
                views.setOnClickFillInIntent(R.id.widget_title, fillInIntent)

                views
            }
            ITEM_TYPE_READING -> {
                val views = RemoteViews(context.packageName, R.layout.widget_list_item_reading)
                views.setTextViewText(R.id.widget_reading_day, readingDay)
                views.setTextViewText(R.id.widget_reading_content, readingContent)

                // 읽기 일자 클릭 시 스프레드시트
                val dayIntent = Intent().apply {
                    putExtra("url", spreadsheetUrl)
                }
                views.setOnClickFillInIntent(R.id.widget_reading_day, dayIntent)

                // 읽기 내용 클릭 시 JW.org 검색
                val contentIntent = Intent().apply {
                    putExtra("url", readingRangeUrl)
                }
                views.setOnClickFillInIntent(R.id.widget_reading_content, contentIntent)

                views
            }
            ITEM_TYPE_BODY -> {
                val views = RemoteViews(context.packageName, R.layout.widget_list_item_body)

                // 본문 설정 (클릭 이벤트 없음 - 읽기만)
                views.setTextViewText(R.id.widget_body, android.text.Html.fromHtml(bodyWithItalic))

                views
            }
            else -> RemoteViews(context.packageName, R.layout.widget_list_item_body)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 3

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true

    // Helper functions
    private fun getTodayDate(): String {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format("%02d-%02d", month, day)
    }

    private fun getJsonArray(context: Context): JSONArray? {
        return try {
            val assetManager = context.assets
            val inputStream: InputStream = assetManager.open("daily_verses.json")
            val jsonStr = inputStream.bufferedReader().use { it.readText() }
            JSONArray(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "JSON 파싱 오류", e)
            null
        }
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

    private fun getBibleReadingForDate(context: Context, dateStr: String): Pair<Int, String>? {
        return try {
            val assetManager = context.assets
            val inputStream: InputStream = assetManager.open("bible_reading_schedule.json")
            val jsonStr = inputStream.bufferedReader().use { it.readText() }
            val jsonObj = JSONObject(jsonStr)

            if (jsonObj.has(dateStr)) {
                val readingObj = jsonObj.getJSONObject(dateStr)
                val day = readingObj.getInt("day")
                val reading = readingObj.getString("reading")
                Pair(day, reading)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "성서 읽기 일정 JSON 파싱 오류", e)
            null
        }
    }
}