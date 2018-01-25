package android.jarsilio.com.scrambledeggsif;

import android.Manifest;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class HandleImageActivity extends AppCompatActivity {
    private static final String TAG = "HandleImageActivity";

    private static Set<String> exifAttributes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handle_image);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        Log.d(TAG, "Type (intent): " + type);
        if (arePermissionsGranted()) {
            if (action.equals(Intent.ACTION_SEND)) {
                handleSendImage(intent);
            } else if (action.equals(Intent.ACTION_SEND_MULTIPLE)) {
                handleSendMultipleImages(intent);
            }
        } else {
            Log.d(TAG, "READ_EXTERNAL_STORAGE has not been granted. Showing toast to tell the user to open the app");
            Toast.makeText(this, getString(R.string.permissions_open_app_toast), Toast.LENGTH_LONG).show();
        }
        finish();
    }

    private boolean arePermissionsGranted() {
        boolean granted = true;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            granted = permissionCheck == PackageManager.PERMISSION_GRANTED;
        }

        return granted;
    }

    private static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
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

    private File copyToCacheDir(Uri imageUri) {
        String path = getRealPathFromURI(imageUri);
        String extension = path.substring(path.lastIndexOf('.'));
        File originalImage = new File(path);
        new File(getApplicationContext().getCacheDir() + "/images").mkdir();
        File scrambledEggsifImage = new File(String.format("%s/images/IMG_EGGSIF_%s%s", getApplicationContext().getCacheDir(), Math.abs(new Random().nextLong()), extension));
        try {
            Log.d(TAG, String.format("Copying '%s' to cache dir '%s'", originalImage, scrambledEggsifImage));
            copy(originalImage, scrambledEggsifImage);
        } catch (IOException e) {
            Log.e(TAG, "Error copying file to cache dir");
            e.printStackTrace();
        }
        return scrambledEggsifImage;
    }

    private void handleSendImage(Intent intent) {
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (imageUri != null) {
            if (isImage(imageUri)) {
                Uri scrambledImage = scrambleImage(imageUri);
                shareImage(scrambledImage);
            }
        }
    }
    private void handleSendMultipleImages(Intent intent) {
        Log.d(TAG, "Scrambling multiple images");
        ArrayList<Uri> imageUriList = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

        ArrayList<Uri> scrambledImagesUriList = new ArrayList<>();
        for (Uri imageUri : imageUriList) {
            if (isImage(imageUri)) {
                Log.d(TAG, "Received image (uri): " + imageUri);
                Uri scrambledImage = scrambleImage(imageUri);
                scrambledImagesUriList.add(scrambledImage);
            } else {
                Log.d(TAG, String.format("Received something that's not an image (%s) in a SEND_MULTIPLE. Skipping...", getRealPathFromURI(imageUri)));
            }
        }

        shareMultipleImages(scrambledImagesUriList);
    }

    private Uri scrambleImage(Uri imageUri) {
        File scrambledImageFile = copyToCacheDir(imageUri);
        removeExifData(scrambledImageFile);
        Uri scrambledImageUri = FileProvider.getUriForFile(getApplicationContext(), "com.jarsilio.android.scrambledeggsif.fileprovider", scrambledImageFile);
        return scrambledImageUri;
    }

    private void shareImage(Uri imageUri) {
        if (imageUri != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setDataAndType(imageUri, getContentResolver().getType(imageUri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
        }
    }

    private void shareMultipleImages(ArrayList<Uri> scrambledImagesUriList) {
        if (scrambledImagesUriList.size() > 0) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, scrambledImagesUriList);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_multiple_via)));
        }
    }

    private void removeExifData(File image) {
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
            Log.e(TAG, "Error while trying to read Exif from file");
            e.printStackTrace();
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(getApplicationContext(), contentUri, projection, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(columnIndex);
        cursor.close();
        return result;
    }

    private boolean isImage(Uri uri) {
        // return BitmapFactory.decodeFile(path) != null; // This is safer but slower
        String allegedMimeType = getAllegedMimeType(getRealPathFromURI(uri));
        Log.d(TAG, "mimeType (alleged): " + allegedMimeType);
        return allegedMimeType.startsWith("image/");
    }

    public static String getAllegedMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    private static Set<String> getExifAttributes() {
        if (exifAttributes == null) {
            exifAttributes = new HashSet<String>();

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
