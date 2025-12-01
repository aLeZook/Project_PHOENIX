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

        fun fromModelResponse(value: String?): TaskCategory? {
            if (value.isNullOrBlank()) return null
            val normalized = value.normalizeCategory()

            return entries.firstOrNull { category -> category.matchesNormalized(normalized) }
        }
        }

        fun matches(value: String?): Boolean {
            if (value.isNullOrBlank()) return false
            return matchesNormalized(value.normalizeCategory())
        }
    }

    private fun TaskCategory.matchesNormalized(normalized: String): Boolean {
        val normalizedName = name.normalizeCategory()
        val normalizedLabel = label.normalizeCategory()

        return normalized == normalizedName || normalized == normalizedLabel ||
                normalized.contains(normalizedName) || normalized.contains(normalizedLabel)
    }
    private fun String.normalizeCategory(): String = this
        .lowercase()
        .replace("&", "and")
        .replace(Regex("[^a-z\\s]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
