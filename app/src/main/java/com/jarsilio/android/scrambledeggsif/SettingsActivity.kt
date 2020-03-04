/*
 * Copyright (c) 2018-2020 Juan García Basilio
 *
 * This file is part of Scrambled Exif.
 *
 * Scrambled Exif is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Scrambled Exif is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Scrambled Exif.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.jarsilio.android.scrambledeggsif

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.jarsilio.android.common.dialog.Dialogs
import com.jarsilio.android.common.logging.LogUtils
import com.jarsilio.android.scrambledeggsif.utils.Settings

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

        private val settings: Settings by lazy { Settings(context!!) }
        private val logUtils: LogUtils by lazy { LogUtils(context!!) }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            bindClickListeners()
        }

        private fun bindClickListeners() {
            findPreference<Preference>(Settings.SEND_LOGS_TO_DEV)?.setOnPreferenceClickListener {
                Dialogs(context!!).showReportIssueDialog()
                true
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            when (key) {
                Settings.LOGGING_ENABLED -> {
                    if (settings.isLoggingEnabled) {
                        logUtils.plantPersistentTreeIfNonePlanted()
                    } else {
                        logUtils.uprootPersistentTrees()
                        logUtils.deletePersistentLogs()
                    }
                }
            }
        }

        override fun onResume() {
            preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
            super.onResume()
        }

        override fun onPause() {
            preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }
    }
}
