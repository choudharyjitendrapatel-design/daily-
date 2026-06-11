package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.db.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisciplineApp(viewModel: DisciplineViewModel) {
    val context = LocalContext.current
    val lang = viewModel.selectedLanguage
    var activeTab by remember { mutableStateOf("Dashboard") }

    // Collect States
    val stats by viewModel.userStatsFlow.collectAsStateWithLifecycle()
    val habits by viewModel.habitsFlow.collectAsStateWithLifecycle()
    val tasks by viewModel.tasksFlow.collectAsStateWithLifecycle()
    val habitLogs by viewModel.habitLogsFlow.collectAsStateWithLifecycle()
    val journal by viewModel.journalFlow.collectAsStateWithLifecycle()
    val totalFocusMinutes by viewModel.totalFocusMinutesFlow.collectAsStateWithLifecycle()

    // Observe Level Up / Toast Events
    LaunchedEffect(Unit) {
        viewModel.uiEventChannel.collectLatest { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "App logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Translations.getString(lang, "app_title"),
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    // Level Badge indicator in top bar
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("top_bar_level")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.OfflineBolt,
                                contentDescription = "Level Icon",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${Translations.getString(lang, "level")} ${stats.level}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    Triple("Dashboard", Icons.Default.Dashboard, Icons.Outlined.Dashboard),
                    Triple("Task Board", Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List),
                    Triple("Timer", Icons.Default.Timer, Icons.Outlined.Timer),
                    Triple("Discipline Hub", Icons.Default.EditNote, Icons.Outlined.EditNote),
                    Triple("Goals & Stats", Icons.Default.EmojiEvents, Icons.Outlined.EmojiEvents)
                )

                tabs.forEach { (tab, filledIcon, outlinedIcon) ->
                    val isSelected = activeTab == tab
                    val tabLabel = when (tab) {
                        "Dashboard" -> Translations.getString(lang, "dashboard")
                        "Task Board" -> Translations.getString(lang, "task_board")
                        "Timer" -> Translations.getString(lang, "timer")
                        "Discipline Hub" -> Translations.getString(lang, "discipline_hub")
                        "Goals & Stats" -> Translations.getString(lang, "goals_stats")
                        else -> tab
                    }
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { activeTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) filledIcon else outlinedIcon,
                                contentDescription = "$tabLabel Tab",
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tabLabel,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag("nav_${tab.lowercase().replace(" ", "_")}")
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "tab_transition"
            ) { targetTab ->
                when (targetTab) {
                    "Dashboard" -> DashboardScreen(
                        viewModel = viewModel,
                        stats = stats,
                        tasks = tasks,
                        habitLogs = habitLogs,
                        onNavigateToTasks = { activeTab = "Task Board" }
                    )
                    "Task Board" -> TaskBoardScreen(
                        viewModel = viewModel,
                        tasks = tasks,
                        habits = habits,
                        habitLogs = habitLogs
                    )
                    "Timer" -> TimerScreen(
                        viewModel = viewModel,
                        totalMinutes = totalFocusMinutes
                    )
                    "Discipline Hub" -> DisciplineHubScreen(
                        viewModel = viewModel,
                        journal = journal
                    )
                    "Goals & Stats" -> GoalsStatsScreen(
                        viewModel = viewModel,
                        stats = stats,
                        tasks = tasks,
                        habits = habits,
                        habitLogs = habitLogs
                    )
                }
            }
        }
    }
}

// ---------------- DASHBOARD SCREEN ----------------
@Composable
fun DashboardScreen(
    viewModel: DisciplineViewModel,
    stats: UserStats,
    tasks: List<Task>,
    habitLogs: List<HabitLog>,
    onNavigateToTasks: () -> Unit
) {
    val lang = viewModel.selectedLanguage
    val scrollState = rememberScrollState()
    
    val totalTasks = tasks.size
    val completedTasks = tasks.count { it.isCompleted }
    val progressPercent = if (totalTasks == 0) 100 else (completedTasks * 100) / totalTasks

    // dynamic quotes list
    val quotes = listOf(
        "Discipline is the bridge between goals and accomplishment." to "Jim Rohn",
        "We are what we repeatedly do. Excellence, then, is not an act, but a habit." to "Aristotle",
        "Suffering of discipline is nothing compared to suffering of regret." to "Jim Rohn",
        "The successful warrior is the average man, with laser-like focus." to "Bruce Lee",
        "Small daily improvements over time lead to stunning results." to "Robin Sharma",
        "Discipline master is not born. It is forged minute-by-minute." to "Stoic Wisdom",
        "Focus on being productive instead of busy." to "Tim Ferriss"
    )
    
    // Pick daily quote based on date hash
    val currentQuoteIndex = Math.abs(viewModel.selectedDate.hashCode()) % quotes.size
    val (quoteText, quoteAuthor) = quotes[currentQuoteIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedKeepTogether(16.dp)
    ) {
        // Date Selector header
        DateSelectionHeader(
            selectedDate = viewModel.selectedDate,
            onDateSelected = { date -> viewModel.selectDate(date) }
        )

        // STREAK & LEVEL HERO CARD (Generous colors & rounded)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = if (isSystemInDarkTheme()) {
                                listOf(DarkSurface, Color(0xFF0F1E32))
                            } else {
                                listOf(LightSurface, Color(0xFFE0F2FE))
                            }
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = Translations.getString(lang, "streak_force"),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak flame",
                                    tint = ColorGold,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${stats.currentStreak} ${Translations.getString(lang, "days")}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Streak recovery shield status
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = Translations.getString(lang, "shield_recovery"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { viewModel.triggerStreakRecovery() }
                                    .border(
                                        1.dp,
                                        if (stats.streakRecoveryCount > 0) DarkPrimary else Color.Gray,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = "Streak recovery Shield icon",
                                    tint = if (stats.streakRecoveryCount > 0) ColorLowPriority else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (stats.streakRecoveryCount > 0) "${Translations.getString(lang, "shield")}: ${stats.streakRecoveryCount}" else Translations.getString(lang, "recover_streak"),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (stats.streakRecoveryCount > 0) ColorLowPriority else Color.Gray
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // LEVEL XP PROGRESS
                    val currentXpLevelFloor = (stats.level - 1) * 120
                    val nextXpLevelGoal = stats.level * 120
                    val levelProgressRaw = stats.xp - currentXpLevelFloor
                    val levelProgressPercent = levelProgressRaw.toFloat() / 120f

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${Translations.getString(lang, "level")} ${stats.level}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$levelProgressRaw / 120 ${Translations.getString(lang, "xp")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { levelProgressPercent.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
                }
            }
        }

        // MOTIVATIONAL QUOTE OF THE DAY
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FormatQuote,
                        contentDescription = "Quote Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translations.getString(lang, "discipline_directive"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "\"$quoteText\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "— $quoteAuthor",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        // DAILY TASK STATISTICS PROGRESS RADIAL RING
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedKeepTogether(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1.3f)
                    .height(160.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                        CircularProgressIndicator(
                            progress = { progressPercent.toFloat() / 100f },
                            modifier = Modifier.size(80.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 8.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$progressPercent%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = Translations.getString(lang, "task_progress"),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1.7f)
                    .height(160.dp)
                    .clickable { onNavigateToTasks() },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Translations.getString(lang, "daily_focus"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    
                    Column {
                        Text(
                            text = "$completedTasks / $totalTasks Tasks",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = Translations.getString(lang, "completed_tasks_on_date"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Translations.getString(lang, "check_checklist"),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Forward Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // SHORT HABITS PREVIEW ROW
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Translations.getString(lang, "habits_integrity"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val defaultCategories = listOf(
                        Triple("Exercise", Icons.Default.FitnessCenter, ColorHighPriority),
                        Triple("Reading", Icons.Default.MenuBook, ColorLowPriority),
                        Triple("Meditation", Icons.Default.Spa, DarkPrimary),
                        Triple("Water", Icons.Default.WaterDrop, ColorLowPriority),
                        Triple("Sleep", Icons.Default.Bedtime, ColorGold)
                    )

                    defaultCategories.forEach { (cat, icon, color) ->
                        val catLabel = when (cat) {
                            "Exercise" -> Translations.getString(lang, "exercise")
                            "Reading" -> Translations.getString(lang, "reading")
                            "Meditation" -> Translations.getString(lang, "meditation")
                            "Water" -> Translations.getString(lang, "water")
                            "Sleep" -> Translations.getString(lang, "sleep")
                            else -> cat
                        }
                        val isAnyDone = habitLogs.any { log ->
                            // Habit matching category
                            log.completed
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        color.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                                    .border(1.dp, color.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = cat,
                                    tint = color,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = catLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------- TASK BOARD SCREEN ----------------
@Composable
fun TaskBoardScreen(
    viewModel: DisciplineViewModel,
    tasks: List<Task>,
    habits: List<Habit>,
    habitLogs: List<HabitLog>
) {
    val lang = viewModel.selectedLanguage
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddHabitDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("task_board_screen"),
        verticalArrangement = Arrangement.spacedKeepTogether(16.dp)
    ) {
        // HEADER ROW WITH SELECTOR
        DateSelectionHeader(
            selectedDate = viewModel.selectedDate,
            onDateSelected = { date -> viewModel.selectDate(date) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Translations.getString(lang, "todays_focus_tasks"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            
            Button(
                onClick = { showAddTaskDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("add_task_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add task")
                Spacer(modifier = Modifier.width(4.dp))
                Text(Translations.getString(lang, "add_task"), fontWeight = FontWeight.Bold)
            }
        }

        // TASK LIST
        if (tasks.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = "Empty Tasks Icon",
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = Translations.getString(lang, "all_disciplines_completed"),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = Translations.getString(lang, "add_high_priority_task"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedKeepTogether(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskRowItem(
                        task = task,
                        onCheckedChange = { viewModel.toggleTaskCompletion(task) },
                        onDeleteClick = { viewModel.deleteTask(task) }
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp))

        // HABIT TRACKING DECK
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Translations.getString(lang, "habit_integrators"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            IconButton(
                onClick = { showAddHabitDialog = true },
                colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add habit")
            }
        }

        Box(modifier = Modifier.weight(1.2f)) {
            if (habits.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(Translations.getString(lang, "no_habits_registered"))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedKeepTogether(8.dp)
                ) {
                    items(habits) { habit ->
                        val isLogged = habitLogs.any { log -> log.habitId == habit.id && log.completed }
                        HabitRowItem(
                            habit = habit,
                            isCompleted = isLogged,
                            onToggle = { viewModel.toggleHabitCompletion(habit, isLogged) },
                            onDelete = { viewModel.deleteHabit(habit) }
                        )
                    }
                }
            }
        }
    }

    // ADD TASK DIALOG
    if (showAddTaskDialog) {
        var title by remember { mutableStateOf("") }
        var priority by remember { mutableStateOf("Medium") }
        var dueTime by remember { mutableStateOf("08:00 AM") }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text(Translations.getString(lang, "add_focus_task"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedKeepTogether(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(Translations.getString(lang, "task_title")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_task_title_input")
                    )

                    Text(Translations.getString(lang, "priority_level"), fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedKeepTogether(8.dp)
                    ) {
                        listOf("Low", "Medium", "High").forEach { level ->
                            val levelLabel = when (level) {
                                "Low" -> Translations.getString(lang, "low")
                                "Medium" -> Translations.getString(lang, "medium")
                                "High" -> Translations.getString(lang, "high")
                                else -> level
                            }
                            FilterChip(
                                selected = priority == level,
                                onClick = { priority = level },
                                label = { Text(levelLabel) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = dueTime,
                        onValueChange = { dueTime = it },
                        label = { Text(Translations.getString(lang, "due_time")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.addTask(title, priority, dueTime)
                            showAddTaskDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_confirm_add_task")
                ) {
                    Text(Translations.getString(lang, "create"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text(Translations.getString(lang, "cancel"))
                }
            }
        )
    }

    // ADD HABIT DIALOG
    if (showAddHabitDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Exercise") }
        var target by remember { mutableStateOf("30 min") }

        AlertDialog(
            onDismissRequest = { showAddHabitDialog = false },
            title = { Text(Translations.getString(lang, "add_habit"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedKeepTogether(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(Translations.getString(lang, "habit_name")) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(Translations.getString(lang, "category"), fontWeight = FontWeight.Bold)
                    val categories = listOf("Exercise", "Reading", "Meditation", "Water Intake", "Sleep", "Custom")
                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            categories.take(3).forEach { cat ->
                                val catLabel = when (cat) {
                                    "Exercise" -> Translations.getString(lang, "exercise")
                                    "Reading" -> Translations.getString(lang, "reading")
                                    "Meditation" -> Translations.getString(lang, "meditation")
                                    else -> cat
                                }
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(catLabel, fontSize = 11.sp) }
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            categories.drop(3).forEach { cat ->
                                val catLabel = when (cat) {
                                    "Water Intake" -> Translations.getString(lang, "water")
                                    "Sleep" -> Translations.getString(lang, "sleep")
                                    else -> cat
                                }
                                FilterChip(
                                    selected = category == cat,
                                    onClick = { category = cat },
                                    label = { Text(catLabel, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it },
                        label = { Text(Translations.getString(lang, "daily_target")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val icon = when (category) {
                                "Exercise" -> "fitness_center"
                                "Reading" -> "book"
                                "Meditation" -> "spa"
                                "Water Intake" -> "water_drop"
                                "Sleep" -> "bedtime"
                                else -> "star"
                            }
                            viewModel.addHabit(name, category, target, icon)
                            showAddHabitDialog = false
                        }
                    }
                ) {
                    Text(Translations.getString(lang, "add"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHabitDialog = false }) {
                    Text(Translations.getString(lang, "cancel"))
                }
            }
        )
    }
}

// ---------------- TIMER SCREEN (POMODORO) ----------------
@Composable
fun TimerScreen(
    viewModel: DisciplineViewModel,
    totalMinutes: Int
) {
    val lang = viewModel.selectedLanguage
    val totalSeconds = viewModel.pomodoroTimeMinutes * 60
    val elapsedSeconds = totalSeconds - viewModel.pomodoroTimeLeftSeconds
    val percentageFinished = elapsedSeconds.toFloat() / totalSeconds.toFloat()

    val formattedTime = String.format(
        Locale.getDefault(),
        "%02d:%02d",
        viewModel.pomodoroTimeLeftSeconds / 60,
        viewModel.pomodoroTimeLeftSeconds % 60
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("timer_screen"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedKeepTogether(16.dp)
    ) {
        Text(
            text = Translations.getString(lang, "focus_pomodoro_engaged"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )

        // Session Quick Selector chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(5, 10, 25, 50).forEach { mins ->
                FilterChip(
                    selected = viewModel.pomodoroTimeMinutes == mins,
                    onClick = { viewModel.updateTimerMinutes(mins) },
                    label = { Text("$mins Min") },
                    enabled = !viewModel.isTimerRunning
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CIRCULAR ADVANCED TIMER
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
            Canvas(modifier = Modifier.size(220.dp)) {
                // Background Track ring
                drawCircle(color = secondaryColor, style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round))
                // Progress Active arc
                drawArc(
                    color = primaryColor,
                    startAngle = -270f,
                    sweepAngle = 360f * (1f - percentageFinished),
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = viewModel.timerCategory.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // TIMER NAVIGATION DETAILS & CONTROL BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedKeepTogether(16.dp)
        ) {
            if (viewModel.isTimerRunning) {
                Button(
                    onClick = { viewModel.pauseTimer() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("timer_pause_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(imageVector = Icons.Default.Pause, contentDescription = "Pause button", tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Translations.getString(lang, "pause"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            } else {
                Button(
                    onClick = { viewModel.startTimer() },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .testTag("timer_start_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Start button")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Translations.getString(lang, "start_session"), fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { viewModel.resetTimer() },
                modifier = Modifier
                    .weight(0.8f)
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceBorderColors())
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset timer", tint = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.width(4.dp))
                Text(Translations.getString(lang, "reset"), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }

        // TIMER FOCUS TYPE SELECTOR
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = Translations.getString(lang, "category_alignment"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedKeepTogether(6.dp)
                ) {
                    listOf("Work / Study", "Meditation", "Exercise").forEach { cat ->
                        OutlinedButton(
                            onClick = { viewModel.timerCategory = cat },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(
                                1.dp,
                                if (viewModel.timerCategory == cat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                            )
                        ) {
                            Text(cat, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        // STATS SUMMARY
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.SportsScore,
                    contentDescription = "Focus icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = Translations.getString(lang, "total_session_accomplished"),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "$totalMinutes ${Translations.getString(lang, "focus_minutes_logged")}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------- DISCIPLINE HUB SCREEN (JOURNAL & COACH) ----------------
@Composable
fun DisciplineHubScreen(
    viewModel: DisciplineViewModel,
    journal: DailyJournal?
) {
    val lang = viewModel.selectedLanguage
    val scrollState = rememberScrollState()

    var activeSubTab by remember { mutableStateOf("Coach") }
    
    // Notes input states
    var tempMood by remember { mutableStateOf(journal?.mood ?: "Calm / Neutral") }
    var tempNotes by remember { mutableStateOf(journal?.notes ?: "") }
    var tempReflection by remember { mutableStateOf(journal?.reflection ?: "") }

    LaunchedEffect(journal) {
        journal?.let {
            tempMood = it.mood
            tempNotes = it.notes
            tempReflection = it.reflection
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("discipline_hub_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Translations.getString(lang, "discipline_hub_title"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )

            // Switch sub tabs
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = activeSubTab == "Coach",
                    onClick = { activeSubTab = "Coach" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text(Translations.getString(lang, "ai_coach"), fontSize = 12.sp)
                }
                SegmentedButton(
                    selected = activeSubTab == "Journal",
                    onClick = { activeSubTab = "Journal" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text(Translations.getString(lang, "daily_journal"), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeSubTab == "Coach") {
            // AI Productivity Coach UI
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedKeepTogether(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "AI face",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = Translations.getString(lang, "ai_productivity_coach"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Translations.getString(lang, "personalized_suggestions"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.queryAICoach() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("ask_coach_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (viewModel.isCoachLoading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Ask coach Icon")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Translations.getString(lang, "generate_coach_recommendations"), fontWeight = FontWeight.Bold)
                    }
                }

                // Coach Feedback area
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = Translations.getString(lang, "coach_directive_report"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (viewModel.coachResponse == null && !viewModel.isCoachLoading) {
                            Text(
                                text = Translations.getString(lang, "generate_insight_placeholder"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            viewModel.coachResponse?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyMedium,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Daily Journal & Reflections UI
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedKeepTogether(12.dp)
            ) {
                Text(
                    text = Translations.getString(lang, "reflections_mood"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Mood selector row
                val moods = listOf(
                    "Positive" to "😀",
                    "Calm" to "🧘",
                    "Stressed" to "🤯",
                    "Focused" to "🎯",
                    "Tired" to "🥱"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    moods.forEach { (moodName, emoji) ->
                        val isSel = tempMood == moodName
                        val moodLabel = when (moodName) {
                            "Positive" -> Translations.getString(lang, "positive_mood")
                            "Calm" -> Translations.getString(lang, "calm_mood")
                            "Stressed" -> Translations.getString(lang, "stressed_mood")
                            "Focused" -> Translations.getString(lang, "focused_mood")
                            "Tired" -> Translations.getString(lang, "tired_mood")
                            else -> moodName
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { tempMood = moodName }
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Text(text = emoji, fontSize = 28.sp)
                            Text(text = moodLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = tempNotes,
                    onValueChange = { tempNotes = it },
                    label = { Text(Translations.getString(lang, "daily_reflection_thoughts")) },
                    placeholder = { Text(Translations.getString(lang, "reflection_placeholder")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = tempReflection,
                    onValueChange = { tempReflection = it },
                    label = { Text(Translations.getString(lang, "consistency_obstacles")) },
                    placeholder = { Text(Translations.getString(lang, "obstacles_placeholder")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        viewModel.saveJournal(tempMood, tempNotes, tempReflection)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("save_journal_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save icon")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Translations.getString(lang, "save_daily_log_entry"), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------- GOALS & STATS SCREEN ----------------
@Composable
fun GoalsStatsScreen(
    viewModel: DisciplineViewModel,
    stats: UserStats,
    tasks: List<Task>,
    habits: List<Habit>,
    habitLogs: List<HabitLog>
) {
    val lang = viewModel.selectedLanguage
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var activeGoalView by remember { mutableStateOf("Challenges") }
    val totalMinutes by viewModel.totalFocusMinutesFlow.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
            .testTag("goals_and_stats_screen"),
        verticalArrangement = Arrangement.spacedKeepTogether(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Translations.getString(lang, "performance_challenges"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )

            // Switch sub panels
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = activeGoalView == "Challenges",
                    onClick = { activeGoalView = "Challenges" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text(Translations.getString(lang, "badges_challenges"), fontSize = 11.sp, maxLines = 1)
                }
                SegmentedButton(
                    selected = activeGoalView == "Reports",
                    onClick = { activeGoalView = "Reports" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text(Translations.getString(lang, "reports"), fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        if (activeGoalView == "Challenges") {
            // ACTIVE CHALLENGE CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Translations.getString(lang, "discipline_challenge_header"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (stats.challengeType != "None") {
                        val challengeLabel = if (stats.challengeType == "30-Day") Translations.getString(lang, "challenge_30_day") else Translations.getString(lang, "challenge_90_day")
                        Text(
                            text = "$challengeLabel ${Translations.getString(lang, "challenge_active")}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${Translations.getString(lang, "enroll_date")}: ${stats.challengeStartDate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Challenge check in track list
                        val limitDays = if (stats.challengeType == "30-Day") 30 else 90
                        Text(
                            text = "${Translations.getString(lang, "daily_benchmarks")} 1 ${Translations.getString(lang, "to")} $limitDays ${Translations.getString(lang, "days")}.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedKeepTogether(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.exitChallenge() },
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, ColorHighPriority)
                            ) {
                                Text(Translations.getString(lang, "exit_challenge"), color = ColorHighPriority, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(
                            text = Translations.getString(lang, "challenge_pitch"),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedKeepTogether(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.enrollInChallenge("30-Day") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(Translations.getString(lang, "challenge_30_day"), fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { viewModel.enrollInChallenge("90-Day") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(Translations.getString(lang, "challenge_90_day"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // GAMIFICATION BADGES
            Text(
                text = Translations.getString(lang, "unlocked_badges_title"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val systemBadges = listOf(
                Triple(Translations.getString(lang, "badge_consistency_rookie"), Translations.getString(lang, "desc_consistency_rookie"), stats.xp >= 10),
                Triple(Translations.getString(lang, "badge_fire_starter"), Translations.getString(lang, "desc_fire_starter"), stats.bestStreak >= 3),
                Triple(Translations.getString(lang, "badge_zen_flow"), Translations.getString(lang, "desc_zen_flow"), stats.xp >= 100),
                Triple(Translations.getString(lang, "badge_persistence_warrior"), Translations.getString(lang, "desc_persistence_warrior"), stats.level >= 3),
                Triple(Translations.getString(lang, "badge_hydration_champion"), Translations.getString(lang, "desc_hydration_champion"), stats.xp >= 200)
            )

            Column(verticalArrangement = Arrangement.spacedKeepTogether(10.dp)) {
                systemBadges.forEach { (badge, desc, unlocked) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (unlocked) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        border = if (unlocked) BorderStroke(1.5.dp, ColorGold) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (unlocked) ColorGold.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                                    contentDescription = "Badge status icon",
                                    tint = if (unlocked) ColorGold else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // REPORT & PRINT EXPORT GENERATOR
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = Translations.getString(lang, "reports_title"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = Translations.getString(lang, "reports_desc"),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val report = """
                                Discipline Master Daily Performance Audit Checklist:
                                ----------------------------------------------------
                                Selected Date: ${viewModel.selectedDate}
                                Streak Rating: ${stats.currentStreak} Day Streak (Best: ${stats.bestStreak} Days)
                                User Gamification Level: Lvl ${stats.level} (Total XP accrued: ${stats.xp} XP)
                                Total Focus Minutes Accomplished: $totalMinutes Min
                                Ongoing Challenges Enrolled: ${stats.challengeType}
                                
                                Tasks summary logged on date:
                                ${tasks.map { (if (it.isCompleted) "[COMPLETED]" else "[TODO]") + " " + it.title + " (Level: " + it.priority + ")"}.joinToString("\n") }
                                
                                Habit Integrators active:
                                ${habits.map { "- " + it.name + " (" + it.target + ")"}.joinToString("\n")}
                            """.trimIndent()
                            clipboardManager.setText(AnnotatedString(report))
                            Toast.makeText(context, "${Translations.getString(lang, "report_copied")}!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("export_data_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Report")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(Translations.getString(lang, "copy_report_action"), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // LANGUAGE SETTINGS SECTION
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("language_settings_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Translate Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = Translations.getString(lang, "language_settings").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = Translations.getString(lang, "select_preferred_language"),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedKeepTogether(8.dp)
                ) {
                    Translations.languages.take(2).forEach { (code, name) ->
                        val isSelected = viewModel.selectedLanguage == code
                        InputChip(
                            selected = isSelected,
                            onClick = { viewModel.selectLanguage(code) },
                            label = { Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge) },
                            modifier = Modifier.weight(1f).testTag("lang_chip_$code"),
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected language indicator",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedKeepTogether(8.dp)
                ) {
                    Translations.languages.drop(2).forEach { (code, name) ->
                        val isSelected = viewModel.selectedLanguage == code
                        InputChip(
                            selected = isSelected,
                            onClick = { viewModel.selectLanguage(code) },
                            label = { Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge) },
                            modifier = Modifier.weight(1f).testTag("lang_chip_$code"),
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected language indicator",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// ---------------- REUSABLE DRAWING DETAILS ----------------

@Composable
fun DateSelectionHeader(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val formatDisplay = SimpleDateFormat("MMM dd", Locale.getDefault())

    val datesListByCalendar = remember {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -3) // show 3 days back to 3 days ahead
        for (i in 0..6) {
            list.add(sdf.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedKeepTogether(8.dp)
    ) {
        datesListByCalendar.forEach { dateStr ->
            val dateObj = sdf.parse(dateStr)
            val displayStr = dateObj?.let { formatDisplay.format(it) } ?: dateStr
            val isSelected = selectedDate == dateStr

            Surface(
                onClick = { onDateSelected(dateStr) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (isSelected) Color.Transparent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
                modifier = Modifier.testTag("date_header_$dateStr")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = displayStr.substringBefore(" "),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayStr.substringAfter(" "),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun TaskRowItem(
    task: Task,
    onCheckedChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_row_${task.id}")
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant().copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag("task_checkbox_done_${task.id}")
        )
        
        Spacer(modifier = Modifier.width(8.dp))

        // Task content and priority pill
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Priority pill
                val priorityColor = when (task.priority) {
                    "High" -> ColorHighPriority
                    "Medium" -> ColorMedPriority
                    else -> ColorLowPriority
                }
                Surface(
                    color = priorityColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(0.5.dp, priorityColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = task.priority + " Priority",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = "Time icon",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = task.dueTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        IconButton(
            onClick = onDeleteClick,
            modifier = Modifier.testTag("delete_task_${task.id}")
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete task icon",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun HabitRowItem(
    habit: Habit,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryIcon = when (habit.icon) {
        "fitness_center" -> Icons.Default.FitnessCenter
        "book" -> Icons.Default.MenuBook
        "spa" -> Icons.Default.Spa
        "water_drop" -> Icons.Default.WaterDrop
        "bedtime" -> Icons.Default.Bedtime
        else -> Icons.Default.Star
    }

    val themeColor = when (habit.category) {
        "Exercise" -> ColorHighPriority
        "Reading" -> ColorLowPriority
        "Meditation" -> DarkPrimary
        "Water Intake" -> ColorLowPriority
        "Sleep" -> ColorGold
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_row_${habit.id}")
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(
                1.dp,
                if (isCompleted) themeColor.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant().copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon Category Box
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isCompleted) themeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon,
                contentDescription = "Habit category icon",
                tint = if (isCompleted) themeColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Target: ${habit.target} / ${habit.category}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Habit Tracker",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(4.dp))

        // Check off Toggle button block
        Button(
            onClick = onToggle,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCompleted) themeColor else MaterialTheme.colorScheme.secondaryContainer
            ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.testTag("habit_toggle_${habit.id}")
        ) {
            if (isCompleted) {
                Icon(imageVector = Icons.Default.Check, contentDescription = "Checked icon", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("DONE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("LOG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
        }
    }
}

// Extension helpers to prevent compatibility issues
@Composable
fun androidx.compose.material3.ColorScheme.surfaceBorderColors(): Color {
    return if (isSystemInDarkTheme()) Color(0xFF2E3B52) else Color(0xFFE2E8F0)
}

@Composable
fun androidx.compose.material3.ColorScheme.outlineVariant(): Color {
    return if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color(0xFFCBD5E1)
}

// Multi spacing spacer arrangement to align items cleanly
fun Arrangement.spacedKeepTogether(value: androidx.compose.ui.unit.Dp) = Arrangement.spacedBy(value)
