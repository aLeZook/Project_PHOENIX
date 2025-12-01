package com.example.project_phoenix.ui.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.project_phoenix.R
import com.example.project_phoenix.data.Task
import java.text.SimpleDateFormat
import java.util.*

private const val ITEM_VIEW_TYPE_HEADER = 0
private const val ITEM_VIEW_TYPE_TASK = 1

sealed class DisplayableItem {
    data class Header(val title: String) : DisplayableItem()
    data class TaskItem(val task: Task) : DisplayableItem()
}

class TaskAdapter(
    val items: MutableList<DisplayableItem>,
    private val onToggle: (Task) -> Unit,
    private val onDelete: (Task) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkbox: CheckBox = view.findViewById(R.id.taskCheckbox)
        val categoryLabel: TextView = view.findViewById(R.id.taskCategoryLabel)
        val dueDateLabel: TextView = view.findViewById(R.id.taskDueDateLabel)
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val headerTitle: TextView = view.findViewById(R.id.header_title)
        fun bind(header: DisplayableItem.Header) {
            headerTitle.text = header.title
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is DisplayableItem.Header -> ITEM_VIEW_TYPE_HEADER
            is DisplayableItem.TaskItem -> ITEM_VIEW_TYPE_TASK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_VIEW_TYPE_HEADER) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_header, parent, false)
            HeaderViewHolder(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_task, parent, false)
            TaskViewHolder(v)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is DisplayableItem.Header -> (holder as HeaderViewHolder).bind(item)
            is DisplayableItem.TaskItem -> {
                val taskHolder = holder as TaskViewHolder
                val task = item.task
                taskHolder.checkbox.text = task.title + if (task.recurring) " (daily)" else ""
                taskHolder.categoryLabel.text = task.category.label

                if (task.dueDate != null && !task.recurring) {
                    val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
                    taskHolder.dueDateLabel.text = sdf.format(task.dueDate)
                    taskHolder.dueDateLabel.visibility = View.VISIBLE
                } else {
                    taskHolder.dueDateLabel.visibility = View.GONE
                }

                taskHolder.checkbox.setOnCheckedChangeListener(null)
                taskHolder.checkbox.isChecked = task.completed

                taskHolder.checkbox.setOnCheckedChangeListener { _, _ ->
                    onToggle(task)
                }

                taskHolder.itemView.setOnLongClickListener {
                    onDelete(task)
                    true
                }
            }
        }
    }
}
