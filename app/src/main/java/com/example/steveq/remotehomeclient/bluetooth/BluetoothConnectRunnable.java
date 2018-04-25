package com.example.steveq.remotehomeclient.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectRunnable implements Runnable{
    private static final String TAG = BluetoothConnectRunnable.class.getSimpleName();

    private final BluetoothSocket socket;
    private final BluetoothDevice device;
    private BluetoothAdapter bluetoothAdapter;
    private static final UUID MY_UUID = UUID.fromString("94f39d29-7d6d-437d-973b-fba39e49d4ee");
    private boolean connected;
    private BluetoothConnectionRunnable bluetoothConnectionRunnable;

    public BluetoothConnectRunnable(BluetoothDevice device, BluetoothAdapter adapter) {
        bluetoothAdapter = adapter;
        BluetoothSocket tmp = null;
        this.device = device;

        try {
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {
            Log.e(TAG, "Socket's create() method failed", e);
        }
        this.socket = tmp;
    }

    @Override
    public void run() {
        bluetoothAdapter.cancelDiscovery();

        try {
            socket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.e(TAG, "Could not close the client socket", closeException);
            }
            return;
        }

        connected = true;
        HandlerThread handlerThread = new HandlerThread("Manage Connection Thread");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        Handler handler = new Handler(looper) {
            @Override
            public void handleMessage(Message msg) {
                Log.d(TAG, "MESSAGE FROM CONNECTION SERVICE :: " + msg.toString());
            }
        };
        bluetoothConnectionRunnable =
                new BluetoothConnectionRunnable(socket, handler);
        handler.post(bluetoothConnectionRunnable);
    }

    public void write(String message) {
        if (bluetoothConnectionRunnable != null) {
            Log.d(TAG, "WRITE 1 :: " + message);
            bluetoothConnectionRunnable.write(message.getBytes());
        }
    }

    // Closes the client socket and causes the thread to finish.
    public void cancel() {
        try {
            socket.close();
            connected = false;
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
