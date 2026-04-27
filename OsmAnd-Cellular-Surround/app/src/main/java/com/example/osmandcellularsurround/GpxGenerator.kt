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

    fun generateGpxString(mainTower: CellTower, surroundingTowers: List<CellTower>): String {
        val timeString = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
        val builder = java.lang.StringBuilder()

        builder.append("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>\n")
        builder.append("<gpx version=\"1.1\" creator=\"OsmAnd Cellular Surround\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")

        // Main connected tower
        builder.append("  <wpt lat=\"${mainTower.lat}\" lon=\"${mainTower.lon}\">\n")
        builder.append("    <time>$timeString</time>\n")
        builder.append("    <name>Connected: ${mainTower.mcc}-${mainTower.mnc}-${mainTower.lac}-${mainTower.cid}</name>\n")
        builder.append("    <desc>Currently Connected Cell Tower</desc>\n")
        builder.append("    <extensions>\n")
        builder.append("      <color>#FF0000</color>\n")
        builder.append("      <icon>radio_tower</icon>\n")
        builder.append("    </extensions>\n")
        builder.append("  </wpt>\n")

        // Surrounding towers
        for (tower in surroundingTowers) {
            if (tower.mcc == mainTower.mcc && tower.mnc == mainTower.mnc && tower.lac == mainTower.lac && tower.cid == mainTower.cid) continue

            builder.append("  <wpt lat=\"${tower.lat}\" lon=\"${tower.lon}\">\n")
            builder.append("    <name>${tower.mcc}-${tower.mnc}-${tower.lac}-${tower.cid}</name>\n")
            builder.append("    <extensions>\n")
            builder.append("      <icon>radio_tower</icon>\n")
            builder.append("    </extensions>\n")
            builder.append("  </wpt>\n")
        }

        builder.append("</gpx>")
        return builder.toString()
    }

    // Calculates bounding box approx `radiusKm` around a center point
    fun calculateBoundingBox(lat: Double, lon: Double, radiusKm: Double): DoubleArray {
        // 1 degree of latitude is ~111km
        val latOffset = radiusKm / 111.0
        // longitude offset depends on latitude
        val lonOffset = (radiusKm / 111.0) / Math.cos(Math.toRadians(lat))

        return doubleArrayOf(
            lat - latOffset, // minLat
            lat + latOffset, // maxLat
            lon - lonOffset, // minLon
            lon + lonOffset  // maxLon
        )
    }
}
