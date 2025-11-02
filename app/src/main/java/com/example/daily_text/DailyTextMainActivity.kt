package com.example.daily_text

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class DailyTextMainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "DailyTextMainActivity"
    }

    private lateinit var updateButton: Button
    private lateinit var versionText: TextView
    private lateinit var testDateChangeButton: Button

    private val githubRepo = "hanjungwoo3/daily_text"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        updateButton = findViewById(R.id.update_button)
        versionText = findViewById(R.id.version_text)
        testDateChangeButton = findViewById(R.id.test_date_change_button)

        // 현재 버전 표시
        val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName
        versionText.text = "버전 $currentVersion"

        // 업데이트 버튼 클릭 리스너 - 브라우저로 GitHub Release 페이지 열기
        updateButton.setOnClickListener {
            openGitHubReleasePage()
        }

        // 테스트 버튼: 날짜 변경 신호 보내기
        testDateChangeButton.setOnClickListener {
            sendDateChangeSignal()
        }

        // 알람 권한 체크 (Android 12 이상)
        checkAndRequestAlarmPermission()
    }

    override fun onResume() {
        super.onResume()
        // 설정에서 돌아왔을 때 권한 재확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager?.canScheduleExactAlarms() == true) {
                // 권한이 허용되었으면 알람 다시 설정
                DailyTextWidgetProvider.scheduleMidnightUpdate(this)
                Log.d(TAG, "Alarm permission granted, rescheduling midnight update")
            }
        }
    }

    private fun checkAndRequestAlarmPermission() {
        // Android 12(API 31) 이상에서만 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager?.canScheduleExactAlarms() == false) {
                showAlarmPermissionDialog()
            }
        }
    }

    private fun showAlarmPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("알람 권한 필요")
            .setMessage("위젯이 매일 0시에 자동으로 업데이트되려면 '알람 및 리마인더' 권한이 필요합니다.\n\n" +
                    "설정에서 이 앱의 권한을 허용해주세요.")
            .setPositiveButton("설정으로 이동") { _, _ ->
                openAlarmPermissionSettings()
            }
            .setNegativeButton("나중에", null)
            .show()
    }

    private fun openAlarmPermissionSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open alarm permission settings", e)
            Toast.makeText(this, "설정 화면을 열 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun openGitHubReleasePage() {
        try {
            val releaseUrl = "https://github.com/$githubRepo/releases/latest"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl))
            startActivity(intent)
            Toast.makeText(this, "브라우저에서 최신 APK를 다운로드하세요", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e(TAG, "브라우저 열기 실패", e)
            Toast.makeText(this, "브라우저를 열 수 없습니다", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 테스트 함수: 날짜 변경 신호를 수동으로 전송하여 위젯 업데이트 테스트
     */
    private fun sendDateChangeSignal() {
        try {
            Log.d(TAG, "Sending date change signal for testing...")

            // DateChangeBroadcastReceiver에 ACTION_UPDATE_DAILY 브로드캐스트 전송
            val intent = Intent(this, DateChangeBroadcastReceiver::class.java).apply {
                action = "com.example.daily_text.ACTION_UPDATE_DAILY"
            }
            sendBroadcast(intent)

            Log.d(TAG, "Date change signal sent successfully")
            Toast.makeText(this, "날짜 변경 신호 전송됨 - 위젯이 업데이트됩니다", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send date change signal", e)
            Toast.makeText(this, "신호 전송 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
