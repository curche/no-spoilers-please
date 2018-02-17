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

import android.app.Application;
import android.os.Build;

import timber.log.Timber;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            // Timber.plant(new Timber.DebugTree());
            Timber.plant(new LongTagTree(getPackageName()));
        }
    }

    public class LongTagTree extends Timber.DebugTree {
        private static final int MAX_TAG_LENGTH = 23;
        private final String packageName;

        public LongTagTree(String packageName) {
            this.packageName = packageName;
        }

        protected String getMessage(String tag, String message) {
            String newMessage;
            if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                // Tag length limitation (<23): Use truncated package name and add class name to message
                newMessage = String.format("%s: %s", tag, message);
            } else {
                // No tag length limit limitation: Use package name *and* class name
                newMessage = message;
            }
            return newMessage;
        }

        protected String getTag(String tag) {
            String newTag;
            if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                // Tag length limitation (<23): Use truncated package name and add class name to message
                newTag = packageName;
                if (newTag.length() > MAX_TAG_LENGTH) {
                    newTag = "..." + packageName.substring(packageName.length() - MAX_TAG_LENGTH + 3, packageName.length());
                }
            } else {
                // No tag length limit limitation: Use package name *and* class name
                newTag = String.format("%s (%s)", packageName, tag);
            }

            return newTag;
        }

        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            String newMessage = getMessage(tag, message);
            String newTag = getTag(tag);

            super.log(priority, newTag, newMessage, t);
        }
    }
}
