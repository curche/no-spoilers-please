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

class ExifScrambler(private val context: Context) {

    private val utils: Utils by lazy { Utils(context) }

    private fun scrambleImage(imageFile: File): Uri {
        val scrambledImageFile = utils.prepareScrambledFileInCacheDir(imageFile) // prepareScrambled renames the file if necessary
        removeExifData(scrambledImageFile)
        return FileProvider.getUriForFile(context, context.applicationId + ".fileprovider", scrambledImageFile)
    }

    @Throws(IOException::class)
    fun scrambleImage(image: Uri): Uri {
        val unscrambledImageFile = utils.createFileFromUri(image)
        return scrambleImage(unscrambledImageFile)
    }

    private fun removeExifData(image: File) {
        when (val imageType = utils.getImageType(image)) {
            Utils.ImageType.JPG -> JpegScrambler(context).scramble(image)
            Utils.ImageType.PNG -> PngScrambler(context).scramble(image)
            else -> {
                Timber.d("Can't remove EXIF data from $imageType.")
                throw ScrambleException("Only JPEG and PNG images are supported (image is $imageType).")
            }
        }
    }
}

fun byteArray(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

class JpegScrambler(private val context: Context) {

    private val marker = 0xFF.toByte()
    // Skip all APPn (0xEn) and COM (0xFE) segments (See: https://en.wikipedia.org/wiki/JPEG_Image#Syntax_and_structure)
    private val skippableSegments = byteArray(0xFE, 0xE0, 0xE1, 0xE2, 0xE3, 0xE4, 0xE5, 0xE6, 0xE7, 0xE8, 0xE9, 0xEA, 0xEB, 0xEC, 0xED, 0xEE, 0xEF)
    private val startOfStream = 0xDA.toByte()

    fun scramble(jpegImage: File) {
        val tempImage = File(context.imagesCacheDir, "${UUID.randomUUID()}.jpg")

        tempImage.sink().buffer().use { sink ->
            jpegImage.inputStream().source().buffer().use { source ->
                sink.write(source, 2)
                val sourceBuffer = source.buffer
                while (true) {
                    source.require(2)
                    if (sourceBuffer[0] != marker) {
                        throw ScrambleException("Invalid JPEG. Expected an FF marker (${sourceBuffer[0]} != $marker)")
                    }
                    val nextByte = sourceBuffer[1]
                    if (skippableSegments.contains(nextByte)) {
                        source.skip(2)
                        val size = source.readShort().toUShort()
                        if (size < 2u) {
                            throw ScrambleException("Invalid JPEG: segment ${"%02x".format(nextByte).toUpperCase()} has wrong size: $size (<2)")
                        }
                        Timber.d("Skipping JPEG segment ${"%02x".format(nextByte).toUpperCase()} (APPn or COM): $size bytes")
                        source.skip((size - 2u).toLong()) // The size counts the 2 bytes of the size itself, and we've already read these
                    } else if (nextByte == startOfStream) {
                        sink.writeAll(source)
                        break
                    } else {
                        sink.write(source, 2)
                        val size = source.readShort().toUShort()
                        if (size < 2u) {
                            throw ScrambleException("Invalid JPEG: segment ${"%02x".format(nextByte).toUpperCase()} has wrong size: $size (<2)")
                        }
                        sink.writeShort(size.toInt())
                        sink.write(source, (size - 2u).toLong()) // The size counts the 2 bytes of the size itself, and we've already read these
                    }
                }
            }
        }
        jpegImage.delete()
        tempImage.renameTo(jpegImage)
    }
}

class PngScrambler(private val context: Context) {

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
                    throw ScrambleException("Invalid PNG file ($pngImage). It doesn't start with a PNG SIGNATURE.")
                }
            }
        }
        pngImage.delete()
        tempImage.renameTo(pngImage)
    }
}

class ScrambleException(message: String) : IOException(message)
