package com.proinnovation.isitonline.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.proinnovation.isitonline.IsItOnlineApp
import com.proinnovation.isitonline.R
import com.proinnovation.isitonline.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settingsContainer, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = IsItOnlineApp.PREFS_NAME
        setPreferencesFromResource(R.xml.preferences, rootKey)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            findPreference<Preference>("pref_exact_alarm")?.setOnPreferenceClickListener {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                true
            }
        } else {
            findPreference<Preference>("pref_exact_alarm")?.isVisible = false
        }
    }
}
