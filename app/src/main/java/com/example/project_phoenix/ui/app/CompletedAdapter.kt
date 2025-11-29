package com.example.project_phoenix.ui.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project_phoenix.R
import com.example.project_phoenix.data.Task

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
        fun bind(task: Task) {
            titleText.text = task.title
        }
    }

}
