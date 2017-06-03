package com.gotenna.sdk.sample.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import com.gotenna.sdk.bluetooth.BluetoothAdapterManager;
import com.gotenna.sdk.bluetooth.GTConnectionManager;
import com.gotenna.sdk.bluetooth.GTConnectionManager.GTConnectionListener;
import com.gotenna.sdk.bluetooth.GTConnectionManager.GTConnectionState;
import com.gotenna.sdk.sample.BuildConfig;
import com.gotenna.sdk.sample.MyApp;
import com.gotenna.sdk.sample.R;
import com.gotenna.sdk.sample.Utils.SampleAlarmReceiver;
import com.gotenna.sdk.sample.Utils.SampleSchedulingService;
import com.gotenna.sdk.sample.managers.PermissionsManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;

public class PairingActivity extends AppCompatActivity implements GTConnectionListener, CompoundButton.OnCheckedChangeListener
{
    // ================================================================================
    // Class Properties
    // ================================================================================

    private static final int REQUEST_ENABLE_BT = 1003;
    private static final int BLUETOOTH_START_SCAN_DELAY = 500;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 54321;
    private static final int SCAN_TIMEOUT = 25000; // 25 seconds
    private static final String WILL_REMEMBER_GOTENNA_KEY = "WILL_REMEMBER_GOTENNA_KEY";

    private ProgressDialog progressDialog;
    private Switch willRememberGotennaSwitch;

    private GTConnectionManager gtConnectionManager;
    private BluetoothAdapterManager bluetoothAdapterManager;
    private Handler handler;
    private boolean willRememberGotenna;


    public boolean gpsStatus = false;
    private AlertDialog mGPSDialog;
    private static final int GPS_ENABLE_REQUEST = 0x1001;
    SampleAlarmReceiver alarmWifi = new SampleAlarmReceiver();
    // ================================================================================
    // Life-Cycle Methods
    // ================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pairing);

        willRememberGotennaSwitch = (Switch) findViewById(R.id.willRememberGotennaSwitch);
        willRememberGotennaSwitch.setOnCheckedChangeListener(this);

        gtConnectionManager = GTConnectionManager.getInstance();
        bluetoothAdapterManager = BluetoothAdapterManager.getInstance();
        handler  = new Handler(Looper.getMainLooper());

        // Listen for general Bluetooth state changes
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(bluetoothStateChangeReceiver, filter);

        setupProgressDialog();
        PermissionsManager.getInstance().setContext(getApplicationContext());

        SharedPreferences prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        willRememberGotenna = prefs.getBoolean(WILL_REMEMBER_GOTENNA_KEY, true);


       /* TextView text= (TextView) findViewById(R.id.cellMessageTextView);
        String uri = "https://maps.google.com/maps?q=765456,-565845";
        StringBuffer smsBody = new StringBuffer();
        smsBody.append(Uri.parse(uri));
        String messageText = smsBody.toString();
        text.setText(messageText);
*/
        if (!MyApp.tokenIsValid())
        {
            finish();
        }

        sendGPSCoordinates();
        //sendDataToServer();
    }


    private void sendGPSCoordinates() {
        ContentResolver contentResolver = getBaseContext().getContentResolver();
        boolean gpsStatus = Settings.Secure.isLocationProviderEnabled(contentResolver, LocationManager.GPS_PROVIDER);

        if (gpsStatus) {
            alarmWifi.setAlarm(PairingActivity.this);
        } else {
            showGPSDiabledDialog();
        }

    }

    public void showGPSDiabledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("GPS Disabled");
        builder.setMessage("Gps is disabled, in order to use the application properly you need to enable GPS of your device");
        builder.setPositiveButton("Enable GPS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivityForResult(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS), GPS_ENABLE_REQUEST);
            }
        }).setNegativeButton("No, Just Exit", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // chkBoxLocation.setChecked(false);
            }
        });
        mGPSDialog = builder.create();
        mGPSDialog.show();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        gtConnectionManager.addGtConnectionListener(this);
        willRememberGotennaSwitch.setChecked(willRememberGotenna);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        gtConnectionManager.removeGtConnectionListener(this);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        // Callback for the alert asking the user to turn on their Bluetooth
        if (requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode == Activity.RESULT_OK)
            {
                // User Accepted the app's request to enable Bluetooth
                showToast(R.string.enabled_bluetooth_message);
                startBluetoothPairingIfPossible();
            }
            else
            {
                // User Denied the app's request to enable Bluetooth
                showToast(R.string.disabled_bluetooth_message);
            }
        }


        if (requestCode == GPS_ENABLE_REQUEST) {

            gpsStatus=displayGpsStatus();
            //System.out.println("gps "+gpsStatus);

            if(gpsStatus==true){

                alarmWifi.setAlarm(PairingActivity.this);

            }
            if(gpsStatus==false) {

            }

        }
        else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private Boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext().getContentResolver();
        boolean gpsStatus = Settings.Secure.isLocationProviderEnabled(contentResolver, LocationManager.GPS_PROVIDER);
        //System.out.println("" + gpsStatus);
        if (gpsStatus) {
            return true;

        } else {
            return false;
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(bluetoothStateChangeReceiver);
    }

    // ================================================================================
    // Class Instance Methods
    // ================================================================================

    private void setupProgressDialog()
    {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.searching_for_gotenna));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        progressDialog.setOnDismissListener(new DialogInterface.OnDismissListener()
        {
            @Override
            public void onDismiss(DialogInterface dialog)
            {
                GTConnectionState connectionState = gtConnectionManager.getGtConnectionState();

                // The progress dialog was dismissed while we were in the middle of scanning for the goTenna
                if (connectionState == GTConnectionState.SCANNING)
                {
                    stopScanning();
                    showToast(R.string.toast_stopped_scanning);
                }
            }
        });
    }

    protected void startBluetoothPairingIfPossible()
    {
        BluetoothAdapterManager.BluetoothStatus status = bluetoothAdapterManager.getBluetoothStatus();

        switch(status)
        {
            case SUPPORTED_AND_ENABLED:
            {
                if (PermissionsManager.getInstance().hasLocationPermission())
                {
                    if (!willRememberGotenna)
                    {
                        // If we clear the last connected goTenna address,
                        // then the connection manager will look for any goTenna and try to connect to it.
                        // If we do not clear the address, it will specifically look for that last goTenna
                        // that it remembers being connected to.
                        gtConnectionManager.clearConnectedGotennaAddress();
                    }

                    gtConnectionManager.scanAndConnect();

                    progressDialog.show();
                    handler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT);
                }
                else
                {
                    PermissionsManager.getInstance().requestLocationPermission(this, LOCATION_PERMISSION_REQUEST_CODE);
                }
            }
            break;

            case SUPPORTED_NOT_ENABLED:
            {
                BluetoothAdapterManager.showRequestBluetoothPermissionDialog(this, REQUEST_ENABLE_BT);
            }
            break;

            case NOT_SUPPORTED:
            {
                showToast(R.string.no_bluetooth_support_message);
            }
            break;
        }
    }

    private void stopScanning()
    {
        handler.removeCallbacks(scanTimeoutRunnable);
        gtConnectionManager.disconnect();

        if (!willRememberGotenna)
        {
            gtConnectionManager.clearConnectedGotennaAddress();
        }
    }

    private void showToast(int resId)
    {
        Toast.makeText(getApplicationContext(), resId, Toast.LENGTH_LONG).show();
    }

    // ================================================================================
    // Final Class Properties
    // ================================================================================

    private final Runnable scanTimeoutRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            stopScanning();
            progressDialog.dismiss();
            showToast(R.string.toast_could_not_find_gotenna);
        }
    };

    private final BroadcastReceiver bluetoothStateChangeReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED))
            {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state)
                {
                    case BluetoothAdapter.STATE_OFF:
                        gtConnectionManager.disconnect();
                        break;

                    case BluetoothAdapter.STATE_ON:
                    {
                        // We need to delay the starting of the scan because even though we got the
                        // STATE_ON notification, the Bluetooth Adapter still needs some time
                        // to actually be ready to use.
                        Handler handler = new Handler(Looper.getMainLooper());
                        handler.postDelayed(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                startBluetoothPairingIfPossible();
                            }
                        }, BLUETOOTH_START_SCAN_DELAY);
                    }
                    break;
                }
            }
        }
    };

    // ================================================================================
    // Button onClick Methods
    // ================================================================================

    public void onStartSearchButtonClicked(View v)
    {
        GTConnectionState connectionState = gtConnectionManager.getGtConnectionState();

        if (connectionState == GTConnectionState.DISCONNECTED)
        {
            startBluetoothPairingIfPossible();
        }
    }



    // ================================================================================
    // GTConnectionListener Implementation
    // ================================================================================

    @Override
    public void onConnectionStateUpdated(GTConnectionState gtConnectionState)
    {
        switch (gtConnectionState)
        {
            case CONNECTED:
            {
                handler.removeCallbacks(scanTimeoutRunnable);
                progressDialog.dismiss();

                Intent intent = new Intent(this, SdkOptionsActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            }
                break;
        }
    }

    // ================================================================================
    // OnCheckedChangeListener Implementation
    // ================================================================================

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
    {
        willRememberGotenna = isChecked;
        SharedPreferences prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor =  prefs.edit();

        editor.putBoolean(WILL_REMEMBER_GOTENNA_KEY, isChecked);
        editor.apply();
    }


}
