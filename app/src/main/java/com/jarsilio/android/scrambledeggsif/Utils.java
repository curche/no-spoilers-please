package com.jarsilio.android.scrambledeggsif;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

class Utils {

    public static boolean isPermissionGranted(Context context) {
        boolean granted = true;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
            granted = permissionCheck == PackageManager.PERMISSION_GRANTED;
        }

        return granted;
    }
}
