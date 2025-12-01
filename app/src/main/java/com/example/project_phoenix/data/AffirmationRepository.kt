package com.example.project_phoenix.data

import android.content.Context
import android.content.SharedPreferences
import com.google.ai.client.generativeai.GenerativeModel
import java.time.LocalDate

class AffirmationRepository(
    private val model: GenerativeModel,
    private val preferences: SharedPreferences,
) {
    suspend fun fetchDailyAffirmation(): Result<String> = runCatching {
        val today = LocalDate.now().toString()
        val cachedDate = preferences.getString(KEY_LAST_DATE, null)
        val cachedAffirmation = preferences.getString(KEY_LAST_AFFIRMATION, null)

        if (cachedDate == today && !cachedAffirmation.isNullOrBlank()) {
            return@runCatching cachedAffirmation
        }

        val prompt = """Share one concise, uplifting daily affirmation. Return only the affirmation text without any quotation marks or prefixes."""
        val affirmation = model.generateContent(prompt).text?.trim().takeUnless { it.isNullOrBlank() }
            ?: error("Empty response from Gemini")

        preferences.edit()
            .putString(KEY_LAST_DATE, today)
            .putString(KEY_LAST_AFFIRMATION, affirmation)
            .apply()

        affirmation
    }

    companion object {
        private const val KEY_LAST_DATE = "last_affirmation_date"
        private const val KEY_LAST_AFFIRMATION = "last_affirmation_text"
        private const val PREFS_NAME = "affirmations_prefs"

        fun create(context: Context, model: GenerativeModel): AffirmationRepository =
            AffirmationRepository(
                model = model,
                preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            )
    }
}