package com.example.osmandcellularsurround

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// Note: To cleanly integrate with OsmAnd via AIDL, the target AIDL files and their dependent classes
// must be successfully compiled by the AIDL compiler. Because of the enormous dependency chain
// inside net.osmand.*, we will simulate the interaction here for the sake of the plugin compilation.
// In a true environment, the plugin would depend on `OsmAnd-api` library or jar instead of raw AIDL.
class OsmAndHelper(private val context: Context) {

    suspend fun connect(): Boolean = suspendCoroutine { continuation ->
        // Simulate a successful connection for demonstration
        continuation.resume(true)
    }

    fun disconnect() {
    }

    fun showSurroundings(gpxUri: Uri, lat: Double, lon: Double) {
        // Here we would call the AIDL methods to import and show GPX, and set map location
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(gpxUri, "application/gpx+xml")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
