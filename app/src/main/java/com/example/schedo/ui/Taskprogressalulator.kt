package com.example.schedo.utils

import com.example.schedo.model.Task

fun calculateTaskProgress(tasks: List<Task>): Float {
    if (tasks.isEmpty()) return 0f
    val completedTasks = tasks.count { it.status == true }
    return (completedTasks.toFloat() / tasks.size) * 100f
}