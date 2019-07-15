/*
 * Copyright (c) 2018 Juan García Basilio
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

package com.jarsilio.android.scrambledeggsif.utils

import android.content.Context
import android.content.SharedPreferences
import android.preference.CheckBoxPreference
import android.preference.PreferenceActivity
import android.preference.PreferenceManager

import java.util.UUID

internal class Settings(private val context: Context) {
    private var preferenceActivity: PreferenceActivity? = null

    val preferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(context)

    var isRewriteImages: Boolean
        get() = preferences.getBoolean(REWRITE_IMAGES, true)
        set(rewriteImages) = setPreference(REWRITE_IMAGES, rewriteImages)

    val isKeepJpegOrientation: Boolean
        get() = preferences.getBoolean(KEEP_JPEG_ORIENTATION, true)

    var isRenameImages: Boolean
        get() = preferences.getBoolean(RENAME_IMAGES, true)
        set(renameImages) = setPreference(RENAME_IMAGES, renameImages)

    var lastAlreadyScrambledProof: String?
        get() = preferences.getString(LAST_ALREADY_SCRAMBLED_PROOF, "NO_PROOF-" + UUID.randomUUID().toString())
        set(proof) = setPreference(LAST_ALREADY_SCRAMBLED_PROOF, proof)

    private fun setPreference(key: String, value: Boolean) {
        if (preferenceActivity != null) { // This changes the GUI, but it needs the MainActivity to have started
            val checkBox = preferenceActivity!!.findPreference(key) as CheckBoxPreference
            checkBox.isChecked = value
        } else { // This doesn't change the GUI
            preferences.edit().putBoolean(key, value).apply()
        }
    }

    private fun setPreference(key: String, value: String?) {
        preferences.edit().putString(key, value).apply()
    }

    fun setPreferenceActivity(preferenceActivity: PreferenceActivity) {
        /* If a Preference is updated using getPreferences().edit().putBoolean(key, value).commit(),
         * the GUI doesn't update automatically.
         * If it is changed using a CheckBox, then it does work. In order to get a CheckBox object,
         * we need to have the preferenceActivity, which is the MainActivity so we set it the moment
         * it is launched so that we can use it afterwards.
         */
        this.preferenceActivity = preferenceActivity
    }

    companion object {
        const val REWRITE_IMAGES = "pref_rewrite_images"
        const val LAST_ALREADY_SCRAMBLED_PROOF = "pref_last_already_scrambled_proof"
        const val KEEP_JPEG_ORIENTATION = "pref_keep_jpeg_orientation"
        const val RENAME_IMAGES = "pref_rename_images"
    }
}
