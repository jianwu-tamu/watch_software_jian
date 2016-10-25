package com.example.zzw.watchpkgwithoutglass;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;



public class MainActivity extends WearableActivity {
    private final static String TAG = "MainActiviy";

    private Button connectButton;
    private Button checkIpButton;
    private Button changeIpButton;
    public static final String IP_SAVED_KEY = "saved ip";
    public static final String STOP_ACTION = "STOP_BLE_SERVICE";
    static final int INPUT_REQUEST = 1;  // The request code

    private SharedPreferences sharedPref;
    public String ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPref = getPreferences(Context.MODE_PRIVATE);
        ip = sharedPref.getString(IP_SAVED_KEY, "192.168.1.100");

        setContentView(R.layout.activity_main);
        connectButton = (Button) findViewById(R.id.switch_connect_btn);
        if (isServiceRunning(BLEService.class)) {
            connectButton.setText("disconnect");
        }else {
            connectButton.setText("connect");
        }

        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isServiceRunning(BLEService.class)) {

                    // send broadcast to stop the service
                    sendBroadcast(new Intent(STOP_ACTION));
                    connectButton.setText("connect");
                } else {
                    // start the service
                    startBLEService();
                    if (isServiceRunning(BLEService.class)) {
                        connectButton.setText("disconnect");
                    }
                }
            }
        });

        checkIpButton = (Button) findViewById(R.id.check_ip_btn);
        checkIpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSavedIp();
            }
        });

        changeIpButton = (Button) findViewById(R.id.input_ip_btn);
        changeIpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startKeyBoard();
            }
        });
    }

    private void startKeyBoard() {
        Intent keyboardIntent = new Intent();
        keyboardIntent.setClass(this, KeyboardActivity.class);
        startActivityForResult(keyboardIntent, INPUT_REQUEST);
    }

    private void showSavedIp() {
        Toast.makeText(this, ip, Toast.LENGTH_SHORT).show();
    }

    private void startBLEService() {
        Intent mIntent = new Intent(this, BLEService.class);
        mIntent.putExtra(IP_SAVED_KEY, ip);
        startService(mIntent);
    }

    /**
     *  check whether the service is running
     */
    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Make sure the request was successful
        if (resultCode == Activity.RESULT_OK) {
            ip = data.getStringExtra("ip");
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(IP_SAVED_KEY, ip);
            editor.commit();
        }
    }
}
