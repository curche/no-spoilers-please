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

package com.jarsilio.android.scrambledeggsif

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TableLayout
import android.widget.TextView
import com.jarsilio.android.common.dialog.Dialogs
import com.jarsilio.android.common.privacypolicy.PrivacyPolicyBuilder
import com.jarsilio.android.scrambledeggsif.utils.Utils

import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder

import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private val utils: Utils by lazy { Utils(applicationContext) }

    override fun onResume() {
        super.onResume()

        updateLayout()

        Dialogs(this).showSomeLoveDialogIfNecessary()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.request_permission_button)
        button.setOnClickListener { requestPermissions() }

        updateLayout()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        when (id) {
            R.id.privacy_policy_menu_item -> showPrivacyPolicyActivity()
            R.id.settings_menu_item -> startActivity(Intent(applicationContext, SettingsActivity::class.java))
            R.id.licenses_menu_item -> showAboutLicensesActivity()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showPrivacyPolicyActivity() {
        PrivacyPolicyBuilder()
                .withIntro(getString(R.string.app_name), "Juan García Basilio (juanitobananas)")
                .withUrl("https://gitlab.com/juanitobananas/scrambled-exif/blob/master/PRIVACY.md#scrambled-exif-privacy-policy")
                .withMeSection()
                .withFDroidSection()
                .withGooglePlaySection()
                .withEmailSection("juam+scrambled@posteo.net")
                .start(applicationContext)
    }

    private fun showAboutLicensesActivity() {
        LibsBuilder()
                .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                .withAboutIconShown(true)
                .withAboutVersionShown(true)
                .withActivityTitle(getString(R.string.licenses_menu_item))
                .withAboutDescription(getString(R.string.licenses_about_libraries_text))
                .start(applicationContext)
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Timber.d("Requesting READ_EXTERNAL_STORAGE permission")
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
        }
    }

    private fun updateLayout() {
        val button = findViewById<Button>(R.id.request_permission_button)
        val permissionExplanation = findViewById<TextView>(R.id.permissions_explanation)
        val instructionsTitle = findViewById<TextView>(R.id.instructions_title)
        val instructionsTable = findViewById<TableLayout>(R.id.instructions_table)
        val instructionsVoila = findViewById<TextView>(R.id.instructions_voila)

        if (utils.isPermissionGranted) {
            button.visibility = View.GONE
            permissionExplanation.visibility = View.GONE
            instructionsTitle.visibility = View.VISIBLE
            instructionsTable.visibility = View.VISIBLE
            instructionsVoila.visibility = View.VISIBLE
        } else {
            button.visibility = View.VISIBLE
            permissionExplanation.visibility = View.VISIBLE
            instructionsTitle.visibility = View.GONE
            instructionsTable.visibility = View.GONE
            instructionsVoila.visibility = View.GONE
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Permission granted")
                } else {
                    Timber.d("Permission denied")
                }
                updateLayout()
            }
        }
    }

    companion object {
        private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1000
    }
}
