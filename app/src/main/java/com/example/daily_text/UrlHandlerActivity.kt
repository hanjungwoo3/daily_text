package com.example.daily_text

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * 위젯에서 URL을 열기 위한 중간 Activity
 * Android 14+에서 PendingIntent 제약을 우회하기 위해 사용
 */
class UrlHandlerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent에서 URL 추출
        val url = intent.getStringExtra("url")

        if (url != null) {
            // 브라우저로 URL 열기
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        }

        // Activity 즉시 종료
        finish()
    }
}
