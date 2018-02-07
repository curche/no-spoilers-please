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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.io.File;

public class CleanUpAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "ExifScramblerCleanUp";

    private static final long DAY = 24 * 60 * 60 * 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received alarm: cleaning up cache");
        File imagesDir = new File(context.getCacheDir() + "/images");
        File[] files = imagesDir.listFiles();
        if (files == null) {
            Log.e(TAG, "For some reason " + imagesDir + " is not a directory. Skipping cleaning");
        } else {
            for (File image : files) {
                if (System.currentTimeMillis() - image.lastModified() > DAY) {
                    Log.d(TAG, String.format("Found an image older than a day. Deleting '%s'", image));
                    image.delete();
                }
            }

            // If there are no more files left, we don't need to clean up periodically.
            // We will set the alarm again once Scrambled Exif scrambles some Exifs
            if (files.length == 0) {
                Log.d(TAG, "Cache folder is empty. Canceling cleanup alarm until next time somebody shares an image with us");
                PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, 0);
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(sender);
            } else {
                Log.d(TAG, "There are still files left in the cache folder (not old enough to delete)");
            }
        }
    }
}
