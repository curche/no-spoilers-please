/*
 * Copyright (c) 2018-2019 Juan García Basilio
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
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

import java.util.ArrayList

import timber.log.Timber
import android.os.Parcelable

class HandleImageActivity : AppCompatActivity() {

    private val exifScrambler: ExifScrambler by lazy { ExifScrambler(applicationContext) }
    private val utils: Utils by lazy { Utils(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_handle_image)

        val intent = intent
        val action = intent.action
        val type = intent.type
        Timber.d("Intent type: %s", type)
        if (utils.isPermissionGranted) {
            if (action == Intent.ACTION_SEND) {
                handleSendImage(intent)
            } else if (action == Intent.ACTION_SEND_MULTIPLE) {
                handleSendMultipleImages(intent)
            }
        } else {
            Timber.d("READ_EXTERNAL_STORAGE has not been granted. Showing toast to tell the user to open the app")
            Toast.makeText(this, getString(R.string.permissions_open_app_toast), Toast.LENGTH_LONG).show()
        }
        scheduleAlarm()
        finish()
    }

    private fun scheduleAlarm() {
        Timber.d("Scheduling alarm to clean up cache directory (ExifScramblerCleanUp)")
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(applicationContext, CleanUpAlarmReceiver::class.java)
        val alarmPendingIntent = PendingIntent.getBroadcast(applicationContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                AlarmManager.INTERVAL_HALF_DAY,
                alarmPendingIntent)
    }

    private fun handleSendImage(intent: Intent) {
        val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (imageUri != null) {
            if (utils.isImage(imageUri)) {
                val scrambledImage = exifScrambler.scrambleImage(imageUri)
                shareImageExcludingApp(scrambledImage)
            }
        }
    }

    private fun handleSendMultipleImages(intent: Intent) {
        Timber.d("Scrambling multiple images")
        val imageUriList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        val scrambledImagesUriList = ArrayList<Uri>()
        for (imageUri in imageUriList) {
            if (utils.isImage(imageUri)) {
                Timber.d("Received image (uri): %s", imageUri)
                val scrambledImage = exifScrambler.scrambleImage(imageUri)
                scrambledImagesUriList.add(scrambledImage)
            } else {
                Timber.d("Received something that's not an image (%s) in a SEND_MULTIPLE. Skipping...", imageUri)
            }
        }

        shareImagesExcludingApp(scrambledImagesUriList)
    }

    private fun shareImageExcludingApp(imageUri: Uri) {
        shareImagesExcludingApp(arrayListOf(imageUri))
    }

    private fun shareImagesExcludingApp(imageUris: ArrayList<Uri>) {
        val targetedShareIntents = buildTargetedShareIntents(imageUris)
        val chooserIntent = Intent.createChooser(targetedShareIntents.removeAt(0), getString(R.string.share_multiple_via))
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toTypedArray<Parcelable>())
        startActivity(chooserIntent)
    }

    private fun buildTargetedShareIntents(imageUris: ArrayList<Uri>): ArrayList<Intent> {
        /* Remove our own package from the apps to share with */

        val targetedShareIntents = ArrayList<Intent>()
        val resolveInfos = packageManager.queryIntentActivities(createShareIntent(imageUris), 0)
        for (info in resolveInfos) {
            if (info.activityInfo.packageName.toLowerCase() == packageName.toLowerCase()) {
                continue // Don't add out own package
            }

            val targetedShareIntent = createShareIntent(imageUris)
            targetedShareIntent.setPackage(info.activityInfo.packageName)
            targetedShareIntents.add(targetedShareIntent)
        }

        return targetedShareIntents
    }

    private fun createShareIntent(uris: ArrayList<Uri>): Intent {
        return Intent().apply {
            if (uris.size == 1) {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uris[0])
            } else {
                action = Intent.ACTION_SEND_MULTIPLE
                putExtra(Intent.EXTRA_STREAM, uris)
            }
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
        }
    }
}
