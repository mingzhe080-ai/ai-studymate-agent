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
    assertEquals("Assignment Analyzer", appName)
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
    assertEquals("Please paste or type your academic requirements or ideas first!", viewModel.error.value)

    database.close()
  }
}
