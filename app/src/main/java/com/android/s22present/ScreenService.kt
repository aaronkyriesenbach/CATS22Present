package com.android.s22present

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.view.SurfaceControl
import com.topjohnwu.superuser.ipc.RootService

// A service that performs screen changes based on the requests of ListenerService. This service has root access and limited context abilities.
class ScreenService : RootService()
{
    // Create a way for ListenerService to communicate.
    private lateinit var mMessenger: Messenger
    // Manage incoming messages.
    internal class IncomingHandler(
        context: Context,
    ) : Handler() {
        override fun handleMessage(msg: Message) {
            try {
                val setDisplayPowerMode = SurfaceControl::class.java.getMethod(
                    "setDisplayPowerMode", IBinder::class.java, Int::class.java
                )
                val powerModeOff = 0
                when (msg.what) {
                    3 -> {
                        Runtime.getRuntime().exec("input keyevent KEYCODE_WAKEUP")
                        Log.v("S22PresScreenServ", "Wakeup!")
                    }
                    2 -> {
                        setDisplayPowerMode.invoke(null, Globals.token1 as IBinder?, powerModeOff)
                        Log.v("S22PresScreenServ", "Turning off!")
                    }
                    1 -> {
                        setDisplayPowerMode.invoke(null, Globals.token as IBinder?, powerModeOff)
                        Log.v("S22PresScreenServ", "Turning off!")
                    }
                    // If the message isn't recognised.
                    else -> {
                        Log.e("S22PresScreenServ", "I wasn't told anything meaningful... ${msg.what}")
                    }
                }
            } catch (e: ReflectiveOperationException) {
                Log.e("S22PresScreenServ", "SurfaceControl operation failed — root or privileged access may be missing", e)
            } catch (e: SecurityException) {
                Log.e("S22PresScreenServ", "Permission denied for SurfaceControl operation", e)
            }
        }
    }
    override fun onBind(intent: Intent): IBinder
    {
        // When service is bound.
        // Log the change and send the messenger back to the ListenerService.
        Log.i("S22PresScreenServInit", "Hello! I've been bound :3")
        mMessenger = Messenger(IncomingHandler(this))
        return mMessenger.binder

    }
}




