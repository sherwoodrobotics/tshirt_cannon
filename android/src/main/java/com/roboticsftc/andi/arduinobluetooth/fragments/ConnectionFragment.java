package com.roboticsftc.andi.arduinobluetooth.fragments;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
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
 * Layout & buttons for connection-related setup & info
 */

public class ConnectionFragment extends Fragment {

    private List<BluetoothDevice> devices = new ArrayList<>();
    private List<BluetoothGattCharacteristic> characteristics = new ArrayList<>();

    private Spinner connectionSpinner, characteristicSpinner;
    private TextView btAddress, btName, serviceUUID, charUUID;

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
        devices.clear();
        updateSpinner();
    }
    public void addDevice(BluetoothDevice device) {
        if (devices.contains(device)) return; //No dupes
        devices.add(device);
        updateSpinner();
    }

    private void updateSpinner() {

        Log.i("ConnectionFragment", "Updating connection spinner with " + devices.size() + " devices");

        String[] names = new String[0];

        if (devices.size() > 0) { //If there's anything in the devices list
            names = new String[devices.size()];
            for (int i = 0; i < devices.size(); i++)
                names[i] = formatBTDevice(devices.get(i));
        }

        Log.i("ConnectionFragment", "Name size: " + names.length);
        Log.i("ConnectionFragment", "0 is " + names[0]);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, names);
        connectionSpinner.setAdapter(adapter);
    }

    private String formatBTDevice(BluetoothDevice bluetoothDevice) {
        //Format the device descriptor to be NAME (ADDRESS)
        return bluetoothDevice.getName() + " (" + bluetoothDevice.getAddress() + ")";
    }

    public BluetoothDevice getSelectedDevice() {
        //The Spinner is in the same order as the devices list
        if (connectionSpinner.getSelectedItem() == null) return null;
        return devices.get(connectionSpinner.getSelectedItemPosition());
    }

    public void updateBluetooth(BluetoothGatt device) {
        if (device == null) {
            btName.setText("");
            btAddress.setText("");
        } else {
            btName.setText(device.getDevice().getName());
            btAddress.setText(device.getDevice().getAddress());
        }
    }
    public void updateCharacteristics(List<BluetoothGattCharacteristic> characteristics) {
        this.characteristics.clear();
        this.characteristics = characteristics;

        String[] names = new String[characteristics.size()];
        for (int i = 0; i < characteristics.size(); i++) {
            names[i] = "C " + characteristics.get(i).getUuid().toString().substring(4, 8)
                    + " - S " + characteristics.get(i).getService().getUuid().toString().substring(4, 8);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, names);
        characteristicSpinner.setAdapter(adapter);

        if (characteristics.size() > 0) {
            serviceUUID.setText(characteristics.get(0).getService().getUuid().toString());
            charUUID.setText(characteristics.get(0).getUuid().toString());
        } else {
            serviceUUID.setText("");
            charUUID.setText("");
        }
    }
}
