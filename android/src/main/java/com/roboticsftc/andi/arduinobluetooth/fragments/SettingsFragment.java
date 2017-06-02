package com.roboticsftc.andi.arduinobluetooth.fragments;

import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.roboticsftc.andi.arduinobluetooth.Config;
import com.roboticsftc.andi.arduinobluetooth.R;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by andy on 4/21/17.
 *
 * Layout & buttons for controlling buttons & logging
 */

public class SettingsFragment extends Fragment {

    private static final String DEBUG_TAG = "SettingsFragment";

    private static final String file_save = "data.ser";

    //Spinners on the config page
    private Spinner centerSpinner, upSpinner, downSpinner, leftSpinner, rightSpinner,
            aSpinner, bSpinner, cSpinner, oneSpinner, twoSpinner, threeSpinner,
            leftSeekSpinner, rightSeekSpinner;

    //EditTexts on the config page
    private EditText centerEditText, upEditText, downEditText, leftEditText, rightEditText,
            aEditText, bEditText, cEditText, oneEditText, twoEditText, threeEditText,
            leftSeekEditText, rightSeekEditText,
            charDelayEditText, seekbarDelayEditText;

    //Other stuff on the page
    private CheckBox resetSeek;
    private Spinner configSelect;

    //Configuration selection stuff
    private List<Config> configs = new ArrayList<>();
    private int selectedConfig = -1; //Anything not in the configs list is considered null
    private List<BluetoothGattCharacteristic> characteristics = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View masterLayout = inflater.inflate(R.layout.settings_layout, container, false);

        //Let's get our stuff

        configSelect = (Spinner) masterLayout.findViewById(R.id.config_spinner);
        resetSeek = (CheckBox) masterLayout.findViewById(R.id.chkResetSeek);

        centerSpinner = (Spinner) masterLayout.findViewById(R.id.spinner_center);
        upSpinner = (Spinner) masterLayout.findViewById(R.id.spinner_up_arrow);
        downSpinner = (Spinner) masterLayout.findViewById(R.id.spinner_down_arrow);
        leftSpinner = ((Spinner) masterLayout.findViewById(R.id.spinner_left_arrow));
        rightSpinner = (Spinner) masterLayout.findViewById(R.id.spinner_right_arrow);
        oneSpinner = (Spinner) masterLayout.findViewById(R.id.spinner_1);
        twoSpinner = (Spinner) masterLayout.findViewById(R.id.spinner_2);
        threeSpinner = (Spinner) masterLayout.findViewById(R.id.spinner_3);
        aSpinner = (Spinner) masterLayout.findViewById(R.id.spinner_a);
        bSpinner = (Spinner) masterLayout.findViewById(R.id.spinner_b);
        cSpinner = (Spinner) masterLayout.findViewById(R.id.spinner_c);
        leftSeekSpinner = (Spinner) masterLayout.findViewById(R.id.spinner_left_seek);
        rightSeekSpinner = (Spinner) masterLayout.findViewById(R.id.spinner_right_seek);

        centerEditText = (EditText) masterLayout.findViewById(R.id.cmd_center_icon);
        upEditText = (EditText) masterLayout.findViewById(R.id.cmd_up_arrow);
        downEditText = (EditText) masterLayout.findViewById(R.id.cmd_down_arrow);
        rightEditText = (EditText) masterLayout.findViewById(R.id.cmd_right_arrow);
        leftEditText = (EditText) masterLayout.findViewById(R.id.cmd_left_arrow);
        oneEditText = (EditText) masterLayout.findViewById(R.id.cmd_one);
        twoEditText = (EditText) masterLayout.findViewById(R.id.cmd_two);
        threeEditText = (EditText) masterLayout.findViewById(R.id.cmd_three);
        aEditText = (EditText) masterLayout.findViewById(R.id.cmd_a);
        bEditText = (EditText) masterLayout.findViewById(R.id.cmd_b);
        cEditText = (EditText) masterLayout.findViewById(R.id.cmd_c);
        leftSeekEditText = (EditText) masterLayout.findViewById(R.id.cmd_left_seek);
        rightSeekEditText = (EditText) masterLayout.findViewById(R.id.cmd_right_seek);
        charDelayEditText = (EditText) masterLayout.findViewById(R.id.char_delay);
        seekbarDelayEditText = (EditText) masterLayout.findViewById(R.id.seekbar_delay);

        return masterLayout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Update all TextViews on configSelect change
        configSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedConfig = (position >= configs.size()) ? -1 : position; //"New Profile..." will return a -1
                updateScreenForSelectedConfig();
            }
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        //Load up configs and update
        configs = getAvailableConfigs();
        updateConfigSpinner();

        Log.i(DEBUG_TAG, "UI loaded");
    }

    //Update the Adapter on the configSelect Spinner and selects the first on the list
    public void updateConfigSpinner() {
        String[] names = new String[configs.size() + 1];
        if (configs.size() >= 1) for (int i = 0; i < configs.size(); i++) names[i] = configs.get(i).getName();
        names[names.length - 1] = getActivity().getResources().getString(R.string.new_profile); //Tack this on as the last one

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity().getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, names);
        configSelect.setAdapter(adapter);

        //Set selected config to 0 if no more on Spinner
        if (selectedConfig < 0 || selectedConfig > names.length) selectedConfig = 0;
        configSelect.setSelection(selectedConfig);
        updateScreenForSelectedConfig();

        Log.d(DEBUG_TAG, "Config spinner updated");
    }

    //We've received the list of characteristics to use
    public void updateBluetoothCharacteristics(List<BluetoothGattCharacteristic> characteristics) {
        this.characteristics.clear();
        this.characteristics = characteristics;

        String[] names;

        if (characteristics.size() > 0) {
            names= new String[characteristics.size() + 1];
            names[0] = "----";

            for (int i = 0; i < characteristics.size(); i++) {
                //C #### - S ####
                names[i + 1] = "C " + characteristics.get(i).getUuid().toString().substring(4, 8)
                        + " - S " + characteristics.get(i).getService().getUuid().toString().substring(4, 8);
            }

        } else names = new String[0];

        //UUID adapter is the same for all of them
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity().getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, names);
        centerSpinner.setAdapter(adapter);
        upSpinner.setAdapter(adapter);
        downSpinner.setAdapter(adapter);
        leftSpinner.setAdapter(adapter);
        rightSpinner.setAdapter(adapter);
        oneSpinner.setAdapter(adapter);
        twoSpinner.setAdapter(adapter);
        threeSpinner.setAdapter(adapter);
        aSpinner.setAdapter(adapter);
        bSpinner.setAdapter(adapter);
        cSpinner.setAdapter(adapter);
        leftSeekSpinner.setAdapter(adapter);
        rightSeekSpinner.setAdapter(adapter);

        updateScreenForSelectedConfig();

        Log.d(DEBUG_TAG, "Bluetooth characteristics set");
    }

    public Config getSelectedConfig() {
        if (selectedConfig < 0 || selectedConfig >= configs.size()) return null;
        return configs.get(selectedConfig);
    }

    //Saves the current edits to the selected config on the spinner
    public void saveEditedConfig() {

        //Guarantee an existing config to write to
        if (getSelectedConfig() == null) {

            View alertLayout = LayoutInflater.from(getContext()).inflate(R.layout.name_prompt_layout, (ViewGroup) getView(), false);
            final EditText input = (EditText) alertLayout.findViewById(R.id.profile_name);
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setView(alertLayout).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Config c = new Config(input.getText().toString());
                            updateConfig(c);
                            configs.add(c);
                            selectedConfig = configs.indexOf(c);
                            updateConfigSpinner();
                        }
                    });
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    updateConfigSpinner();
                }
            });
            builder.show();

        } else {
            updateConfig(getSelectedConfig());
        }

        Log.i(DEBUG_TAG, "Current edits saved");
    }

    //Updates the given Config with the current values
    private void updateConfig(Config c) {

        //Update the given config with the values set on screen
        c.setCommand(R.id.btn_center, centerEditText.getText().toString(), getServiceUUIDFromInput(centerSpinner), getCharUUIDFromInput(centerSpinner));
        c.setCommand(R.id.btn_up, upEditText.getText().toString(), getServiceUUIDFromInput(upSpinner), getCharUUIDFromInput(upSpinner));
        c.setCommand(R.id.btn_down, downEditText.getText().toString(), getServiceUUIDFromInput(downSpinner), getCharUUIDFromInput(downSpinner));
        c.setCommand(R.id.btn_right, rightEditText.getText().toString(), getServiceUUIDFromInput(rightSpinner), getCharUUIDFromInput(rightSpinner));
        c.setCommand(R.id.btn_left, leftEditText.getText().toString(), getServiceUUIDFromInput(leftSpinner), getCharUUIDFromInput(leftSpinner));
        c.setCommand(R.id.btn_1, oneEditText.getText().toString(), getServiceUUIDFromInput(oneSpinner), getCharUUIDFromInput(oneSpinner));
        c.setCommand(R.id.btn_2, twoEditText.getText().toString(), getServiceUUIDFromInput(twoSpinner), getCharUUIDFromInput(twoSpinner));
        c.setCommand(R.id.btn_3, threeEditText.getText().toString(), getServiceUUIDFromInput(threeSpinner), getCharUUIDFromInput(threeSpinner));
        c.setCommand(R.id.btn_a, aEditText.getText().toString(), getServiceUUIDFromInput(aSpinner), getCharUUIDFromInput(aSpinner));
        c.setCommand(R.id.btn_b, bEditText.getText().toString(), getServiceUUIDFromInput(bSpinner), getCharUUIDFromInput(bSpinner));
        c.setCommand(R.id.btn_c, cEditText.getText().toString(), getServiceUUIDFromInput(cSpinner), getCharUUIDFromInput(cSpinner));
        c.setCommand(R.id.leftSeek, leftSeekEditText.getText().toString(), getServiceUUIDFromInput(leftSeekSpinner), getCharUUIDFromInput(leftSeekSpinner));
        c.setCommand(R.id.rightSeek, rightSeekEditText.getText().toString(), getServiceUUIDFromInput(rightSeekSpinner), getCharUUIDFromInput(rightSeekSpinner));
        c.setCommand(R.id.chkResetSeek, (resetSeek.isChecked()) ? "true" : "false", "", "");
        c.setCommand(R.id.char_delay, charDelayEditText.getText().toString(), "", "");
        c.setCommand(R.id.seekbar_delay, seekbarDelayEditText.getText().toString(), "", "");
    }

    //Updates the screen to display the given config
    private void updateScreenForSelectedConfig() {

        Config config = getSelectedConfig();
        if (config == null) config = new Config(""); //Blank will just return a lot of ""

        centerEditText.setText(config.getCommand(R.id.btn_center));
        upEditText.setText(config.getCommand(R.id.btn_up));
        downEditText.setText(config.getCommand(R.id.btn_down));
        rightEditText.setText(config.getCommand(R.id.btn_right));
        leftEditText.setText(config.getCommand(R.id.btn_left));
        oneEditText.setText(config.getCommand(R.id.btn_1));
        twoEditText.setText(config.getCommand(R.id.btn_2));
        threeEditText.setText(config.getCommand(R.id.btn_3));
        aEditText.setText(config.getCommand(R.id.btn_a));
        bEditText.setText(config.getCommand(R.id.btn_b));
        cEditText.setText(config.getCommand(R.id.btn_c));
        leftSeekEditText.setText(config.getCommand(R.id.leftSeek));
        rightSeekEditText.setText(config.getCommand(R.id.rightSeek));
        charDelayEditText.setText(config.getCommand(R.id.char_delay));
        seekbarDelayEditText.setText(config.getCommand(R.id.seekbar_delay));

        if (characteristics != null && characteristics.size() > 0) {
            //Not finding the right selection returns -1, and so adding 1 = 0

            centerSpinner.setSelection( 1 + getSelectedCharacteristicForUUIDs(config.getServiceUUID(R.id.btn_center), config.getCharUUID(R.id.btn_center)));
            upSpinner.setSelection( 1 + getSelectedCharacteristicForUUIDs(config.getServiceUUID(R.id.btn_up), config.getCharUUID(R.id.btn_up)));
            downSpinner.setSelection( 1 + getSelectedCharacteristicForUUIDs(config.getServiceUUID(R.id.btn_down), config.getCharUUID(R.id.btn_down)));
            leftSpinner.setSelection( 1 + getSelectedCharacteristicForUUIDs(config.getServiceUUID(R.id.btn_left), config.getCharUUID(R.id.btn_right)));
            rightSpinner.setSelection( 1 + getSelectedCharacteristicForUUIDs(config.getServiceUUID(R.id.btn_right), config.getCharUUID(R.id.btn_right)));
            oneSpinner.setSelection( 1 + getSelectedCharacteristicForUUIDs(config.getServiceUUID(R.id.btn_1), config.getCharUUID(R.id.btn_1)));
            twoSpinner.setSelection( 1 + getSelectedCharacteristicForUUIDs(config.getServiceUUID(R.id.btn_2), config.getCharUUID(R.id.btn_2)));
            threeSpinner.setSelection( 1 + getSelectedCharacteristicForUUIDs(config.getServiceUUID(R.id.btn_3), config.getCharUUID(R.id.btn_3)));
            aSpinner.setSelection( 1 + getSelectedCharacteristicForUUIDs(config.getServiceUUID(R.id.btn_a), config.getCharUUID(R.id.btn_a)));
            bSpinner.setSelection( 1 + getSelectedCharacteristicForUUIDs(config.getServiceUUID(R.id.btn_b), config.getCharUUID(R.id.btn_b)));
            cSpinner.setSelection( 1 + getSelectedCharacteristicForUUIDs(config.getServiceUUID(R.id.btn_c), config.getCharUUID(R.id.btn_c)));
            leftSeekSpinner.setSelection( 1 + getSelectedCharacteristicForUUIDs(config.getServiceUUID(R.id.leftSeek), config.getCharUUID(R.id.leftSeek)));
            rightSeekSpinner.setSelection( 1 + getSelectedCharacteristicForUUIDs(config.getServiceUUID(R.id.rightSeek), config.getCharUUID(R.id.rightSeek)));
        }

        Log.d(DEBUG_TAG, "Screen updated for config " + config.getName());
    }

    //Delete the selected config (if they're on the new profile then this does nothing)
    public void deleteConfig() {
        if (getSelectedConfig() != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Are you sure you want to delete config " + getSelectedConfig().getName() + "?").setTitle("Delete config")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            String configName = configs.get(selectedConfig).getName();
                            configs.remove(selectedConfig);
                            selectedConfig = selectedConfig - 1;
                            updateConfigSpinner();

                            Log.d(DEBUG_TAG, "Config " + configName + " deleted");
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            // Create the AlertDialog object and return it
            builder.create().show();
        }
    }

    //Gets the selected BluetoothGattCharacteristic UUID String based on the selected Spinner value
    private String getCharUUIDFromInput(Spinner s) {
        if (characteristics == null || characteristics.size() == 0) return "";
        int i = s.getSelectedItemPosition() - 1;
        if (i < 0) return "null"; //Return null to overwrite previous value if existing
        return characteristics.get(i).getUuid().toString();
    }

    //Gets the selected BluetoothGattService UUID String based on the selected Spinner value
    private String getServiceUUIDFromInput(Spinner s) {
        if (characteristics == null || characteristics.size() == 0) return "";
        int i = s.getSelectedItemPosition() - 1;
        if (i < 0) return "null"; //Return null to overwrite previous value if existing
        return characteristics.get(i).getService().getUuid().toString();
    }

    //Gets the proper characteristic, given the UUIDs
    private int getSelectedCharacteristicForUUIDs(String serviceUUID, String charUUID) {

        if (characteristics == null || characteristics.size() <= 0) return -1;
        if (serviceUUID.equals("") || charUUID.equals("")) return -1;

        for (BluetoothGattCharacteristic c : characteristics)
            if (c.getUuid().equals(UUID.fromString(charUUID)) &&
                    c.getService().getUuid().equals(UUID.fromString(serviceUUID))) return characteristics.indexOf(c);

        return -1;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_save_config:
                saveEditedConfig();
                break;
            case R.id.btn_delete_config:
                deleteConfig();
                break;
        }
    }

    //Call this as little as you can - reads from save
    private List<Config> getAvailableConfigs() {

        FileInputStream fis = null;
        ObjectInputStream is = null;

        try {
            fis = getContext().openFileInput(file_save);
            is = new ObjectInputStream(fis);
            return (List<Config>) is.readObject();
        } catch (FileNotFoundException e) {
            Log.e(DEBUG_TAG, "FileNotFound when loading; probably never created");
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "IOException on loading", e);
        } catch (ClassNotFoundException e) {
            Log.e(DEBUG_TAG, "ClassNotFound; probably cast exception", e);
        } finally {
            if (is != null) try {
                is.close();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "IOException closing", e);
            }
            if (fis != null) try {
                fis.close();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "IOException closing", e);
            }
        }

        return new ArrayList<>();
    }

    //Save the available configs to memory - run only onDestroy or onStop
    public void commitConfigs() {

        FileOutputStream fos = null;
        ObjectOutputStream os = null;
        try {
            fos = getContext().openFileOutput(file_save, Context.MODE_PRIVATE);
            os = new ObjectOutputStream(fos);
            os.writeObject(configs);
            os.close();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.i(DEBUG_TAG, "FileNotFound when saving; probably never created");
        } catch (IOException e) {
            Log.i(DEBUG_TAG, "IOException when saving");
            e.printStackTrace();
        } finally {
            if (os != null) try {
                os.close();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "IOException closing", e);
            }
            if (fos != null) try {
                fos.close();
            } catch (IOException e) {
                Log.e(DEBUG_TAG, "IOException closing", e);
            }
        }
    }

}
