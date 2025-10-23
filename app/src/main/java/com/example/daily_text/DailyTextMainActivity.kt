package com.example.daily_text

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DailyTextMainActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "DailyTextMainActivity"
    }
    
    private lateinit var updateButton: Button
    private lateinit var versionText: TextView
    
    private val githubRepo = "hanjungwoo3/daily_text"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        updateButton = findViewById(R.id.update_button)
        versionText = findViewById(R.id.version_text)
        
        // 현재 버전 표시
        val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName
        versionText.text = "버전 $currentVersion"
        
        // 업데이트 버튼 클릭 리스너 - 브라우저로 GitHub Release 페이지 열기
        updateButton.setOnClickListener {
            openGitHubReleasePage()
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
}
