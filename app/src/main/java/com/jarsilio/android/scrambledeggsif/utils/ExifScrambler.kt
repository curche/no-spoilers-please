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

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.jarsilio.android.scrambledeggsif.BuildConfig
import java.io.File
import java.io.IOException
import java.lang.reflect.Modifier
import timber.log.Timber

class ExifScrambler(private val context: Context) {

    private val settings: Settings by lazy { Settings(context) }
    private val utils: Utils by lazy { Utils(context) }

    fun scrambleImage(imageFile: File): Uri {
        val scrambledImageFile = utils.prepareScrambledFileInCacheDir(imageFile) // prepareScrambled renames the file if necessary
        removeExifData(scrambledImageFile)
        return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileprovider", scrambledImageFile)
    }

    fun scrambleImage(image: Uri): Uri {
        val unscrambledImageFile = utils.createFileFromUri(image)
        return scrambleImage(unscrambledImageFile)
    }

    private fun removeExifData(image: File) {
        removeExifDataWithExifInterface(image)
    }

    private fun removeExifDataWithExifInterface(image: File) {
        try {
            val exifInterface = ExifInterface(image.toString())
            for (attribute in exifAttributes) {
                val value = exifInterface.getAttribute(attribute)
                if (settings.isKeepJpegOrientation && attribute == ExifInterface.TAG_ORIENTATION) {
                    Timber.d("Keep orientation is on: skipping ExifInterface.TAG_ORIENTATION attribute. Orientation is set to $value")
                    continue
                }
                if (value != null) {
                    Timber.v("Exif attribute $attribute is set. Removing (setting to null)")
                    exifInterface.setAttribute(attribute, null)
                }
            }
            exifInterface.saveAttributes()
        } catch (e: IOException) {
            Timber.e(e, "Failed to remove exif data with ExifInterface.")
        }
    }

    companion object {
        private val exifAttributes = HashSet<String>()

        init {
            // From my current Android SDK
            exifAttributes.add("FNumber")
            exifAttributes.add("ApertureValue")
            exifAttributes.add("Artist")
            exifAttributes.add("BitsPerSample")
            exifAttributes.add("BrightnessValue")
            exifAttributes.add("CFAPattern")
            exifAttributes.add("ColorSpace")
            exifAttributes.add("ComponentsConfiguration")
            exifAttributes.add("CompressedBitsPerPixel")
            exifAttributes.add("Compression")
            exifAttributes.add("Contrast")
            exifAttributes.add("Copyright")
            exifAttributes.add("CustomRendered")
            exifAttributes.add("DateTime")
            exifAttributes.add("DateTimeDigitized")
            exifAttributes.add("DateTimeOriginal")
            exifAttributes.add("DefaultCropSize")
            exifAttributes.add("DeviceSettingDescription")
            exifAttributes.add("DigitalZoomRatio")
            exifAttributes.add("DNGVersion")
            exifAttributes.add("ExifVersion")
            exifAttributes.add("ExposureBiasValue")
            exifAttributes.add("ExposureIndex")
            exifAttributes.add("ExposureMode")
            exifAttributes.add("ExposureProgram")
            exifAttributes.add("ExposureTime")
            exifAttributes.add("FileSource")
            exifAttributes.add("Flash")
            exifAttributes.add("FlashpixVersion")
            exifAttributes.add("FlashEnergy")
            exifAttributes.add("FocalLength")
            exifAttributes.add("FocalLengthIn35mmFilm")
            exifAttributes.add("FocalPlaneResolutionUnit")
            exifAttributes.add("FocalPlaneXResolution")
            exifAttributes.add("FocalPlaneYResolution")
            exifAttributes.add("FNumber")
            exifAttributes.add("GainControl")
            exifAttributes.add("GPSAltitude")
            exifAttributes.add("GPSAltitudeRef")
            exifAttributes.add("GPSAreaInformation")
            exifAttributes.add("GPSDateStamp")
            exifAttributes.add("GPSDestBearing")
            exifAttributes.add("GPSDestBearingRef")
            exifAttributes.add("GPSDestDistance")
            exifAttributes.add("GPSDestDistanceRef")
            exifAttributes.add("GPSDestLatitude")
            exifAttributes.add("GPSDestLatitudeRef")
            exifAttributes.add("GPSDestLongitude")
            exifAttributes.add("GPSDestLongitudeRef")
            exifAttributes.add("GPSDifferential")
            exifAttributes.add("GPSDOP")
            exifAttributes.add("GPSImgDirection")
            exifAttributes.add("GPSImgDirectionRef")
            exifAttributes.add("GPSLatitude")
            exifAttributes.add("GPSLatitudeRef")
            exifAttributes.add("GPSLongitude")
            exifAttributes.add("GPSLongitudeRef")
            exifAttributes.add("GPSMapDatum")
            exifAttributes.add("GPSMeasureMode")
            exifAttributes.add("GPSProcessingMethod")
            exifAttributes.add("GPSSatellites")
            exifAttributes.add("GPSSpeed")
            exifAttributes.add("GPSSpeedRef")
            exifAttributes.add("GPSStatus")
            exifAttributes.add("GPSTimeStamp")
            exifAttributes.add("GPSTrack")
            exifAttributes.add("GPSTrackRef")
            exifAttributes.add("GPSVersionID")
            exifAttributes.add("ImageDescription")
            exifAttributes.add("ImageLength")
            exifAttributes.add("ImageUniqueID")
            exifAttributes.add("ImageWidth")
            exifAttributes.add("InteroperabilityIndex")
            exifAttributes.add("ISOSpeedRatings")
            exifAttributes.add("ISOSpeedRatings")
            exifAttributes.add("JPEGInterchangeFormat")
            exifAttributes.add("JPEGInterchangeFormatLength")
            exifAttributes.add("LightSource")
            exifAttributes.add("Make")
            exifAttributes.add("MakerNote")
            exifAttributes.add("MaxApertureValue")
            exifAttributes.add("MeteringMode")
            exifAttributes.add("Model")
            exifAttributes.add("NewSubfileType")
            exifAttributes.add("OECF")
            exifAttributes.add("AspectFrame")
            exifAttributes.add("PreviewImageLength")
            exifAttributes.add("PreviewImageStart")
            exifAttributes.add("ThumbnailImage")
            exifAttributes.add("Orientation")
            exifAttributes.add("PhotometricInterpretation")
            exifAttributes.add("PixelXDimension")
            exifAttributes.add("PixelYDimension")
            exifAttributes.add("PlanarConfiguration")
            exifAttributes.add("PrimaryChromaticities")
            exifAttributes.add("ReferenceBlackWhite")
            exifAttributes.add("RelatedSoundFile")
            exifAttributes.add("ResolutionUnit")
            exifAttributes.add("RowsPerStrip")
            exifAttributes.add("ISO")
            exifAttributes.add("JpgFromRaw")
            exifAttributes.add("SensorBottomBorder")
            exifAttributes.add("SensorLeftBorder")
            exifAttributes.add("SensorRightBorder")
            exifAttributes.add("SensorTopBorder")
            exifAttributes.add("SamplesPerPixel")
            exifAttributes.add("Saturation")
            exifAttributes.add("SceneCaptureType")
            exifAttributes.add("SceneType")
            exifAttributes.add("SensingMethod")
            exifAttributes.add("Sharpness")
            exifAttributes.add("ShutterSpeedValue")
            exifAttributes.add("Software")
            exifAttributes.add("SpatialFrequencyResponse")
            exifAttributes.add("SpectralSensitivity")
            exifAttributes.add("StripByteCounts")
            exifAttributes.add("StripOffsets")
            exifAttributes.add("SubfileType")
            exifAttributes.add("SubjectArea")
            exifAttributes.add("SubjectDistance")
            exifAttributes.add("SubjectDistanceRange")
            exifAttributes.add("SubjectLocation")
            exifAttributes.add("SubSecTime")
            exifAttributes.add("SubSecTimeDigitized")
            exifAttributes.add("SubSecTimeDigitized")
            exifAttributes.add("SubSecTimeOriginal")
            exifAttributes.add("SubSecTimeOriginal")
            exifAttributes.add("ThumbnailImageLength")
            exifAttributes.add("ThumbnailImageWidth")
            exifAttributes.add("TransferFunction")
            exifAttributes.add("UserComment")
            exifAttributes.add("WhiteBalance")
            exifAttributes.add("WhitePoint")
            exifAttributes.add("XResolution")
            exifAttributes.add("YCbCrCoefficients")
            exifAttributes.add("YCbCrPositioning")
            exifAttributes.add("YCbCrSubSampling")
            exifAttributes.add("YResolution")

            // Get all fields that the concrete Android-Java implementation have and delete them
            val fields = ExifInterface::class.java.declaredFields
            for (field in fields) {
                if (Modifier.isPublic(field.modifiers) &&
                        Modifier.isStatic(field.modifiers) &&
                        Modifier.isFinal(field.modifiers)) {

                    if (field.type == String::class.java) {
                        try {
                            val attribute = field.get(String::class.java) as String
                            exifAttributes.add(attribute)
                        } catch (e: IllegalAccessException) {
                            Timber.e(e, "Error trying to read ExifAttributes fields")
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}
