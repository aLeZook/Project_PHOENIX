package com.example.project_phoenix.data

enum class TaskCategory(val label: String) {
    //the four labels to our categories
    PERSONAL_SELF_CARE("Personal & self-care"),
    SCHOOL_WORK("School & work"),
    HOME_ERRANDS("Home & errands"),
    PROJECTS_GOALS("Projects & goals");

    companion object {
        fun fromLabel(value: String?): TaskCategory? {
            if (value.isNullOrBlank()) return null
            val normalized = value.trim().lowercase()
            return entries.firstOrNull { normalized == it.label.lowercase() }
        }
    }}