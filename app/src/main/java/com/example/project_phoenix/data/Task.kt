package com.example.project_phoenix.data

data class Task(
    val id: String = "",
    val title: String = "",
    var completed: Boolean = false,
    val recurring: Boolean = false,
    val date: String = "", // ISO date "yyyy-MM-dd"
    //This will give us our category
    val category: TaskCategory = TaskCategory.PERSONAL_SELF_CARE
)
