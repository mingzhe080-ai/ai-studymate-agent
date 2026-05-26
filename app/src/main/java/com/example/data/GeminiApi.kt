package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini REST API Request Models ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<ContentRequest>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: ContentRequest? = null
)

@JsonClass(generateAdapter = true)
data class ContentRequest(
    @Json(name = "parts") val parts: List<PartRequest>
)

@JsonClass(generateAdapter = true)
data class PartRequest(
    @Json(name = "text") val text: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = "application/json",
    @Json(name = "temperature") val temperature: Double? = 0.7
)

// --- Gemini REST API Response Models ---

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: ContentResponse?
)

@JsonClass(generateAdapter = true)
data class ContentResponse(
    @Json(name = "parts") val parts: List<PartResponse>?
)

@JsonClass(generateAdapter = true)
data class PartResponse(
    @Json(name = "text") val text: String?
)

// --- Structured Academic Output Model ---

@JsonClass(generateAdapter = true)
data class AcademicAnalysisJson(
    @Json(name = "mainTask") val mainTask: String,
    @Json(name = "problemToSolve") val problemToSolve: String,
    @Json(name = "targetUsers") val targetUsers: String,
    @Json(name = "userNeeds") val userNeeds: String,
    @Json(name = "keyFunctions") val keyFunctions: String,
    @Json(name = "serviceConcept") val serviceConcept: String,
    @Json(name = "aiValue") val aiValue: String,
    @Json(name = "developmentPlan") val developmentPlan: String,

    // NEW Feature: Structured Breakdown Sections
    @Json(name = "requiredOutput") val requiredOutput: String? = "",
    @Json(name = "importantKeywords") val importantKeywords: String? = "",
    @Json(name = "professorFocus") val professorFocus: String? = "",
    @Json(name = "suggestedSteps") val suggestedSteps: String? = "",

    // NEW Feature: AI Value Matrix quadrants
    @Json(name = "matrixProductivity") val matrixProductivity: String? = "",
    @Json(name = "matrixDecisionMaking") val matrixDecisionMaking: String? = "",
    @Json(name = "matrixCreativity") val matrixCreativity: String? = "",
    @Json(name = "matrixInteraction") val matrixInteraction: String? = "",

    // NEW Feature: Rubric Score Checker (0-5 per metric)
    @Json(name = "scoreProblemClarity") val scoreProblemClarity: Int? = 0,
    @Json(name = "scoreTargetClarity") val scoreTargetClarity: Int? = 0,
    @Json(name = "scoreServiceValue") val scoreServiceValue: Int? = 0,
    @Json(name = "scoreAiValue") val scoreAiValue: Int? = 0,
    @Json(name = "scoreFeasibility") val scoreFeasibility: Int? = 0,
    @Json(name = "scorePresentation") val scorePresentation: Int? = 0,
    @Json(name = "scoreSuggestions") val scoreSuggestions: String? = "",

    // NEW Feature: Presentation Script Generator
    @Json(name = "presentationPitch") val presentationPitch: String? = "",
    @Json(name = "presentationScript") val presentationScript: String? = "",
    @Json(name = "presentationQna") val presentationQna: String? = ""
)

// --- Retrofit API Interface ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    // Helper to parse the JSON string returned by Gemini into the structured object
    fun parseAcademicJson(jsonStr: String): AcademicAnalysisJson? {
        return try {
            val adapter = moshi.adapter(AcademicAnalysisJson::class.java)
            adapter.fromJson(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
