package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.okkhor.data.preferences.KeyboardPreferencesRepository
import com.example.okkhor.ui.settings.MainSettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private var pendingMicPermissionRequest by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (intent?.getBooleanExtra(EXTRA_REQUEST_MIC_PERMISSION, false) == true) {
            pendingMicPermissionRequest = true
        }

        val repository = KeyboardPreferencesRepository.getInstance(applicationContext)

        setContent {
            val requestMicLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                val message = if (granted) {
                    "Microphone permission granted! Voice typing is ready."
                } else {
                    "Microphone permission is required for voice typing."
                }
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }

            LaunchedEffect(pendingMicPermissionRequest) {
                if (pendingMicPermissionRequest) {
                    pendingMicPermissionRequest = false
                    if (ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestMicLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }

            MyApplicationTheme {
                MainSettingsScreen(repository = repository)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_REQUEST_MIC_PERMISSION, false)) {
            pendingMicPermissionRequest = true
        }
    }

    companion object {
        const val EXTRA_REQUEST_MIC_PERMISSION = "extra_request_mic_permission"
    }
}
