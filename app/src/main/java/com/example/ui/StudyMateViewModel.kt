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
            try {
                repository.deleteById(analysis.id)
                if (currentAnalysis.value?.id == analysis.id) {
                    currentAnalysis.value = null
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                error.value = "Failed to delete log: ${e.localizedMessage ?: e.message}"
            }
        }
    }

    fun clearAllAnalyses() {
        viewModelScope.launch {
            try {
                repository.deleteAll()
                currentAnalysis.value = null
            } catch (e: Throwable) {
                e.printStackTrace()
                error.value = "Failed to clear history log: ${e.localizedMessage ?: e.message}"
            }
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
                    You are Analyzing Academic Requirements for an AI StudyMate Agent, a service focusing on "Assignment Requirement Analyzer for International Students".
                    Mode Selected: $modeTitle ($modeDesc)
                    Target Language of Output: $lang
                    
                    Here is the Student's Input (Assignment prompt / Requirement brief / Project idea):
                    --------------------------------------------------
                    $input
                    --------------------------------------------------
                    
                    Analyze this input deeply and generate a structured academic response.
                    All analysis and output MUST focus strictly on international university students who study in English-medium or Korean-medium classes. Highlight their specific needs, language challenges, and study workflows.
                    
                    You MUST return your response as a JSON object, written ENTIRELY in $lang.
                    
                    The JSON object MUST have exactly these 8 keys:
                    1. "mainTask": A structured Assignment Breakdown written exactly with these headers:
                       Main Task: [Detailed description of the primary academic core task]
                       Required Output: [Specific deliverables, formats, word count, length, etc.]
                       Important Keywords: [Key terms or concepts extracted from the prompt]
                       What the Professor Cares About: [Grading metrics, main assessment criteria, or focus areas]
                       Suggested Next Steps: [Detailed bullet points on immediate tactical next steps]
                       
                    2. "problemToSolve": What workflow or academic problem this AI agent idea/service solves, incorporating linguistic barriers, study anxiety, and AI Management Theory perspective.
                    
                    3. "targetUsers": Define the specific target student profile (international students studying in English or Korean-medium classes, highlighting their cultural and educational background).
                    
                    4. "userNeeds": Core academic and language needs, lecture/materials comprehension gaps, or reference search struggles of these students.
                    
                    5. "keyFunctions": Systematic functional blocks or concrete study features needed to address those needs.
                    
                    6. "serviceConcept": Explain the overall "Service Value" proposition, what experience/outcome it provides, and how it acts as an intelligent partner.
                    
                    7. "aiValue": An AI Value Matrix structured exactly with these headers:
                       Productivity: [Detailed explanation of how AI improves study productivity, reading speed, or formatting efficiency]
                       Decision-making: [Detailed explanation of how AI supports evaluation of ideas, logic checkers, or reference relevance judgment]
                       Creativity: [Detailed explanation of how AI helps brainstorm novel angles, structure arguments, or write expressive drafts]
                       Interaction: [Detailed explanation of how AI aids language polishing, terminology translation, speech rehearsal, or interactive feedback]
                       
                    8. "developmentPlan": A practical, structured milestone project compilation plan detailing research, draft generation, and final review check steps.
                    
                    Make sure each section is very detailed and uses bullet points formatting to be highly readable. DO NOT return empty keys. Return RAW JSON text without markdown formatting blocks.
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
            } catch (e: Throwable) {
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
