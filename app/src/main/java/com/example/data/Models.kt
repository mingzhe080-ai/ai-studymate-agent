package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "study_analyses")
data class StudyAnalysis(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val inputPrompt: String,
    val mode: String,
    val language: String,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Output sections based on the prompt's structured academic response and theory focus
    val mainTask: String,
    val problemToSolve: String,
    val targetUsers: String,
    val userNeeds: String,
    val keyFunctions: String,
    val serviceConcept: String,
    val aiValue: String,
    val developmentPlan: String
)

@Dao
interface StudyAnalysisDao {
    @Query("SELECT * FROM study_analyses ORDER BY timestamp DESC")
    fun getAllAnalyses(): Flow<List<StudyAnalysis>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(analysis: StudyAnalysis)

    @Query("DELETE FROM study_analyses WHERE id = :id")
    suspend fun deleteAnalysisById(id: Int)

    @Query("DELETE FROM study_analyses")
    suspend fun deleteAllAnalyses()
}

@Database(entities = [StudyAnalysis::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studyAnalysisDao(): StudyAnalysisDao
}

class StudyAnalysisRepository(private val dao: StudyAnalysisDao) {
    val allAnalyses: Flow<List<StudyAnalysis>> = dao.getAllAnalyses()

    suspend fun insert(analysis: StudyAnalysis) {
        dao.insertAnalysis(analysis)
    }

    suspend fun deleteById(id: Int) {
        dao.deleteAnalysisById(id)
    }

    suspend fun deleteAll() {
        dao.deleteAllAnalyses()
    }
}
