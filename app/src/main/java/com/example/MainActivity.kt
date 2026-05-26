package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.StudyAnalysisRepository
import com.example.ui.StudyMateViewModel
import com.example.ui.StudyMateViewModelFactory
import com.example.ui.components.StudyMateAppScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: StudyAnalysisRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database and Repository lazily on Startup
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "studymate_academic_database"
        )
        .fallbackToDestructiveMigration() // safeguard for schema changes during prototyping
        .build()
        
        repository = StudyAnalysisRepository(database.studyAnalysisDao())
        
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                // Fetch the ViewModel scoped to this activity using the custom Repository Factory
                val viewModel: StudyMateViewModel = viewModel(
                    factory = StudyMateViewModelFactory(repository)
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    StudyMateAppScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
