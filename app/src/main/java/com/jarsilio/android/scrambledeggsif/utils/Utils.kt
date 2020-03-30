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

package com.jarsilio.android.scrambledeggsif.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.mediautil.image.jpeg.LLJTran
import android.mediautil.image.jpeg.LLJTranException
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import com.jarsilio.android.scrambledeggsif.extensions.imagesCacheDir
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.StringBuilder
import java.util.UUID
import okio.buffer
import okio.source
import timber.log.Timber

const val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1000

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

    fun requestPermissionsIfNecessary(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            Timber.d("Requesting READ_EXTERNAL_STORAGE permission")
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
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

    fun prepareImageInCache(imageUri: Uri): File {
        val unscrambledCopy = if (settings.isRenameImages) {
            File(context.imagesCacheDir, getRandomImageFilename(imageUri))
        } else {
            File(context.imagesCacheDir, getRealFilenameFromURI(imageUri))
        }

        Timber.d("Copying image from uri (probably from an intent) ${imageUri.path} to 'unscrambled' dir: $unscrambledCopy")
        copy(context.contentResolver.openInputStream(imageUri)!!, FileOutputStream(unscrambledCopy))
        return unscrambledCopy
    }

    private fun getRandomImageFilename(imageType: ImageType = ImageType.JPG): String {
        return "${UUID.randomUUID()}.${imageType.name.toLowerCase()}"
    }

    private fun getRandomImageFilename(uri: Uri): String {
        return getRandomImageFilename(getImageType(uri))
    }

    private fun getMagicNumbers(inputStream: InputStream): String {
        inputStream.source().buffer().use { source ->
            return try {
                val magicBytes = source.readByteArray(8).toHexString()
                Timber.d("First bytes: $magicBytes")
                magicBytes
            } catch (e: IOException) {
                Timber.e(e, "An error occurred while trying to read the file. Supposing it is not an image")
                ""
            }
        }
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
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val imageType = getImageType(inputStream)
            inputStream?.close()
            imageType
        } catch (e: FileNotFoundException) {
            Timber.e(e, "Couldn't open input stream from content resolver for uri $uri")
            ImageType.UNKNOWN
        } catch (e: IOException) {
            Timber.e(e, "Couldn't close input stream from content resolver for uri $uri")
            ImageType.UNKNOWN
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

    fun isScrambleableImage(uri: Uri): Boolean {
        Timber.d("Checking if uri $uri corresponds to a scrambleable image (i.e. is a jpeg)...")
        return getImageType(uri) == ImageType.JPG || getImageType(uri) == ImageType.PNG
    }

    fun getRealFilenameFromURI(uri: Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.moveToFirst()
        val index = cursor?.getColumnIndex(MediaStore.Images.ImageColumns.DATA)

        var realPath: String? = null
        if (index != null && index != -1) {
            realPath = cursor.getString(index)
        }
        cursor?.close()

        return if (realPath != null) {
            File(realPath).name
        } else {
            Timber.e("Couldn't get real filename from uri (probably came from GET_CONTENT intent). Returning a random name.")
            getRandomImageFilename(uri)
        }
    }

    fun rotateImageAccordingToExifOrientation(imageFile: File) {
        val operation = when (ExifInterface(imageFile).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_270 -> { Timber.v("Exif rotation 270°"); LLJTran.ROT_270 }
            ExifInterface.ORIENTATION_ROTATE_180 -> { Timber.v("Exif rotation 180°"); LLJTran.ROT_180 }
            ExifInterface.ORIENTATION_ROTATE_90 -> { Timber.d("Exif rotation 90°"); LLJTran.ROT_90 }
            else -> 0
        }

        if (operation == 0) {
            Timber.d("The image ($imageFile) doesn't need to be rotated. Skipping...")
            return
        }

        Timber.d("Trying to rotate image with LLJTran")

        val output = File(context.imagesCacheDir, "${UUID.randomUUID()}.jpg")

        val rotated = try {
            val lljTran = LLJTran(imageFile)
            lljTran.read(LLJTran.READ_ALL, false) // This could throw an LLJTranException. I am not catching it for now... Let's see.
            lljTran.transform(operation, LLJTran.OPT_DEFAULTS or LLJTran.OPT_XFORM_ORIENTATION)
            BufferedOutputStream(FileOutputStream(output)).use {
                writer -> lljTran.save(writer, LLJTran.OPT_WRITE_ALL)
            }
            lljTran.freeMemory()
            true
        } catch (e: LLJTranException) {
            Timber.e(e, "Error occurred while trying to rotate image with LLJTrans (AndroidMediaUtil).")
            false
        }

        if (rotated) {
            imageFile.delete()
            output.renameTo(imageFile)
            Timber.d("Done rotating image")
        }
    }
}

fun byteArrayFromInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }

fun ByteArray.toHexString(): String {
    val stringBuilder = StringBuilder()

    for (byte in this) {
        stringBuilder.append(byte.toHexString())
    }

    return stringBuilder.toString()
}

fun Byte.toHexString(): String { return "%02X".format(this) }
