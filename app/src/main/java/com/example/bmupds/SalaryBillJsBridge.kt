package com.example.bmupds

import android.content.Context
import android.webkit.JavascriptInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * JS Bridge for detecting salary bill submission.
 * Receives calls from the injected JavaScript in the salary list page.
 */
class SalaryBillJsBridge(private val context: Context) {

    @JavascriptInterface
    fun onBillSubmitted() {
        val prefs = context.getSharedPreferences("salary_prefs", Context.MODE_PRIVATE)
        
        // Mark the current month as submitted
        val currentMonth = SimpleDateFormat("MMM-yyyy", Locale.US).format(Date())
        prefs.edit().putBoolean("submitted_$currentMonth", true).apply()
    }
}
