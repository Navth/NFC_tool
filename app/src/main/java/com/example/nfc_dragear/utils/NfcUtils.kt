package com.example.nfc.dragear.utils

import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import java.io.IOException

// This file contains all the NFC helper functions

/**
 * Reads the content of an NDEF formatted tag from an Intent.
 * Returns the URL string if found, or null.
 */
fun readFromTag(intent: Intent, log: (String, Boolean) -> Unit): String? {
    log("Tag detected. Reading...", false)
    val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)

    if (rawMessages.isNullOrEmpty()) {
        log("Tag is empty or not NDEF formatted.", true)
        return null
    }

    try {
        val messages = rawMessages.map { it as NdefMessage }
        val record = messages[0].records[0]

        // Check if it's a URI record
        if (record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI)) {
            val payload = record.payload

            // The first byte is a prefix code, the rest is the URL
            val prefixCode = payload[0].toInt() and 0xFF
            val uri = payload.decodeToString(1, payload.size - 1)

            val prefix = getUriPrefix(prefixCode)
            val fullUrl = prefix + uri

            log("Read URL: $fullUrl", false)
            return fullUrl
        } else {
            log("Tag contains data, but it's not a URL.", true)
            return null
        }
    } catch (e: Exception) {
        log("Error reading tag: ${e.message}", true)
        return null
    }
}

/**
 * Writes a URL to a given NFC Tag.
 */
fun writeToTag(tag: Tag, url: String, log: (String, Boolean) -> Unit, showToast: (String) -> Unit) {
    log("Attempting to write URL: $url", false)

    // Create a URI record
    val uriRecord = NdefRecord.createUri(url)
    val message = NdefMessage(arrayOf(uriRecord))

    try {
        // Get Ndef instance for the tag
        val ndef = Ndef.get(tag)

        if (ndef == null) {
            log("Error: Tag does not support NDEF.", true)
            return
        }

        ndef.connect()
        if (!ndef.isWritable) {
            log("Error: Tag is read-only.", true)
            ndef.close()
            return
        }

        // Check if there's enough space
        val messageSize = message.toByteArray().size
        if (ndef.maxSize < messageSize) {
            log("Error: Not enough space on tag. (Need $messageSize bytes, have ${ndef.maxSize})", true)
            ndef.close()
            return
        }

        // Write the message
        ndef.writeNdefMessage(message)
        ndef.close()

        log("✅ Success! Tag written with URL.", false)
        showToast("Tag written successfully!")

    } catch (e: IOException) {
        log("Error writing tag (IO): ${e.message}", true)
        showToast("Write failed: IO Error")
    } catch (e: Exception) {
        log("Error writing tag: ${e.message}", true)
        showToast("Write failed: ${e.message}")
    }
}

/**
 * Helper function to map NDEF URI prefix codes to their string representation.
 */
private fun getUriPrefix(prefixCode: Int): String {
    return when (prefixCode) {
        0x00 -> ""
        0x01 -> "http://www."
        0x02 -> "https://www."
        0x03 -> "http://"
        0x04 -> "https://"
        0x05 -> "tel:"
        0x06 -> "mailto:"
        0x07 -> "ftp://anonymous:anonymous@"
        0x08 -> "ftp://ftp."
        0x09 -> "ftps://"
        0x0A -> "sftp://"
        0x0B -> "smb://"
        0x0C -> "nfs://"
        0x0D -> "ftp://"
        0x0E -> "dav://"
        0x0F -> "news:"
        0x10 -> "telnet://"
        0x11 -> "imap:"
        0x12 -> "rtsp://"
        0x13 -> "urn:"
        0x14 -> "pop:"
        0x15 -> "sip:"
        0x16 -> "sips:"
        0x17 -> "tftp:"
        0x18 -> "btspp://"
        0x19 -> "btl2cap://"
        0x1A -> "btgoep://"
        0x1B -> "tcpobex://"
        0x1C -> "irdaobex://"
        0x1D -> "file://"
        0x1E -> "urn:epc:id:"
        0x1F -> "urn:epc:tag:"
        0x20 -> "urn:epc:pat:"
        0x21 -> "urn:epc:raw:"
        0x22 -> "urn:epc:"
        0x23 -> "urn:nfc:"
        else -> ""
    }
}
