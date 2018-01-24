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
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Random;

public class HandleImageActivity extends AppCompatActivity {
    private static final String TAG = "HandleImageActivity";
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_handle_image);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if (type.startsWith("image/")) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "READ_EXTERNAL_STORAGE permission not granted. Showing toast to tell the user to open the app");
                        Toast.makeText(this, getString(R.string.permissions_open_app_toast), Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Log.d(TAG, "READ_EXTERNAL_STORAGE permission already granted. Handling sent image...    ");
                        handleSendImage(intent);
                    }
                } else {
                    Log.d(TAG, "READ_EXTERNAL_STORAGE not needed due to old Android version. Handling sent image...    ");
                    handleSendImage(intent);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }

    public static void copy(File src, File dst) throws IOException {
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
        Log.d(TAG, "Image path: " + path);
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
            File scrambledEggsifImage = copyToCacheDir(imageUri);
            removeExifData(scrambledEggsifImage);
            shareImage(scrambledEggsifImage);
            finish();
        }
    }

    private void shareImage(File image) {
        Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "com.jarsilio.android.scrambledeggsif.fileprovider", image);

        if (contentUri != null) {
            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
            shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_via)));
        }
    }

    private void removeExifData(File image) {
        try {
            ExifInterface exifInterface = new ExifInterface(image.toString());
            Field[] fields = ExifInterface.class.getDeclaredFields();
            // Get all fields that the concrete Android-Java implementation have and delete them
            for (Field field : fields) {
                if (Modifier.isPublic(field.getModifiers()) &&
                        Modifier.isStatic(field.getModifiers()) &&
                        Modifier.isFinal(field.getModifiers())) {

                    if (field.getType() == String.class) {
                        String attribute = (String) field.get(String.class);
                        Log.d(TAG, String.format("%s (%s) old value: %s", field.getName(), attribute, exifInterface.getAttribute(attribute)));
                        exifInterface.setAttribute(attribute, null);
                        Log.d(TAG, String.format("%s (%s) new value: %s", field.getName(), attribute, exifInterface.getAttribute(attribute)));
                    }
                }
            }
            exifInterface.saveAttributes();
        } catch (IOException e) {
            Log.e(TAG, "Error reading Exif data from image");
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Error setting Exif data for new temp image");
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
}
