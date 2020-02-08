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

import android.app.Application
import android.content.Context

import com.jarsilio.android.common.logging.LongTagTree

import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraMailSender
import org.acra.annotation.AcraNotification

import timber.log.Timber

@AcraCore(buildConfigClass = BuildConfig::class)
@AcraMailSender(mailTo = "juam+scrambled@posteo.net")
@AcraNotification(resTitle = R.string.acra_notification_title, resText = R.string.acra_notification_text, resChannelName = R.string.acra_notification_channel_name, resSendButtonText = R.string.acra_notification_send, resDiscardButtonText = android.R.string.cancel)
class App : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // The following line triggers the initialization of ACRA
        if (!BuildConfig.DEBUG) {
            ACRA.init(this)
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(LongTagTree(this))
        }
    }
}
