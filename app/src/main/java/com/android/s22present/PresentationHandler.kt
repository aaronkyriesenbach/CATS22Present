package com.android.s22present

import android.animation.ObjectAnimator
import android.app.Presentation
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import com.chibde.visualizer.BarVisualizer
import com.chibde.visualizer.SquareBarVisualizer
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Manages the Presentation and it's contents.
class PresentationHandler(context: Context, display: Display?): Presentation(context,display)
{
    @Suppress("DEPRECATION") // FLAG_FULLSCREEN deprecated but no WindowInsetsController on Presentation (Dialog)
    override fun onCreate(savedInstanceState: Bundle?)
    {
        Log.i("S22PresHandlerInit", "Presentation start triggered")
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.presentation)
        window?.let { w ->
            w.setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            w.addFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
            w.setBackgroundDrawableResource(android.R.color.black)
        }
        // Get todays date and the "local" format (although im in the UK and this displays the month first!)
        val today = LocalDateTime.now()
        val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
        val localtoday = today.format(format)
        // Push the date to the presentation.
        Globals.datefield = findViewById(R.id.textView2)
        Globals.datefield.text = localtoday
        Globals.titlefield = findViewById(R.id.textViewTitle)
        Globals.timefield = findViewById(R.id.textClock)
        Globals.contentfield = findViewById(R.id.textViewContent)
        ObjectAnimator.ofFloat(Globals.titlefield, "translationY", 20f).apply { duration = 500; start() }
        ObjectAnimator.ofFloat(Globals.contentfield, "translationY", 20f).apply { duration = 500; start() }
        ObjectAnimator.ofFloat(Globals.datefield, "translationY", 0f).apply { duration = 5; start() }
        ObjectAnimator.ofFloat(Globals.timefield, "translationY", 0f).apply { duration = 5; start() }
        val visual: BarVisualizer = findViewById(R.id.visualizerBar)
        val visualSquare: SquareBarVisualizer = findViewById(R.id.visualizerSquare)
        val digifont = resources.getFont(R.font.digital7)
        val pixelfont = resources.getFont(R.font.dogica)
        Globals.visualbar = visual
        Globals.visualsquare = visualSquare

        val clockView = Globals.timefield
        val dateView = Globals.datefield
        val titleView = Globals.titlefield
        val contentView = Globals.contentfield

        fun digifontset()
        {
            clockView.typeface = digifont
            dateView.typeface = digifont
            titleView.typeface = digifont
            contentView.typeface = digifont
            clockView.textSize = 17f
            dateView.textSize = 17f
            titleView.textSize = 14f
            contentView.textSize = 10f
        }
        fun pixelfontset()
        {
            clockView.typeface = pixelfont
            dateView.typeface = pixelfont
            titleView.typeface = pixelfont
            contentView.typeface = pixelfont
            contentView.letterSpacing = -0.05f
            contentView.setLineSpacing(3f, 1f)
            clockView.letterSpacing = -0.05f
            dateView.letterSpacing = -0.05f
            titleView.letterSpacing = -0.05f
            clockView.textSize = 12f
            dateView.textSize = 12f
            titleView.textSize = 10f
            contentView.textSize = 9f
        }
        fun squarevis()
        {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.w("S22PresHandlerInit", "RECORD_AUDIO not granted, skipping visualizer")
                return
            }
            try {
                visualSquare.isEnabled = true
                visualSquare.isInvisible = true
                visualSquare.setPlayer(0)
                visualSquare.setDensity(12F)
                Globals.visual = 2
            } catch (e: SecurityException) {
                Log.e("S22PresHandlerInit", "SecurityException initializing visualizer", e)
            }
        }
        fun barvis()
        {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.w("S22PresHandlerInit", "RECORD_AUDIO not granted, skipping visualizer")
                return
            }
            try {
                visual.isEnabled = true
                visual.isInvisible = true
                visual.setPlayer(0)
                visual.setDensity(20F)
                Globals.visual = 1
            } catch (e: SecurityException) {
                Log.e("S22PresHandlerInit", "SecurityException initializing visualizer", e)
            }
        }
        if(Globals.style == 1)
        {
            pixelfontset()
            squarevis()
            val textColor = ContextCompat.getColor(context, R.color.style1_text)
            clockView.setTextColor(textColor)
            dateView.setTextColor(textColor)
            titleView.setTextColor(textColor)
            contentView.setTextColor(textColor)
            findViewById<View>(R.id.view).setBackgroundColor(ContextCompat.getColor(context, R.color.style1_background))
            visualSquare.setColor(ContextCompat.getColor(context, R.color.style1_visualizer))
        }

        if(Globals.style == 2)
        {
            digifontset()
            squarevis()
            val textColor = ContextCompat.getColor(context, R.color.style2_text)
            clockView.setTextColor(textColor)
            dateView.setTextColor(textColor)
            titleView.setTextColor(textColor)
            contentView.setTextColor(textColor)
            visualSquare.setColor(ContextCompat.getColor(context, R.color.style2_visualizer))
        }
        if(Globals.style == 0)
        {
            barvis()
            visual.setColor(Color.WHITE)
        }
        if(Globals.font == 1)
        {
            digifontset()
        }
        if(Globals.font == 2)
        {
            pixelfontset()
        }
        Log.i("S22PresHandlerInit", "Presentation displayed")
    }
}

