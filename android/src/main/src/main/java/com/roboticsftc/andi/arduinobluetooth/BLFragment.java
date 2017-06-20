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
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Created by andy on 6/12/17.

 * Everything Bluetooth-related goes in here
 * Both Bluetooth and Location Settings must be on for LE to work
 */

// The scanning used in this class is deprecated as of Android 5.0 (API 21)
// We are still on 4.4

@SuppressWarnings("deprecation")
public class BLFragment extends Fragment implements BluetoothAdapter.LeScanCallback{

    private static final String DEBUG_TAG = "BLFragment";

    //Misc. values
    public static final int REQUEST_ENABLE_BT = 420, REQUEST_LOCATION = 666,        // Flags used to enabled BT and location services
            SCAN_TIME = 10 * 1000, DEFAULT_DELAY = 110;             // Milliseconds

    private enum ConnectionState {
        NULL, //BT and or Loc not available
        AWAIT_ENABLED, //Awaiting
        ENABLED_IDLE, //Everything is enabled, we're just sitting here
        SCANNING, //Currently scanning
        CONNECTED //Connected completely
    }

    //Related to enabling & scanning bluetooth
    private BluetoothAdapter bluetoothAdapter = null;
    private boolean scanning = false;
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();

    //Related to the actual connection
    private BluetoothGatt connectedDevice;
    private Handler commandInputHandler = null; //Post to this to send to BT

    //Report back to activity
    private boolean hasConnectedBefore = false;
    private MainActivity reporterContext = null;
    public interface BluetoothReporter {
        void updateDeviceList(ArrayList<BluetoothDevice> devices); //Called when the list of scanned devices changes
        void deviceConnected(BluetoothGatt device); //Called when a BLE device is connected or disconnected
        void characteristicRead(String message); //Called when a message is received from the BLE device
    }

    //Attached to activity
    public void onAttach(Context context) {
        super.onAttach(context);
        reporterContext = (MainActivity) context;
    }

    //Initial creation
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    //Fragment is actually started - "configure" class
    public void onStart() {
        super.onStart();

        if (reporterContext != null) {
            if (!hasConnectedBefore) {
                ActivityCompat.requestPermissions(reporterContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
                bluetoothAdapter = ((BluetoothManager) reporterContext.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
                enableBTAndLoc();
                hasConnectedBefore = true;
                Log.i(DEBUG_TAG, "BTFragment successfully attached - first instance");
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        //For whatever reason, onResume is actually called before device rotation
        //So we have to delay this a little to compensate for that
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                reporterContext.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        reporterContext.updateDeviceList(devices);
                        if (getCurrentState() == ConnectionState.CONNECTED) reporterContext.deviceConnected(connectedDevice);
                    }
                });
            }
        }).start();
    }

    //Detached from activity - should be automatically called
    public void onDetach() {
        super.onDetach();
        reporterContext = null;
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

        if (getCurrentState() == ConnectionState.NULL) {
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

        if (reporterContext == null) return false;

        int locationMode;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(reporterContext.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                Log.w(DEBUG_TAG, "Settings not found for location - assuming no location");
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        } else {
            locationProviders = Settings.Secure.getString(reporterContext.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
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
        if (!isLocationEnabled() && reporterContext != null) {
            reporterContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(reporterContext, getString(R.string.please_enable_lc), Toast.LENGTH_SHORT).show();
                }
            });
            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_LOCATION);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
    //This is on the UI thread
    public boolean scanBTLEDevices() {
        if (reporterContext == null) return false;
        if (getCurrentState() == ConnectionState.SCANNING) {
            Toast.makeText(reporterContext, getString(R.string.already_scanning), Toast.LENGTH_SHORT).show();
            return false;
        }
        else if (getCurrentState() == ConnectionState.CONNECTED) {
            Toast.makeText(reporterContext, getString(R.string.already_connected), Toast.LENGTH_SHORT).show();
            return false;
        } else if (getCurrentState() != ConnectionState.ENABLED_IDLE) {
            //Some other state, something is probably turned off
            notifyNoBluetooth();
            return false;
        }

        Log.i(DEBUG_TAG, "Beginning BTLE scan");
        Toast.makeText(reporterContext, getString(R.string.scanning_bt), Toast.LENGTH_SHORT).show();
        scanning = true;
        devices.clear();

        //Post now and post delayed a guarantee to shut down scanner after SCAN_TIME ms
        final BluetoothAdapter.LeScanCallback leScanCallback = this;
        HandlerThread thread = new HandlerThread("BTFragment");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.startLeScan(leScanCallback);
            }
        });
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getCurrentState() == ConnectionState.SCANNING) {
                    stopScanning();
                    if (reporterContext != null)
                        reporterContext.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(reporterContext, reporterContext.getString(R.string.finished_scanning), Toast.LENGTH_SHORT).show();
                            }
                        });
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
        if (devices.contains(device)) return; //No dupes
        devices.add(device);
        Log.i(DEBUG_TAG, "Found device with address: " + device.getAddress());
        if (reporterContext != null) reporterContext.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                reporterContext.updateDeviceList(devices);
            }
        });
    }

    //This is on the UI thread
    public void connectToDevice(final BluetoothDevice device) {
        //Make sure correct state
        if (reporterContext == null) return;
        if (getCurrentState() == ConnectionState.CONNECTED) {
            Toast.makeText(reporterContext, getString(R.string.already_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        if (getCurrentState() != ConnectionState.ENABLED_IDLE && getCurrentState() != ConnectionState.SCANNING) {
            notifyNoBluetooth();
            return;
        }
        if (device == null) return;
        if (scanning) stopScanning();

        Toast.makeText(reporterContext, getString(R.string.connecting), Toast.LENGTH_SHORT).show();

        new Thread(new Runnable() { //Don't want any UI Thread hanging
            @Override
            public void run() {
                connectedDevice = device.connectGatt(reporterContext, false, callback);
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
                    if (reporterContext != null) reporterContext.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            reporterContext.deviceConnected(null); //Reset UI
                        }
                    });
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, int status) {
            devices.clear();
            if (reporterContext == null) return;
            reporterContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    reporterContext.updateDeviceList(devices);
                    Toast.makeText(reporterContext, getString(R.string.connected), Toast.LENGTH_SHORT).show();
                }
            });
            reporterContext.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    reporterContext.deviceConnected(gatt);
                }
            });
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            //Log the message read
            Log.i(DEBUG_TAG, "Got payload from char with UUID " + characteristic.getUuid().toString() + ": " + characteristic.getStringValue(0));
            if (reporterContext != null) reporterContext.characteristicRead(characteristic.getStringValue(0));
        }
    };

    //This handler is for sending messages to the Bluetooth when sent to
    public Handler getWriteHandler() {
        if (commandInputHandler == null) { //Create new
            HandlerThread thread = new HandlerThread("BTFragment");
            thread.start();
            commandInputHandler = new Handler(thread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    int delay = (msg.arg1 > 0) ? msg.arg1 : DEFAULT_DELAY; //Guarantees a delay interval > 0
                    String serviceUUID = msg.getData().getString("serviceUUID", "");
                    String charUUID = msg.getData().getString("charUUID", "");

                    //Null checks
                    if (serviceUUID.equals("") || charUUID.equals("")) return;
                    BluetoothGattCharacteristic c = connectedDevice.getService(UUID.fromString(serviceUUID)).
                            getCharacteristic(UUID.fromString(charUUID));
                    if (c == null) return;

                    Log.i(DEBUG_TAG, "Sending " + msg.obj.toString());

                    sendMessagePayload(c, msg.obj.toString(), delay);
                }
            };
        }

        return commandInputHandler;
    }
    public void sendMessagePayload(final BluetoothGattCharacteristic gattCharacteristic, final String message, final int delay) {

        if (delay <= 0 || message == null || message.equals("")) return;
        if (getCurrentState() == ConnectionState.CONNECTED) {

            byte[] payload;

            try {
                //Is our message a float? Will have 4 bytes
                int intPayload = Float.floatToRawIntBits(Float.parseFloat(message)); //Will error here if not
                payload = new byte[4];
                for (int i = 0; i < 4; i++)
                    payload[i] = (byte) ((intPayload >> (i * 8)) & 0xff);

                Log.i(DEBUG_TAG, "Sending payload as float");

                /*
                original code replaced by for loop (easier to understand here)

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
            Toast.makeText(reporterContext, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        connectedDevice.close();

        connectedDevice = null;
        commandInputHandler = null;
    }

    private void notifyNoBluetooth() {
        //Popup saying that bluetooth is not available
        if (reporterContext == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(reporterContext);
        builder.setMessage(getString(R.string.no_bluetooth)).setCancelable(false).setNeutralButton("Okay", null).create().show();
    }

    public ConnectionState getCurrentState() {

        if (bluetoothAdapter == null) return ConnectionState.NULL;
        if (!bluetoothAdapter.isEnabled() || !isLocationEnabled()) return ConnectionState.AWAIT_ENABLED;
        if (scanning) return ConnectionState.SCANNING;
        if (connectedDevice != null) return ConnectionState.CONNECTED;

        return ConnectionState.ENABLED_IDLE;
    }

}