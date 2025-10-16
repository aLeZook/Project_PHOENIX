package com.example.project_phoenix

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottom_nav)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        bottomNavigationView = findViewById(R.id.bottom_nav)

        bottomNavigationView.setOnItemSelectedListener { menuItem ->

            when (menuItem.itemId) {
                R.id.bottom_home -> replaceFragment(HomeFragment())
                R.id.bottom_settings -> replaceFragment(SettingsFragment())
                R.id.bottom_add -> replaceFragment(HomeFragment())
                R.id.bottom_calendar -> replaceFragment(CalendarFragment())
                R.id.bottom_challenges -> replaceFragment(ChallengesFragment())
                R.id.bottom_stats -> replaceFragment(StatsFragment())

            }
            true
        }

    }
    private fun replaceFragment(fragment: Fragment)
    {
        supportFragmentManager.beginTransaction().replace(R.id.frame_container,fragment).commit()
    }
}