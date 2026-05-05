package com.android.s22present

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.util.Log
import org.lsposed.hiddenapibypass.HiddenApiBypass

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.i("S22PresBootRecv", "Boot completed, checking for secondary display...")

        // Required before Globals accesses SurfaceControl hidden APIs
        HiddenApiBypass.addHiddenApiExemptions("L")

        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val displays = displayManager.displays

        if (displays.size < 2 || displays[1] == null) {
            Log.w("S22PresBootRecv", "No secondary display found, not starting service.")
            return
        }

        Log.i("S22PresBootRecv", "Secondary display found, starting ListenerService.")
        val serviceIntent = Intent(context, ListenerService::class.java)
        context.startService(serviceIntent)
    }
}
