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
import androidx.core.content.FileProvider
import com.jarsilio.android.common.extensions.applicationId
import com.jarsilio.android.scrambledeggsif.extensions.imagesCacheDir
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber

private const val MARKER = 0xFF.toByte()
private const val APP1 = 0xE1.toByte()
private const val COMMENT = 0xFE.toByte()
private const val START_OF_STREAM = 0xDA.toByte()

class ExifScrambler(private val context: Context) {

    private val utils: Utils by lazy { Utils(context) }

    private fun scrambleImage(imageFile: File): Uri {
        val scrambledImageFile = utils.prepareScrambledFileInCacheDir(imageFile) // prepareScrambled renames the file if necessary
        removeExifData(scrambledImageFile)
        return FileProvider.getUriForFile(context, context.applicationId + ".fileprovider", scrambledImageFile)
    }

    fun scrambleImage(image: Uri): Uri {
        val unscrambledImageFile = utils.createFileFromUri(image)
        return scrambleImage(unscrambledImageFile)
    }

    private fun removeExifData(image: File) {
        when (val imageType = utils.getImageType(image)) {
            Utils.ImageType.JPG -> removeExifDataManually(image)
            Utils.ImageType.PNG -> PngScrambler(context).scramble(image)
            else -> Timber.d("Can't remove EXIF data from $imageType.")
        }
    }

    private fun removeExifDataManually(jpegImage: File) {
        val output = File(context.imagesCacheDir, "${UUID.randomUUID()}.jpg")

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
    }
}

class PngScrambler(private val context: Context) {
    private fun byteArray(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

    private val pngSignature = byteArray(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val pngCriticalChunks = listOf("IHDR", "PLTE", "IDAT", "IEND")

    fun scramble(pngImage: File) {
        val tempImage = File(context.imagesCacheDir, "${UUID.randomUUID()}.png")

        tempImage.sink().buffer().use { sink ->
            pngImage.inputStream().source().buffer().use { source ->
                val byteArray = source.readByteArray(8)
                if (byteArray contentEquals pngSignature) {
                    sink.write(byteArray)

                    while (!source.exhausted()) {
                        val chunkLength = source.readInt()
                        val chunkName = source.readString(4, Charset.forName("ASCII"))
                        val chunkData = source.readByteArray(chunkLength.toLong())
                        val chunkCrc = source.readByteArray(4)

                        if (pngCriticalChunks.contains(chunkName)) {
                            // Only write chunk to scrambled png file if it's one of the four critical chunks (see https://en.wikipedia.org/wiki/Portable_Network_Graphics#Critical_chunks)
                            sink.writeInt(chunkLength)
                            sink.writeString(chunkName, Charset.forName("ASCII"))
                            sink.write(chunkData)
                            sink.write(chunkCrc)
                        }
                    }
                } else {
                    throw IOException("Error scrambling PNG file $pngImage")
                }
            }
        }
        pngImage.delete()
        tempImage.renameTo(pngImage)
    }
}
