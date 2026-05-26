package com.example

import android.content.Context
import androidx.room.*
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.ui.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Assignment Analyzer Pro", appName)
  }

  @Test
  fun `launch main activity`() {
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    scenario.onActivity { activity ->
      assertNotNull(activity)
    }
    scenario.close()
  }

  @Test
  fun `test analyzeWithStudyMate button click`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repository = StudyAnalysisRepository(database.studyAnalysisDao())
    val viewModel = StudyMateViewModel(repository)

    // With empty input, it should set an error state and return immediately
    viewModel.analyzeWithStudyMate()
    assertEquals("Please paste some assignment text first!", viewModel.error.value)

    database.close()
  }

  @Test
  fun `test fallback generator produces expected rubric scores and sections`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val repository = StudyAnalysisRepository(database.studyAnalysisDao())
    val viewModel = StudyMateViewModel(repository)

    val fallback = viewModel.generateLocalFallback("AI MANAGEMENT RESEARCH IN KOREAN", "Assignment Analyzer", "Korean")
    assertNotNull(fallback)
    assertEquals(4, fallback.scoreProblemClarity)
    assertEquals(5, fallback.scoreTargetClarity)
    assertEquals(4, fallback.scoreServiceValue)
    assertEquals(4, fallback.scoreAiValue)
    assertEquals(5, fallback.scoreFeasibility)
    assertEquals(4, fallback.scorePresentation)
    
    // Total should equal 26
    val totalScore = fallback.scoreProblemClarity + fallback.scoreTargetClarity + fallback.scoreServiceValue +
                     fallback.scoreAiValue + fallback.scoreFeasibility + fallback.scorePresentation
    assertEquals(26, totalScore)
    
    // Matrix fields should be populated
    assertNotNull(fallback.matrixProductivity)
    assertNotNull(fallback.matrixDecisionMaking)
    assertNotNull(fallback.matrixCreativity)
    assertNotNull(fallback.matrixInteraction)

    // Presentation script fields should be populated
    assertNotNull(fallback.presentationPitch)
    assertNotNull(fallback.presentationScript)
    assertNotNull(fallback.presentationQna)

    database.close()
  }
}
