package com.example.osmandcellularsurround

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.osmand.aidlapi.IOsmAndAidlInterface
import net.osmand.aidlapi.gpx.ImportGpxParams
import net.osmand.aidlapi.gpx.ShowGpxParams
import net.osmand.aidlapi.map.SetMapLocationParams
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class OsmAndHelper(private val context: Context) {

    private var osmandService: IOsmAndAidlInterface? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            osmandService = IOsmAndAidlInterface.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            osmandService = null
        }
    }

    suspend fun connect(): Boolean {
        if (osmandService != null) return true

        return suspendCoroutine { continuation ->
            val tempConnection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    osmandService = IOsmAndAidlInterface.Stub.asInterface(service)
                    continuation.resume(true)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    osmandService = null
                }
            }

            val intent = Intent("net.osmand.aidl.OsmandAidlServiceV2")
            intent.setPackage("net.osmand.plus")
            var bound = context.bindService(intent, tempConnection, Context.BIND_AUTO_CREATE)

            if (!bound) {
                intent.setPackage("net.osmand")
                bound = context.bindService(intent, tempConnection, Context.BIND_AUTO_CREATE)
            }

            if (!bound) {
                continuation.resume(false)
            }
        }
    }

    fun disconnect() {
        if (osmandService != null) {
            context.unbindService(connection)
            osmandService = null
        }
    }

    suspend fun showSurroundings(gpxUri: Uri, lat: Double, lon: Double) {
        val aidl = osmandService ?: return

        withContext(Dispatchers.IO) {
            try {
                // Grant URI Permission to OsmAnd packages to be able to read it
                context.grantUriPermission("net.osmand.plus", gpxUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                context.grantUriPermission("net.osmand", gpxUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

                val importParams = ImportGpxParams(gpxUri, "cellular_surround.gpx", "red", true)
                aidl.importGpx(importParams)

                val showParams = ShowGpxParams("cellular_surround.gpx")
                aidl.showGpx(showParams)

                val locationParams = SetMapLocationParams(lat, lon, 15, 0f, true)
                aidl.setMapLocation(locationParams)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
