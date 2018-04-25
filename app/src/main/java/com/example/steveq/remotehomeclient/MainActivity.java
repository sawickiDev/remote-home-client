package com.example.steveq.remotehomeclient;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.example.steveq.remotehomeclient.bluetooth.BluetoothConnectRunnable;
import com.example.steveq.remotehomeclient.services.PermissionChecker;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int BLUETOOTH_REQUEST = 10;
    private static final int BLUETOOTH_ENABLE_REQUEST = 20;
    private static final String[] NEEDED_PERMISSIONS =
            new String[]{
                    "android.permission.BLUETOOTH",
                    "android.permission.BLUETOOTH_ADMIN",
                    "android.permission.ACCESS_COARSE_LOCATION"
            };

    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout swipeRefreshLayout;

    @BindView(R.id.parentLinearLayout)
    LinearLayout parentLinearLayout;

    @BindView(R.id.messageEditText)
    EditText messageEditText;

    @BindView(R.id.sendButton)
    Button sendButton;

    @BindView(R.id.discoverProgressBar)
    ProgressBar discoverProgressBar;

    private BluetoothAdapter bluetoothAdapter;
    private boolean bluetoothEnabled;
    private static String NEEDED_DEVICE_NAME = "raspberrypi";
    private BluetoothDevice raspberryDevice;
    private BroadcastReceiver bluetoothDiscoveryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress();
                if (NEEDED_DEVICE_NAME.equals(deviceName)) {
                    raspberryDevice = device;
                    Log.d(TAG, "DISCOVERED :: " + deviceName + " :: " + deviceHardwareAddress);
                    toggleProgressBar();
                    initConection();
                }
            }
        }
    };

    private BluetoothConnectRunnable bluetoothConnectRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        handlePermissionCheck();
        sendButton.setOnClickListener(v -> {
            if (bluetoothConnectRunnable != null
                && bluetoothConnectRunnable.isConnected()) {
                Log.d(TAG, "SEND :: " + messageEditText.getText().toString());
                bluetoothConnectRunnable.write(messageEditText.getText().toString());
            }
        });
        swipeRefreshLayout.setOnRefreshListener(() -> handlePermissionCheck());
    }

    private void handlePermissionCheck() {

        PermissionChecker permissionChecker = new PermissionChecker(this);
        List<String> falsyPermissions = permissionChecker.getFalsyPermissions(NEEDED_PERMISSIONS);

        if (!falsyPermissions.isEmpty()) {
            permissionChecker.requestPermissions(falsyPermissions, BLUETOOTH_REQUEST);
        } else {
            enableBluetooth();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case BLUETOOTH_REQUEST: {
                boolean allGranted = Arrays.stream(grantResults).allMatch(g -> g == PackageManager.PERMISSION_GRANTED);

                if (!allGranted) {
                    showSimpleSnackbar("You can't use app without permissions");
                } else {
                    enableBluetooth();
                }
                break;
            }
        }
    }

    public void enableBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            showSimpleSnackbar("Bluetooth communication is not supported");
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intent, BLUETOOTH_ENABLE_REQUEST);
            } else {
                discoveryDevice();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BLUETOOTH_ENABLE_REQUEST: {
                if (resultCode == RESULT_OK) {
                    bluetoothEnabled = true;
                    discoveryDevice();
                } else {
                    showSimpleSnackbar("You must enable bluetooth to use the app");
                }
            }

        }
    }

    public void discoveryDevice() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            if (pairedDevices.stream().anyMatch(pd -> NEEDED_DEVICE_NAME.equals(pd.getName()))) {
                raspberryDevice = pairedDevices.stream().filter(pd -> NEEDED_DEVICE_NAME.equals(pd.getName())).findFirst().get();
                initConection();
            } else {
                toggleProgressBar();
                IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(bluetoothDiscoveryReceiver, filter);
                bluetoothAdapter.startDiscovery();
                new android.os.Handler().postDelayed(() -> {
                    if (raspberryDevice == null) {
                        bluetoothAdapter.cancelDiscovery();
                        showSimpleSnackbar("CANNOT FIND RASPBERRY DEVICE");
                        toggleProgressBar();
                    }
                },
                1000 * 12);
            }
        }
    }

    private void initConection() {
        swipeRefreshLayout.setRefreshing(false);
        HandlerThread handlerThread = new HandlerThread("Bluetooth Connection Thread");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper);
        //TODO : handle connected message to change status icon
        bluetoothConnectRunnable = new BluetoothConnectRunnable(raspberryDevice, bluetoothAdapter);
        handler.post(bluetoothConnectRunnable);
    }

    private void toggleProgressBar() {
        if (discoverProgressBar.getVisibility() == View.INVISIBLE) {
            discoverProgressBar.setVisibility(View.VISIBLE);
        } else {
            discoverProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private void showSimpleSnackbar(String message){
        Snackbar
                .make(parentLinearLayout, message, Snackbar.LENGTH_LONG)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(bluetoothDiscoveryReceiver);
    }
}
