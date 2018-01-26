package android.jarsilio.com.scrambledeggsif;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1000;

    @Override
    protected void onResume() {
        super.onResume();

        updateLayout();
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

        updateLayout();
    }
    @Override

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.privacy_policy_menu_item:
                showPrivacyPolicyDialog();
                break;
        }

        return super.onOptionsItemSelected(item);
    }
    private void showPrivacyPolicyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.privacy_policy_dialog_title);
        builder.setMessage(R.string.privacy_policy_dialog_text);
        builder.setPositiveButton(android.R.string.ok, null);

        AlertDialog dialog = builder.show();

        // Work-around to make links clickable (don't ask me why this works):
        // See: https://stackoverflow.com/questions/1997328/how-can-i-get-clickable-hyperlinks-in-alertdialog-from-a-string-resource
        if (Build.VERSION.SDK_INT > 8) {
            ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private void requestPermissions() {
        if (Utils.isPermissionGranted(getApplicationContext())) {
            Toast.makeText(this, getString(R.string.permissions_already_granted), Toast.LENGTH_SHORT).show();
        } else {
            Log.d(TAG, "Requesting READ_EXTERNAL_STORAGE permission");
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        }
    }

    private void updateLayout() {
        final Button button = findViewById(R.id.request_permission_button);
        final TextView textView = findViewById(R.id.permissions_explanation);

        if (Utils.isPermissionGranted(getApplicationContext())) {
            button.setBackgroundResource(R.color.yolkLight);
            button.setText(R.string.permission_granted);
            button.setTextColor(Color.BLACK);
            textView.setText(R.string.ready_to_roll);
        } else {
            button.setBackgroundResource(R.color.yolk);
            button.setText(R.string.permission_request);
            button.setTextColor(Color.BLACK);
            textView.setText(R.string.permission_sdcard_text);
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
                updateLayout();
                return;
            }
        }
    }
}
