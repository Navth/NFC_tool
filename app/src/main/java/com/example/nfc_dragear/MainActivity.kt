package com.example.nfc.dragear

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.example.nfc.dragear.ui.MainScreen
import com.example.nfc_dragear.ui.theme.NFC_DragearTheme
import com.example.nfc.dragear.utils.readFromTag
import com.example.nfc.dragear.utils.writeToTag

class MainActivity : ComponentActivity() {

    // NFC Adapter instance
    private var nfcAdapter: NfcAdapter? = null
    // PendingIntent to handle NFC intents when app is in foreground
    private var pendingIntent: PendingIntent? = null

    // --- STATE ---
    // Mutable state for the URL input field
    val urlState = mutableStateOf("https://open.spotify.com/track/...")
    // Mutable state for the status log
    val logState = mutableStateListOf("App started. Waiting for action.")
    // Mutable state to track if we are in "Write" mode
    val isWriteMode = mutableStateOf(false)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize NFC Adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        // Check if NFC is supported and enabled
        if (nfcAdapter == null) {
            log("NFC not supported on this device.", isError = true)
            Toast.makeText(this, "NFC not supported on this device.", Toast.LENGTH_LONG).show()
        } else if (!nfcAdapter!!.isEnabled) {
            log("NFC is not enabled. Please enable it in settings.", isError = true)
            Toast.makeText(this, "Please enable NFC in settings.", Toast.LENGTH_LONG).show()
        } else {
            log("NFC is supported and enabled.")
        }

        // Create a PendingIntent for foreground dispatch
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flag)

        // Handle the intent that started the app (if it was from an NFC tag)
        handleIntent(intent)

        setContent {
            NFC_DragearTheme {
                // Call the Composable from the separate UI file
                MainScreen(urlState, logState, isWriteMode)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Enable foreground dispatch to intercept NFC tags when app is open
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        // Disable foreground dispatch when app is paused
        nfcAdapter?.disableForegroundDispatch(this)
    }

    /**
     * Called when a new NFC intent is received (e.g., tag scanned)
     * while the app is already in the foreground.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle the new intent
        handleIntent(intent)
    }

    /**
     * Main logic to handle a new NFC intent.
     */
    private fun handleIntent(intent: Intent) {
        val action = intent.action

        // Check if the intent is for a discovered NDEF tag
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == action || NfcAdapter.ACTION_TAG_DISCOVERED == action) {

            if (isWriteMode.value) {
                // --- WRITE MODE ---
                val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                if (tag != null) {
                    // Call writeToTag from NfcUtils.kt
                    writeToTag(
                        tag = tag,
                        url = urlState.value,
                        log = ::log, // Pass the log function as a callback
                        showToast = ::showToast // Pass the showToast function as a callback
                    )
                } else {
                    log("Error: Tag object was null.", isError = true)
                }
                // Automatically switch back to read mode after attempting to write
                isWriteMode.value = false

            } else {
                // --- READ MODE ---
                // Call readFromTag from NfcUtils.kt
                val url = readFromTag(intent, ::log) // Pass log function
                if (url != null) {
                    urlState.value = url
                }
            }
        }
    }

    // Helper function to add a message to the log state
    private fun log(message: String, isError: Boolean = false) {
        val prefix = if (isError) "❌ " else " > "
        logState.add(0, "$prefix$message") // Add to top of list
        Log.d("NFCTool", message) // Also log to Android's Logcat
    }

    // Helper function to show Toasts from background threads
    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}

