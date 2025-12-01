package com.example.project_phoenix.ui.app

import android.Manifest
import android.media.RingtoneManager
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
import com.example.project_phoenix.notifications.NotificationConstants
import com.example.project_phoenix.notifications.NotificationHelper
import com.google.firebase.messaging.FirebaseMessaging
import androidx.activity.result.contract.ActivityResultContracts


class SettingsFragment : Fragment() {


    private var updatingUi = false
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.onNotificationPermissionGranted()
            } else {
                updatingUi = true
                notificationsSwitch.isChecked = false
                updatingUi = false
                viewModel.onNotificationsToggleRequested(false, false)
            }
        }

    private lateinit var soundSwitch: MaterialSwitch
    private lateinit var notificationsSwitch: MaterialSwitch
    private lateinit var soundStatus: TextView
    private lateinit var notificationStatus: TextView
    private val viewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(
            SettingsRepository(requireContext().applicationContext),
            FirebaseAuth.getInstance(),
            FirebaseMessaging.getInstance()
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

        NotificationHelper.ensureNotificationChannel(requireContext())

        val emailText = view.findViewById<TextView>(R.id.emailText)
        val usernameText = view.findViewById<TextView>(R.id.usernameText)
        val appVersionText = view.findViewById<TextView>(R.id.appVersionNumber)
        soundSwitch = view.findViewById(R.id.switchSound)
        notificationsSwitch = view.findViewById(R.id.switchNotifications)
        soundStatus = view.findViewById(R.id.soundStatus)
        notificationStatus = view.findViewById(R.id.notificationStatus)

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
                    soundStatus.text = if (state.soundEnabled) {
                        getString(R.string.settings_sound_on)
                    } else {
                        getString(R.string.settings_sound_off)
                    }
                    notificationStatus.text = if (state.notificationsEnabled) {
                        getString(R.string.settings_notifications_on)
                    } else {
                        getString(R.string.settings_notifications_off)
                    }
                    updatingUi = false
                    emailText.text = getString(R.string.email_label, state.email)
                    usernameText.text = getString(R.string.username_label, state.username)
                    appVersionText.text = state.appVersion
                    if (state.notificationsEnabled && !checkNotificationPermission()) {
                        updatingUi = true
                        notificationsSwitch.isChecked = false
                        updatingUi = false
                        viewModel.onNotificationsToggleRequested(false, false)
                        requestNotificationPermission()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collectLatest { event ->
                    when (event) {
                        SettingsEvent.RequestNotificationPermission -> {
                            updatingUi = true
                            notificationsSwitch.isChecked = false
                            updatingUi = false
                            requestNotificationPermission()
                        }
                        SettingsEvent.ShowNotificationEnabled -> sendNotification()
                    }
                }
            }
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
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    //Sends notification
    private fun sendNotification() {
        if (!checkNotificationPermission()) return

        NotificationHelper.ensureNotificationChannel(requireContext())

        val builder = NotificationCompat.Builder(requireContext(), NotificationConstants.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.notification_enabled_title))
            .setContentText(getString(R.string.notification_enabled_body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        val notificationManager = NotificationManagerCompat.from(requireContext())
        notificationManager.notify(1001, builder.build())
    }
}
