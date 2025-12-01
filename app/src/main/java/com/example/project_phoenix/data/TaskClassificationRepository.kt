package com.example.project_phoenix.data

import com.google.ai.client.generativeai.GenerativeModel

class TaskClassificationRepository(
    private val model: GenerativeModel
) {
    suspend fun classify(title: String): TaskCategory {
        val prompt = """
            Classify the following task title into one of these categories exactly:
            1) Personal & self-care
            2) School & work
            3) Home & errands
            4) Projects & goals

            Reply with only the category text.
            Task title: "$title"
        """.trimIndent()

        val response = model.generateContent(prompt).text.orEmpty().trim()
        return TaskCategory.fromLabel(response) ?: TaskCategory.PERSONAL_SELF_CARE
    }
}