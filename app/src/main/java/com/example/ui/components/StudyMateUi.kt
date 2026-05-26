package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StudyAnalysis
import com.example.ui.StudyMode
import com.example.ui.StudyMateViewModel
import java.text.SimpleDateFormat
import java.util.*

// Dynamic scholastic micro-messages during API loading sequence to keep users engaged and informed
private val academicMessages = listOf(
    "Deconstructing syllabus expectations...",
    "Correlating with AI Management Theory modules...",
    "Refining structural outlines for international benchmarks...",
    "Translating academic expressions and vocabulary context...",
    "Evaluating interaction dynamics & productivity value...",
    "Drafting step-by-step milestone timeline..."
)

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun StudyMateAppScreen(
    viewModel: StudyMateViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val inputText by viewModel.inputText.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentAnalysis by viewModel.currentAnalysis.collectAsState()
    val repositoryAnalyses by viewModel.allAnalyses.collectAsState()

    var activeTab by remember { mutableStateOf("workspace") } // "workspace" or "history"

    // Toast configuration for copy operations or deletions
    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            AcademicTopBar()
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White, // High Density clean white background for bottom navigation
                tonalElevation = 2.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == "workspace",
                    onClick = { activeTab = "workspace" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001C38),
                        selectedTextColor = Color(0xFF001C38),
                        indicatorColor = Color(0xFFD3E4FF), // High density soft blue active pill
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("workspace_tab_button")
                )
                NavigationBarItem(
                    selected = activeTab == "history",
                    onClick = { activeTab = "history" },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF001C38),
                        selectedTextColor = Color(0xFF001C38),
                        indicatorColor = Color(0xFFD3E4FF), // Soft blue active pill
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("history_tab_button")
                )
            }
        },
        containerColor = Color(0xFFF7F9FB), // Off-white clean academic background
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    slideInHorizontally { width -> if (targetState == "history") width else -width } togetherWith
                    slideOutHorizontally { width -> if (targetState == "history") -width else width }
                },
                label = "TabTransition"
            ) { tab ->
                when (tab) {
                    "workspace" -> {
                        if (currentAnalysis != null) {
                            // If user already clicked and result is ready, display output scroll
                            OutputReportView(
                                analysis = currentAnalysis!!,
                                onBackToInput = {
                                    viewModel.clearCurrent()
                                },
                                onCopyFull = { reportText ->
                                    clipboardManager.setText(AnnotatedString(reportText))
                                    Toast.makeText(context, "Full academic report copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                onDelete = {
                                    viewModel.deleteAnalysis(currentAnalysis!!)
                                    Toast.makeText(context, "Report cleared from history log.", Toast.LENGTH_SHORT).show()
                                }
                            )
                        } else {
                            // Default interactive input screen
                            WorkspaceView(
                                inputText = inputText,
                                onInputTextChange = { viewModel.updateInputText(it) },
                                selectedLanguage = selectedLanguage,
                                onLanguageSelect = { viewModel.updateLanguage(it) },
                                selectedMode = selectedMode,
                                onModeSelect = { viewModel.updateMode(it) },
                                onAnalyzeClick = { viewModel.analyzeWithStudyMate() },
                                isLoading = isLoading
                            )
                        }
                    }
                    "history" -> {
                        HistoryView(
                            analyses = repositoryAnalyses,
                            onSelect = { analysis ->
                                viewModel.selectAnalysis(analysis)
                                activeTab = "workspace" // open directly in workspace
                            },
                            onDelete = { analysis ->
                                viewModel.deleteAnalysis(analysis)
                                Toast.makeText(context, "Deleted from history.", Toast.LENGTH_SHORT).show()
                            },
                            onClearAll = {
                                viewModel.clearAllAnalyses()
                                Toast.makeText(context, "All history logs cleared.", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // High priority loading blocker overlay to emphasize academic processing
            if (isLoading) {
                AcademicLoadingOverlay()
            }
        }
    }
}

@Composable
fun AcademicTopBar() {
    Surface(
        color = Color(0xFFF7F9FB), // Matches exactly the canvas background `#F7F9FB`
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Assignment Analyzer",
                color = Color(0xFF005AC1), // Exactly `#005AC1`
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Requirement Analyzer for International Students",
                color = Color(0xFF64748B), // Slate-500 (#64748B) text
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun WorkspaceView(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    selectedLanguage: String,
    onLanguageSelect: (String) -> Unit,
    selectedMode: StudyMode,
    onModeSelect: (StudyMode) -> Unit,
    onAnalyzeClick: () -> Unit,
    isLoading: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcome and Academic workshop header introduction block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Hello, Student! 👋",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF005AC1)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This premium analyzer is designed specifically for international university students studying in English or Korean-medium classes. Paste your assignment prompt, syllabus brief, or requirements below. We will break them down systematically and generate structural study strategies aligning with AI Management Theory core principles.",
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 17.sp
                    )
                }
            }
        }

        // Section 1: Requirement input paste box
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Assignment,
                        contentDescription = null,
                        tint = Color(0xFF005AC1),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Assignment Instructions / Requirement Brief",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )
                }
                
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChange,
                    placeholder = {
                        Text(
                            text = "Paste assignment description, syllabus prompt guidelines, paper structure, or project concept details here...",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("input_text_field"),
                    textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF005AC1),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(
                                onClick = { onInputTextChange("") },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("clear_input_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear Input",
                                    tint = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                )
            }
        }

        // Section 2: Country / Target Output Language Selector
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color(0xFF005AC1),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Target Report Language",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("English", "Chinese", "Korean").forEach { lang ->
                        val isSelected = selectedLanguage == lang
                        FilterChip(
                            selected = isSelected,
                            onClick = { onLanguageSelect(lang) },
                            label = { 
                                Text(
                                    text = lang, 
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFD3E4FF), // High Density highlight
                                selectedLabelColor = Color(0xFF001C38),      // Deep navy/dark blue text
                                containerColor = Color.White,
                                labelColor = Color(0xFF475569)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF005AC1) else Color(0xFFCBD5E1)
                            ),
                            leadingIcon = {
                                val flagIcon = when (lang) {
                                    "English" -> Icons.Outlined.Translate
                                    "Chinese" -> Icons.Outlined.CheckCircle
                                    else -> Icons.Outlined.AccountCircle
                                }
                                Icon(
                                    imageVector = flagIcon, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(14.dp),
                                    tint = if (isSelected) Color(0xFF001C38) else Color(0xFF64748B)
                                )
                            },
                            modifier = Modifier
                                .weight(1.0f)
                                .height(40.dp)
                                .testTag("lang_chip_${lang.lowercase()}")
                        )
                    }
                }
            }
        }

        // Section 3: Study Analysis Mode Card List
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Grid3x3,
                        contentDescription = null,
                        tint = Color(0xFF005AC1),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Study Analysis Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF1E293B)
                    )
                }

                StudyMode.values().forEach { mode ->
                    val isSelected = selectedMode == mode
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White // Elegant light blue background
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF005AC1) else Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelect(mode) }
                            .testTag("mode_chip_${mode.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF005AC1) else Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                val modeIcon = when (mode) {
                                    StudyMode.ASSIGNMENT_ANALYZER -> Icons.Default.Analytics
                                    StudyMode.IDEA_GENERATOR -> Icons.Default.Psychology
                                    StudyMode.REPORT_BUILDER -> Icons.Default.ListAlt
                                    StudyMode.PRESENTATION_HELPER -> Icons.Default.Slideshow
                                    StudyMode.FEEDBACK_CHECKER -> Icons.Default.FactCheck
                                }
                                Icon(
                                    imageVector = modeIcon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF475569),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = mode.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color(0xFF005AC1) else Color(0xFF1E293B)
                                )
                                Text(
                                    text = mode.description,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 13.sp
                                )
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = { onModeSelect(mode) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF005AC1),
                                    unselectedColor = Color(0xFFCBD5E1)
                                )
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Trigger Analysis Button (Elevated Academic Button)
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = onAnalyzeClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF005AC1), // Matches exactly `#005AC1`
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp), // High Density active prompt style
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("analyze_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Analyze with StudyMate",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun OutputReportView(
    analysis: StudyAnalysis,
    onBackToInput: () -> Unit,
    onCopyFull: (String) -> Unit,
    onDelete: () -> Unit
) {
    // Generate simple compiled text version of the entire report for easy copy-paste
    val fullReportText = """
        *** AI STUDYMATE AGENT REPORT ***
        Mode: ${analysis.mode}
        Language: ${analysis.language}
        Topic Title: ${analysis.title}
        
        [MAIN TASK]
        ${analysis.mainTask}
        
        [PROBLEM TO SOLVE]
        ${analysis.problemToSolve}
        
        [TARGET USERS]
        ${analysis.targetUsers}
        
        [USER NEEDS]
        ${analysis.userNeeds}
        
        [KEY FUNCTIONS]
        ${analysis.keyFunctions}
        
        [SERVICE CONCEPT]
        ${analysis.serviceConcept}
        
        [AI VALUE - MANAGEMENT THEORY]
        ${analysis.aiValue}
        
        [DEVELOPMENT TIMELINE PLAN]
        ${analysis.developmentPlan}
        
        Generated by StudyMate Academic Support Agent on ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(analysis.timestamp))}.
    """.trimIndent()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Output title header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBackToInput,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF005AC1)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Prompt", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onCopyFull(fullReportText) },
                        modifier = Modifier
                            .testTag("copy_report_button")
                            .background(Color(0xFFD3E4FF), RoundedCornerShape(10.dp))
                            .size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Report Plan",
                            tint = Color(0xFF001C38),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .background(Color(0xFFFEF2F2), RoundedCornerShape(10.dp))
                            .size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove Analysis",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Active State Brief Header card (matching High Density setup)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF001C38)), // Deep Navy header
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFFD3E4FF),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = analysis.mode.replace("_", " "), 
                                fontSize = 9.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = Color(0xFF001C38),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = analysis.language, 
                                fontSize = 9.sp, 
                                fontWeight = FontWeight.Bold, 
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Report: ${analysis.title}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Formulated dynamically based on AI Management Theory class workshop methodology.",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        // Output section structured dashboard
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section Title
                Text(
                    text = "ACADEMIC STRATEGY REPORT",
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )

                // 1. Professor Requirement Checklist Card
                ProfessorChecklistCard(analysis = analysis)

                // 2. Assignment Breakdown Card
                AssignmentBreakdownCard(rawText = analysis.mainTask)

                // 3. AI Value Matrix Card
                AiValueMatrixCard(rawText = analysis.aiValue)

                // 4. Problem card
                AcademicDetailCard(
                    title = "Problem to Solve & Pain Points",
                    content = analysis.problemToSolve,
                    icon = Icons.Default.ErrorOutline,
                    iconColor = Color(0xFFDC2626)
                )

                // 5. Target Users Card
                AcademicDetailCard(
                    title = "Target Student Profile Focus",
                    content = analysis.targetUsers,
                    icon = Icons.Default.Groups,
                    iconColor = Color(0xFF0284C7)
                )

                // 6. User Needs Card
                AcademicDetailCard(
                    title = "Academic & Language Hurdles",
                    content = analysis.userNeeds,
                    icon = Icons.Default.Psychology,
                    iconColor = Color(0xFFD97706)
                )

                // 7. Key Functions Card
                AcademicDetailCard(
                    title = "Proposed System / App Key Functions",
                    content = analysis.keyFunctions,
                    icon = Icons.Default.Build,
                    iconColor = Color(0xFF0F766E)
                )

                // 8. Service Value Card
                AcademicDetailCard(
                    title = "Service Value & Conceptual Framework",
                    content = analysis.serviceConcept,
                    icon = Icons.Default.Star,
                    iconColor = Color(0xFF7C3AED),
                    borderColor = Color(0xFFDDD6FE),
                    backgroundColor = Color(0xFFFAF5FF)
                )

                // 9. Development Plan Card
                AcademicDetailCard(
                    title = "Development Timeline & Milestone Plan",
                    content = analysis.developmentPlan,
                    icon = Icons.Default.Timeline,
                    iconColor = Color(0xFF2563EB)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

// --- Parse Helpers for Assignment Breakdown and AI Value Matrix ---

fun parseAssignmentBreakdown(rawText: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val lines = rawText.split("\n")
    var currentKey = "Main Task"
    val sb = StringBuilder()
    
    val keys = listOf("Main Task", "Required Output", "Important Keywords", "What the Professor Cares About", "Suggested Next Steps")
    
    for (line in lines) {
        val trimmed = line.trim()
        var foundKey = false
        for (key in keys) {
            val pattern1 = "$key:"
            val pattern2 = "- $key:"
            val pattern3 = "**$key**:"
            val pattern4 = "**$key:**"
            if (trimmed.startsWith(pattern1, ignoreCase = true) || 
                trimmed.startsWith(pattern2, ignoreCase = true) || 
                trimmed.startsWith(pattern3, ignoreCase = true) || 
                trimmed.startsWith(pattern4, ignoreCase = true)) {
                if (sb.isNotEmpty()) {
                    map[currentKey] = sb.toString().trim()
                    sb.clear()
                }
                currentKey = key
                val idx = trimmed.indexOf(":")
                val content = if (idx != -1) trimmed.substring(idx + 1).trim() else ""
                sb.append(content)
                foundKey = true
                break
            }
        }
        if (!foundKey) {
            if (sb.isNotEmpty()) sb.append("\n")
            sb.append(trimmed)
        }
    }
    if (sb.isNotEmpty()) {
        map[currentKey] = sb.toString().trim()
    }
    return map
}

fun parseValueMatrix(rawText: String): Map<String, String> {
    val map = mutableMapOf<String, String>()
    val lines = rawText.split("\n")
    var currentKey = "Productivity"
    val sb = StringBuilder()
    
    val keys = listOf("Productivity", "Decision-making", "Creativity", "Interaction")
    
    for (line in lines) {
        val trimmed = line.trim()
        var foundKey = false
        for (key in keys) {
            val pattern1 = "$key:"
            val pattern2 = "- $key:"
            val pattern3 = "**$key**:"
            val pattern4 = "**$key:**"
            if (trimmed.startsWith(pattern1, ignoreCase = true) || 
                trimmed.startsWith(pattern2, ignoreCase = true) || 
                trimmed.startsWith(pattern3, ignoreCase = true) || 
                trimmed.startsWith(pattern4, ignoreCase = true)) {
                if (sb.isNotEmpty()) {
                    map[currentKey] = sb.toString().trim()
                    sb.clear()
                }
                currentKey = key
                val idx = trimmed.indexOf(":")
                val content = if (idx != -1) trimmed.substring(idx + 1).trim() else ""
                sb.append(content)
                foundKey = true
                break
            }
        }
        if (!foundKey) {
            if (sb.isNotEmpty()) sb.append("\n")
            sb.append(trimmed)
        }
    }
    if (sb.isNotEmpty()) {
        map[currentKey] = sb.toString().trim()
    }
    return map
}

// --- Custom Presentation Cards ---

@Composable
fun ProfessorChecklistCard(analysis: StudyAnalysis) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)), // Beautiful Light Mint 
        border = BorderStroke(1.5.dp, Color(0xFF15803D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FactCheck,
                    contentDescription = null,
                    tint = Color(0xFF166534),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Professor's Requirement Checklist",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF166534),
                    letterSpacing = (-0.3).sp
                )
            }
            Text(
                text = "These points fulfill critical criteria for an AI Management Theory Class project focused on international university challenges:",
                fontSize = 11.sp,
                color = Color(0xFF15803D),
                lineHeight = 15.sp
            )
            
            HorizontalDivider(color = Color(0xFFDCFCE7), thickness = 1.dp)

            ChecklistItemBox(
                badge = "Problem Solved",
                question = "What problem does the AI agent/service solve?",
                answer = analysis.problemToSolve
            )
            ChecklistItemBox(
                badge = "Target Users",
                question = "Who are the target international students?",
                answer = analysis.targetUsers
            )
            ChecklistItemBox(
                badge = "Service Value",
                question = "What primary value or experience does the service provide?",
                answer = analysis.serviceConcept
            )
            ChecklistItemBox(
                badge = "AI Capability Matrix",
                question = "How does AI improve decision-making, productivity, creativity, and interaction?",
                answer = "Addressed fully via the four distinct quadrants of the AI Value Matrix (Productivity, Decision-Making, Creativity, Interaction) to help students excel."
            )
        }
    }
}

@Composable
fun ChecklistItemBox(badge: String, question: String, answer: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Fulfills requirements",
            tint = Color(0xFF16A34A),
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = badge,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(Color(0xFF16A34A), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = question,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF166534)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = answer.take(130) + if (answer.length > 130) "..." else "",
                fontSize = 11.sp,
                color = Color(0xFF166534),
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun AssignmentBreakdownCard(rawText: String) {
    val parsed = remember(rawText) { parseAssignmentBreakdown(rawText) }
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = Color(0xFF005AC1),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Assignment Breakdown Plan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
            
            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            val items = listOf(
                Triple("Main Task", parsed["Main Task"] ?: parsed.values.firstOrNull() ?: rawText, Color(0xFF1E3A8A)),
                Triple("Required Output", parsed["Required Output"] ?: "Analyze according to syllabus and specified structures.", Color(0xFF0F766E)),
                Triple("Important Keywords", parsed["Important Keywords"] ?: "AI, Management Theory, Assignment Metrics", Color(0xFFB45309)),
                Triple("What the Professor Cares About", parsed["What the Professor Cares About"] ?: "Strong logical ties, clear target audience, and academic rigor.", Color(0xFF7C3AED)),
                Triple("Suggested Next Steps", parsed["Suggested Next Steps"] ?: "1. Deconstruct instructions\n2. Outline core components\n3. Proofread drafts", Color(0xFF0369A1))
            )

            items.forEach { (label, content, iconColor) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = label.uppercase(Locale.ROOT),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconColor,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = content,
                        fontSize = 11.sp,
                        color = Color(0xFF334155),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AiValueMatrixCard(rawText: String) {
    val parsed = remember(rawText) { parseValueMatrix(rawText) }
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)), // Light blue workspace color
        border = BorderStroke(1.5.dp, Color(0xFF3B82F6)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF1D4ED8),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "AI Value Matrix",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D4ED8)
                )
            }
            Text(
                text = "Four-dimensional analysis of how AI improves student outcomes aligned with AI Management Theory:",
                fontSize = 11.sp,
                color = Color(0xFF1D4ED8),
                lineHeight = 14.sp
            )
            
            HorizontalDivider(color = Color(0xFFDBEAFE), thickness = 1.dp)

            // 4 Quadrants
            val quadrants = listOf(
                Triple("Productivity", parsed["Productivity"] ?: "Accelerates analysis of core materials and automates early draft structures.", Color(0xFF2563EB)),
                Triple("Decision-making", parsed["Decision-making"] ?: "Highlights critical parameters from syllabus to prevent logical scope drift.", Color(0xFF059669)),
                Triple("Creativity", parsed["Creativity"] ?: "Proposes alternative perspectives, unique case study links, and conceptual frameworks.", Color(0xFFD97706)),
                Triple("Interaction", parsed["Interaction"] ?: "Refines tone transitions, translates specialized texts, and supports presentation layouts.", Color(0xFF7C3AED))
            )

            quadrants.forEach { (title, description, color) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        val icon = when (title) {
                            "Productivity" -> Icons.Default.CheckCircle
                            "Decision-making" -> Icons.Default.List
                            "Creativity" -> Icons.Default.Lightbulb
                            else -> Icons.Default.Language
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = description,
                            fontSize = 11.sp,
                            color = Color(0xFF334155),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AcademicDetailCard(
    title: String,
    content: String,
    icon: ImageVector,
    iconColor: Color,
    borderColor: Color = Color(0xFFE2E8F0),
    backgroundColor: Color = Color.White
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
            
            Text(
                text = content,
                fontSize = 11.7.sp,
                color = Color(0xFF334155),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun HistoryView(
    analyses: List<StudyAnalysis>,
    onSelect: (StudyAnalysis) -> Unit,
    onDelete: (StudyAnalysis) -> Unit,
    onClearAll: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Report History Log",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1E293B)
            )

            if (analyses.isNotEmpty()) {
                TextButton(
                    onClick = onClearAll,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (analyses.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFFCBD5E1),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No saved analysis logs yet.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Paste assignments in the 'Workspace' tab to construct academic studies. They will be logged securely here.",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(analyses) { analysis ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(analysis) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                val logIcon = when (analysis.mode) {
                                    StudyMode.ASSIGNMENT_ANALYZER.displayName -> Icons.Default.Analytics
                                    StudyMode.IDEA_GENERATOR.displayName -> Icons.Default.Psychology
                                    StudyMode.REPORT_BUILDER.displayName -> Icons.Default.ListAlt
                                    StudyMode.PRESENTATION_HELPER.displayName -> Icons.Default.Slideshow
                                    else -> Icons.Default.FactCheck
                                }
                                Icon(
                                    imageVector = logIcon,
                                    contentDescription = null,
                                    tint = Color(0xFF1E3A8A),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = analysis.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF1E293B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = analysis.language,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F766E)
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(analysis.timestamp)),
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onDelete(analysis) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Item",
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AcademicLoadingOverlay() {
    var messageIndex by remember { mutableStateOf(0) }
    
    // Cycle scholastic micro-messages during process execution
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            messageIndex = (messageIndex + 1) % academicMessages.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(enabled = false, onClick = {}),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .width(280.dp)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF0F766E),
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = "Generating Academic Insights",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF1E293B)
                )

                Text(
                    text = academicMessages[messageIndex],
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Inline delay helper to avoid adding standard coroutines delay directly inside raw Composable context of loop
private suspend fun delay(timeMs: Long) {
    kotlinx.coroutines.delay(timeMs)
}
