package com.example.daily_text

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 날짜 변경 및 부팅 시 위젯을 업데이트하는 BroadcastReceiver
 */
class DateChangeBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "DateChangeReceiver"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "======================================")
        Log.d(TAG, "Received broadcast: $action")
        Log.d(TAG, "Time: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}")
        Log.d(TAG, "======================================")

        when (action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "com.example.daily_text.ACTION_UPDATE_DAILY" -> {
                // 모든 위젯 인스턴스 업데이트
                updateAllWidgets(context)

                // 다음 자정 알람 설정 (BOOT_COMPLETED나 업데이트 후에만)
                if (action == Intent.ACTION_BOOT_COMPLETED ||
                    action == Intent.ACTION_MY_PACKAGE_REPLACED ||
                    action == "com.example.daily_text.ACTION_UPDATE_DAILY") {
                    Log.d(TAG, "Rescheduling midnight alarm")
                    DailyTextWidgetProvider.scheduleMidnightUpdate(context)
                }
            }
        }
    }
    
    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, DailyTextWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        // 오늘 날짜 계산
        val calendar = android.icu.util.Calendar.getInstance()
        val month = calendar.get(android.icu.util.Calendar.MONTH) + 1
        val day = calendar.get(android.icu.util.Calendar.DAY_OF_MONTH)
        val todayDate = String.format("%02d-%02d", month, day)

        Log.d(TAG, "Updating ${appWidgetIds.size} widget(s) to date: $todayDate")

        for (appWidgetId in appWidgetIds) {
            Log.d(TAG, "Updating widget ID: $appWidgetId")
            DailyTextWidgetProvider.updateAppWidget(
                context,
                appWidgetManager,
                appWidgetId,
                dateStrParam = todayDate // 무조건 오늘 날짜로 업데이트
            )
        }

        Log.d(TAG, "All widgets updated successfully")
    }
}
