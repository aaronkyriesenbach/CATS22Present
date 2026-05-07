package com.android.s22present

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.util.Log
import android.view.Gravity
import android.view.SurfaceControl
import android.view.View
import android.view.WindowManager
import com.topjohnwu.superuser.ipc.RootService
import java.io.FileOutputStream

class ScreenService : RootService()
{
    companion object {
        const val CMD_SECONDARY_OFF = 1
        const val CMD_MAIN_OFF = 2
        const val CMD_SECONDARY_WAKE = 3
        const val CMD_SHOW_OVERLAY = 4
        const val CMD_REMOVE_OVERLAY = 5
        const val CMD_UPDATE_BITMAP = 6
    }

    private lateinit var mMessenger: Messenger

    internal class IncomingHandler(
        private val context: Context,
    ) : Handler(Looper.getMainLooper()) {
        private var overlayView: View? = null
        private var windowManager: WindowManager? = null

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
                    CMD_SHOW_OVERLAY -> {
                        showKeyguardOverlay()
                    }
                    CMD_REMOVE_OVERLAY -> {
                        removeKeyguardOverlay()
                    }
                    CMD_UPDATE_BITMAP -> {
                        val bytes = msg.data?.getByteArray("bitmap")
                        if (bytes != null) {
                            updateOverlayBitmap(bytes)
                        }
                    }
                }
            } catch (e: ReflectiveOperationException) {
                Log.e("S22PresScreenServ", "SurfaceControl operation failed", e)
            } catch (e: SecurityException) {
                Log.e("S22PresScreenServ", "Permission denied", e)
            }
        }

        private fun updateOverlayBitmap(bytes: ByteArray) {
            val view = overlayView ?: return
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
            view.background = BitmapDrawable(context.resources, bitmap)
        }

        private fun showKeyguardOverlay() {
            // Clean up any stale overlay (system removes TYPE_KEYGUARD_DIALOG on keyguard dismiss)
            if (overlayView != null) {
                try {
                    windowManager?.removeView(overlayView)
                } catch (_: Exception) {}
                overlayView = null
                windowManager = null
            }
            try {
                val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
                val displays = dm.displays
                if (displays.size < 2) return
                val secondaryDisplay = displays[1]
                val windowContext = context.createWindowContext(
                    secondaryDisplay,
                    WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG,
                    null
                )
                val wm = windowContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager

                val view = View(windowContext)
                view.setBackgroundColor(Color.BLACK)

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        @Suppress("DEPRECATION")
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                    PixelFormat.OPAQUE
                )
                params.gravity = Gravity.FILL
                wm.addView(view, params)
                overlayView = view
                windowManager = wm
            } catch (e: Exception) {
                Log.e("S22PresScreenServ", "Failed to show keyguard overlay", e)
            }
        }

        private fun removeKeyguardOverlay() {
            val view = overlayView ?: return
            try {
                windowManager?.removeView(view)
                overlayView = null
                windowManager = null
            } catch (_: Exception) {}
        }
    }

    override fun onBind(intent: Intent): IBinder
    {
        Log.i("S22PresScreenServInit", "Hello! I've been bound :3")
        mMessenger = Messenger(IncomingHandler(this))
        return mMessenger.binder
    }
}
