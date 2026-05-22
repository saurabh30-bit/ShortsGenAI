package com.aistudio.shortsgen.ui

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // Background Gradient Animation
    val infiniteTransition = rememberInfiniteTransition(label = "bg_anim")
    val bgOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_offset"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Slate950, Slate900, Color(0xFF0F172A), Slate950),
                    start = Offset(0f, bgOffset),
                    end = Offset(bgOffset, 2000f)
                )
            )
    ) {
        // Glowing ambient orbs
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-100).dp)
                .size(300.dp)
                .blur(100.dp)
                .background(Violet500.copy(alpha = 0.15f), CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 100.dp, y = 100.dp)
                .size(350.dp)
                .blur(120.dp)
                .background(Sky500.copy(alpha = 0.15f), CircleShape)
        )

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp)
            ) {
                // 1. Studio Header
                item {
                    StudioHeader(
                        isSimulationMode = uiState.settings.isSimulationMode,
                        isPipelineRunning = uiState.isRendering || uiState.isGeneratingScript
                    )
                }

                // 2. Configuration Panel (Glassmorphic Card)
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isSettingsExpanded = !isSettingsExpanded },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Settings",
                                        tint = Sky400,
                                        modifier = Modifier
                                            .size(28.dp)
                                            .padding(end = 12.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Studio Configuration",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 17.sp,
                                            color = Slate50
                                        )
                                        Text(
                                            text = if (isSimulationMode) "Simulation Mode Active" else "Connected to \$fastapiUrl",
                                            fontSize = 12.sp,
                                            color = Slate400
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isSettingsExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Expand",
                                    tint = Slate400
                                )
                            }

                            AnimatedVisibility(visible = isSettingsExpanded) {
                                Column(
                                    modifier = Modifier
                                        .padding(top = 20.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    HorizontalDivider(color = Slate700.copy(alpha = 0.5f))

                                    // Mode Toggle
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Pipeline Mode", fontWeight = FontWeight.Bold, color = Slate50, fontSize = 14.sp)
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
                                                checkedTrackColor = Emerald900.copy(alpha = 0.5f),
                                                uncheckedThumbColor = Amber400,
                                                uncheckedTrackColor = Amber900.copy(alpha = 0.5f)
                                            )
                                        )
                                    }

                                    PremiumTextField(
                                        value = fastapiUrl,
                                        onValueChange = { fastapiUrl = it },
                                        label = "FastAPI Target URL",
                                        enabled = !isSimulationMode
                                    )

                                    PremiumTextField(
                                        value = geminiApiKey,
                                        onValueChange = { geminiApiKey = it },
                                        label = "Gemini API Key Keyring",
                                        isPassword = !isApiKeyVisible,
                                        trailingIcon = {
                                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                                Icon(
                                                    imageVector = if (isApiKeyVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                                    contentDescription = "Toggle Visibility",
                                                    tint = Slate400
                                                )
                                            }
                                        }
                                    )

                                    // Voice Selector
                                    Text("Voice Selection", fontWeight = FontWeight.Bold, color = Slate50, fontSize = 14.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) Sky500 else Slate800.copy(alpha = 0.5f))
                                                    .border(1.dp, if (isSelected) Sky400 else Slate700, RoundedCornerShape(12.dp))
                                                    .clickable { selectedVoice = voiceId }
                                                    .padding(vertical = 10.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSelected) Slate950 else Slate100,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.sp,
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
                                            text = "Threads: \${renderingThreads.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            color = Slate50,
                                            fontSize = 14.sp
                                        )
                                        Slider(
                                            value = renderingThreads,
                                            onValueChange = { renderingThreads = it },
                                            valueRange = 1f..16f,
                                            steps = 14,
                                            modifier = Modifier.width(200.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = Sky400,
                                                activeTrackColor = Sky400,
                                                inactiveTrackColor = Slate700.copy(alpha = 0.5f)
                                            )
                                        )
                                    }

                                    HorizontalDivider(color = Slate700.copy(alpha = 0.5f))

                                    // Automation Bot Permissions
                                    Text("Automation Bot Configuration", fontWeight = FontWeight.Bold, color = Slate100, fontSize = 14.sp)
                                    Text("To use the UI Automation Bot (AutoPublisher), you must grant Accessibility permissions in Android Settings.", color = Slate400, fontSize = 12.sp)
                                    
                                    GradientButton(
                                        text = "Grant Accessibility Permission",
                                        colors = listOf(Slate700, Slate800),
                                        textColor = Slate100,
                                        onClick = {
                                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                            context.startActivity(intent)
                                        }
                                    )

                                    HorizontalDivider(color = Slate700.copy(alpha = 0.5f))

                                    // Publisher Credentials
                                    Text("Publisher Credentials (Optional)", fontWeight = FontWeight.Bold, color = Slate100, fontSize = 14.sp)
                                    
                                    PremiumTextField(value = instagramUser, onValueChange = { instagramUser = it }, label = "Instagram Username")
                                    PremiumTextField(value = instagramPass, onValueChange = { instagramPass = it }, label = "Instagram Password", isPassword = true)
                                    PremiumTextField(value = youtubeChannel, onValueChange = { youtubeChannel = it }, label = "YouTube Channel")

                                    Spacer(modifier = Modifier.height(8.dp))

                                    GradientButton(
                                        text = "Apply & Save Configurations",
                                        colors = listOf(Sky400, Blue500),
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
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Script Generation Control Panel
                item {
                    GlassCard {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = "Magic",
                                    tint = Violet400,
                                    modifier = Modifier.size(24.dp).padding(end = 8.dp)
                                )
                                Text(
                                    text = "Short-form Video Pipeline",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = Slate50
                                )
                            }

                            // Topic prompt input
                            PremiumTextField(
                                value = uiState.topicPrompt,
                                onValueChange = { viewModel.updateTopicPrompt(it) },
                                label = "Video Topic Prompt",
                                placeholder = "e.g. 5 Shocking Facts About the Deep Ocean",
                                highlightColor = Violet400
                            )

                            // Action Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Generate script button
                                GradientButton(
                                    text = "Generate Script",
                                    colors = listOf(Violet400, Fuchsia500),
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isGeneratingScript && !uiState.isRendering,
                                    isLoading = uiState.isGeneratingScript,
                                    icon = Icons.Rounded.AutoAwesome
                                ) { viewModel.generateScript() }

                                // Render & publish button
                                GradientButton(
                                    text = "Render & Publish",
                                    colors = listOf(Emerald400, Teal500),
                                    modifier = Modifier.weight(1f),
                                    enabled = !uiState.isGeneratingScript && !uiState.isRendering && uiState.generatedScript.isNotEmpty(),
                                    isLoading = uiState.isRendering,
                                    icon = Icons.Rounded.Publish
                                ) { viewModel.renderAndPublish() }
                            }

                            // Auto-Bot Row
                            GradientButton(
                                text = "Run Automation Bot (Veo + Insta + YT)",
                                colors = listOf(Amber400, Orange500),
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isGeneratingScript && !uiState.isRendering && uiState.topicPrompt.isNotEmpty(),
                                icon = Icons.Rounded.SmartToy
                            ) { viewModel.triggerAutomationBot() }

                            // Generated script fields if they exist
                            if (uiState.generatedScript.isNotEmpty()) {
                                HorizontalDivider(color = Slate700.copy(alpha = 0.5f))
                                Text("Generated Script Draft", fontWeight = FontWeight.Bold, color = Slate100, fontSize = 14.sp)
                                
                                PremiumTextField(
                                    value = uiState.generatedScript,
                                    onValueChange = { viewModel.updateScript(it) },
                                    label = "",
                                    singleLine = false,
                                    modifier = Modifier.heightIn(min = 120.dp, max = 250.dp)
                                )

                                Text("Hashtags", fontWeight = FontWeight.Bold, color = Slate100, fontSize = 14.sp)
                                PremiumTextField(
                                    value = uiState.generatedHashtags,
                                    onValueChange = { viewModel.updateHashtags(it) },
                                    label = ""
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
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Name with animated gradient
            val gradientColors = listOf(Sky400, Violet400, Fuchsia400)
            
            Text(
                text = "ShortsGen AI",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                style = LocalTextStyle.current.copy(
                    brush = Brush.linearGradient(colors = gradientColors)
                ),
                letterSpacing = (-0.5).sp
            )

            // Status Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSimulationMode) Amber500.copy(alpha = 0.15f) else Emerald500.copy(alpha = 0.15f))
                    .border(
                        1.dp,
                        if (isSimulationMode) Amber500.copy(alpha = 0.3f) else Emerald500.copy(alpha = 0.3f),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Pulsing dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isSimulationMode) Amber400 else Emerald400)
                        .alpha(if (isPipelineRunning) alphaAnim else 1f)
                        .shadow(if (isPipelineRunning) 8.dp else 0.dp, spotColor = if (isSimulationMode) Amber400 else Emerald400)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSimulationMode) "SIMULATION" else "LIVE BACKEND",
                    color = if (isSimulationMode) Amber400 else Emerald400,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }
        Text(
            text = "Automated Short-form Video Studio",
            color = Slate300,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun GlassCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900.copy(alpha = 0.65f)),
        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.5f))
    ) {
        content()
    }
}

@Composable
fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    highlightColor: Color = Sky400,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = if (label.isNotEmpty()) { { Text(label, fontWeight = FontWeight.SemiBold) } } else null,
        placeholder = if (placeholder.isNotEmpty()) { { Text(placeholder) } } else null,
        singleLine = singleLine,
        enabled = enabled,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = highlightColor,
            unfocusedBorderColor = Slate700.copy(alpha = 0.8f),
            disabledBorderColor = Slate800,
            focusedLabelColor = highlightColor,
            unfocusedLabelColor = Slate400,
            focusedTextColor = Slate50,
            unfocusedTextColor = Slate100,
            disabledTextColor = Slate500,
            focusedContainerColor = Slate950.copy(alpha = 0.5f),
            unfocusedContainerColor = Slate950.copy(alpha = 0.3f),
            disabledContainerColor = Slate950.copy(alpha = 0.1f)
        )
    )
}

@Composable
fun GradientButton(
    text: String,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    textColor: Color = Slate950,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled) Brush.horizontalGradient(colors) 
                else Brush.horizontalGradient(listOf(Slate700, Slate800))
            )
            .clickable(enabled = enabled && !isLoading, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = textColor,
                strokeWidth = 3.dp
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(20.dp).padding(end = 8.dp)
                    )
                }
                Text(
                    text = text,
                    color = if (enabled) textColor else Slate500,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
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

    GlassCard {
        Column(modifier = Modifier.padding(16.dp)) {
            // Terminal Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Terminal,
                        contentDescription = "Terminal",
                        tint = Sky400,
                        modifier = Modifier.size(20.dp).padding(end = 8.dp)
                    )
                    Text(
                        text = "STU_TERMINAL_LOG",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Slate100,
                        letterSpacing = 1.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate800.copy(alpha = 0.5f))
                        .clickable { onClearLogs() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Clear",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Sky400
                    )
                }
            }

            // Monospaced output box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate950.copy(alpha = 0.8f))
                    .border(1.dp, Slate800, RoundedCornerShape(12.dp))
                    .padding(12.dp)
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
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(logs) { entry ->
                            val textColor = when (entry.level) {
                                LogLevel.SYSTEM -> Slate300
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
