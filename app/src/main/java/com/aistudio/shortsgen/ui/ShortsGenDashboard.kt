package com.aistudio.shortsgen.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.shortsgen.LogLevel
import com.aistudio.shortsgen.LogEntry
import com.aistudio.shortsgen.ShortsGenViewModel
import com.aistudio.shortsgen.data.AppSettings
import com.aistudio.shortsgen.theme.*
import kotlinx.coroutines.launch

val LightBackground = Color(0xFFFFFFFF)
val TextDark = Color(0xFF1E293B)
val TextLight = Color(0xFF94A3B8)
val DarkPill = Color(0xFF1E293B)
val LightPill = Color(0xFFF1F5F9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsGenDashboard(
    viewModel: ShortsGenViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentTab by remember { mutableStateOf("Home") } // Home, Settings, Terminal
    var selectedMode by remember { mutableStateOf("Auto-Bot") } // Auto-Bot, Draft Script, Render Video

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = LightBackground,
        bottomBar = {
            BottomNavigationBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                "Home" -> HomeTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    selectedMode = selectedMode,
                    onModeSelected = { selectedMode = it }
                )
                "Settings" -> SettingsTab(uiState, viewModel)
                "Terminal" -> TerminalTab(uiState, viewModel)
            }
        }
    }
}

@Composable
fun HomeTab(
    uiState: com.aistudio.shortsgen.DashboardUiState,
    viewModel: ShortsGenViewModel,
    selectedMode: String,
    onModeSelected: (String) -> Unit
) {
    val modes = listOf("Auto-Bot", "Draft Script", "Render Video")
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 32.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hi, Creator \uD83D\uDC4B",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = "Automate the world",
                        fontSize = 14.sp,
                        color = TextLight,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Sky500),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = "Profile", tint = Color.White)
                }
            }
        }

        // 2. Search / Topic Input
        item {
            OutlinedTextField(
                value = uiState.topicPrompt,
                onValueChange = { viewModel.updateTopicPrompt(it) },
                placeholder = { Text("Video topic prompt...", color = TextLight) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = LightBackground,
                    focusedContainerColor = LightBackground,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedBorderColor = TextDark,
                    focusedTextColor = TextDark,
                    unfocusedTextColor = TextDark
                ),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Tune,
                        contentDescription = "Filter",
                        tint = TextLight,
                        modifier = Modifier.clickable { /* Optional filters */ }
                    )
                },
                singleLine = true
            )
        }

        // 3. Modes / Tags
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Pipeline Mode", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text("View all", fontSize = 14.sp, color = Sky500, fontWeight = FontWeight.Medium)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(modes) { mode ->
                    val isSelected = selectedMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) DarkPill else LightPill)
                            .clickable { onModeSelected(mode) }
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = mode,
                            color = if (isSelected) Color.White else TextLight,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // 4. Large Status Card (Mountain equivalent)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Sky400, Blue500)
                        )
                    )
            ) {
                // Background elements
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .size(150.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 40.dp, y = (-20).dp)
                )

                // Favorite Icon
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(40.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                        .clickable { },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.FavoriteBorder, contentDescription = null, tint = Color.White)
                }

                // Text Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.7f))
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.topicPrompt.isNotEmpty()) uiState.topicPrompt else "Ready to Generate",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.isGeneratingScript || uiState.isRendering) "Processing..." else "Status: Idle",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("5.0", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Overview (Script details)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.padding(top = 8.dp)) {
                Text("Overview", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Text("Details", fontSize = 18.sp, fontWeight = FontWeight.Medium, color = TextLight)
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Badges
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoBadge(Icons.Rounded.Schedule, "60 Secs")
                InfoBadge(Icons.Rounded.Cloud, "Cloud")
                InfoBadge(Icons.Rounded.Tag, uiState.generatedHashtags.takeIf { it.isNotEmpty() } ?: "Tags")
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = uiState.generatedScript.takeIf { it.isNotEmpty() } ?: "Enter a topic above and run the automation bot. The script and details will appear here once generated.",
                fontSize = 14.sp,
                color = TextLight,
                lineHeight = 22.sp
            )
        }
    }

    // Floating Button (Book Now equivalent)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        val buttonText = when (selectedMode) {
            "Auto-Bot" -> "Run Automation Bot"
            "Draft Script" -> "Generate Script"
            else -> "Render & Publish"
        }
        val isEnabled = when (selectedMode) {
            "Draft Script" -> !uiState.isGeneratingScript
            "Render Video" -> uiState.generatedScript.isNotEmpty() && !uiState.isRendering
            else -> uiState.topicPrompt.isNotEmpty() && !uiState.isGeneratingScript && !uiState.isRendering
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (isEnabled) DarkPill else TextLight)
                .clickable(enabled = isEnabled) {
                    when (selectedMode) {
                        "Auto-Bot" -> viewModel.triggerAutomationBot()
                        "Draft Script" -> viewModel.generateScript()
                        "Render Video" -> viewModel.renderAndPublish()
                    }
                }
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (uiState.isGeneratingScript || uiState.isRendering) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = buttonText,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!uiState.isGeneratingScript && !uiState.isRendering) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Rounded.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun InfoBadge(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = TextLight, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SettingsTab(uiState: com.aistudio.shortsgen.DashboardUiState, viewModel: ShortsGenViewModel) {
    var fastapiUrl by remember(uiState.settings.fastapiUrl) { mutableStateOf(uiState.settings.fastapiUrl) }
    var geminiApiKey by remember(uiState.settings.geminiApiKey) { mutableStateOf(uiState.settings.geminiApiKey) }
    var instagramUser by remember(uiState.settings.instagramUser) { mutableStateOf(uiState.settings.instagramUser) }
    var youtubeChannel by remember(uiState.settings.youtubeChannel) { mutableStateOf(uiState.settings.youtubeChannel) }
    var isSimulationMode by remember(uiState.settings.isSimulationMode) { mutableStateOf(uiState.settings.isSimulationMode) }
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 32.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Text(
                text = "Settings",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Simulation Mode", fontWeight = FontWeight.Bold, color = TextDark, fontSize = 16.sp)
                    Text(
                        text = "Run without actual API calls",
                        color = TextLight,
                        fontSize = 13.sp
                    )
                }
                Switch(checked = isSimulationMode, onCheckedChange = { isSimulationMode = it })
            }
        }

        item {
            OutlinedTextField(
                value = geminiApiKey,
                onValueChange = { geminiApiKey = it },
                label = { Text("Gemini API Key") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = LightBackground,
                    focusedContainerColor = LightBackground
                ),
                singleLine = true
            )
        }
        
        item {
            OutlinedTextField(
                value = instagramUser,
                onValueChange = { instagramUser = it },
                label = { Text("Instagram Username") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = LightBackground,
                    focusedContainerColor = LightBackground
                ),
                singleLine = true
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightPill)
                    .clickable { 
                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        context.startActivity(intent)
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Grant Accessibility Permission", color = TextDark, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkPill)
                    .clickable { 
                        viewModel.saveSettings(
                            AppSettings(
                                fastapiUrl = fastapiUrl,
                                geminiApiKey = geminiApiKey,
                                instagramUser = instagramUser,
                                instagramPass = "",
                                youtubeChannel = youtubeChannel,
                                renderingThreads = 4,
                                selectedVoice = "en_us_male",
                                isSimulationMode = isSimulationMode
                            )
                        )
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Save Settings", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun TerminalTab(uiState: com.aistudio.shortsgen.DashboardUiState, viewModel: ShortsGenViewModel) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.logs.size) {
        if (uiState.logs.isNotEmpty()) {
            listState.animateScrollToItem(uiState.logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 32.dp, bottom = 100.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Logs",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "Clear",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Sky500,
                modifier = Modifier.clickable { viewModel.clearLogs() }
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A))
                .padding(16.dp)
        ) {
            LazyColumn(state = listState) {
                items(uiState.logs) { entry ->
                    val color = when (entry.level) {
                        LogLevel.SUCCESS -> Emerald400
                        LogLevel.ERROR -> Rose400
                        LogLevel.GEMINI -> Violet400
                        else -> TextLight
                    }
                    Text(
                        text = "[${entry.timestamp}] ${entry.message}",
                        color = color,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(currentTab: String, onTabSelected: (String) -> Unit) {
    val tabs = listOf(
        Pair("Home", Icons.Rounded.Home),
        Pair("Logs", Icons.Rounded.Article), // Replaced heart with logs
        Pair("Settings", Icons.Rounded.Person)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { (title, icon) ->
                val isSelected = currentTab == title
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { if(title == "Logs") onTabSelected("Terminal") else onTabSelected(title) }
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (isSelected) TextDark else TextLight,
                        modifier = Modifier.size(28.dp)
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(TextDark)
                        )
                    }
                }
            }
        }
    }
}
