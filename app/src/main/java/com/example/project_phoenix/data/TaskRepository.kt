package com.example.project_phoenix.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class TaskRepository(private val db: FirebaseFirestore) {

    /**
     * Get all active (not completed) tasks for a user.
     * This includes:
     * - recurring tasks
     * - one-time tasks for today
     * - one-time tasks for the future
     *
     * It also handles the daily reset of recurring tasks.
     */
    fun getTasks(uid: String): Flow<List<Task>> = callbackFlow {
        val today = LocalDate.now().toString()

        val listener = db.collection("users")
            .document(uid)
            .collection("tasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents ?: emptyList()
                val list = mutableListOf<Task>()

                for (doc in docs) {
                    val id = doc.id
                    val title = doc.getString("title") ?: ""
                    var completed = doc.getBoolean("completed") ?: false
                    val recurring = doc.getBoolean("recurring") ?: false
                    var date = doc.getString("date") ?: ""
                    val dueDate = doc.getTimestamp("dueDate")?.toDate()
                    val category = TaskCategory.fromLabel(doc.getString("category"))
                        ?: TaskCategory.PERSONAL_SELF_CARE

                    // Reset recurring tasks for new day. This will trigger a new snapshot.
                    if (recurring && date != today) {
                        doc.reference.update(
                            mapOf("completed" to false, "date" to today)
                        )
                        // For the current pass, we can treat the task as updated to prevent flicker.
                        completed = false
                        date = today
                    }

                    // Add all non-completed tasks to the list.
                    if (!completed) {
                        list.add(
                            Task(
                                id = id,
                                title = title,
                                completed = completed,
                                recurring = recurring,
                                date = date,
                                category = category,
                                dueDate = dueDate
                            )
                        )
                    }
                }

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    /**
     * Get all completed tasks for a user.
     */
    fun getCompletedTasks(uid: String): Flow<List<Task>> = callbackFlow {
        val listener = db.collection("users")
            .document(uid)
            .collection("tasks")
            .whereEqualTo("completed", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val docs = snapshot?.documents ?: emptyList()
                val list = mutableListOf<Task>()

                for (doc in docs) {
                    try {
                        val id = doc.id
                        val title = doc.getString("title") ?: ""
                        val completed = doc.getBoolean("completed") ?: true
                        val recurring = doc.getBoolean("recurring") ?: false
                        val date = doc.getString("date") ?: ""
                        val dueDate = doc.getTimestamp("dueDate")?.toDate()
                        val category = TaskCategory.fromLabel(doc.getString("category"))
                            ?: TaskCategory.PERSONAL_SELF_CARE

                        list.add(Task(id, title, completed, recurring, dueDate, date, category))
                    } catch (e: Exception) {
                        // In case of a parsing error, log it and continue
                    }
                }

                trySend(list)
            }

        awaitClose { listener.remove() }
    }


    /**
     * Clear all completed tasks for the user
     * (does not touch recurring daily tasks that reset)
     */
    fun clearCompletedTasks(uid: String) {
        val completedTasksRef = db.collection("users")
            .document(uid)
            .collection("tasks")
            .whereEqualTo("completed", true)

        completedTasksRef.get().addOnSuccessListener { snapshot ->
            for (doc in snapshot.documents) {
                doc.reference.delete()
            }
        }
    }

    /**
     * Add a new task
     */
    suspend fun addTask(uid: String, title: String, recurring: Boolean, category: TaskCategory, dueDate: Date?) {
        val dateString = if (dueDate != null && !recurring) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.format(dueDate)
        } else {
            LocalDate.now().toString()
        }

        val docRef = db.collection("users").document(uid)
            .collection("tasks")
            .document()

        val data = mutableMapOf<String, Any>(
            "id" to docRef.id,
            "title" to title,
            "completed" to false,
            "recurring" to recurring,
            "date" to dateString,
            "category" to category.label
        )

        if (dueDate != null && !recurring) {
            data["dueDate"] = dueDate
        }

        docRef.set(data).await()
    }

    /**
     * Update task (toggle complete, rename, etc.)
     */
    suspend fun updateTask(uid: String, task: Task) {
        db.collection("users").document(uid)
            .collection("tasks")
            .document(task.id)
            .update(
                mapOf(
                    "title" to task.title,
                    "completed" to task.completed,
                    "recurring" to task.recurring,
                    "date" to task.date,
                    "category" to task.category.label,
                    "dueDate" to task.dueDate
                )
            ).await()
    }

    /**
     * Delete a task
     */
    suspend fun deleteTask(uid: String, taskId: String) {
        db.collection("users").document(uid)
            .collection("tasks")
            .document(taskId)
            .delete()
            .await()
    }

}
