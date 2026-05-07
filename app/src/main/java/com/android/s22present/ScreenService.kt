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

class ScreenService : RootService()
{
    private lateinit var mMessenger: Messenger

    internal class IncomingHandler(
        private val context: Context,
    ) : Handler(Looper.getMainLooper()) {
        private var overlayView: View? = null
        private var windowManager: WindowManager? = null

        override fun handleMessage(msg: Message) {
            try {
                val setDisplayPowerMode = SurfaceControl::class.java.getMethod(
                    "setDisplayPowerMode", IBinder::class.java, Int::class.java
                )
                val powerModeOff = 0
                when (msg.what) {
                    3 -> {
                        Runtime.getRuntime().exec("input keyevent KEYCODE_WAKEUP")
                    }
                    4 -> {
                        Runtime.getRuntime().exec("input keyevent KEYCODE_SLEEP")
                    }
                    2 -> {
                        setDisplayPowerMode.invoke(null, Globals.token1 as IBinder?, powerModeOff)
                    }
                    1 -> {
                        setDisplayPowerMode.invoke(null, Globals.token as IBinder?, powerModeOff)
                    }
                    6 -> {
                        showKeyguardOverlay()
                    }
                    7 -> {
                        removeKeyguardOverlay()
                    }
                    9 -> {
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
