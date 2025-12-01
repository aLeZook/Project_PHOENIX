package com.example.project_phoenix.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class LevelState(
    val level: Int = 1,
    val points: Int = 0,
    val pointsPerLevel: Int = POINTS_PER_LEVEL
)

const val POINTS_PER_TASK = 10
const val POINTS_PER_LEVEL = 70

class LevelRepository(private val db: FirebaseFirestore) {

    fun getLevelState(uid: String): Flow<LevelState> = callbackFlow {
        val listener = db.collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val level = snapshot?.getLong("level")?.toInt() ?: 1
                val points = snapshot?.getLong("points")?.toInt() ?: 0
                trySend(LevelState(level = level, points = points))
            }

        awaitClose { listener.remove() }
    }

    suspend fun applyPointDelta(uid: String, deltaPoints: Int) {
        val docRef = db.collection("users").document(uid)
        db.runTransaction { txn ->
            val snapshot = txn.get(docRef)
            val currentPoints = snapshot.getLong("points")?.toInt() ?: 0
            val updatedPoints = (currentPoints + deltaPoints).coerceAtLeast(0)
            val updatedLevel = calculateLevel(updatedPoints)

            txn.set(
                docRef,
                mapOf(
                    "points" to updatedPoints,
                    "level" to updatedLevel
                ),
                SetOptions.merge()
            )
        }.await()
    }

    private fun calculateLevel(points: Int): Int = (points / POINTS_PER_LEVEL) + 1
}