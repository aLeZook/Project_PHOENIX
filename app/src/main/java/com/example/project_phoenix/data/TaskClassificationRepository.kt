package com.example.project_phoenix.data
import android.util.Log

import com.google.ai.client.generativeai.GenerativeModel

class TaskClassificationRepository(
    private val model: GenerativeModel
) {
    private companion object {
        const val TAG = "TaskClassifier"
    }
    suspend fun classify(title: String): TaskCategory {
        Log.i(TAG, "classify() called with title: '$title'")
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
        Log.i(TAG, "Raw model response: '$raw'")

        val cleanedResponse = raw.trim()
        val mappedCategory = TaskCategory.fromModelResponse(cleanedResponse)
            ?: TaskCategory.fromLabel(cleanedResponse)

        val category = mappedCategory ?: TaskCategory.PERSONAL_SELF_CARE

        if (mappedCategory == null && !category.matches(cleanedResponse)) {
            Log.w(TAG, "Falling back to default category; could not map response: '$cleanedResponse'")
        }

        Log.i(TAG, "Mapped response to category: ${category.name} (${category.label})")

        return category
        }
}