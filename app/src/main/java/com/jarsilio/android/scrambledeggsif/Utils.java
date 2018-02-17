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

import android.Manifest;
import android.content.Context;
import android.content.CursorLoader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import timber.log.Timber;

class Utils {
    public enum ImageType {JPG, PNG, BMP, GIF, TIFF, UNKNOWN};

    public static boolean isPermissionGranted(Context context) {
        boolean granted = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
            granted = permissionCheck == PackageManager.PERMISSION_GRANTED;
        }

        return granted;
    }

    private static void copy(InputStream in, File dst) throws IOException {
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    public static File copyToCacheDir(Context context, Uri imageUri) {
        Timber.d("Copying image '%s' to cache dir", imageUri.getPath());

        String extension = getImageType(context, imageUri).name().toLowerCase();
        new File(context.getCacheDir() + "/images").mkdir();
        File scrambledEggsifImage = new File(String.format("%s/images/IMG_EGGSIF_%s.%s", context.getCacheDir(), Math.abs(new Random().nextLong()), extension));
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Timber.d("Copying '%s' to cache dir '%s'", imageUri, scrambledEggsifImage);
            copy(inputStream, scrambledEggsifImage);
        } catch (IOException e) {
            Timber.e(e,"Error copying file to cache dir");
            e.printStackTrace();
        }
        return scrambledEggsifImage;
    }

    private static String getRealPathFromURI(Context context, Uri contentUri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(context, contentUri, projection, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(columnIndex);
        cursor.close();
        return result;
    }

    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();

        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String getMagicNumbers(InputStream inputStream) {
        final int BYTES_TO_READ = 8;
        byte[] magicBytes = new byte[BYTES_TO_READ];
        int bytesRead;

        try {
            bytesRead = inputStream.read(magicBytes, 0, BYTES_TO_READ);
            inputStream.close();
        } catch (IOException e) {
            Timber.e("An error ocurred while trying to read the file. Supposing it is not an image");
            e.printStackTrace();
            return "";
        }

        if (bytesRead != BYTES_TO_READ) {
            Timber.e("Failed to read the first %s bytes for file", BYTES_TO_READ);
            return "";
        }

        String magicBytesAsHexString = bytesToHex(magicBytes);
        Timber.d("First %s bytes: %s", BYTES_TO_READ, magicBytesAsHexString);

        return magicBytesAsHexString;
    }

    private static ImageType getImageType(Context context, InputStream inputStream) {
        String magicNumbers = getMagicNumbers(inputStream);

        ImageType imageType;

        if (magicNumbers.startsWith("FFD8")) {
            imageType = ImageType.JPG;
            Timber.d("It's a JPEG image");
        } else if (magicNumbers.startsWith("89504E470D0A1A0A")) {
            imageType = ImageType.PNG;
            Timber.d("It's a PNG image");
        } else if (magicNumbers.startsWith("424D")) {
            imageType = ImageType.BMP;
            Timber.d("It's a BMP image");
        } else if (magicNumbers.startsWith("474946383961") ||
                magicNumbers.startsWith("474946383761")) {
            imageType = ImageType.GIF;
            Timber.d("It's a GIF image");
        } else if (magicNumbers.startsWith("49492A00") ||
                magicNumbers.startsWith("4D4D002A")) {
            imageType = ImageType.TIFF;
            Timber.d("It's a TIFF image");
        } else {
            imageType = ImageType.UNKNOWN;
            Timber.d("It's (probably) not an image. Failed to recognize type.");
        }

        return imageType;
    }

    public static ImageType getImageType(Context context, Uri uri) {
        Timber.d("Getting ImageType from uri '%s'...", uri);
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            return getImageType(context, inputStream);
        } catch (FileNotFoundException e) {
            Timber.e(e, "Couldn't open input stream from content resolver for uri '%s'", uri);
            e.printStackTrace();
            return ImageType.UNKNOWN;
        }
    }

    public static ImageType getImageType(Context context, File file) {
        Timber.d("Getting ImageType from file '%s'...", file);
        try {
            InputStream inputStream = new FileInputStream(file);
            return getImageType(context, inputStream);
        } catch (FileNotFoundException e) {
            Timber.e(e, "Couldn't open input stream from content resolver for file '%s'", file);
            e.printStackTrace();
            return ImageType.UNKNOWN;
        }
    }

    public static boolean isImage(Context context, Uri uri) {
        Timber.d("Checking if uri '%s' corresponds to an image...", uri);
        return getImageType(context, uri) != ImageType.UNKNOWN;
    }
}
