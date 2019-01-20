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

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;

import com.jarsilio.android.privacypolicy.PrivacyPolicyBuilder;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1000;
    private Utils utils;

    @Override
    protected void onResume() {
        super.onResume();

        updateLayout();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = findViewById(R.id.request_permission_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                requestPermissions();
            }
        });

        updateLayout();
    }
    @Override

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.privacy_policy_menu_item:
                showPrivacyPolicyActivity();
                break;
            case R.id.settings_menu_item:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                break;
            case R.id.licenses_menu_item:
                showAboutLicensesActivity();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
    private void showPrivacyPolicyActivity() {
        new PrivacyPolicyBuilder()
                .withIntro(getString(R.string.app_name), "Juan García Basilio (juanitobananas)")
                .withUrl("https://gitlab.com/juanitobananas/scrambled-exif/blob/master/PRIVACY.md#scrambled-exif-privacy-policy")
                .withMeSection()
                .withFDroidSection()
                .withGooglePlaySection()
                .withEmailSection("juam+scrambled@posteo.net")
                .start(getApplicationContext());
    }

    private void showAboutLicensesActivity() {
        new LibsBuilder()
                .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                .withAboutIconShown(true)
                .withAboutVersionShown(true)
                .withActivityTitle(getString(R.string.licenses_menu_item))
                .withAboutDescription(getString(R.string.licenses_about_libraries_text))
                .start(getApplicationContext());
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Timber.d("Requesting READ_EXTERNAL_STORAGE permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    private void updateLayout() {
        final Button button = findViewById(R.id.request_permission_button);
        final TextView permissionExplanation = findViewById(R.id.permissions_explanation);
        final TextView instructionsTitle = findViewById(R.id.instructions_title);
        final TableLayout instructionsTable  = findViewById(R.id.instructions_table);
        final TextView instructionsVoila = findViewById(R.id.instructions_voila);

        if (getUtils().isPermissionGranted()) {
            button.setVisibility(View.GONE);
            permissionExplanation.setVisibility(View.GONE);
            instructionsTitle.setVisibility(View.VISIBLE);
            instructionsTable.setVisibility(View.VISIBLE);
            instructionsVoila.setVisibility(View.VISIBLE);
        } else {
            button.setVisibility(View.VISIBLE);
            permissionExplanation.setVisibility(View.VISIBLE);
            instructionsTitle.setVisibility(View.GONE);
            instructionsTable.setVisibility(View.GONE);
            instructionsVoila.setVisibility(View.GONE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Permission granted");
                } else {
                    Timber.d("Permission denied");
                }
                updateLayout();
            }
        }
    }

    private Utils getUtils() {
        if (utils == null) {
            utils = new Utils(getApplicationContext());
        }
        return utils;
    }
}
