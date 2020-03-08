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

package com.jarsilio.android.scrambledeggsif.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

internal class Settings(private val context: Context) {

    private val preferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(context)

    val isKeepJpegOrientation: Boolean
        get() = preferences.getBoolean(KEEP_JPEG_ORIENTATION, true)

    var isRenameImages: Boolean
        get() = preferences.getBoolean(RENAME_IMAGES, true)
        set(renameImages) = setPreference(RENAME_IMAGES, renameImages)

    var isLoggingEnabled: Boolean
        get() = preferences.getBoolean(LOGGING_ENABLED, false)
        set(loggingEnabled) = setPreference(LOGGING_ENABLED, loggingEnabled)

    var isPanicDeleteCachedImages: Boolean
        get() = preferences.getBoolean(PANIC_DELETE_CACHED_IMAGES, true)
        set(deleteCachedImages) = setPreference(PANIC_DELETE_CACHED_IMAGES, deleteCachedImages)

    var isPanicClearAppData: Boolean
        get() = preferences.getBoolean(PANIC_CLEAR_APP_DATA, false)
        set(loggingEnabled) = setPreference(PANIC_CLEAR_APP_DATA, loggingEnabled)

    private fun setPreference(key: String, value: Boolean) {
        preferences.edit().putBoolean(key, value).apply()
    }

    companion object {
        const val KEEP_JPEG_ORIENTATION = "pref_keep_jpeg_orientation"
        const val RENAME_IMAGES = "pref_rename_images"
        const val LOGGING_ENABLED = "pref_logging_enabled"
        const val SEND_LOGS_TO_DEV = "pref_send_logs_to_dev"
        const val PANIC_DELETE_CACHED_IMAGES = "pref_panic_delete_cached_images"
        const val PANIC_CLEAR_APP_DATA = "pref_panic_clear_app_data"
    }
}
