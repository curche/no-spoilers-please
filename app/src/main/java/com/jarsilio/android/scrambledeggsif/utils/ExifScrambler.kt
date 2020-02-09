/*
 * Copyright (c) 2018-2020 Juan García Basilio
 * Copyright (c) 2019 Eric Cochran (see https://github.com/NightlyNexus/ExifDataRemover/blob/master/app/src/main/java/com/nightlynexus/exifdataremover/Activity.kt#L107)
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

package com.jarsilio.android.scrambledeggsif.utils

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.jarsilio.android.scrambledeggsif.BuildConfig
import com.jarsilio.android.scrambledeggsif.extensions.imagesCacheDir
import java.io.File
import java.io.IOException
import java.util.UUID
import okio.buffer
import okio.sink
import okio.source

private const val MARKER = 0xFF.toByte()
private const val APP1 = 0xE1.toByte()
private const val COMMENT = 0xFE.toByte()
private const val START_OF_STREAM = 0xDA.toByte()

class ExifScrambler(private val context: Context) {

    private val utils: Utils by lazy { Utils(context) }

    private fun scrambleImage(imageFile: File): Uri {
        val scrambledImageFile = utils.prepareScrambledFileInCacheDir(imageFile) // prepareScrambled renames the file if necessary
        removeExifData(scrambledImageFile)
        return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", scrambledImageFile)
    }

    fun scrambleImage(image: Uri): Uri {
        val unscrambledImageFile = utils.createFileFromUri(image)
        return scrambleImage(unscrambledImageFile)
    }

    private fun removeExifData(image: File) {
        removeExifDataManually(image)
    }

    private fun removeExifDataManually(jpegImage: File) {
        val output = File(context.imagesCacheDir, "${UUID.randomUUID()}.jpg")

        try {
            output.sink().buffer().use { sink ->
                jpegImage.inputStream().source().buffer().use { source ->
                    sink.write(source, 2)
                    val sourceBuffer = source.buffer
                    while (true) {
                        source.require(2)
                        if (sourceBuffer[0] != MARKER) {
                            throw IOException("${sourceBuffer[0]} != $MARKER")
                        }
                        val nextByte = sourceBuffer[1]
                        if (nextByte == APP1 || nextByte == COMMENT) {
                            source.skip(2)
                            val size = source.readShort()
                            source.skip((size - 2).toLong())
                        } else if (nextByte == START_OF_STREAM) {
                            sink.writeAll(source)
                            break
                        } else {
                            sink.write(source, 2)
                            val size = source.readShort()
                            sink.writeShort(size.toInt())
                            sink.write(source, (size - 2).toLong())
                        }
                    }
                }
            }
            jpegImage.delete()
            output.renameTo(jpegImage)
        } catch (e: IOException) {
            Toast.makeText(context, "Failed to create ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
