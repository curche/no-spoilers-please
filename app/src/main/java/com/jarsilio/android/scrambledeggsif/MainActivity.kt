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

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TableLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.jarsilio.android.common.dialog.Dialogs
import com.jarsilio.android.common.extensions.appName
import com.jarsilio.android.common.extensions.flavor
import com.jarsilio.android.common.menu.CommonMenu
import com.jarsilio.android.common.prefs.Values
import com.jarsilio.android.common.privacypolicy.PrivacyPolicyBuilder
import com.jarsilio.android.common.utils.VendorUtils
import com.jarsilio.android.scrambledeggsif.utils.MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
import com.jarsilio.android.scrambledeggsif.utils.Utils
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.LibsBuilder
import timber.log.Timber

class MainActivity : AppCompatActivity() {
    private val dialogs: Dialogs by lazy { Dialogs(this) }
    private val utils: Utils by lazy { Utils(applicationContext) }
    private val vendorUtils: VendorUtils by lazy { VendorUtils(applicationContext) }
    private val commonMenu: CommonMenu by lazy { CommonMenu(this) }

    override fun onResume() {
        super.onResume()

        updateLayout()

        dialogs.showSomeLoveDialogIfNecessary()
        // No Spoilers Please -->
        // dialogs.showSoLongAndThanksForAllTheFishDialog()
        // <-- No Spoilers Please

        showMiuiPermissionDialogIfNecessary()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button = findViewById<Button>(R.id.request_permission_button)
        button.setOnClickListener { utils.requestPermissionsIfNecessary(this) }

        updateLayout()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        commonMenu.addImpressumToMenu(menu)
        if (flavor == "fortuneCookies") {
            commonMenu.addCookiesToMenu(menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            // R.id.privacy_policy_menu_item -> showPrivacyPolicyActivity()
            R.id.settings_menu_item -> startActivity(Intent(applicationContext, SettingsActivity::class.java))
            R.id.licenses_menu_item -> showAboutLicensesActivity()
        }

        return super.onOptionsItemSelected(item)
    }

    /* No Spoilers Please -->
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
    <-- No Spoilers Please */

    private fun showAboutLicensesActivity() {
        var style = Libs.ActivityStyle.LIGHT_DARK_TOOLBAR
        var theme = R.style.AppTheme_About_Light

        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            style = Libs.ActivityStyle.DARK
            theme = R.style.AppTheme_About_Dark
        }

        LibsBuilder()
                .withActivityStyle(style)
                .withActivityTheme(theme)
                .withAboutIconShown(true)
                .withAboutVersionShown(true)
                .withActivityTitle(getString(R.string.licenses_menu_item))
                .withAboutDescription(getString(R.string.licenses_about_libraries_text))
                .start(applicationContext)
    }

    private fun showMiuiPermissionDialogIfNecessary() {
        if (Values.getInstance(this).showMiuiPermissionsDialog) {
            AlertDialog.Builder(this).apply {
                setTitle(R.string.miui_permissions_title)
                setMessage(getString(R.string.miui_permissions_message, context.appName))
                setPositiveButton(R.string.miui_permissions_ok_button) { _, _ -> vendorUtils.openMiuiPermissionsDialogIfNecessary() }
                setNegativeButton(R.string.miui_permissions_dont_show_again_button) { _, _ -> Values.getInstance(context).showMiuiPermissionsDialog = false }
                show()
            }
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
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Permission granted")
                } else {
                    Timber.d("Permission denied")
                }
                updateLayout()
            }
        }
    }
}
