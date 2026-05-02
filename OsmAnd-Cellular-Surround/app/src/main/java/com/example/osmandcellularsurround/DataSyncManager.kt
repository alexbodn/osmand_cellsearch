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

            // 2. We check the API directly first if the tower is missing
            val location = OpenCellidApi.getCellLocation(apiKey, mcc, mnc, lac, cid)
            if (location != null) {
                tower = CellTower(mcc = mcc, mnc = mnc, lac = lac, cid = cid, lat = location.first, lon = location.second)
                dao.insert(tower)
            }

            // 3. To get the bounding box surrounding data without fetching individual towers by API,
            // check if we have the MCC data locally. If not, download it now.
            val mccCount = dao.countTowersByMcc(mcc)
            if (mccCount == 0) {
                OpenCellidDownloader.downloadAndImportMcc(context, apiKey, mcc)

                // Refresh tower from DB if API failed but we downloaded the MCC
                if (tower == null) {
                    tower = dao.getCellTower(mcc, mnc, lac, cid)
                }
            }

            return@withContext tower
        }
    }
}
