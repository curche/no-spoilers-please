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
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public class HandleImageActivity extends AppCompatActivity {
    private static final String TAG = "HandleImageActivity";

    private ExifScrambler exifScrambler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handle_image);

        exifScrambler = ExifScrambler.getInstance(getApplicationContext());

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Log.d(TAG, "Type (intent): " + type);
        if (Utils.isPermissionGranted(getApplicationContext())) {
            if (action.equals(Intent.ACTION_SEND)) {
                handleSendImage(intent);
            } else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
                handleSendMultipleImages(intent);
            }
        } else {
            Log.d(TAG, "READ_EXTERNAL_STORAGE has not been granted. Showing toast to tell the user to open the app");
            Toast.makeText(this, getString(R.string.permissions_open_app_toast), Toast.LENGTH_LONG).show();
        }
        scheduleAlarm();
        finish();
    }

    public void scheduleAlarm() {
        Log.d(TAG, "Scheduling alarm to clean up cache directory (ExifScramblerCleanUp)");
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
        if (imageUri != null) {
            if (Utils.isImage(getApplicationContext(), imageUri)) {
                Uri scrambledImage = exifScrambler.scrambleImage(imageUri);
                shareImage(scrambledImage);
            }
        }
    }
    private void handleSendMultipleImages(Intent intent) {
        Log.d(TAG, "Scrambling multiple images");
        ArrayList<Uri> imageUriList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

        ArrayList<Uri> scrambledImagesUriList = new ArrayList<>();
        for (Uri imageUri : imageUriList) {
            if (Utils.isImage(getApplicationContext(), imageUri)) {
                Log.d(TAG, "Received image (uri): " + imageUri);
                Uri scrambledImage = exifScrambler.scrambleImage(imageUri);
                scrambledImagesUriList.add(scrambledImage);
            } else {
                Log.d(TAG, String.format("Received something that's not an image (%s) in a SEND_MULTIPLE. Skipping...", imageUri));
            }
        }

        shareMultipleImages(scrambledImagesUriList);
    }



    private void shareImage(Uri imageUri) {
        if (imageUri != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setDataAndType(imageUri, getContentResolver().getType(imageUri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
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
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_multiple_via)));
        }
    }
}
