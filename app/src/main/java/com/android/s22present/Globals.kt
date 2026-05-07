package com.android.s22present

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.RemoteException
import android.view.SurfaceControl
import android.view.SurfaceView
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.chibde.visualizer.BarVisualizer
import com.chibde.visualizer.LineBarVisualizer
import com.chibde.visualizer.SquareBarVisualizer
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
        var rootMessenger: Messenger? = null
        var overlayFont: Typeface? = null

        private val wakeTimeoutValues = longArrayOf(3000, 5000, 10000, 15000, 30000)
        const val DEFAULT_WAKE_TIMEOUT_INDEX = 0

        fun wakeTimeoutMsForIndex(index: Int): Long =
            wakeTimeoutValues.getOrElse(index) { wakeTimeoutValues[DEFAULT_WAKE_TIMEOUT_INDEX] }

        fun sendOverlayBitmap() {
            val messenger = rootMessenger ?: return
            try {
                val bitmap = renderOverlayBitmap()
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val bytes = stream.toByteArray()
                bitmap.recycle()

                val msg = Message.obtain(null, ScreenService.CMD_UPDATE_BITMAP, 0, 0)
                msg.data = Bundle().apply {
                    putByteArray("bitmap", bytes)
                }
                messenger.send(msg)
            } catch (e: RemoteException) {
                rootMessenger = null
            }
        }

        private fun renderOverlayBitmap(): Bitmap {
            val width = 128
            val height = 128
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            var bgColor = Color.BLACK
            var textColor = Color.WHITE
            val typeface = overlayFont ?: Typeface.DEFAULT

            when (style) {
                "1" -> {
                    bgColor = Color.parseColor("#093c6c")
                    textColor = Color.parseColor("#052745")
                }
                "2" -> {
                    textColor = Color.parseColor("#cc0000")
                }
            }
            canvas.drawColor(bgColor)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                this.typeface = typeface
                textAlign = Paint.Align.CENTER
            }

            val centerX = width / 2f

            val time = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))
            paint.textSize = 16f
            canvas.drawText(time, centerX, 38f, paint)

            val date = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
            paint.textSize = 12f
            canvas.drawText(date, centerX, 56f, paint)

            val title = titlefield.text?.toString() ?: ""
            if (title.isNotEmpty()) {
                paint.textSize = 11f
                paint.isFakeBoldText = true
                val maxWidth = width - 20f
                val ellipsized = ellipsize(title, paint, maxWidth)
                canvas.drawText(ellipsized, centerX, 78f, paint)
                paint.isFakeBoldText = false
            }

            val content = contentfield.text?.toString() ?: ""
            if (content.isNotEmpty()) {
                paint.textSize = 9f
                val maxWidth = width - 20f
                val lines = wrapText(content, paint, maxWidth, 3)
                var y = 92f
                for (line in lines) {
                    canvas.drawText(line, centerX, y, paint)
                    y += 12f
                }
            }

            return bitmap
        }

        private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
            if (paint.measureText(text) <= maxWidth) return text
            for (i in text.length - 1 downTo 1) {
                val truncated = text.substring(0, i) + "\u2026"
                if (paint.measureText(truncated) <= maxWidth) return truncated
            }
            return "\u2026"
        }

        private fun wrapText(text: String, paint: Paint, maxWidth: Float, maxLines: Int): List<String> {
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var current = ""
            for (word in words) {
                val test = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(test) <= maxWidth) {
                    current = test
                } else {
                    if (current.isNotEmpty()) {
                        lines.add(current)
                        if (lines.size >= maxLines) {
                            lines[lines.size - 1] = ellipsize(lines.last(), paint, maxWidth)
                            return lines
                        }
                    }
                    current = word
                }
            }
            if (current.isNotEmpty() && lines.size < maxLines) {
                lines.add(current)
            }
            return lines
        }
    }
}