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
    
    // Original Output sections for backward-compatibility
    val mainTask: String,
    val problemToSolve: String,
    val targetUsers: String,
    val userNeeds: String,
    val keyFunctions: String,
    val serviceConcept: String,
    val aiValue: String,
    val developmentPlan: String,

    // NEW Feature: Structured Breakdown Sections
    val requiredOutput: String = "",
    val importantKeywords: String = "",
    val professorFocus: String = "",
    val suggestedSteps: String = "",

    // NEW Feature: AI Value Matrix quadrants
    val matrixProductivity: String = "",
    val matrixDecisionMaking: String = "",
    val matrixCreativity: String = "",
    val matrixInteraction: String = "",

    // NEW Feature: Rubric Score Checker (0-5 per metric)
    val scoreProblemClarity: Int = 0,
    val scoreTargetClarity: Int = 0,
    val scoreServiceValue: Int = 0,
    val scoreAiValue: Int = 0,
    val scoreFeasibility: Int = 0,
    val scorePresentation: Int = 0,
    val scoreSuggestions: String = "",

    // NEW Feature: Presentation Script Generator
    val presentationPitch: String = "",
    val presentationScript: String = "",
    val presentationQna: String = ""
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

@Database(entities = [StudyAnalysis::class], version = 2, exportSchema = false)
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
