package com.android.s22present

import android.annotation.SuppressLint
import android.os.IBinder
import android.view.SurfaceControl
import android.widget.TextView
import com.chibde.visualizer.BarVisualizer
import com.chibde.visualizer.SquareBarVisualizer

class Globals
{
    @SuppressLint("StaticFieldLeak")
    companion object
    {
        val sfids = SurfaceControl::class.java.getMethod("getPhysicalDisplayIds").invoke(null) as LongArray
        val token = SurfaceControl::class.java.getMethod("getPhysicalDisplayToken", Long::class.java).invoke(null, sfids[1])
        val token1 = SurfaceControl::class.java.getMethod("getPhysicalDisplayToken", Long::class.java).invoke(null, sfids[0])
        lateinit var datefield : TextView
        lateinit var titlefield : TextView
        var statusText: TextView? = null
        lateinit var timefield : TextView
        lateinit var contentfield : TextView
        var visual : Int = 0
        lateinit var visualbar: BarVisualizer
        lateinit var visualsquare: SquareBarVisualizer
        var style = "0"
        var font = "0"
        var wakeTimeoutMs = 3000L
        var rootAvailable = false
        var onRootStatusChanged: (() -> Unit)? = null

        private val wakeTimeoutValues = longArrayOf(3000, 5000, 10000, 15000, 30000)
        const val DEFAULT_WAKE_TIMEOUT_INDEX = 0

        fun wakeTimeoutMsForIndex(index: Int): Long =
            wakeTimeoutValues.getOrElse(index) { wakeTimeoutValues[DEFAULT_WAKE_TIMEOUT_INDEX] }
    }
}
