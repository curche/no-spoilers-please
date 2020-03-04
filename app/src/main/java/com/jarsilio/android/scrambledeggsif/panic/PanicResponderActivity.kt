package com.jarsilio.android.scrambledeggsif.panic

import android.app.Activity
import android.os.Bundle
import com.jarsilio.android.scrambledeggsif.utils.Settings

const val PANIC_TRIGGER_ACTION = "info.guardianproject.panic.action.TRIGGER"

class PanicResponderActivity : Activity() {
    private val settings: Settings by lazy { Settings(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action != PANIC_TRIGGER_ACTION) {
            finish()
            return
        }

        if (settings.isPanicDeleteCachedImages) {
            cacheDir.deleteRecursively()
        }

        if (settings.isPanicClearAppData) {
            try {
                // Clear app data (and force-close app)
                Runtime.getRuntime().exec("pm clear $packageName")
                } catch (e: Exception) {
                // Don't show anything if it fails (in case somebody's watching)
            }
        }

        finish()
    }
}
