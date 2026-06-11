package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val level: Int = 1,
    val xp: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val streakRecoveryCount: Int = 1,
    val lastCheckInDate: String = "",
    val challengeType: String = "None", // "30-Day", "90-Day", "None"
    val challengeStartDate: String = ""
)

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val priority: String, // "High", "Medium", "Low"
    val dueTime: String, // "08:30 AM"
    val isCompleted: Boolean = false,
    val date: String // "YYYY-MM-DD"
)

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // "Exercise", "Reading", "Meditation", "Water Intake", "Sleep", "Custom"
    val target: String,
    val icon: String,
    val isSystemHabit: Boolean = false
)

@Entity(tableName = "habit_logs")
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val date: String, // "YYYY-MM-DD"
    val completed: Boolean = true
)

@Entity(tableName = "focus_sessions")
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val durationMinutes: Int,
    val category: String, // "Work / Study", "Meditation", "Exercise", "Leisure"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_journals")
data class DailyJournal(
    @PrimaryKey val date: String, // "YYYY-MM-DD"
    val mood: String, // "Happy / Positive", "Calm / Neutral", "Stressed / Anxious", "Productive", "Exhausted / Tired"
    val notes: String,
    val reflection: String
)
