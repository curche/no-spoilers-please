package com.jarsilio.android.scrambledeggsif.panic

import android.app.Activity
import android.os.Bundle

const val PANIC_TRIGGER_ACTION = "info.guardianproject.panic.action.TRIGGER"

class PanicResponderActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action == PANIC_TRIGGER_ACTION) {
            try {
                // Clear app data (and force-close app)
                Runtime.getRuntime().exec("pm clear $packageName")
                } catch (e: Exception) {
                // Don't show anything if it fails (in case somebody's watching)
            }
        }
    }
}
