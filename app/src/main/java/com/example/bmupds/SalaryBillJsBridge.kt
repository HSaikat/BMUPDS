package com.example.bmupds

import android.content.Context
import android.webkit.JavascriptInterface

class SalaryBillJsBridge(private val context: Context) {

    // Called by JS when it detects a salary row exists on the list page
    // — meaning the bill has been submitted for this month.
    @JavascriptInterface
    fun onBillDetected() {
        SalaryReminderWorker.markSubmitted(context)
    }
}
