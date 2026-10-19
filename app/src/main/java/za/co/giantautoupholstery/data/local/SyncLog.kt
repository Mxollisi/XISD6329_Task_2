package za.co.giantautoupholstery.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(
    tableName = "api_sync_logs",
    indices = [
        Index("timestamp"),
        Index("endpoint"),
        Index("statusCode"),
    ]
)
data class SyncLogEntity(
    @PrimaryKey val id: String,
    val endpoint: String,
    val httpMethod: String,       // GET, POST
    val statusCode: Int,          // 200, 201, 400, 500
    val statusText: String,       // OK, Created, Bad Request, Internal Server Error
    val requestPayload: String?,
    val responsePayload: String?,
    val durationMs: Long,
    val success: Boolean,
    val errorMessage: String?,
    val timestamp: Long,          // Epoch millis
)

@Dao
interface SyncLogDao {
    @Query("SELECT * FROM api_sync_logs ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<SyncLogEntity>>

    @Query("SELECT * FROM api_sync_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<SyncLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SyncLogEntity)

    @Query("DELETE FROM api_sync_logs")
    suspend fun clear()

    @Query("SELECT COUNT(*) FROM api_sync_logs")
    suspend fun count(): Int
}
