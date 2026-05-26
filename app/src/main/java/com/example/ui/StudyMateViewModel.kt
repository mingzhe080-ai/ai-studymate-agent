package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class StudyMode(val displayName: String, val description: String) {
    ASSIGNMENT_ANALYZER("Assignment Analyzer", "Deconstruct custom assignment briefs, define grading indicators, and decode expectations."),
    IDEA_GENERATOR("AI Agent Idea Generator", "Brainstorm innovative AI product/service concepts tailored to workflow pain-points."),
    REPORT_BUILDER("Report Structure Builder", "Establish targeted outlines, layout milestones, and structural blueprints for academic reports."),
    PRESENTATION_HELPER("Presentation Script Helper", "Detail slides organization, pacing guidelines, speech scripts, and formal transitions."),
    FEEDBACK_CHECKER("Final Feedback Checker", "Evaluate drafts on academic tone, critical argument support, and polish english expression.")
}

class StudyMateViewModel(private val repository: StudyAnalysisRepository) : ViewModel() {

    val inputText = MutableStateFlow("")
    val selectedLanguage = MutableStateFlow("English")
    val selectedMode = MutableStateFlow(StudyMode.ASSIGNMENT_ANALYZER)
    
    val isLoading = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    
    // The current active analysis result on screen
    val currentAnalysis = MutableStateFlow<StudyAnalysis?>(null)

    // Historical records from Room database
    val allAnalyses: StateFlow<List<StudyAnalysis>> = repository.allAnalyses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun updateInputText(text: String) {
        inputText.value = text
    }

    fun updateLanguage(lang: String) {
        selectedLanguage.value = lang
    }

    fun updateMode(mode: StudyMode) {
        selectedMode.value = mode
    }

    fun selectAnalysis(analysis: StudyAnalysis) {
        currentAnalysis.value = analysis
        // Pre-populate input for review
        inputText.value = analysis.inputPrompt
        selectedLanguage.value = analysis.language
        val matchedMode = StudyMode.values().find { it.displayName == analysis.mode }
        if (matchedMode != null) {
            selectedMode.value = matchedMode
        }
    }

    fun deleteAnalysis(analysis: StudyAnalysis) {
        viewModelScope.launch {
            repository.deleteById(analysis.id)
            if (currentAnalysis.value?.id == analysis.id) {
                currentAnalysis.value = null
            }
        }
    }

    fun clearAllAnalyses() {
        viewModelScope.launch {
            repository.deleteAll()
            currentAnalysis.value = null
        }
    }

    fun clearCurrent() {
        currentAnalysis.value = null
        inputText.value = ""
    }

    fun analyzeWithStudyMate() {
        val input = inputText.value.trim()
        if (input.isEmpty()) {
            error.value = "Please paste or type your academic requirements or ideas first!"
            return
        }

        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY" || apiKey == "GEMINI_API_KEY") {
            error.value = "Gemini API Key is not configured. Please configure your API key securely in the 'Secrets' panel in AI Studio."
            return
        }

        isLoading.value = true
        error.value = null

        viewModelScope.launch {
            try {
                val modeTitle = selectedMode.value.displayName
                val modeDesc = selectedMode.value.description
                val lang = selectedLanguage.value

                // Detailed prompting strategy to secure strict JSON output structure
                val promptText = """
                    You are Analyzing Academic Requirements for a student.
                    Mode Selected: $modeTitle ($modeDesc)
                    Target Language of Output: $lang
                    
                    Here is the Student's Input (Assignment prompt / Requirement brief / Project idea):
                    --------------------------------------------------
                    $input
                    --------------------------------------------------
                    
                    Analyze this input deeply and generate a structured academic response.
                    Your analytical output MUST follow the terms of AI Management Theory and Academic Development workflows.
                    You MUST return your response as a JSON object, written ENTIRELY in $lang.
                    
                    The JSON object MUST have exactly these 8 keys:
                    1. "mainTask": Clear summary of the primary academic expectation/assignment core task.
                    2. "problemToSolve": What workflow, educational or expression problem this solves, incorporating AI Management Theory perspective.
                    3. "targetUsers": Who is writing/studying (e.g., international students, target user definitions).
                    4. "userNeeds": Core needs, language hurdles, structural guidelines, or conceptual gaps that must be bridged for this user profile.
                    5. "keyFunctions": Functional blocks or study steps to carry out the assignment systematically.
                    6. "serviceConcept": Ideal AI Agent conceptual framework or service blueprint to carry out this assignment successfully.
                    7. "aiValue": Academic and professional explanation of how AI improves decision-making, productivity, creativity, and interaction in this context (AI Management Theory Class Workshop focus).
                    8. "developmentPlan": A practical, structured milestone project execution plan to research, write, and review the final deliverable.
                    
                    Make sure each section contains detailed paragraphs or bullet points written inside the String value of that JSON key. DO NOT provide empty values. Return raw JSON text without surrounding markdown fences.
                """.trimIndent()

                val systemPrompt = "You are 'AI StudyMate Agent', a highly professional academic support agent assisting international university students. Return your responses in valid JSON format only."

                val request = GenerateContentRequest(
                    contents = listOf(ContentRequest(parts = listOf(PartRequest(text = promptText)))),
                    generationConfig = GenerationConfig(responseMimeType = "application/json", temperature = 0.5),
                    systemInstruction = ContentRequest(parts = listOf(PartRequest(text = systemPrompt)))
                )

                val response = withContext(Dispatchers.IO) {
                    RetrofitClient.service.generateContent(apiKey, request)
                }

                val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (rawText != null) {
                    val cleanedJson = cleanJsonBody(rawText)
                    val parsed = RetrofitClient.parseAcademicJson(cleanedJson)
                    
                    if (parsed != null) {
                        // Generate a clean summary title for the history list
                        val cleanTitle = if (input.length > 30) input.take(30) + "..." else input
                        val finalAnalysis = StudyAnalysis(
                            title = "[$modeTitle] $cleanTitle",
                            inputPrompt = input,
                            mode = modeTitle,
                            language = lang,
                            mainTask = parsed.mainTask,
                            problemToSolve = parsed.problemToSolve,
                            targetUsers = parsed.targetUsers,
                            userNeeds = parsed.userNeeds,
                            keyFunctions = parsed.keyFunctions,
                            serviceConcept = parsed.serviceConcept,
                            aiValue = parsed.aiValue,
                            developmentPlan = parsed.developmentPlan
                        )

                        withContext(Dispatchers.IO) {
                            repository.insert(finalAnalysis)
                        }

                        currentAnalysis.value = finalAnalysis
                    } else {
                        // Parsing fallback: if JSON parsing failed but we got some text, let's parse it manually or make a readable draft.
                        fallbackResponse(rawText, input, modeTitle, lang)
                    }
                } else {
                    error.value = "Received an empty response from Gemini. Please try again."
                }
            } catch (e: Exception) {
                e.printStackTrace()
                error.value = "Failed to connect to StudyMate service: ${e.localizedMessage ?: e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun cleanJsonBody(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.substringAfter("```json")
        } else if (text.startsWith("```")) {
            text = text.substringAfter("```")
        }
        if (text.endsWith("```")) {
            text = text.substringBeforeLast("```")
        }
        return text.trim()
    }

    private suspend fun fallbackResponse(rawText: String, input: String, modeTitle: String, lang: String) {
        // Form a fallback if JSON structure is messy but holds descriptive text
        val title = if (input.length > 30) input.take(30) + "..." else input
        val finalAnalysis = StudyAnalysis(
            title = "[$modeTitle] $title",
            inputPrompt = input,
            mode = modeTitle,
            language = lang,
            mainTask = "Extracted Analysis:\n" + rawText.take(500),
            problemToSolve = "See details in Main Task. Analysis formatting was restored from raw output.",
            targetUsers = "International students facing assignment challenges.",
            userNeeds = "Guidance tailored to course goals.",
            keyFunctions = "Follow the structured raw report steps.",
            serviceConcept = "AI Academic Advisor Service Model.",
            aiValue = "Enhances student comprehension and structures work outline.",
            developmentPlan = "1. Refine analysis\n2. Structure draft timeline."
        )
        withContext(Dispatchers.IO) {
            repository.insert(finalAnalysis)
        }
        currentAnalysis.value = finalAnalysis
    }
}

class StudyMateViewModelFactory(private val repository: StudyAnalysisRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyMateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudyMateViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
