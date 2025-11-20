package com.example.project_phoenix.ui.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.project_phoenix.R
import java.text.SimpleDateFormat
import java.util.Locale

class CalendarFragment : Fragment() {

    // Declare your views here to access them later
    private lateinit var calendarView: CalendarView
    private lateinit var selectedDateText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_calendar, container, false)
    }

    // onViewCreated is the best place to find views and set listeners
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Find the views by their ID
        calendarView = view.findViewById(R.id.calendarView)
        selectedDateText = view.findViewById(R.id.selectedDateText)

        // Optional: Set an initial text
        val sdf = SimpleDateFormat("MM/dd/yyyy", Locale.US)
        val initialDate = sdf.format(calendarView.date) // Get today's date from the calendar
        selectedDateText.text = "Selected Date: $initialDate"


        // 2. Set a listener to handle date changes
        calendarView.setOnDateChangeListener { calView, year, month, dayOfMonth ->
            // Note: month is 0-indexed (0 for January, 11 for December)
            val correctMonth = month + 1
            val dateString = "$correctMonth/$dayOfMonth/$year"

            // Update the TextView with the selected date
            selectedDateText.text = "Selected Date: $dateString"

            // You can also show a Toast message or perform any other action
            Toast.makeText(requireContext(), "Selected: $dateString", Toast.LENGTH_SHORT).show()
        }
    }
}
