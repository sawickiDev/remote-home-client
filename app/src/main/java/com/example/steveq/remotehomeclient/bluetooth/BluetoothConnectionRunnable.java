package com.example.steveq.remotehomeclient.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothConnectionRunnable implements Runnable {
    private static final String TAG = BluetoothConnectionRunnable.class.getSimpleName();

    private Handler handler; // handler that gets info from Bluetooth service

    // Defines several constants used when transmitting messages between the
    // service and the UI.
    private interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;

        // ... (Add other message types here as needed.)
    }
    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private byte[] mmBuffer;

    public BluetoothConnectionRunnable(BluetoothSocket socket, Handler handler) {
        this.socket = socket;
        this.handler = handler;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        Message msg = handler.obtainMessage();

        try {
            tmpIn = socket.getInputStream();
        } catch (IOException e) {
            msg.arg1 = 0;
            Log.e(TAG, "Error occurred when creating input stream", e);
        }
        try {
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            msg.arg1 = 0;
            Log.e(TAG, "Error occurred when creating output stream", e);
        }

        this.inputStream = tmpIn;
        this.outputStream = tmpOut;
        msg.arg1 = 1;
        handler.sendMessage(msg);
    }

    public void run() {
        mmBuffer = new byte[1024];
        int numBytes; // bytes returned from read()

        // Keep listening to the InputStream until an exception occurs.
        while (true) {
            try {
                numBytes = this.inputStream.read(mmBuffer);
                Message readMsg = handler.obtainMessage(
                        BluetoothConnectionRunnable.MessageConstants.MESSAGE_READ, numBytes, -1,
                        mmBuffer);
                readMsg.sendToTarget();
            } catch (IOException e) {
                Log.d(TAG, "Input stream was disconnected", e);
                break;
            }
        }
    }

    // Call this from the main activity to send data to the remote device.
    public void write(byte[] bytes) {
        try {
            outputStream.write(bytes);

            // Share the sent message with the UI activity.
            Message writtenMsg = handler.obtainMessage(
                    BluetoothConnectionRunnable.MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
            writtenMsg.sendToTarget();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);

            // Send a failure message back to the activity.
            Message writeErrorMsg =
                    handler.obtainMessage(BluetoothConnectionRunnable.MessageConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast",
                    "Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            handler.sendMessage(writeErrorMsg);
        }
    }

    // Call this method from the main activity to shut down the connection.
    public void cancel() {
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
}
