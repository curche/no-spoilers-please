/*
 * Copyright (c) 2018-2019 Juan García Basilio
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

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

import androidx.core.content.ContextCompat
import com.jarsilio.android.scrambledeggsif.extensions.imagesCacheDir
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

import timber.log.Timber
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Random
import kotlin.math.abs

internal class Utils(private val context: Context) {

    val settings: Settings by lazy { Settings(context) }

    val isPermissionGranted: Boolean
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                permissionCheck == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

    enum class ImageType {
        JPG, PNG, BMP, GIF, TIFF, UNKNOWN
    }

    fun copy(inputStream: InputStream, outputStream: OutputStream) {
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    }

    fun prepareScrambledFileInCacheDir(imageFile: File): File {
        val unscrambledCopy = if (settings.isRenameImages) {
            getRandomScrambledImage(getImageType(imageFile))
        } else {
            File(context.imagesCacheDir, imageFile.name)
        }

        if (imageFile.canonicalPath == unscrambledCopy.canonicalPath) {
            Timber.d("Not copying to 'images' dir. It's already there.")
        } else {
            Timber.d("Copying image $imageFile to 'images' dir: $unscrambledCopy")
            copy(FileInputStream(imageFile), FileOutputStream(unscrambledCopy))
        }

        return unscrambledCopy
    }

    fun createFileFromBytesArray(imageBytes: ByteArray): File {
        @SuppressLint("SimpleDateFormat")
        val now = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())

        val unscrambledCopy = File(context.imagesCacheDir, "IMG_$now.jpg")
        Timber.d("Writing bytes to 'images' dir: $unscrambledCopy")

        unscrambledCopy.writeBytes(imageBytes)

        return unscrambledCopy
    }

    fun createFileFromUri(imageUri: Uri): File {
        val unscrambledCopy = File(context.imagesCacheDir, getRealFilenameFromURI(imageUri))
        Timber.d("Copying image from uri (probably from an intent) ${imageUri.path} to 'unscrambled' dir: $unscrambledCopy")

        copy(context.contentResolver.openInputStream(imageUri)!!, FileOutputStream(unscrambledCopy))

        return unscrambledCopy
    }

    private fun getRandomScrambledImage(imageType: ImageType = ImageType.JPG): File {
        val scrambledImageFilename = "img_eggsif_${abs(Random().nextLong())}.${imageType.name.toLowerCase()}"
        return File(context.imagesCacheDir, scrambledImageFilename)
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val stringBuilder = StringBuilder()

        for (byte in bytes) {
            stringBuilder.append(String.format("%02X", byte))
        }

        return stringBuilder.toString()
    }

    private fun getMagicNumbers(inputStream: InputStream): String {
        val bytesToRead = 8
        val magicBytes = ByteArray(bytesToRead)
        val bytesRead: Int

        try {
            bytesRead = inputStream.read(magicBytes, 0, bytesToRead)
            inputStream.close()
        } catch (e: IOException) {
            Timber.e(e, "An error ocurred while trying to read the file. Supposing it is not an image")
            e.printStackTrace()
            return ""
        }

        if (bytesRead != bytesToRead) {
            Timber.e("Failed to read the first %s bytes for file", bytesToRead)
            return ""
        }

        val magicBytesAsHexString = bytesToHex(magicBytes)
        Timber.d("First $bytesRead bytes: $magicBytesAsHexString")

        return magicBytesAsHexString
    }

    private fun getImageType(inputStream: InputStream?): ImageType {
        val magicNumbers = getMagicNumbers(inputStream!!)

        val imageType: ImageType

        if (magicNumbers.startsWith("FFD8")) {
            imageType = ImageType.JPG
            Timber.d("It's a JPEG image")
        } else if (magicNumbers.startsWith("89504E470D0A1A0A")) {
            imageType = ImageType.PNG
            Timber.d("It's a PNG image")
        } else if (magicNumbers.startsWith("424D")) {
            imageType = ImageType.BMP
            Timber.d("It's a BMP image")
        } else if (magicNumbers.startsWith("474946383961") || magicNumbers.startsWith("474946383761")) {
            imageType = ImageType.GIF
            Timber.d("It's a GIF image")
        } else if (magicNumbers.startsWith("49492A00") || magicNumbers.startsWith("4D4D002A")) {
            imageType = ImageType.TIFF
            Timber.d("It's a TIFF image")
        } else {
            imageType = ImageType.UNKNOWN
            Timber.d("It's (probably) not an image. Failed to recognize type.")
        }

        return imageType
    }

    private fun getImageType(uri: Uri): ImageType {
        Timber.d("Getting ImageType from uri $uri...")
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val imageType = getImageType(inputStream)
            inputStream?.close()
            return imageType
        } catch (e: FileNotFoundException) {
            Timber.e(e, "Couldn't open input stream from content resolver for uri $uri")
            return ImageType.UNKNOWN
        } catch (e: IOException) {
            Timber.e(e, "Couldn't close input stream from content resolver for uri $uri")
            return ImageType.UNKNOWN
        }
    }

    fun getImageType(file: File): ImageType {
        Timber.d("Getting ImageType from file $file...")
        return try {
            val inputStream = FileInputStream(file)
            getImageType(inputStream)
        } catch (e: FileNotFoundException) {
            Timber.e(e, "Couldn't open input stream from content resolver for file $file")
            ImageType.UNKNOWN
        }
    }

    fun isImage(uri: Uri): Boolean {
        Timber.d("Checking if uri $uri corresponds to an image...")
        return getImageType(uri) != ImageType.UNKNOWN
    }

    private fun getRealFilenameFromURI(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.moveToFirst()
        val index = cursor?.getColumnIndex(MediaStore.Images.ImageColumns.DATA)

        var realPath: String? = null
        if (index != null && index != -1) {
           realPath = cursor.getString(index)
        }

        return if (realPath != null) {
            File(realPath).name
        } else {
            Timber.e("Couldn't get real filename from uri (probably came from GET_CONTENT intent). Returning a random name.")
            getRandomScrambledImage().name
        }
    }
}
