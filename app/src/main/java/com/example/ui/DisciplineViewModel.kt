package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.*
import com.example.data.network.GeminiApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class DisciplineViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val statsDao = db.statsDao()
    private val taskDao = db.taskDao()
    private val habitDao = db.habitDao()
    private val focusDao = db.focusDao()
    private val journalDao = db.journalDao()

    // Language state backing local persistence
    private val prefs = application.getSharedPreferences("discipline_prefs", android.content.Context.MODE_PRIVATE)
    var selectedLanguage by mutableStateOf(prefs.getString("language", "en") ?: "en")
        private set

    fun selectLanguage(langCode: String) {
        selectedLanguage = langCode
        prefs.edit().putString("language", langCode).apply()
        viewModelScope.launch {
            uiEventChannel.emit(when(langCode) {
                "hi" -> "सफलतापूर्वक भाषा बदलकर हिंदी कर दी गई है!"
                "es" -> "¡Idioma cambiado con éxito a Español!"
                "de" -> "Sprache erfolgreich auf Deutsch geändert!"
                else -> "Language changed to English successfully!"
            })
        }
    }

    // Date state: active day we are looking at
    var selectedDate by mutableStateOf(getTodayDateString())
        private set

    // Flows
    val userStatsFlow: StateFlow<UserStats> = statsDao.getUserStatsFlow()
        .map { it ?: UserStats() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStats())

    val habitsFlow: StateFlow<List<Habit>> = habitDao.getAllHabitsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Live reactive flows for selectedDate
    private val _selectedDateState = MutableStateFlow(selectedDate)
    
    val tasksFlow: StateFlow<List<Task>> = _selectedDateState
        .flatMapLatest { date -> taskDao.getTasksForDate(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitLogsFlow: StateFlow<List<HabitLog>> = _selectedDateState
        .flatMapLatest { date -> habitDao.getLogsForDateFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val journalFlow: StateFlow<DailyJournal?> = _selectedDateState
        .flatMapLatest { date -> journalDao.getJournalForDateFlow(date) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val focusSessionsFlow: StateFlow<List<FocusSession>> = focusDao.getAllSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalFocusMinutesFlow: StateFlow<Int> = focusDao.getTotalFocusMinutesFlow()
        .map { it ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allJournalsFlow: StateFlow<List<DailyJournal>> = journalDao.getAllJournalsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Pomodoro Timer States
    var pomodoroTimeMinutes by mutableStateOf(25)
    var pomodoroTimeLeftSeconds by mutableStateOf(25 * 60)
        private set
    var isTimerRunning by mutableStateOf(false)
        private set
    var timerCategory by mutableStateOf("Work / Study")

    // Radha Chanting Counter States
    var radhaChantCount by mutableStateOf(prefs.getInt("radha_chant_count", 0))
        private set
    var radhaMalaCount by mutableStateOf(prefs.getInt("radha_mala_count", 0))
        private set
    var radhaTotalChants by mutableStateOf(prefs.getInt("radha_total_chants", 0))
        private set
    var radhaMalaTarget by mutableStateOf(prefs.getInt("radha_mala_target", 1)) // target in Malas, default 1 Mala
        private set
    var radhaSoundEnabled by mutableStateOf(prefs.getBoolean("radha_sound_enabled", true))
        private set
    var radhaVibrationEnabled by mutableStateOf(prefs.getBoolean("radha_vibration_enabled", true))
        private set

    fun incrementRadhaChant() {
        val nextChant = radhaChantCount + 1
        var nextMala = radhaMalaCount
        
        radhaTotalChants++
        
        if (nextChant >= 108) {
            radhaChantCount = 0
            nextMala++
            radhaMalaCount = nextMala
            viewModelScope.launch {
                awardXp(20, "Completed Radhe Mala! \uD83D\uDE4F")
                uiEventChannel.emit("Mala Complete: Radhe Radhe! +20 XP 🙏")
                
                // Add a short meditation focus session of 5 mins for each Mala
                addFocusSession(5, "Meditation")
            }
        } else {
            radhaChantCount = nextChant
        }
        
        prefs.edit().apply {
            putInt("radha_chant_count", radhaChantCount)
            putInt("radha_mala_count", radhaMalaCount)
            putInt("radha_total_chants", radhaTotalChants)
            apply()
        }
    }
    
    fun resetRadhaCounter() {
        radhaChantCount = 0
        radhaMalaCount = 0
        radhaTotalChants = 0
        prefs.edit().apply {
            putInt("radha_chant_count", 0)
            putInt("radha_mala_count", 0)
            putInt("radha_total_chants", 0)
            apply()
        }
    }
    
    fun updateRadhaMalaTarget(target: Int) {
        radhaMalaTarget = target
        prefs.edit().putInt("radha_mala_target", target).apply()
    }
    
    fun toggleRadhaSound() {
        radhaSoundEnabled = !radhaSoundEnabled
        prefs.edit().putBoolean("radha_sound_enabled", radhaSoundEnabled).apply()
    }
    
    fun toggleRadhaVibration() {
        radhaVibrationEnabled = !radhaVibrationEnabled
        prefs.edit().putBoolean("radha_vibration_enabled", radhaVibrationEnabled).apply()
    }

    // UI Toast Messages or level-up signals
    val uiEventChannel = MutableSharedFlow<String>()
    
    // AI Coach states
    var isCoachLoading by mutableStateOf(false)
        private set
    var coachResponse by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            // Seed system habits if empty
            val currentHabits = habitDao.getAllHabits()
            if (currentHabits.isEmpty()) {
                val systemHabits = listOf(
                    Habit(name = "Daily Gym / Exercise", category = "Exercise", target = "30 min", icon = "fitness_center", isSystemHabit = true),
                    Habit(name = "Read Books / Articles", category = "Reading", target = "10 pages", icon = "book", isSystemHabit = true),
                    Habit(name = "Mindfulness Meditation", category = "Meditation", target = "15 min", icon = "spa", isSystemHabit = true),
                    Habit(name = "Hydration / Water Intake", category = "Water Intake", target = "2 Liters", icon = "water_drop", isSystemHabit = true),
                    Habit(name = "Restorative Sleep", category = "Sleep", target = "8 hours", icon = "bedtime", isSystemHabit = true)
                )
                systemHabits.forEach { habitDao.insertHabit(it) }
            }

            // Initalize Stats row if missing
            val stats = statsDao.getUserStats()
            if (stats == null) {
                statsDao.insertOrUpdateStats(
                    UserStats(
                        id = 1,
                        level = 1,
                        xp = 0,
                        currentStreak = 1,
                        bestStreak = 1,
                        streakRecoveryCount = 1,
                        lastCheckInDate = getTodayDateString()
                    )
                )
            } else {
                checkAndUpdateStreak(stats)
            }
        }

        // Start Pomodoro timer ticker
        viewModelScope.launch {
            while (true) {
                delay(1000)
                if (isTimerRunning) {
                    if (pomodoroTimeLeftSeconds > 0) {
                        pomodoroTimeLeftSeconds--
                    } else {
                        // Timer completed
                        isTimerRunning = false
                        viewModelScope.launch {
                            val duration = pomodoroTimeMinutes
                            addFocusSession(duration, timerCategory)
                            awardXp(50, "Focus Session Complete!")
                            uiEventChannel.emit("Pomodoro session completed! +50 XP")
                            resetTimer()
                        }
                    }
                }
            }
        }
    }

    // Helper to get Today's Date - "YYYY-MM-DD"
    fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun selectDate(date: String) {
        selectedDate = date
        _selectedDateState.value = date
        viewModelScope.launch {
            val stats = statsDao.getUserStats()
            if (stats != null) {
                checkAndUpdateStreak(stats)
            }
        }
    }

    // Checking streaks
    private suspend fun checkAndUpdateStreak(stats: UserStats) {
        val today = getTodayDateString()
        val lastChecked = stats.lastCheckInDate
        if (lastChecked == today) return // already checked in today

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val lastDate = sdf.parse(lastChecked)
        val todayDate = sdf.parse(today)
        
        if (lastDate != null && todayDate != null) {
            val diffMs = todayDate.time - lastDate.time
            val diffDays = diffMs / (1000 * 60 * 60 * 24)
            
            if (diffDays == 1L) {
                // Consecutive day
                val newStreak = stats.currentStreak + 1
                val newBest = if (newStreak > stats.bestStreak) newStreak else stats.bestStreak
                statsDao.insertOrUpdateStats(stats.copy(
                    currentStreak = newStreak,
                    bestStreak = newBest,
                    lastCheckInDate = today
                ))
                awardXp(15, "Day Streak Maintained!")
            } else if (diffDays > 1L) {
                // Streak broken! Unless recovery option gets completed
                // Let's not reset streak to 0 immediately if they have recovery option, we can let user recover manually
                // We show an option to do "Streak Recovery"
            }
        }
    }

    // Recover Streak
    fun triggerStreakRecovery() {
        viewModelScope.launch {
            val stats = statsDao.getUserStats() ?: return@launch
            if (stats.streakRecoveryCount > 0) {
                val restoredStreak = stats.bestStreak // recover it back to best streak or best streak / 2
                val updated = stats.copy(
                    currentStreak = restoredStreak,
                    streakRecoveryCount = stats.streakRecoveryCount - 1,
                    lastCheckInDate = getTodayDateString()
                )
                statsDao.insertOrUpdateStats(updated)
                uiEventChannel.emit("Streak recovered to $restoredStreak! Used 1 Shield.")
            } else {
                uiEventChannel.emit("No Streak Recovery Shields left!")
            }
        }
    }

    // XP gamification
    suspend fun awardXp(amount: Int, reason: String) {
        val stats = statsDao.getUserStats() ?: return
        val totalXp = stats.xp + amount
        val currentLevel = stats.level
        val newLevel = (totalXp / 120) + 1 // 120 XP per level
        
        val updated = stats.copy(
            xp = totalXp,
            level = newLevel
        )
        statsDao.insertOrUpdateStats(updated)
        
        if (newLevel > currentLevel) {
            uiEventChannel.emit("LEVEL UP! Reached Level $newLevel! \uD83C\uDFC6")
        } else {
            uiEventChannel.emit("+$amount XP ($reason)")
        }
    }

    // Task operations
    fun addTask(title: String, priority: String, dueTime: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = Task(title = title, priority = priority, dueTime = dueTime, date = selectedDate)
            taskDao.insertTask(task)
            awardXp(10, "Task added")
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = task.copy(isCompleted = !task.isCompleted)
            taskDao.updateTask(updated)
            if (updated.isCompleted) {
                val xpAward = when (task.priority) {
                    "High" -> 40
                    "Medium" -> 25
                    else -> 15
                }
                awardXp(xpAward, "${task.priority} Priority Task Completed")
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.deleteTask(task)
        }
    }

    // Habit operations
    fun addHabit(name: String, category: String, target: String, icon: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val habit = Habit(name = name, category = category, target = target, icon = icon)
            habitDao.insertHabit(habit)
            awardXp(15, "Habit created")
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch(Dispatchers.IO) {
            habitDao.deleteHabit(habit)
        }
    }

    fun toggleHabitCompletion(habit: Habit, wasLogged: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val today = selectedDate
            if (wasLogged) {
                habitDao.deleteLog(habit.id, today)
            } else {
                habitDao.insertLog(HabitLog(habitId = habit.id, date = today))
                awardXp(20, "Completed ${habit.category}")
            }
        }
    }

    // Focus operations
    private suspend fun addFocusSession(minutes: Int, category: String) {
        val session = FocusSession(durationMinutes = minutes, category = category)
        focusDao.insertSession(session)
    }

    // Timer controls
    fun startTimer() {
        isTimerRunning = true
    }

    fun pauseTimer() {
        isTimerRunning = false
    }

    fun resetTimer() {
        isTimerRunning = false
        pomodoroTimeLeftSeconds = pomodoroTimeMinutes * 60
    }

    fun updateTimerMinutes(minutes: Int) {
        pomodoroTimeMinutes = minutes
        resetTimer()
    }

    // Journal operations
    fun saveJournal(mood: String, notes: String, reflection: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val journal = DailyJournal(date = selectedDate, mood = mood, notes = notes, reflection = reflection)
            // If new journal entry, award XP
            val existing = journalDao.getJournalForDate(selectedDate)
            journalDao.insertOrUpdateJournal(journal)
            if (existing == null) {
                awardXp(20, "Daily Note Reflection logged")
            } else {
                uiEventChannel.emit("Reflection entry updated")
            }
        }
    }

    // Challenges Setup
    fun enrollInChallenge(type: String) {
        viewModelScope.launch {
            val stats = statsDao.getUserStats() ?: return@launch
            val updated = stats.copy(
                challengeType = type,
                challengeStartDate = getTodayDateString()
            )
            statsDao.insertOrUpdateStats(updated)
            uiEventChannel.emit("Enrolled in $type Challenge!")
            awardXp(30, "Challenge Enrollment Badge unlocked!")
        }
    }

    fun exitChallenge() {
        viewModelScope.launch {
            val stats = statsDao.getUserStats() ?: return@launch
            val updated = stats.copy(
                challengeType = "None",
                challengeStartDate = ""
            )
            statsDao.insertOrUpdateStats(updated)
            uiEventChannel.emit("Exited active transformation challenge.")
        }
    }

    // AI Coach Action
    fun queryAICoach() {
        viewModelScope.launch {
            isCoachLoading = true
            coachResponse = null
            
            val stats = userStatsFlow.value
            val tasks = tasksFlow.value
            val habits = habitsFlow.value
            val logs = habitLogsFlow.value
            val journal = journalFlow.value
            
            val totalTasks = tasks.size
            val completedTasks = tasks.count { it.isCompleted }
            val completedHabitsCount = logs.size
            
            val prompt = """
                Here is my active productivity dashboard details for $selectedDate:
                - Day Streak: ${stats.currentStreak} days
                - User Level: ${stats.level} (Total XP: ${stats.xp})
                - Tasks Completed today: $completedTasks out of $totalTasks
                - Habit status today: Completed $completedHabitsCount habits.
                - My Daily Journal Mood: ${journal?.mood ?: "Not tracked yet"}
                - My Reflection Notes: ${journal?.notes ?: "None logged yet."}
                - Ongoing Challenge Enrolled: ${stats.challengeType}
                
                Please generate:
                1. Personalized recommendations for maintaining consistency.
                2. One deep, highly motivational routine optimization suggestion.
                3. Habit improvement tips based on this data.
                Keep the tone warm, empowering, direct, and focused on discipline.
            """.trimIndent()

            val systemInstruction = """
                You are the master AI Productivity Coach in "Discipline Master".
                Your primary goal is to empower users to become consistent, focused, and overcome distraction.
                Format the answer beautifully using bullet points with title emojis. Keep it concise, professional and action-oriented.
            """.trimIndent()

            val response = GeminiApiClient.getCoachFeedback(prompt, systemInstruction)
            coachResponse = response
            isCoachLoading = false
        }
    }
}
