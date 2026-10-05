package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "solve_records")
data class SolveRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val durationMillis: Long,
    val movesCount: Int,
    val scrambleDifficulty: String,
    val usedInstantSolve: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface SolveDao {
    @Query("SELECT * FROM solve_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<SolveRecord>>

    @Query("SELECT * FROM solve_records WHERE usedInstantSolve = 0 ORDER BY durationMillis ASC LIMIT 1")
    fun getBestRecord(): Flow<SolveRecord?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: SolveRecord)

    @Query("DELETE FROM solve_records WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    @Query("DELETE FROM solve_records")
    suspend fun clearAllRecords()
}

@Database(entities = [SolveRecord::class], version = 1, exportSchema = false)
abstract class RubikDatabase : RoomDatabase() {
    abstract fun solveDao(): SolveDao
}

class SolveRepository(private val solveDao: SolveDao) {
    val allRecords: Flow<List<SolveRecord>> = solveDao.getAllRecords()
    val bestRecord: Flow<SolveRecord?> = solveDao.getBestRecord()

    suspend fun insert(record: SolveRecord) = solveDao.insertRecord(record)
    suspend fun deleteById(id: Int) = solveDao.deleteRecordById(id)
    suspend fun clearAll() = solveDao.clearAllRecords()
}
