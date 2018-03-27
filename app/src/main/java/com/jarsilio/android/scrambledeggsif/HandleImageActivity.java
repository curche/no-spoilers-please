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

package com.jarsilio.android.scrambledeggsif;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.security.SecureRandom;
import java.util.ArrayList;

import timber.log.Timber;

public class HandleImageActivity extends AppCompatActivity {
    private ExifScrambler exifScrambler;
    private Utils utils;
    private Settings settings;

    private static final String ALREADY_SCRAMBLED_PROOF_KEY = "already_scrambled_proof_key";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handle_image);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Timber.d("Intent type: %s", type);
        if (getUtils().isPermissionGranted()) {
            if (action.equals(Intent.ACTION_SEND)) {
                handleSendImage(intent);
            } else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
                handleSendMultipleImages(intent);
            }
        } else {
            Timber.d("READ_EXTERNAL_STORAGE has not been granted. Showing toast to tell the user to open the app");
            Toast.makeText(this, getString(R.string.permissions_open_app_toast), Toast.LENGTH_LONG).show();
        }
        scheduleAlarm();
        finish();
    }

    private void scheduleAlarm() {
        Timber.d("Scheduling alarm to clean up cache directory (ExifScramblerCleanUp)");
        AlarmManager alarmManager =(AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getApplicationContext(), CleanUpAlarmReceiver.class);
        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime(),
                AlarmManager.INTERVAL_HALF_DAY,
                alarmPendingIntent);
    }

    private void handleSendImage(Intent intent) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        int alreadyScrambledProof = intent.getExtras().getInt(ALREADY_SCRAMBLED_PROOF_KEY);
        boolean alreadyScrambled = alreadyScrambledProof == getSettings().getLastAlreadyScrambledProof();
        if (alreadyScrambled) {
            Timber.d("Image already scrambled (did you tap twice on 'Scrambled Exif'?). Directly sharing");
            shareImage(imageUri);
        } else if (imageUri != null) {
            if (getUtils().isImage(imageUri)) {
                Uri scrambledImage = getExifScrambler().scrambleImage(imageUri);
                shareImage(scrambledImage);
            }
        }
    }

    private void handleSendMultipleImages(Intent intent) {
        Timber.d("Scrambling multiple images");
        ArrayList<Uri> imageUriList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        int alreadyScrambledProof = intent.getExtras().getInt(ALREADY_SCRAMBLED_PROOF_KEY);
        boolean alreadyScrambled = alreadyScrambledProof == getSettings().getLastAlreadyScrambledProof();
        if (alreadyScrambled) {
            Timber.d("Images already scrambled (did you tap twice on 'Scrambled Exif'?). Directly sharing");
            shareMultipleImages(imageUriList);
        } else {
            ArrayList<Uri> scrambledImagesUriList = new ArrayList<>();
            for (Uri imageUri : imageUriList) {
                if (getUtils().isImage(imageUri)) {
                    Timber.d("Received image (uri): %s", imageUri);
                    Uri scrambledImage = getExifScrambler().scrambleImage(imageUri);
                    scrambledImagesUriList.add(scrambledImage);
                } else {
                    Timber.d("Received something that's not an image (%s) in a SEND_MULTIPLE. Skipping...", imageUri);
                }
            }

            shareMultipleImages(scrambledImagesUriList);
        }
    }



    private void shareImage(Uri imageUri) {
        if (imageUri != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setDataAndType(imageUri, getContentResolver().getType(imageUri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);

            int alreadyScrambledProof = new SecureRandom().nextInt();
            shareIntent.putExtra(ALREADY_SCRAMBLED_PROOF_KEY, alreadyScrambledProof);
            getSettings().setLastAlreadyScrambledProof(alreadyScrambledProof);

            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
        }
    }

    private void shareMultipleImages(ArrayList<Uri> scrambledImagesUriList) {
        if (scrambledImagesUriList.size() > 0) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, scrambledImagesUriList);

            int alreadyScrambledProof = new SecureRandom().nextInt();
            shareIntent.putExtra(ALREADY_SCRAMBLED_PROOF_KEY, alreadyScrambledProof);
            getSettings().setLastAlreadyScrambledProof(alreadyScrambledProof);

            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_multiple_via)));
        }
    }

    private ExifScrambler getExifScrambler() {
        if (exifScrambler == null) {
            exifScrambler = new ExifScrambler(getApplicationContext());
        }
        return exifScrambler;
    }

    private Utils getUtils() {
        if (utils == null) {
            utils = new Utils(getApplicationContext());
        }
        return utils;
    }

    private Settings getSettings() {
        if (settings == null) {
            settings = new Settings(getApplicationContext());
        }
        return settings;
    }
}
