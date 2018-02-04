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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.CheckBoxPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

class Settings {
    private Context context;
    private PreferenceActivity preferenceActivity = null;

    Settings(Context context) {
        this.context = context;
    }

    SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    private void setPreference(String key, boolean value) {
        if (preferenceActivity != null) { // This changes the GUI, but it needs the MainActivity to have started
            CheckBoxPreference checkBox = (CheckBoxPreference) preferenceActivity.findPreference(key);
            checkBox.setChecked(value);
        } else { // This doesn't change the GUI
            getPreferences().edit().putBoolean(key, value).apply();
        }
    }

    private void setPreference(String key, long value) {
        getPreferences().edit().putLong(key, value).apply();
    }

    private void setPreference(String key, String value) {
        getPreferences().edit().putString(key, value).apply();
    }

    public void setPreferenceActivity(PreferenceActivity preferenceActivity) {
        /* If a Preference is updated using getPreferences().edit().putBoolean(key, value).commit(),
         * the GUI doesn't update automatically.
         * If it is changed using a CheckBox, then it does work. In order to get a CheckBox object,
         * we need to have the preferenceActivity, which is the MainActivity so we set it the moment
         * it is launched so that we can use it afterwards.
         */
        this.preferenceActivity = preferenceActivity;
    }
}
