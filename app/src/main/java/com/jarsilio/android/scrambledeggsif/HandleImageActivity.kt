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
import java.util.UUID

import timber.log.Timber

class HandleImageActivity : AppCompatActivity() {

    private val exifScrambler: ExifScrambler by lazy { ExifScrambler(applicationContext) }
    private val utils: Utils by lazy { Utils(applicationContext) }
    private val settings: Settings by lazy { Settings(applicationContext) }

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
        if (isAlreadyScrambled(intent)) {
            Timber.d("Image already scrambled (did you tap twice on 'Scrambled Exif'?). Directly sharing")
            shareImage(imageUri)
        } else if (imageUri != null) {
            if (utils.isImage(imageUri)) {
                val scrambledImage = exifScrambler.scrambleImage(imageUri)
                shareImage(scrambledImage)
            }
        }
    }

    private fun handleSendMultipleImages(intent: Intent) {
        Timber.d("Scrambling multiple images")
        val imageUriList = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        if (isAlreadyScrambled(intent)) {
            Timber.d("Images already scrambled (did you tap twice on 'Scrambled Exif'?). Directly sharing")
            shareMultipleImages(imageUriList)
        } else {
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

            shareMultipleImages(scrambledImagesUriList)
        }
    }

    private fun shareImage(imageUri: Uri?) {
        imageUri?.apply {
            shareMultipleImages(arrayListOf(imageUri))
        }
    }

    private fun shareMultipleImages(scrambledImagesUriList: ArrayList<Uri>) {
        if (scrambledImagesUriList.size > 0) {
            val shareIntent = createShareIntent(scrambledImagesUriList)
            setAlreadyScrambled(shareIntent)
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_multiple_via)))
        }
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

    private fun isAlreadyScrambled(intent: Intent): Boolean {
        val alreadyScrambledProof = intent.extras?.getString(ALREADY_SCRAMBLED_PROOF_KEY)
        if (alreadyScrambledProof == null) {
            return false
        } else {
            val lastAlreadyScrambledProof = settings.lastAlreadyScrambledProof
            Timber.v("Current intent's 'already scrambled proof': %s", alreadyScrambledProof)
            Timber.v("Last 'already scrambled proof' we generated: %s", lastAlreadyScrambledProof)
            return alreadyScrambledProof == lastAlreadyScrambledProof
        }
    }

    private fun setAlreadyScrambled(shareIntent: Intent) {
        val alreadyScrambledProof = UUID.randomUUID().toString()
        shareIntent.putExtra(ALREADY_SCRAMBLED_PROOF_KEY, alreadyScrambledProof)
        settings.lastAlreadyScrambledProof = alreadyScrambledProof
    }

    companion object {

        private val ALREADY_SCRAMBLED_PROOF_KEY = "already_scrambled_proof_key"
    }
}
