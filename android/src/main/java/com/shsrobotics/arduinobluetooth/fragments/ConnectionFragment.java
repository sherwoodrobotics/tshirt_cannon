package com.shsrobotics.arduinobluetooth.fragments;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.roboticsftc.andi.arduinobluetooth.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andy on 4/21/17.
 *
 * Layout & buttons for connection-related setup & info (tab 1)
 */

public class ConnectionFragment extends Fragment {

    private static final String DEBUG_TAG = "ConnectionFragment";

    //Connection-related
    private List<BluetoothDevice> devices = new ArrayList<>();
    private List<BluetoothGattCharacteristic> characteristics = new ArrayList<>();

    //UI-related
    private Spinner connectionSpinner, characteristicSpinner;
    private TextView btAddress, btName, serviceUUID, charUUID;

    //Attached Activity
    private Context context;

    public void onAttach(Context context) {
        //Called when added to the Activity FragmentManager
        super.onAttach(context);
        this.context = context;

        Log.i(DEBUG_TAG, "Attached to activity " + context.getPackageName());
    }
    public void onDetach() {
        //Should not be called ever
        super.onDetach();
        context = null;

        Log.i(DEBUG_TAG, "Detached from activity");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.connection_layout, container, false);

        //Get stuff
        connectionSpinner = (Spinner) v.findViewById(R.id.bluetooth_connections);
        characteristicSpinner = (Spinner) v.findViewById(R.id.spinner_characteristics);
        btAddress = (TextView) v.findViewById(R.id.txt_bt_address);
        btName = (TextView) v.findViewById(R.id.txt_bt_name);
        serviceUUID = (TextView) v.findViewById(R.id.txt_service_uuid);
        charUUID = (TextView) v.findViewById(R.id.txt_char_uuid);

        return v;

    }

    public void onActivityCreated(Bundle savedInstancedState) {
        super.onActivityCreated(savedInstancedState);

        //Setup characteristicSpinner
        characteristicSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                serviceUUID.setText(characteristics.get(position).getService().getUuid().toString());
                charUUID.setText(characteristics.get(position).getUuid().toString());
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    public void clearDevices() {
        //Clears potential devices
        devices.clear();
        updateSpinner();
    }
    public void updateDeviceList(ArrayList<BluetoothDevice> devices) {
        //Called when new potential devices are found
        //Use argument as read-only
        this.devices.clear();
        for (BluetoothDevice bd : devices) this.devices.add(bd);
        updateSpinner();
    }

    private void updateSpinner() {

        //Update potential devices spinner

        Log.i(DEBUG_TAG, "Updating device spinner with " + devices.size() + " devices");

        String[] names = new String[0];

        if (devices.size() > 0) { //If there's anything in the devices list
            names = new String[devices.size()];
            for (int i = 0; i < devices.size(); i++)
                names[i] = formatBTDevice(devices.get(i));
        }

        //Set the Spinner
        if (context != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, names);
            connectionSpinner.setAdapter(adapter);
        }
    }

    private String formatBTDevice(BluetoothDevice bluetoothDevice) {
        //Format the device descriptor to be "NAME (ADDRESS)"
        return bluetoothDevice.getName() + " (" + bluetoothDevice.getAddress() + ")";
    }

    public BluetoothDevice getSelectedDevice() {
        //The Spinner is in the same order as the devices list
        return devices.get(connectionSpinner.getSelectedItemPosition());
    }

    public void updateBluetooth(BluetoothGatt device) {
        if (device == null) {
            //Disconnected - reset UI
            btName.setText("");
            btAddress.setText("");
        } else {
            //Connected device
            btName.setText(device.getDevice().getName());
            btAddress.setText(device.getDevice().getAddress());
        }
    }

    public void updateCharacteristics(List<BluetoothGattCharacteristic> characteristics) {
        this.characteristics.clear();
        this.characteristics = characteristics;

        //Format each characteristic to be "C #### - S ####"
        String[] names = new String[characteristics.size()];
        for (int i = 0; i < characteristics.size(); i++) {
            names[i] = "C " + characteristics.get(i).getUuid().toString().substring(4, 8)
                    + " - S " + characteristics.get(i).getService().getUuid().toString().substring(4, 8);
        }

        if (context != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, names);
            characteristicSpinner.setAdapter(adapter);
        }
        if (characteristics.size() > 0) {
            //Set UI to first selection on the Spinner
            serviceUUID.setText(characteristics.get(0).getService().getUuid().toString());
            charUUID.setText(characteristics.get(0).getUuid().toString());
        } else {
            //Disconnected - reset UI
            serviceUUID.setText("");
            charUUID.setText("");
        }
    }
}
