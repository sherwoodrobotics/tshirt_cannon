package com.roboticsftc.andi.arduinobluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.UUID;


/**
 * Created by andy on 4/24/17.
 *
 * Virtually everything Bluetooth-related goes in here
 * Both Bluetooth and Location Settings must be on for LE to work
 */

// The scanning used in this class is deprecated as of Android 5.0 (API 21)
// We are still on 4.4

@SuppressWarnings("deprecation")
public abstract class BTActivity extends AppCompatActivity implements BluetoothAdapter.LeScanCallback {
    private static final String DEBUG_TAG = "BTActivity";

    //Flag for catching activity restart - we don't need to redo connection
    public static final String RESTART_DETECT_FLAG = "RestartDetect";

    //Misc. values
    public static final int REQUEST_ENABLE_BT = 420, REQUEST_LOCATION = 666,        // Flags used to enabled BT and location services
                            MESSAGE_ERROR = -1, MESSAGE_READ = 0, MESSAGE_SEND = 1, // Handler message types
                            SCAN_TIME = 10 * 1000, DEFAULT_DELAY = 110;             // Milliseconds

    private enum ConnectionState {
        NULL, //BT and or Loc not available
        AWAIT_ENABLED, //Awaiting
        ENABLED_IDLE, //Everything is enabled, we're just sitting here
        SCANNING, //Currently scanning
        CONNECTED //Connected completely
    }

    //Related to enabling & scanning bluetooth
    private BluetoothAdapter bluetoothAdapter;
    private boolean scanning = false;

    //Related to the actual connection
    private BluetoothGatt connectedDevice;
    private Handler commandInputHandler = null; //Post to this to send to BT

    //In-app logging
    private ArrayList<Handler> consoleOutputHandlers = new ArrayList<>(); //We post to this

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set up app UI first
        activityCreated(savedInstanceState);

        //If app has been created before then don't bother with BT/Location stuff
        if (savedInstanceState != null && savedInstanceState.getString(RESTART_DETECT_FLAG, "").equals(RESTART_DETECT_FLAG)) return;

        //Required
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
        bluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        enableBTAndLoc();

        Log.i(DEBUG_TAG, "Activity successfully created");
    }

    protected abstract void activityCreated(Bundle savedInstanceState); // Addressed by subclasses when onCreate called first time

    //If activity is being recreated for whatever reason, add a flag (String same as key)
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(RESTART_DETECT_FLAG, RESTART_DETECT_FLAG);
        Log.i(DEBUG_TAG, "Flag set - app will not restart");
    }

    //Turns on Bluetooth & Location Services sequentially, then scans devices
    private void enableBTAndLoc() {
        /*
            If BT is not available / non-existant, no bluetooth
            Else
                if BT is not enabled, try to enable it
                else
                    if loc services aren't enabled, enable location services
                    else scan
         */
        if (getCurrentState() == ConnectionState.NULL || !getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            notifyNoBluetooth();
        } else {
            if (!isBluetoothEnabled()) turnOnBTSettings();
            else {
                if (!isLocationEnabled()) turnOnLocationServices();
                else scanBTLEDevices();
            }
        }
    }

    //Checks if enabled
    private boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }
    private boolean isLocationEnabled() {
        //Copied from somewhere on StackOverflow - works at least

        int locationMode;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                Log.w(DEBUG_TAG, "Settings not found for location - assuming no location");
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    //Intents to enable BT / location settings
    public void turnOnBTSettings() {
        if (!isBluetoothEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }
    public void turnOnLocationServices() {
        if (!isLocationEnabled()) {
            Toast.makeText(getApplicationContext(), "Please enable location services", Toast.LENGTH_SHORT).show();
            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_LOCATION);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(DEBUG_TAG, "Received Intent " + requestCode + " with result " + resultCode);
        if (requestCode == REQUEST_ENABLE_BT) {
            //If bt is ready to go, move onto location services
            if (isBluetoothEnabled()) enableBTAndLoc();
            else notifyNoBluetooth();
        } else if (requestCode == REQUEST_LOCATION) {
            //If loc
            if (isLocationEnabled()) enableBTAndLoc();
            else notifyNoBluetooth();
        }
    }

    //Returns whether or not scanning has begun
    public boolean scanBTLEDevices() {
        if (getCurrentState() == ConnectionState.SCANNING) {
            Toast.makeText(getApplicationContext(), "Already scanning!", Toast.LENGTH_SHORT).show();
            return false;
        }
        else if (getCurrentState() == ConnectionState.CONNECTED) {
            Toast.makeText(getApplicationContext(), "Already connected!", Toast.LENGTH_SHORT).show();
            return false;
        } else if (getCurrentState() != ConnectionState.ENABLED_IDLE) {
            //Some other state, something is probably turned off
            notifyNoBluetooth();
            return false;
        }

        Log.i(DEBUG_TAG, "Beginning BTLE scan");
        Toast.makeText(getApplicationContext(), "Scanning Bluetooth devices...", Toast.LENGTH_SHORT).show();
        scanning = true;

        //Post now and post delayed a guarantee to shut down scanner after SCAN_TIME ms
        final BluetoothAdapter.LeScanCallback leScanCallback = this;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            public void run() {
                bluetoothAdapter.startLeScan(leScanCallback);
            }
        });
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getCurrentState() == ConnectionState.SCANNING) {
                    stopScanning();
                    Toast.makeText(getApplicationContext(), "Finished scanning", Toast.LENGTH_SHORT).show();
                }
            }
        }, SCAN_TIME);

         return true; //Scanning has begun
    }

    private void stopScanning() {
        //State check doesn't really matter here
        if (getCurrentState() == ConnectionState.SCANNING) {
            bluetoothAdapter.stopLeScan(this);
            scanning = false;
            Log.i(DEBUG_TAG, "BTLE scan stopped");
        }
    }

    @Override
    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        //When a device is found
        Log.i(DEBUG_TAG, "Found device with address: " + device.getAddress());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateDeviceList(device);
            }
        });
    }
    protected abstract void updateDeviceList(BluetoothDevice device); //Addressed by sub-classes when device found

    public void connectToDevice(final BluetoothDevice device) {
        //Make sure correct state
        if (getCurrentState() == ConnectionState.CONNECTED) {
            Toast.makeText(getApplicationContext(), "Already connected!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (getCurrentState() != ConnectionState.ENABLED_IDLE && getCurrentState() != ConnectionState.SCANNING) {
            notifyNoBluetooth();
            return;
        }
        if (device == null) return;
        if (scanning) stopScanning();

        final Context context = this;
        Toast.makeText(getApplicationContext(), "Connecting...", Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() { //Don't want any UI Thread hanging
            @Override
            public void run() {
                connectedDevice = device.connectGatt(context, false, callback);
            }
        }).start();
    }

    //When devices connected or disconnected
    private BluetoothGattCallback callback = new BluetoothGattCallback() {

        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    Log.i(DEBUG_TAG, "Connected to device with address: " + gatt.getDevice().getAddress());
                    //Shove on different thread to not hang UI Thread
                    new Thread() {
                        public void run() {
                            connectedDevice.discoverServices();
                        }
                    }.start();
                    break;

                case BluetoothProfile.STATE_DISCONNECTED:
                    Log.i(DEBUG_TAG, "Disconnected from device with address: " + gatt.getDevice().getAddress());
                    connectedDevice = null;
                    deviceConnected(null); //Reset UI
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Connected and ready to go", Toast.LENGTH_SHORT).show();
                    deviceConnected(gatt); //Run this on UI too
                }
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //Log the message read
            Log.i(DEBUG_TAG, "Got payload from char with UUID " + characteristic.getUuid().toString() + ": " + characteristic.getStringValue(0));

            for (Handler handler : consoleOutputHandlers)
                handler.obtainMessage(MESSAGE_READ, characteristic.getStringValue(0)).sendToTarget();
        }
    };

    protected abstract void deviceConnected(BluetoothGatt device); // Addressed by sub-classes when a device is connected

    //Output handler for received data
    public void addHandler(Handler handler) {
        consoleOutputHandlers.add(handler);
    }


    //This handler is for sending messages to the Bluetooth when sent to
    public Handler getWriteHandler() {
        if (commandInputHandler == null) { //Create new
            HandlerThread thread = new HandlerThread("BTActivity");
            thread.start();
            commandInputHandler = new Handler(thread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == MESSAGE_SEND) {

                        int delay = (msg.arg1 > 0) ? msg.arg1 : DEFAULT_DELAY; //Guarantees a delay interval > 0
                        String serviceUUID = msg.getData().getString("serviceUUID", "");
                        String charUUID = msg.getData().getString("charUUID", "");

                        //Null checks
                        if (serviceUUID.equals("") || charUUID.equals("")) return;
                        BluetoothGattCharacteristic c = connectedDevice.getService(UUID.fromString(serviceUUID)).
                                getCharacteristic(UUID.fromString(charUUID));
                        if (c == null) return;

                        Log.i(DEBUG_TAG, "Sending " + msg.obj.toString());

                        BTActivity.this.sendMessage(c, msg.obj.toString(), delay);
                    }
                }
            };
        }

        return commandInputHandler;
    }
    public void sendMessage(final BluetoothGattCharacteristic gattCharacteristic, final String message, final int delay) {

        if (delay <= 0 || message == null || message.equals("")) return;
        if (getCurrentState() == ConnectionState.CONNECTED) {

            byte[] payload;

            try {
                //Is our message a float? Will have 4 bytes
                int intPayload = Float.floatToRawIntBits(Float.parseFloat(message)); //Will error here if not
                payload = new byte[4];
                for (int i = 0; i < 4; i++) {
                    payload[i] = (byte) ((intPayload >> (i * 8)) & 0xff);
                }
                Log.i(DEBUG_TAG, "Sending payload as float");
                /*
                payload[0] = (byte)(intPayload & 0xff);
                payload[1] = (byte)((intPayload >> 8) & 0xff);
                payload[2] = (byte)((intPayload >> 16) & 0xff);
                payload[3] = (byte)((intPayload >> 24) & 0xff);
                */
            } catch (NumberFormatException nfe) {
                payload = null;
            }

            try {
                //Is our message a int? Will have 4 bytes
                if (payload == null)
                    payload = BigInteger.valueOf(Integer.parseInt(message)).toByteArray(); //Will error here if not
                Log.i(DEBUG_TAG, "Sending payload as int");
            } catch (NumberFormatException nfe) {
                payload = null;
            }

            if (payload != null) {

                //If float or int, send it

                final byte[] sendPayload = payload;

                getWriteHandler().post(new Runnable() {
                    @Override
                    public void run() { //Use this handler to make use of the sleep
                        gattCharacteristic.setValue(sendPayload);
                        connectedDevice.writeCharacteristic(gattCharacteristic);

                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            Log.w(DEBUG_TAG, "InterruptedException in payload");
                        }
                    }
                });
            } else

                //String message as char array - send each char individually
                Log.i(DEBUG_TAG, "Sending payload as char array");

                for (final char c : message.toCharArray()) {

                    getWriteHandler().post(new Runnable() {
                        @Override
                        public void run() { //Use this handler to make use of the sleep
                            gattCharacteristic.setValue((c + "").getBytes());
                            connectedDevice.writeCharacteristic(gattCharacteristic);

                            try {
                                Thread.sleep(delay);
                            } catch (InterruptedException e) {
                                Log.w(DEBUG_TAG, "InterruptedException in payload");
                            }
                        }
                    });
                }
        } else if (getCurrentState() != ConnectionState.ENABLED_IDLE && getCurrentState() != ConnectionState.SCANNING) {
            notifyNoBluetooth();
        }
    }

    public void closeConnection() {
        if (getCurrentState() != ConnectionState.CONNECTED) {
            Log.w(DEBUG_TAG, "Incorrect state");
            for (Handler h : consoleOutputHandlers)
                h.obtainMessage(MESSAGE_ERROR, "Incorrect state: Expected CONNECTED actual " + getCurrentState()).sendToTarget();
            return;
        }

        connectedDevice.close();

        connectedDevice = null;
        commandInputHandler = null;
    }

    private void notifyNoBluetooth() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Error: Bluetooth and/or location services is unsupported or is disabled.")
                .setCancelable(false).setNeutralButton("Okay", null).create().show();
        for (Handler h : consoleOutputHandlers)
            h.obtainMessage(MESSAGE_ERROR, "Error: Bluetooth and/or locations services are unsupported or are disabled.").sendToTarget();
    }

    public ConnectionState getCurrentState() {

        if (bluetoothAdapter == null) return ConnectionState.NULL;
        if (!bluetoothAdapter.isEnabled() || !isLocationEnabled()) return ConnectionState.AWAIT_ENABLED;
        if (scanning) return ConnectionState.SCANNING;
        if (connectedDevice != null) return ConnectionState.CONNECTED;

        return ConnectionState.ENABLED_IDLE;
    }
}
