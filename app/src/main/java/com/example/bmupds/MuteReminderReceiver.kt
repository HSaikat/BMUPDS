package com.example.bmupds

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MuteReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SalaryReminderWorker.markMuted(context)
    }
}
