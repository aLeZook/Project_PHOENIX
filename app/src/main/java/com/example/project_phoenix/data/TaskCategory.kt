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
            val normalized = value.normalizeCategory()

            return entries.firstOrNull { category ->
                val categoryLabel = category.label.normalizeCategory()
                normalized == categoryLabel || normalized.contains(categoryLabel)
            }
        }
        private fun String.normalizeCategory(): String = this
            .lowercase()
            .replace("&", "and")
            .replace(Regex("[^a-z\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    }