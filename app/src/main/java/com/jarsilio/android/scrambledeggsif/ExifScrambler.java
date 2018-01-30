/*
 * Copyright (c) 2018 Juan García Basilio
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

package com.jarsilio.android.scrambledeggsif;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.media.ExifInterface;
import android.support.v4.content.FileProvider;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

class ExifScrambler {
    private static final String TAG = "ExifScrambler";
    private final Context context;
    private static ExifScrambler instance;
    private static Set<String> exifAttributes;

    private ExifScrambler(Context context) {
        this.context = context;
    }

    public static ExifScrambler getInstance(Context context) {
        if (instance == null) {
            instance = new ExifScrambler(context);
        }
        return instance;
    }

    public Uri scrambleImage(Uri imageUri) {
        File scrambledImageFile = Utils.copyToCacheDir(context, imageUri);
        removeExifData(scrambledImageFile);
        return FileProvider.getUriForFile(context, "com.jarsilio.android.scrambledeggsif.fileprovider", scrambledImageFile);
    }

    private void removeExifData(File image) {
        /* First try to delete Exif data because it is the fastest way to do it.
        *  If this fails, open the image and save it again with the resave image
        */
        try {
            ExifInterface exifInterface = new ExifInterface(image.toString());
            for (String attribute : getExifAttributes()) {
                if (exifInterface.getAttribute(attribute) != null) {
                    Log.d(TAG, "Exif attribute " + attribute + " exists. Setting to null...");
                    exifInterface.setAttribute(attribute, null);
                } else {
                    Log.d(TAG, "Exif attribute " + attribute + " doesn't exist. Skipping...");
                }
            }
            exifInterface.saveAttributes();

        } catch (IOException e) {
            Log.e(TAG, "Error while trying to read or write image Exif properties");
            e.printStackTrace();
            // Rewrite whole file
            Log.d(TAG, "Trying to resave whole image to get rid of the Exif properties");
            resaveImage(image);
        }
    }

    private void resaveImage(File image) {
        String allegedMimeType = Utils.getAllegedMimeType(image);
        Bitmap originalImage = BitmapFactory.decodeFile(image.getPath());
        try {
            FileOutputStream outputStream = new FileOutputStream(image);
            if (allegedMimeType.equals("image/png")) {
                originalImage.compress(Bitmap.CompressFormat.PNG, 95, outputStream);
            } else {
                // If I don't know what type of image it is (or it is a JPEG), save as JPEG
                originalImage.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Couldn't find file to write to:" + image);
            e.printStackTrace();
        }
    }

    private static Set<String> getExifAttributes() {
        if (exifAttributes == null) {
            exifAttributes = new HashSet<>();

            // From my current Android SDK
            exifAttributes.add("FNumber");
            exifAttributes.add("ApertureValue");
            exifAttributes.add("Artist");
            exifAttributes.add("BitsPerSample");
            exifAttributes.add("BrightnessValue");
            exifAttributes.add("CFAPattern");
            exifAttributes.add("ColorSpace");
            exifAttributes.add("ComponentsConfiguration");
            exifAttributes.add("CompressedBitsPerPixel");
            exifAttributes.add("Compression");
            exifAttributes.add("Contrast");
            exifAttributes.add("Copyright");
            exifAttributes.add("CustomRendered");
            exifAttributes.add("DateTime");
            exifAttributes.add("DateTimeDigitized");
            exifAttributes.add("DateTimeOriginal");
            exifAttributes.add("DefaultCropSize");
            exifAttributes.add("DeviceSettingDescription");
            exifAttributes.add("DigitalZoomRatio");
            exifAttributes.add("DNGVersion");
            exifAttributes.add("ExifVersion");
            exifAttributes.add("ExposureBiasValue");
            exifAttributes.add("ExposureIndex");
            exifAttributes.add("ExposureMode");
            exifAttributes.add("ExposureProgram");
            exifAttributes.add("ExposureTime");
            exifAttributes.add("FileSource");
            exifAttributes.add("Flash");
            exifAttributes.add("FlashpixVersion");
            exifAttributes.add("FlashEnergy");
            exifAttributes.add("FocalLength");
            exifAttributes.add("FocalLengthIn35mmFilm");
            exifAttributes.add("FocalPlaneResolutionUnit");
            exifAttributes.add("FocalPlaneXResolution");
            exifAttributes.add("FocalPlaneYResolution");
            exifAttributes.add("FNumber");
            exifAttributes.add("GainControl");
            exifAttributes.add("GPSAltitude");
            exifAttributes.add("GPSAltitudeRef");
            exifAttributes.add("GPSAreaInformation");
            exifAttributes.add("GPSDateStamp");
            exifAttributes.add("GPSDestBearing");
            exifAttributes.add("GPSDestBearingRef");
            exifAttributes.add("GPSDestDistance");
            exifAttributes.add("GPSDestDistanceRef");
            exifAttributes.add("GPSDestLatitude");
            exifAttributes.add("GPSDestLatitudeRef");
            exifAttributes.add("GPSDestLongitude");
            exifAttributes.add("GPSDestLongitudeRef");
            exifAttributes.add("GPSDifferential");
            exifAttributes.add("GPSDOP");
            exifAttributes.add("GPSImgDirection");
            exifAttributes.add("GPSImgDirectionRef");
            exifAttributes.add("GPSLatitude");
            exifAttributes.add("GPSLatitudeRef");
            exifAttributes.add("GPSLongitude");
            exifAttributes.add("GPSLongitudeRef");
            exifAttributes.add("GPSMapDatum");
            exifAttributes.add("GPSMeasureMode");
            exifAttributes.add("GPSProcessingMethod");
            exifAttributes.add("GPSSatellites");
            exifAttributes.add("GPSSpeed");
            exifAttributes.add("GPSSpeedRef");
            exifAttributes.add("GPSStatus");
            exifAttributes.add("GPSTimeStamp");
            exifAttributes.add("GPSTrack");
            exifAttributes.add("GPSTrackRef");
            exifAttributes.add("GPSVersionID");
            exifAttributes.add("ImageDescription");
            exifAttributes.add("ImageLength");
            exifAttributes.add("ImageUniqueID");
            exifAttributes.add("ImageWidth");
            exifAttributes.add("InteroperabilityIndex");
            exifAttributes.add("ISOSpeedRatings");
            exifAttributes.add("ISOSpeedRatings");
            exifAttributes.add("JPEGInterchangeFormat");
            exifAttributes.add("JPEGInterchangeFormatLength");
            exifAttributes.add("LightSource");
            exifAttributes.add("Make");
            exifAttributes.add("MakerNote");
            exifAttributes.add("MaxApertureValue");
            exifAttributes.add("MeteringMode");
            exifAttributes.add("Model");
            exifAttributes.add("NewSubfileType");
            exifAttributes.add("OECF");
            exifAttributes.add("AspectFrame");
            exifAttributes.add("PreviewImageLength");
            exifAttributes.add("PreviewImageStart");
            exifAttributes.add("ThumbnailImage");
            exifAttributes.add("Orientation");
            exifAttributes.add("PhotometricInterpretation");
            exifAttributes.add("PixelXDimension");
            exifAttributes.add("PixelYDimension");
            exifAttributes.add("PlanarConfiguration");
            exifAttributes.add("PrimaryChromaticities");
            exifAttributes.add("ReferenceBlackWhite");
            exifAttributes.add("RelatedSoundFile");
            exifAttributes.add("ResolutionUnit");
            exifAttributes.add("RowsPerStrip");
            exifAttributes.add("ISO");
            exifAttributes.add("JpgFromRaw");
            exifAttributes.add("SensorBottomBorder");
            exifAttributes.add("SensorLeftBorder");
            exifAttributes.add("SensorRightBorder");
            exifAttributes.add("SensorTopBorder");
            exifAttributes.add("SamplesPerPixel");
            exifAttributes.add("Saturation");
            exifAttributes.add("SceneCaptureType");
            exifAttributes.add("SceneType");
            exifAttributes.add("SensingMethod");
            exifAttributes.add("Sharpness");
            exifAttributes.add("ShutterSpeedValue");
            exifAttributes.add("Software");
            exifAttributes.add("SpatialFrequencyResponse");
            exifAttributes.add("SpectralSensitivity");
            exifAttributes.add("StripByteCounts");
            exifAttributes.add("StripOffsets");
            exifAttributes.add("SubfileType");
            exifAttributes.add("SubjectArea");
            exifAttributes.add("SubjectDistance");
            exifAttributes.add("SubjectDistanceRange");
            exifAttributes.add("SubjectLocation");
            exifAttributes.add("SubSecTime");
            exifAttributes.add("SubSecTimeDigitized");
            exifAttributes.add("SubSecTimeDigitized");
            exifAttributes.add("SubSecTimeOriginal");
            exifAttributes.add("SubSecTimeOriginal");
            exifAttributes.add("ThumbnailImageLength");
            exifAttributes.add("ThumbnailImageWidth");
            exifAttributes.add("TransferFunction");
            exifAttributes.add("UserComment");
            exifAttributes.add("WhiteBalance");
            exifAttributes.add("WhitePoint");
            exifAttributes.add("XResolution");
            exifAttributes.add("YCbCrCoefficients");
            exifAttributes.add("YCbCrPositioning");
            exifAttributes.add("YCbCrSubSampling");
            exifAttributes.add("YResolution");

            // Get all fields that the concrete Android-Java implementation have and delete them
            Field[] fields = ExifInterface.class.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isPublic(field.getModifiers()) &&
                        Modifier.isStatic(field.getModifiers()) &&
                        Modifier.isFinal(field.getModifiers())) {

                    if (field.getType() == String.class) {
                        String attribute;
                        try {
                            attribute = (String) field.get(String.class);
                            exifAttributes.add(attribute);
                        } catch (IllegalAccessException e) {
                            Log.e(TAG, "Error trying to read ExifAttributes fields");
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return exifAttributes;
    }
}
