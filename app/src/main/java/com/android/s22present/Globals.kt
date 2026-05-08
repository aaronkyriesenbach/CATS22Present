package com.android.s22present

import android.annotation.SuppressLint
import android.content.Context
import android.os.IBinder
import android.util.Log
import android.view.SurfaceControl
import android.widget.TextView
import com.chibde.visualizer.BarVisualizer
import com.chibde.visualizer.SquareBarVisualizer
import java.io.File

class Globals
{
    @SuppressLint("StaticFieldLeak")
    companion object
    {
        val sfids: LongArray by lazy {
            try {
                SurfaceControl::class.java.getMethod("getPhysicalDisplayIds").invoke(null) as LongArray
            } catch (e: ReflectiveOperationException) {
                Log.e("S22PresGlobals", "Failed to get physical display IDs", e)
                longArrayOf()
            }
        }
        val token: Any? by lazy {
            try {
                if (sfids.size > 1) {
                    SurfaceControl::class.java.getMethod("getPhysicalDisplayToken", Long::class.java)
                        .invoke(null, sfids[1])
                } else {
                    Log.w("S22PresGlobals", "No secondary display ID available for token")
                    null
                }
            } catch (e: ReflectiveOperationException) {
                Log.e("S22PresGlobals", "Failed to get secondary display token", e)
                null
            }
        }
        val token1: Any? by lazy {
            try {
                if (sfids.isNotEmpty()) {
                    SurfaceControl::class.java.getMethod("getPhysicalDisplayToken", Long::class.java)
                        .invoke(null, sfids[0])
                } else {
                    Log.w("S22PresGlobals", "No primary display ID available for token1")
                    null
                }
            } catch (e: ReflectiveOperationException) {
                Log.e("S22PresGlobals", "Failed to get primary display token", e)
                null
            }
        }
        lateinit var datefield : TextView
        lateinit var titlefield : TextView
        var statusText: TextView? = null
        lateinit var timefield : TextView
        lateinit var contentfield : TextView
        var visual : Int = 0
        lateinit var visualbar: BarVisualizer
        lateinit var visualsquare: SquareBarVisualizer
        var style = 0
        var font = 0
        var wakeTimeoutMs = 3000L
        var rootAvailable = false
        var onRootStatusChanged: (() -> Unit)? = null

        // SharedPreferences group names
        const val PREFS_APP = "s22present_settings"
        const val PREFS_MODULE = "s22present_module"

        // SharedPreferences keys
        const val KEY_STYLE = "style"
        const val KEY_FONT = "font"
        const val KEY_WAKE_TIMEOUT_INDEX = "wake_timeout_index"
        const val KEY_DEBOUNCE_MS = "debounce_ms"

        // Defaults
        const val DEFAULT_STYLE = 0
        const val DEFAULT_FONT = 0
        const val DEFAULT_DEBOUNCE_MS = 100

        private val wakeTimeoutValues = longArrayOf(3000, 5000, 10000, 15000, 30000)
        const val DEFAULT_WAKE_TIMEOUT_INDEX = 0

        fun wakeTimeoutMsForIndex(index: Int): Long =
            wakeTimeoutValues.getOrElse(index) { wakeTimeoutValues[DEFAULT_WAKE_TIMEOUT_INDEX] }

        private val debounceValues = intArrayOf(0, 50, 75, 100, 125, 150, 200)
        const val DEFAULT_DEBOUNCE_INDEX = 3 // 100ms

        fun debounceMsForIndex(index: Int): Int =
            debounceValues.getOrElse(index) { debounceValues[DEFAULT_DEBOUNCE_INDEX] }

        fun debounceIndexForMs(ms: Int): Int =
            debounceValues.indexOf(ms).takeIf { it >= 0 } ?: DEFAULT_DEBOUNCE_INDEX

        fun migrateSettingsIfNeeded(context: Context) {
            val oldFile = File(context.filesDir, "settings")
            if (!oldFile.exists()) return

            try {
                val parts = oldFile.readText().split("|")
                val appPrefs = context.getSharedPreferences(PREFS_APP, Context.MODE_PRIVATE)
                appPrefs.edit()
                    .putInt(KEY_STYLE, parts.getOrNull(0)?.toIntOrNull() ?: DEFAULT_STYLE)
                    .putInt(KEY_FONT, parts.getOrNull(1)?.toIntOrNull() ?: DEFAULT_FONT)
                    .putInt(KEY_WAKE_TIMEOUT_INDEX, parts.getOrNull(2)?.toIntOrNull() ?: DEFAULT_WAKE_TIMEOUT_INDEX)
                    .apply()
                oldFile.delete()
                Log.i("S22PresMigrate", "Settings migrated from flat file to SharedPreferences")
            } catch (e: Exception) {
                Log.e("S22PresMigrate", "Migration failed, using defaults", e)
                oldFile.delete()
            }
        }
    }
}
