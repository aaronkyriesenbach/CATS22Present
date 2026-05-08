package com.android.s22present

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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

        Globals.migrateSettingsIfNeeded(this)

        val appPrefs = getSharedPreferences(Globals.PREFS_APP, Context.MODE_PRIVATE)
        val modulePrefs = getSharedPreferences(Globals.PREFS_MODULE, Context.MODE_PRIVATE)

        var styleset = appPrefs.getInt(Globals.KEY_STYLE, Globals.DEFAULT_STYLE)
        var fontset = appPrefs.getInt(Globals.KEY_FONT, Globals.DEFAULT_FONT)
        var wakeTimeoutSet = appPrefs.getInt(Globals.KEY_WAKE_TIMEOUT_INDEX, Globals.DEFAULT_WAKE_TIMEOUT_INDEX)
        var debounceSet = Globals.debounceIndexForMs(
            modulePrefs.getInt(Globals.KEY_DEBOUNCE_MS, Globals.DEFAULT_DEBOUNCE_MS))

        Log.i("S22PresSetting", "Settings loaded.")
        Log.i("S22PresSetting", "Got style=$styleset font=$fontset wakeTimeout=$wakeTimeoutSet debounce=$debounceSet")

        findViewById<Spinner>(R.id.spinnerstyle).setSelection(styleset)
        findViewById<Spinner>(R.id.spinnerfont).setSelection(fontset)
        findViewById<Spinner>(R.id.spinnerWakeTimeout).setSelection(wakeTimeoutSet)
        findViewById<Spinner>(R.id.spinnerDebounce).setSelection(debounceSet)

        findViewById<Button>(R.id.buttonReset).setOnClickListener {
            styleset = Globals.DEFAULT_STYLE
            fontset = Globals.DEFAULT_FONT
            wakeTimeoutSet = Globals.DEFAULT_WAKE_TIMEOUT_INDEX
            debounceSet = Globals.DEFAULT_DEBOUNCE_INDEX
            findViewById<Spinner>(R.id.spinnerstyle).setSelection(styleset)
            findViewById<Spinner>(R.id.spinnerfont).setSelection(fontset)
            findViewById<Spinner>(R.id.spinnerWakeTimeout).setSelection(wakeTimeoutSet)
            findViewById<Spinner>(R.id.spinnerDebounce).setSelection(debounceSet)
        }
        findViewById<Spinner>(R.id.spinnerstyle).onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    styleset = position
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    findViewById<Spinner>(R.id.spinnerstyle).setSelection(styleset)
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
                    fontset = position
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    findViewById<Spinner>(R.id.spinnerfont).setSelection(fontset)
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
                    wakeTimeoutSet = position
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    findViewById<Spinner>(R.id.spinnerWakeTimeout).setSelection(wakeTimeoutSet)
                }
            }
        findViewById<Spinner>(R.id.spinnerDebounce).onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View,
                    position: Int,
                    id: Long
                ) {
                    debounceSet = position
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    findViewById<Spinner>(R.id.spinnerDebounce).setSelection(debounceSet)
                }
            }
        findViewById<Button>(R.id.buttonsave).setOnClickListener {
            appPrefs.edit()
                .putInt(Globals.KEY_STYLE, styleset)
                .putInt(Globals.KEY_FONT, fontset)
                .putInt(Globals.KEY_WAKE_TIMEOUT_INDEX, wakeTimeoutSet)
                .apply()
            modulePrefs.edit()
                .putInt(Globals.KEY_DEBOUNCE_MS, Globals.debounceMsForIndex(debounceSet))
                .apply()
            val serviceintent = Intent(this, ListenerService::class.java)
            Log.i("S22PresListServ", "Restarting...")
            stopService(serviceintent)
            startService(serviceintent)
            Toast.makeText(this, "Settings Saved.", 2000).show()
        }

    }

}

