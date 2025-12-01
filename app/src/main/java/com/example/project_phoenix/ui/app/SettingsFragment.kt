package com.example.project_phoenix.ui.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.project_phoenix.R
import android.widget.TextView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.project_phoenix.data.SettingsRepository
import com.example.project_phoenix.viewm.SettingsEvent
import com.example.project_phoenix.viewm.SettingsViewModel
import com.example.project_phoenix.viewm.SettingsViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsFragment : Fragment() {

    private val REQUEST_CODE_POST_NOTIFICATIONS = 101
    private var updatingUi = false

    private lateinit var soundSwitch: MaterialSwitch
    private lateinit var notificationsSwitch: MaterialSwitch
    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(
            SettingsRepository(requireContext().applicationContext),
            FirebaseAuth.getInstance()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        createNotificationChannel()

        val emailText = view.findViewById<TextView>(R.id.emailText)
        val usernameText = view.findViewById<TextView>(R.id.usernameText)
        val appVersionText = view.findViewById<TextView>(R.id.appVersionNumber)
        soundSwitch = view.findViewById(R.id.switchSound)
        notificationsSwitch = view.findViewById(R.id.switchNotifications)

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (updatingUi) return@setOnCheckedChangeListener
            viewModel.onNotificationsToggleRequested(isChecked, checkNotificationPermission())
        }

        soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (updatingUi) return@setOnCheckedChangeListener
            viewModel.onSoundToggled(isChecked)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collectLatest { state ->
                    updatingUi = true
                    notificationsSwitch.isChecked = state.notificationsEnabled
                    soundSwitch.isChecked = state.soundEnabled
                    updatingUi = false
                    emailText.text = getString(R.string.email_label, state.email)
                    usernameText.text = getString(R.string.username_label, state.username)
                    appVersionText.text = state.appVersion
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        SettingsEvent.RequestNotificationPermission -> requestNotificationPermission()
                        SettingsEvent.ShowNotificationEnabled -> sendNotification()
                    }
                }
            }
        }
    }

    //Creates the notification
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "MyChannel"
            val descriptionText = "Channel for app notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("my_channel_id", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    //Is notification permission granted (should be yes)
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    //Asks for notification permission
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_POST_NOTIFICATIONS
            )
        }
    }

    //Check's user answer on "permission for notifications"
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                viewModel.onNotificationPermissionGranted()
            }
        }
    }

    //Sends notification
    private fun sendNotification() {
        if (!checkNotificationPermission()) return

        val builder = NotificationCompat.Builder(requireContext(), "my_channel_id")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Notification")
            .setContentText("Notifications Turned On!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager = NotificationManagerCompat.from(requireContext())
        notificationManager.notify(1001, builder.build())
    }
}
