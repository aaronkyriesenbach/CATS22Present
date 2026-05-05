package com.android.s22present

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.TextUtils.split
import android.util.Log
import android.view.SurfaceControl
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("S22PresSetting", "Hello!")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings))
        { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val file = "settings"
        val filedir = File(filesDir, file)
        val fontdefault = "0"
        val styledefault = "0"
        val wakeTimeoutDefault = Globals.DEFAULT_WAKE_TIMEOUT_INDEX.toString()
        var fontset = fontdefault
        var styleset = styledefault
        var wakeTimeoutSet = wakeTimeoutDefault
        lateinit var settings: Array<String>
        try {
            Log.i("S22PresSetting", "Looking for settings..")
            openFileInput(file)
        } catch (e: FileNotFoundException) {
            Log.w("S22PresSetting", "No settings present. Writing defaults to file.")
            openFileOutput(file, Context.MODE_PRIVATE)
            filedir.writeText(styledefault.plus("|"))
            filedir.appendText(fontdefault.plus("|"))
            filedir.appendText(wakeTimeoutDefault.plus("|"))
        }
        Log.i("S22PresSetting", "Settings loaded.")
        try {
            settings = filedir.readText().split("|").toTypedArray()
            styleset = settings[0].toString()
            fontset = settings[1].toString()
            wakeTimeoutSet = if (settings.size > 2 && settings[2].isNotEmpty()) settings[2] else wakeTimeoutDefault
            findViewById<Spinner>(R.id.spinnerstyle).setSelection(settings[0].toInt())
            findViewById<Spinner>(R.id.spinnerfont).setSelection(settings[1].toInt())
            findViewById<Spinner>(R.id.spinnerWakeTimeout).setSelection(wakeTimeoutSet.toInt())
        } catch (e: Exception) {
            Log.e("S22PresSetting", "Settings in wrong format! Likely due to an update. Resetting!")
            openFileOutput(file, Context.MODE_PRIVATE)
            filedir.writeText(styledefault)
            filedir.appendText(fontdefault)
            filedir.appendText(wakeTimeoutDefault)
            settings = filedir.readText().split("|").toTypedArray()
            styleset = settings[0].toString()
            fontset = settings[1].toString()
            wakeTimeoutSet = wakeTimeoutDefault
            findViewById<Spinner>(R.id.spinnerstyle).setSelection(settings[0].toInt())
            findViewById<Spinner>(R.id.spinnerfont).setSelection(settings[1].toInt())
            findViewById<Spinner>(R.id.spinnerWakeTimeout).setSelection(wakeTimeoutSet.toInt())
        }
        Log.i("S22PresSetting", "Got $styleset $fontset")
        findViewById<Button>(R.id.buttonReset).setOnClickListener {
            styleset = "0"
            fontset = "0"
            wakeTimeoutSet = wakeTimeoutDefault
            findViewById<Spinner>(R.id.spinnerstyle).setSelection(settings[0].toInt())
            findViewById<Spinner>(R.id.spinnerfont).setSelection(settings[1].toInt())
            findViewById<Spinner>(R.id.spinnerWakeTimeout).setSelection(wakeTimeoutSet.toInt())
        }
        findViewById<Spinner>(R.id.spinnerstyle).onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    styleset =
                        findViewById<Spinner>(R.id.spinnerstyle).selectedItemPosition.toString()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    findViewById<Spinner>(R.id.spinnerstyle).setSelection(settings[0].toInt())
                }
            }
        findViewById<Spinner>(R.id.spinnerfont).onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    fontset =
                        findViewById<Spinner>(R.id.spinnerfont).selectedItemPosition.toString()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    findViewById<Spinner>(R.id.spinnerfont).setSelection(settings[1].toInt())
                }
            }
        findViewById<Spinner>(R.id.spinnerWakeTimeout).onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    wakeTimeoutSet =
                        findViewById<Spinner>(R.id.spinnerWakeTimeout).selectedItemPosition.toString()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    findViewById<Spinner>(R.id.spinnerWakeTimeout).setSelection(wakeTimeoutSet.toInt())
                }
            }
        findViewById<Button>(R.id.buttonsave).setOnClickListener {
            filedir.writeText(styleset.plus("|"))
            filedir.appendText(fontset.plus("|"))
            filedir.appendText(wakeTimeoutSet.plus("|"))
            val serviceintent = Intent(this, ListenerService::class.java)
            Log.i("S22PresListServ", "Restarting...")
            stopService(serviceintent)
            startService(serviceintent)
            Toast.makeText(this, "Settings Saved.", 2000).show()
        }

    }

}

