package com.example.osmandcellularsurround

import android.content.Context
import com.example.osmandcellularsurround.api.OpenCellidApi
import com.example.osmandcellularsurround.api.OpenCellidDownloader
import com.example.osmandcellularsurround.db.AppDatabase
import com.example.osmandcellularsurround.db.CellTower
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataSyncManager(private val context: Context) {
    private val dao = AppDatabase.getDatabase(context).cellTowerDao()

    suspend fun ensureCellTowerExistsAndGet(apiKey: String, mcc: Int, mnc: Int, lac: Int, cid: Long): CellTower? {
        return withContext(Dispatchers.IO) {
            // 1. Check local DB
            var tower = dao.getCellTower(mcc, mnc, lac, cid)
            if (tower != null) {
                return@withContext tower
            }

            // 2. If completely missing the MCC, attempt to download CSV
            val mccCount = dao.countTowersByMcc(mcc)
            if (mccCount == 0) {
                val downloaded = OpenCellidDownloader.downloadAndImportMcc(context, apiKey, mcc)
                if (downloaded) {
                    // Try to fetch again
                    tower = dao.getCellTower(mcc, mnc, lac, cid)
                    if (tower != null) {
                        return@withContext tower
                    }
                }
            }

            // 3. Fallback: single API request for the current tower
            val location = OpenCellidApi.getCellLocation(apiKey, mcc, mnc, lac, cid)
            if (location != null) {
                tower = CellTower(mcc = mcc, mnc = mnc, lac = lac, cid = cid, lat = location.first, lon = location.second)
                dao.insert(tower)
                return@withContext tower
            }

            return@withContext null
        }
    }
}
