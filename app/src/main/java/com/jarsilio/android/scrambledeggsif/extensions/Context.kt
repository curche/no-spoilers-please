package com.jarsilio.android.scrambledeggsif.extensions

import android.content.Context
import java.io.File


val Context.imagesCacheDir : File
    get() {
        File(cacheDir, "images").mkdirs()
        return File(cacheDir, "images")
    }
