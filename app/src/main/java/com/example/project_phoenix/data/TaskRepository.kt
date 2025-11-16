package com.example.project_phoenix.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose

class TaskRepository(private val db: FirebaseFirestore) {

    fun getTasks(uid: String): Flow<List<Task>> = callbackFlow {
        val listener = db.collection("users").document(uid)
            .collection("tasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                val list = snapshot?.documents?.map { doc ->
                    Task(doc.id, doc.getString("title") ?: "", doc.getBoolean("completed") ?: false)
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    suspend fun addTask(uid: String, title: String) {
        val doc = db.collection("users").document(uid)
            .collection("tasks")
            .document()
        doc.set(Task(doc.id, title)).await()
    }

    suspend fun updateTask(uid: String, task: Task) {
        db.collection("users").document(uid)
            .collection("tasks").document(task.id)
            .set(task).await()
    }
}
