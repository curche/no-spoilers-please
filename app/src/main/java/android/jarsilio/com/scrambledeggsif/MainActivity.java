package android.jarsilio.com.scrambledeggsif;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1000;

    @Override
    protected void onResume() {
        super.onResume();

        changePermissionsButton();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = findViewById(R.id.request_permission_button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                requestPermissions();
            }
        });

        changePermissionsButton();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting READ_EXTERNAL_STORAGE permission");
                ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            } else {
                Toast.makeText(this, getString(R.string.permissions_already_granted), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void changePermissionsButton() {
        final Button button = findViewById(R.id.request_permission_button);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            button.setBackgroundResource(R.color.yolkLight);
            button.setText(R.string.permission_granted);
            button.setTextColor(Color.BLACK);
        } else {
            button.setBackgroundResource(R.color.yolk);
            button.setText(R.string.permission_request);
            button.setTextColor(Color.BLACK);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG,"Permission granted");
                } else {
                    Log.d(TAG,"Permission denied");
                }
                changePermissionsButton();
                return;
            }
        }
    }
}
