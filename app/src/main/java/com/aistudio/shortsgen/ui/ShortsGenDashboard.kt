package com.aistudio.shortsgen.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.provider.Settings
import com.aistudio.shortsgen.DashboardUiState
import com.aistudio.shortsgen.LogLevel
import com.aistudio.shortsgen.LogEntry
import com.aistudio.shortsgen.ShortsGenViewModel
import com.aistudio.shortsgen.data.AppSettings
import com.aistudio.shortsgen.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsGenDashboard(
    viewModel: ShortsGenViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    var isSettingsExpanded by remember { mutableStateOf(false) }

    // Settings fields (holding local edit state)
    var fastapiUrl by remember(uiState.settings.fastapiUrl) { mutableStateOf(uiState.settings.fastapiUrl) }
    var geminiApiKey by remember(uiState.settings.geminiApiKey) { mutableStateOf(uiState.settings.geminiApiKey) }
    var instagramUser by remember(uiState.settings.instagramUser) { mutableStateOf(uiState.settings.instagramUser) }
    var instagramPass by remember(uiState.settings.instagramPass) { mutableStateOf(uiState.settings.instagramPass) }
    var youtubeChannel by remember(uiState.settings.youtubeChannel) { mutableStateOf(uiState.settings.youtubeChannel) }
    var renderingThreads by remember(uiState.settings.renderingThreads) { mutableFloatStateOf(uiState.settings.renderingThreads.toFloat()) }
    var selectedVoice by remember(uiState.settings.selectedVoice) { mutableStateOf(uiState.settings.selectedVoice) }
    var isSimulationMode by remember(uiState.settings.isSimulationMode) { mutableStateOf(uiState.settings.isSimulationMode) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate900
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
        ) {
            // 1. Studio Header
            item {
                StudioHeader(
                    isSimulationMode = uiState.settings.isSimulationMode,
                    isPipelineRunning = uiState.isRendering || uiState.isGeneratingScript
                )
            }

            // 2. Configuration Panel (Expandable Card)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    border = BorderStroke(1.dp, Slate700)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSettingsExpanded = !isSettingsExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "⚙",
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(end = 8.dp),
                                    color = Sky400
                                )
                                Column {
                                    Text(
                                        text = "Studio Configuration",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Slate50
                                    )
                                    Text(
                                        text = if (isSimulationMode) "Simulation Mode Active" else "Connected to $fastapiUrl",
                                        fontSize = 12.sp,
                                        color = Slate400
                                    )
                                }
                            }
                            Text(
                                text = if (isSettingsExpanded) "▲" else "▼",
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                fontSize = 12.sp
                            )
                        }

                        AnimatedVisibility(visible = isSettingsExpanded) {
                            Column(
                                modifier = Modifier
                                    .padding(top = 16.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Divider(color = Slate700)

                                // Mode Toggle
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Pipeline Mode", fontWeight = FontWeight.Medium, color = Slate50, fontSize = 14.sp)
                                        Text(
                                            text = if (isSimulationMode) "Simulation (Run offline without backend)" else "Live Backend (FastAPI endpoint)",
                                            color = Slate400,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Switch(
                                        checked = !isSimulationMode,
                                        onCheckedChange = { isSimulationMode = !it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Emerald400,
                                            checkedTrackColor = Slate700,
                                            uncheckedThumbColor = Amber400,
                                            uncheckedTrackColor = Slate700
                                        )
                                    )
                                }

                                // FastAPI Endpoint URL
                                OutlinedTextField(
                                    value = fastapiUrl,
                                    onValueChange = { fastapiUrl = it },
                                    label = { Text("FastAPI Target URL") },
                                    placeholder = { Text("e.g. 10.0.2.2:8000") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Sky400,
                                        unfocusedBorderColor = Slate700,
                                        focusedLabelColor = Sky400,
                                        unfocusedLabelColor = Slate400,
                                        focusedTextColor = Slate50,
                                        unfocusedTextColor = Slate50
                                    ),
                                    enabled = !isSimulationMode
                                )

                                // Gemini API Key Keyring
                                OutlinedTextField(
                                    value = geminiApiKey,
                                    onValueChange = { geminiApiKey = it },
                                    label = { Text("Gemini API Key Keyring") },
                                    placeholder = { Text("Custom API Key (or fallback to BuildConfig)") },
                                    singleLine = true,
                                    visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                            Text(if (isApiKeyVisible) "👁" else "🔒", color = Slate400)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Sky400,
                                        unfocusedBorderColor = Slate700,
                                        focusedLabelColor = Sky400,
                                        unfocusedLabelColor = Slate400,
                                        focusedTextColor = Slate50,
                                        unfocusedTextColor = Slate50
                                    )
                                )

                                // Voice Selector
                                Text("Voice Selection", fontWeight = FontWeight.Medium, color = Slate50, fontSize = 14.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val voices = listOf(
                                        "en_us_male" to "Male US",
                                        "en_us_female" to "Female US",
                                        "en_uk_male" to "Male UK",
                                        "es_male" to "Spanish"
                                    )
                                    voices.forEach { (voiceId, label) ->
                                        val isSelected = selectedVoice == voiceId
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) Sky500 else Slate700)
                                                .clickable { selectedVoice = voiceId }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                color = if (isSelected) Slate950 else Slate100,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                // Rendering Threads Slider
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Rendering Threads: ${renderingThreads.toInt()}",
                                        fontWeight = FontWeight.Medium,
                                        color = Slate50,
                                        fontSize = 14.sp
                                    )
                                    Slider(
                                        value = renderingThreads,
                                        onValueChange = { renderingThreads = it },
                                        valueRange = 1f..16f,
                                        steps = 14,
                                        modifier = Modifier.width(180.dp),
                                        colors = SliderDefaults.colors(
                                            thumbColor = Sky400,
                                            activeTrackColor = Sky400,
                                            inactiveTrackColor = Slate700
                                        )
                                    )
                                }

                                Divider(color = Slate700)

                                // Automation Bot Permissions
                                Text("Automation Bot Configuration", fontWeight = FontWeight.Bold, color = Slate100, fontSize = 14.sp)
                                Text("To use the UI Automation Bot (AutoPublisher), you must grant Accessibility permissions in Android Settings.", color = Slate400, fontSize = 12.sp)
                                Button(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate700)
                                ) {
                                    Text("Grant Accessibility Permission", color = Slate100, fontWeight = FontWeight.Bold)
                                }

                                Divider(color = Slate700)

                                // Publisher Credentials
                                Text("Publisher Credentials (Optional)", fontWeight = FontWeight.Bold, color = Slate100, fontSize = 14.sp)
                                
                                OutlinedTextField(
                                    value = instagramUser,
                                    onValueChange = { instagramUser = it },
                                    label = { Text("Instagram Username") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Sky400,
                                        unfocusedBorderColor = Slate700,
                                        focusedTextColor = Slate50,
                                        unfocusedTextColor = Slate50
                                    )
                                )

                                OutlinedTextField(
                                    value = instagramPass,
                                    onValueChange = { instagramPass = it },
                                    label = { Text("Instagram Password") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Sky400,
                                        unfocusedBorderColor = Slate700,
                                        focusedTextColor = Slate50,
                                        unfocusedTextColor = Slate50
                                    )
                                )

                                OutlinedTextField(
                                    value = youtubeChannel,
                                    onValueChange = { youtubeChannel = it },
                                    label = { Text("YouTube Channel") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Sky400,
                                        unfocusedBorderColor = Slate700,
                                        focusedTextColor = Slate50,
                                        unfocusedTextColor = Slate50
                                    )
                                )

                                Button(
                                    onClick = {
                                        viewModel.saveSettings(
                                            AppSettings(
                                                fastapiUrl = fastapiUrl,
                                                geminiApiKey = geminiApiKey,
                                                instagramUser = instagramUser,
                                                instagramPass = instagramPass,
                                                youtubeChannel = youtubeChannel,
                                                renderingThreads = renderingThreads.toInt(),
                                                selectedVoice = selectedVoice,
                                                isSimulationMode = isSimulationMode
                                            )
                                        )
                                        isSettingsExpanded = false
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Sky500)
                                ) {
                                    Text("Apply & Save Configurations", color = Slate950, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 3. Script Generation Control Panel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    border = BorderStroke(1.dp, Slate700)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "⚡ Short-form Video Pipeline",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Slate50
                        )

                        // Topic prompt input
                        OutlinedTextField(
                            value = uiState.topicPrompt,
                            onValueChange = { viewModel.updateTopicPrompt(it) },
                            label = { Text("Video Topic Prompt") },
                            placeholder = { Text("e.g. 5 Shocking Facts About the Deep Ocean") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Violet400,
                                unfocusedBorderColor = Slate700,
                                focusedLabelColor = Violet400,
                                focusedTextColor = Slate50,
                                unfocusedTextColor = Slate50
                            )
                        )

                        // Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Generate script button (Gemini)
                            Button(
                                onClick = { viewModel.generateScript() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Violet500),
                                enabled = !uiState.isGeneratingScript && !uiState.isRendering
                            ) {
                                if (uiState.isGeneratingScript) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Slate950,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Generate Script", color = Slate50, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Render & publish button
                            Button(
                                onClick = { viewModel.renderAndPublish() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                enabled = !uiState.isGeneratingScript && !uiState.isRendering && uiState.generatedScript.isNotEmpty()
                            ) {
                                if (uiState.isRendering) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Slate950,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text("Render & Publish", color = Slate950, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Auto-Bot Row
                        Button(
                            onClick = { viewModel.triggerAutomationBot() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Amber500),
                            enabled = !uiState.isGeneratingScript && !uiState.isRendering && uiState.topicPrompt.isNotEmpty()
                        ) {
                            Text("🤖 Run Automation Bot (Veo + Insta + YT)", color = Slate950, fontWeight = FontWeight.Black)
                        }

                        // Generated script fields if they exist
                        if (uiState.generatedScript.isNotEmpty()) {
                            Divider(color = Slate700)
                            Text("Generated Script Draft", fontWeight = FontWeight.SemiBold, color = Slate100, fontSize = 14.sp)
                            
                            OutlinedTextField(
                                value = uiState.generatedScript,
                                onValueChange = { viewModel.updateScript(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 100.dp, max = 200.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Slate700,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Slate100,
                                    unfocusedTextColor = Slate100
                                )
                            )

                            Text("Hashtags", fontWeight = FontWeight.SemiBold, color = Slate100, fontSize = 14.sp)
                            OutlinedTextField(
                                value = uiState.generatedHashtags,
                                onValueChange = { viewModel.updateHashtags(it) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Slate700,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Slate100,
                                    unfocusedTextColor = Slate100
                                )
                            )
                        }
                    }
                }
            }

            // 4. Interactive Terminal Panel
            item {
                TerminalPanel(
                    logs = uiState.logs,
                    onClearLogs = { viewModel.clearLogs() }
                )
            }
        }
    }
}

@Composable
fun StudioHeader(
    isSimulationMode: Boolean,
    isPipelineRunning: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Name with Sky-to-Violet Gradient
            Text(
                text = "ShortsGen AI",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                style = LocalTextStyle.current.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Sky400, Violet400)
                    )
                )
            )

            // Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSimulationMode) Amber500.copy(alpha = 0.15f) else Emerald500.copy(alpha = 0.15f))
                    .border(
                        1.dp,
                        if (isSimulationMode) Amber500.copy(alpha = 0.4f) else Emerald500.copy(alpha = 0.4f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Pulsing dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isSimulationMode) Amber400 else Emerald400
                        )
                        .alpha(if (isPipelineRunning) alphaAnim else 1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSimulationMode) "SIMULATION" else "LIVE BACKEND",
                    color = if (isSimulationMode) Amber400 else Emerald400,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
        Text(
            text = "Automated Short-form Video Generation & Publishing Companion",
            color = Slate400,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun TerminalPanel(
    logs: List<LogEntry>,
    onClearLogs: () -> Unit
) {
    val listState = rememberLazyListState()

    // Auto-scroll when logs change
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        border = BorderStroke(1.dp, Slate700)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Terminal Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⌨",
                        fontSize = 16.sp,
                        color = Sky400,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "STU_TERMINAL_LOG",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Slate100,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = "Clear Logs",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Sky400,
                    modifier = Modifier
                        .clickable { onClearLogs() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Monospaced output box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Slate950)
                    .border(1.dp, Slate700, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "Terminal idle. Launch a pipeline stage to begin logging...",
                        style = TerminalTextStyle,
                        color = Slate600
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs) { entry ->
                            val textColor = when (entry.level) {
                                LogLevel.SYSTEM -> Slate100
                                LogLevel.GEMINI -> Violet400
                                LogLevel.FASTAPI -> Sky400
                                LogLevel.SUCCESS -> Emerald400
                                LogLevel.ERROR -> Rose400
                            }
                            Text(
                                text = "[${entry.timestamp}] ${entry.message}",
                                style = TerminalTextStyle,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}
