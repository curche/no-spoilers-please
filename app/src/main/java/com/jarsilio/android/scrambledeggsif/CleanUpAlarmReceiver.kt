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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File
import timber.log.Timber

class CleanUpAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Timber.d("Received alarm: cleaning up cache")
        val imagesDir = File(context.cacheDir, "images")
        val images = imagesDir.listFiles()

        if (images == null) {
            Timber.e("For some reason $imagesDir is not a directory (couldn't list files). Skipping cleaning")
            return
        }

        for (image in images) {
            if (System.currentTimeMillis() - image.lastModified() > DAY) {
                Timber.d("Found an image older than a day. Deleting '%s'", image)
                image.delete()
            }
        }

        // If there are no more files left, we don't need to clean up periodically.
        // We will set the alarm again once Scrambled Exif scrambles some Exifs
        if (imagesDir.listFiles().isEmpty()) {
            Timber.d("Cache folder is empty. Canceling cleanup alarm until next time somebody shares an image with us")
            val sender = PendingIntent.getBroadcast(context, 0, intent, 0)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager != null) {
                alarmManager.cancel(sender)
            } else {
                Timber.e("Failed to get alarm service from system. Cannot cancel the scheduled task to clean up files!")
            }
        } else {
            Timber.d("There are still files left in the cache folder (not old enough to delete). Not canceling scheduled cleanup task.")
        }
    }

    companion object {
        private const val DAY = (24 * 60 * 60 * 1000).toLong()
    }
}
