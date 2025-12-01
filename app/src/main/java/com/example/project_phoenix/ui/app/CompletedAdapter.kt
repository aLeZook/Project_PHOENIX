package com.example.project_phoenix.ui.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project_phoenix.R
import com.example.project_phoenix.data.Task
import java.text.SimpleDateFormat
import java.util.*

class CompletedAdapter : RecyclerView.Adapter<CompletedAdapter.CompletedViewHolder>() {

    private var tasks: List<Task> = emptyList()

    fun submitList(list: List<Task>) {
        tasks = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CompletedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_completed_task, parent, false)
        return CompletedViewHolder(view)
    }

    override fun onBindViewHolder(holder: CompletedViewHolder, position: Int) {
        holder.bind(tasks[position])
    }

    override fun getItemCount() = tasks.size

    inner class CompletedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.completedTaskTitle)
        private val categoryText: TextView = itemView.findViewById(R.id.completedTaskCategory)
        private val dateText: TextView = itemView.findViewById(R.id.completedTaskDate)

        fun bind(task: Task) {
            titleText.text = task.title
            categoryText.text = task.category.label

            val date = task.dueDate
            if (date != null) {
                val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                dateText.text = sdf.format(date)
                dateText.visibility = View.VISIBLE
            } else {
                dateText.visibility = View.GONE
            }
        }
    }

}
