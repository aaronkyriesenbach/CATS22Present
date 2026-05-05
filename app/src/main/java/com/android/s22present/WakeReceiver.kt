package com.android.s22present

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class WakeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_WAKE_DISPLAY) {
            val serviceIntent = Intent(context, ListenerService::class.java)
            serviceIntent.action = ACTION_WAKE_DISPLAY
            context.startService(serviceIntent)
        }
    }

    companion object {
        const val ACTION_WAKE_DISPLAY = "com.android.s22present.WAKE_DISPLAY"
    }
}
