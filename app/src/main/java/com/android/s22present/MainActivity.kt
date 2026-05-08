package com.android.s22present

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.lsposed.hiddenapibypass.HiddenApiBypass

// Clue's in the name.
class MainActivity : AppCompatActivity() {

    private val requestRecordAudioLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Audio visualizer requires Record Audio permission", Toast.LENGTH_LONG).show()
            }
            refreshPermissionStatuses()
        }

    private val requestPhoneStateLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Call detection requires Phone State permission", Toast.LENGTH_LONG).show()
            }
            refreshPermissionStatuses()
        }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        // On start
        Log.i("S22PresMain", "Hello!")
        Log.i("S22PresMainInit", "Tasks associated with initial setup will be appended with Init")
        HiddenApiBypass.addHiddenApiExemptions("L")
        super.onCreate(savedInstanceState)
        // Create UI
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main))
        {
            v, insets -> val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val statusText: TextView = findViewById(R.id.textViewStatus)
        // Try to find display [1]
        val displaymanager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        try {
            Log.i("S22PresMainInit", "Looking for second display...")
            val display1 = displaymanager.displays[1]
            // If the display isn't found.
            if(display1 == null)
            {
                statusText.text = "Not running \u2717"
                Log.w("S22PresMainInit", "No second display found. Run the commands!")
                Toast.makeText(this, "No second display found. Run the commands!", 2500).show()
            }
            // If the display is found.
            else
            {
                Globals.statusText = statusText

                if (ListenerService.isRunning) {
                    statusText.text = "Running \u2713"
                } else {
                    val serviceintent = Intent(this, ListenerService::class.java)
                    Log.i("S22PresMainInit", "Asking Services to Run.")
                    startService(serviceintent)
                    statusText.text = "Running \u2713"
                }
            }
        }
        // If a crash occurs whilst looking for a second display.
        catch (e: Exception)
        {
            // Notify user.
            Log.e("S22PresMainInit", "An Exception occurred trying to find the second screen. It is likely because the second screen isn't activated.")
            Toast.makeText(this, "No second display found. Run the commands!", 2500).show()
            statusText.text = "Not running \u2717"
        }
        findViewById<LinearLayout>(R.id.rowRecordAudio).setOnClickListener {
            requestRuntimePermission(android.Manifest.permission.RECORD_AUDIO, requestRecordAudioLauncher)
        }
        findViewById<LinearLayout>(R.id.rowReadPhoneState).setOnClickListener {
            requestRuntimePermission(android.Manifest.permission.READ_PHONE_STATE, requestPhoneStateLauncher)
        }
        findViewById<LinearLayout>(R.id.rowNotificationListener).setOnClickListener {
            if (!isNotificationListenerEnabled()) {
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            }
        }
        findViewById<Button>(R.id.buttonSetting).setOnClickListener{
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        refreshPermissionStatuses()
        Globals.onRootStatusChanged = { refreshPermissionStatuses() }
    }

    override fun onResume()
    {
        super.onResume()
        refreshPermissionStatuses()
    }

    private fun refreshPermissionStatuses() {
        val grantedColor = ContextCompat.getColor(this, R.color.permission_granted)
        val deniedColor = ContextCompat.getColor(this, R.color.permission_denied)

        val recordAudioGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val statusRecordAudio: TextView = findViewById(R.id.statusRecordAudio)
        statusRecordAudio.text = if (recordAudioGranted) "\u2713" else "\u2717"
        statusRecordAudio.setTextColor(if (recordAudioGranted) grantedColor else deniedColor)

        val phoneStateGranted = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val statusPhoneState: TextView = findViewById(R.id.statusReadPhoneState)
        statusPhoneState.text = if (phoneStateGranted) "\u2713" else "\u2717"
        statusPhoneState.setTextColor(if (phoneStateGranted) grantedColor else deniedColor)

        val notificationListenerEnabled = isNotificationListenerEnabled()
        val statusNotification: TextView = findViewById(R.id.statusNotificationListener)
        statusNotification.text = if (notificationListenerEnabled) "\u2713" else "\u2717"
        statusNotification.setTextColor(if (notificationListenerEnabled) grantedColor else deniedColor)

        val statusRoot: TextView = findViewById(R.id.statusRootAccess)
        statusRoot.text = if (Globals.rootAvailable) "\u2713" else "\u2717"
        statusRoot.setTextColor(if (Globals.rootAvailable) grantedColor else deniedColor)
    }

    private fun isNotificationListenerEnabled(): Boolean {
        return NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
    }

    private fun requestRuntimePermission(permission: String, launcher: ActivityResultLauncher<String>) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        launcher.launch(permission)
    }

    override fun onStop()
    {
        super.onStop()
        Globals.onRootStatusChanged = null
    }
}
