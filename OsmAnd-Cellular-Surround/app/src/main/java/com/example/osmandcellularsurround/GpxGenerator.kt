package com.example.osmandcellularsurround

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.osmandcellularsurround.db.CellTower
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GpxGenerator {

    fun generateGpx(context: Context, mainTower: CellTower, surroundingTowers: List<CellTower>): Uri {
        val fileName = "cellular_surround_${mainTower.mcc}_${mainTower.mnc}_${mainTower.lac}_${mainTower.cid}.gpx"
        val file = File(context.cacheDir, fileName)

        val timeString = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

        FileOutputStream(file).bufferedWriter().use { writer ->
            writer.write("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n")
            writer.write("<gpx version=\"1.1\" creator=\"OsmAnd Cellular Surround\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")

            // Main connected tower (highlighted in description or color)
            writer.write("  <wpt lat=\"${mainTower.lat}\" lon=\"${mainTower.lon}\">\n")
            writer.write("    <time>$timeString</time>\n")
            writer.write("    <name>Connected: ${mainTower.mcc}-${mainTower.mnc}-${mainTower.lac}-${mainTower.cid}</name>\n")
            writer.write("    <desc>Currently Connected Cell Tower</desc>\n")
            writer.write("    <extensions>\n")
            writer.write("      <color>#FF0000</color>\n")
            writer.write("    </extensions>\n")
            writer.write("  </wpt>\n")

            // Surrounding towers
            for (tower in surroundingTowers) {
                // Don't duplicate the main tower
                if (tower.mcc == mainTower.mcc && tower.mnc == mainTower.mnc && tower.lac == mainTower.lac && tower.cid == mainTower.cid) continue

                writer.write("  <wpt lat=\"${tower.lat}\" lon=\"${tower.lon}\">\n")
                writer.write("    <name>${tower.mcc}-${tower.mnc}-${tower.lac}-${tower.cid}</name>\n")
                writer.write("  </wpt>\n")
            }

            writer.write("</gpx>")
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // Calculates bounding box approx 20km around a center point
    fun calculateBoundingBox(lat: Double, lon: Double): DoubleArray {
        // 1 degree of latitude is ~111km
        // 20km is ~ 0.18 degrees
        val latOffset = 0.18
        // longitude offset depends on latitude
        val lonOffset = 0.18 / Math.cos(Math.toRadians(lat))

        return doubleArrayOf(
            lat - latOffset, // minLat
            lat + latOffset, // maxLat
            lon - lonOffset, // minLon
            lon + lonOffset  // maxLon
        )
    }
}
