package com.example.project_phoenix.data
import android.util.Log

import com.google.ai.client.generativeai.GenerativeModel

class TaskClassificationRepository(
    private val model: GenerativeModel
) {
    suspend fun classify(title: String): TaskCategory {
        Log.d("TaskClassifier", "classify() called with title: '$title'")
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
        val response = raw.trim().uppercase()
        val category = TaskCategory.fromLabel(response)

        return when (response) {
            "PERSONAL_SELF_CARE" -> TaskCategory.PERSONAL_SELF_CARE
            "SCHOOL_WORK"        -> TaskCategory.SCHOOL_WORK
            "HOME_ERRANDS"       -> TaskCategory.HOME_ERRANDS
            "PROJECTS_GOALS"     -> TaskCategory.PROJECTS_GOALS
            else                 -> TaskCategory.PERSONAL_SELF_CARE
        }
    }
}