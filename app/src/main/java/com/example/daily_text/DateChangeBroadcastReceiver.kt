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
        Log.d(TAG, "Received broadcast: ${intent.action}")
        
        when (intent.action) {
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "com.example.daily_text.ACTION_UPDATE_DAILY" -> {
                // 모든 위젯 인스턴스 업데이트
                updateAllWidgets(context)
                
                // 다음 자정 알람 설정
                DailyTextWidgetProvider.scheduleMidnightUpdate(context)
            }
        }
    }
    
    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, DailyTextWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
        
        Log.d(TAG, "Updating ${appWidgetIds.size} widget(s)")
        
        for (appWidgetId in appWidgetIds) {
            DailyTextWidgetProvider.updateAppWidget(
                context,
                appWidgetManager,
                appWidgetId,
                dateStrParam = null // 오늘 날짜로 업데이트
            )
        }
    }
}
