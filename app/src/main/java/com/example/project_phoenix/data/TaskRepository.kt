package com.example.project_phoenix.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import java.time.LocalDate

class TaskRepository(private val db: FirebaseFirestore) {

    /**
     * ACTIVE tasks for today:
     * - recurring tasks (reset daily)
     * - one-time tasks whose date == today
     * - NOT completed
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
                    val completed = doc.getBoolean("completed") ?: false
                    val recurring = doc.getBoolean("recurring") ?: false
                    val date = doc.getString("date") ?: ""
                    //this will add the category to each task
                    val category = TaskCategory.fromLabel(doc.getString("category"))
                        ?: TaskCategory.PERSONAL_SELF_CARE

                    // Reset recurring tasks for new day
                    if (recurring && date != today) {
                        doc.reference.update(
                            mapOf("completed" to false, "date" to today)
                        )
                    }

                    // ACTIVE if:
                    // recurring OR date==today AND not completed
                    if (!completed && (recurring || date == today)) {
                        list.add(
                            Task(
                                id = id,
                                title = title,
                                completed = false,
                                recurring = recurring,
                                date = today,
                                category = category
                            )
                        )
                    }
                }

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

    /**
     * COMPLETED tasks for today.
     */
    fun getCompletedTasks(uid: String): Flow<List<Task>> = callbackFlow {
        val today = LocalDate.now().toString()

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
                    val id = doc.id
                    val title = doc.getString("title") ?: ""
                    val recurring = doc.getBoolean("recurring") ?: false
                    val date = doc.getString("date") ?: today
                    val category = TaskCategory.fromLabel(doc.getString("category"))
                        ?: TaskCategory.PERSONAL_SELF_CARE

                    // Completed tasks are shown only if:
                    // - recurring (completed anytime today)
                    // - OR one-time tasks completed today
                    if (recurring || date == today) {
                        list.add(
                            Task(
                                id = id,
                                title = title,
                                completed = true,
                                recurring = recurring,
                                date = date,
                                category = category
                            )
                        )
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
    suspend fun addTask(uid: String, title: String, recurring: Boolean, category: TaskCategory) {
        val today = LocalDate.now().toString()
        val docRef = db.collection("users").document(uid)
            .collection("tasks")
            .document()

        val data = mapOf(
            "id" to docRef.id,
            "title" to title,
            "completed" to false,
            "recurring" to recurring,
            "date" to today,
            "category" to category.label
        )

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
                    "category" to task.category.label
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
