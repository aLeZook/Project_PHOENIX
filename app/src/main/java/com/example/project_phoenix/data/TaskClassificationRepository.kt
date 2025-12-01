package com.example.project_phoenix.data

import com.google.ai.client.generativeai.GenerativeModel

class TaskClassificationRepository(
    private val model: GenerativeModel
) {
    suspend fun classify(title: String): TaskCategory {
        val prompt = """
        You are a strict classifier.
        Classify the following task title into ONE of these categories:

        - PERSONAL_SELF_CARE
        - SCHOOL_WORK
        - HOME_ERRANDS
        - PROJECTS_GOALS

        Reply with ONLY the category label, exactly as written above.

        Task title: "$title"
    """.trimIndent()

        val raw = model.generateContent(prompt).text.orEmpty()

        val cleanedResponse = raw.trim()
        val mappedCategory = TaskCategory.fromModelResponse(cleanedResponse)
            ?: TaskCategory.fromLabel(cleanedResponse)

        val category = mappedCategory ?: TaskCategory.PERSONAL_SELF_CARE

        return category
        }
}