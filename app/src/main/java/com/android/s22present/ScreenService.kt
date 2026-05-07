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
import java.io.FileOutputStream

class ScreenService : RootService()
{
    companion object {
        const val CMD_SECONDARY_OFF = 1
        const val CMD_MAIN_OFF = 2
        const val CMD_SECONDARY_WAKE = 3
    }

    private lateinit var mMessenger: Messenger

    internal class IncomingHandler(
        private val context: Context,
    ) : Handler(Looper.getMainLooper()) {

        companion object {
            private const val MAIN_BACKLIGHT = "/sys/class/leds/lcd-backlight/brightness"
            private const val SECONDARY_BACKLIGHT = "/sys/class/leds/lcd-backlight2/brightness"
        }

        // Requires SELinux: allow magisk sysfs/sysfs_graphics file { open write }
        private fun writeBacklight(path: String, value: Int) {
            try {
                FileOutputStream(path).use { fos ->
                    fos.write(value.toString().toByteArray())
                }
            } catch (e: Exception) {
                Log.w("S22PresScreenServ", "Failed to write backlight $path=$value", e)
            }
        }

        override fun handleMessage(msg: Message) {
            try {
                val setDisplayPowerMode = SurfaceControl::class.java.getMethod(
                    "setDisplayPowerMode", IBinder::class.java, Int::class.java
                )
                val powerModeOff = 0
                val powerModeNormal = 2
                when (msg.what) {
                    CMD_SECONDARY_WAKE -> {
                        // Both displays share DisplayGroup 0, so KEYCODE_WAKEUP wakes both.
                        // Suppress main by racing sysfs backlight=0 against the wake event,
                        // then power off the main display controller after wake completes.
                        writeBacklight(MAIN_BACKLIGHT, 0)
                        Runtime.getRuntime().exec("input keyevent KEYCODE_WAKEUP")
                        writeBacklight(MAIN_BACKLIGHT, 0)
                        setDisplayPowerMode.invoke(
                            null, Globals.token as IBinder?, powerModeNormal
                        )
                        postDelayed({
                            writeBacklight(MAIN_BACKLIGHT, 0)
                            try {
                                setDisplayPowerMode.invoke(
                                    null, Globals.token1 as IBinder?, powerModeOff
                                )
                                setDisplayPowerMode.invoke(
                                    null, Globals.token as IBinder?, powerModeNormal
                                )
                            } catch (e: ReflectiveOperationException) {
                                Log.e(
                                    "S22PresScreenServ",
                                    "Failed to set display power after wake", e
                                )
                            }
                        }, 1000)
                        Log.v("S22PresScreenServ", "Secondary display wake")
                    }
                    CMD_SECONDARY_OFF -> {
                        setDisplayPowerMode.invoke(null, Globals.token as IBinder?, powerModeOff)
                        Log.v("S22PresScreenServ", "Secondary display off")
                    }
                    CMD_MAIN_OFF -> {
                        setDisplayPowerMode.invoke(null, Globals.token1 as IBinder?, powerModeOff)
                        Log.v("S22PresScreenServ", "Main display off")
                    }
                }
            } catch (e: ReflectiveOperationException) {
                Log.e("S22PresScreenServ", "SurfaceControl operation failed", e)
            } catch (e: SecurityException) {
                Log.e("S22PresScreenServ", "Permission denied", e)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder
    {
        Log.i("S22PresScreenServInit", "Hello! I've been bound :3")
        mMessenger = Messenger(IncomingHandler(this))
        return mMessenger.binder
    }
}
