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

    suspend fun ensureCellTowerExistsAndGet(apiKey: String, radio: String, mcc: Int, mnc: Int, lac: Int, cid: Long): CellTower? {
        return withContext(Dispatchers.IO) {
            // 1. Check local DB
            var tower = dao.getCellTower(mcc, mnc, lac, cid)
            if (tower != null) {
                return@withContext tower
            }

            // 2. If completely missing the MCC, attempt to download CSV
            val mccCount = dao.countTowersByMcc(mcc)
            if (mccCount == 0) {
                OpenCellidDownloader.downloadAndImportMcc(context, apiKey, mcc)

                // Try to fetch again
                tower = dao.getCellTower(mcc, mnc, lac, cid)
                if (tower != null) {
                    return@withContext tower
                }
            }

            // 3. Fallback: single API request for the current tower if we STILL don't have it
            // (either the MCC download failed or it's a very new tower not in the DB dump)
            val location = OpenCellidApi.getCellLocation(apiKey, radio, mcc, mnc, lac, cid)
            if (location != null) {
                tower = CellTower(mcc = mcc, mnc = mnc, lac = lac, cid = cid, lat = location.first, lon = location.second)
                dao.insert(tower)
            }

            return@withContext tower
        }
    }
}
