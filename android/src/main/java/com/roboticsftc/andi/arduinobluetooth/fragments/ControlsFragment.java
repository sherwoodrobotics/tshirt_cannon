package com.roboticsftc.andi.arduinobluetooth.fragments;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.roboticsftc.andi.arduinobluetooth.BTActivity;
import com.roboticsftc.andi.arduinobluetooth.Config;
import com.roboticsftc.andi.arduinobluetooth.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by andy on 4/21/17.
 *
 * Layout & buttons for controlling buttons & logging
 */

public class ControlsFragment extends Fragment implements SeekBar.OnSeekBarChangeListener {

    private static final String DEBUG_TAG = "ControlsFragment";

    //Logging and console
    private TextView console = null;
    private Handler logHandler = null, writeHandler = null;

    //Seekbar
    private VerticalSeekBar leftSeek, rightSeek;
    private boolean isLeftDown = false, isRightDown = true;

    //Commands and input
    private Config currentConfig = null;
    private Spinner consoleSpinner;
    private EditText input;
    private final int default_charDelay = 110, default_seekbarDelay = 500;
    private int charDelay = default_charDelay, seekbarDelay = default_seekbarDelay;

    private List<BluetoothGattCharacteristic> characteristics = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.controls_layout, container, false);

        //Get our stuffs
        console = (TextView) v.findViewById(R.id.text_console);
        consoleSpinner = (Spinner) v.findViewById(R.id.spinner_console);
        input = (EditText) v.findViewById(R.id.cmd_input);
        leftSeek = (VerticalSeekBar) v.findViewById(R.id.leftSeek);
        rightSeek = (VerticalSeekBar) v.findViewById(R.id.rightSeek);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //setRetainInstance(true);

        //Recover previous console text
        if (console != null) {
            console.setMovementMethod(new ScrollingMovementMethod());
            if (savedInstanceState != null) console.setText(savedInstanceState.getCharSequence("console", ""));
            else log("Console", "Initiated");
        }
        
        //Setup seekbars - we got them earlier
        leftSeek.setOnSeekBarChangeListener(this);
        leftSeek.setMax(200);
        leftSeek.setProgress(100);
        rightSeek.setOnSeekBarChangeListener(this);
        rightSeek.setMax(200);
        rightSeek.setProgress(100);

        Log.i(DEBUG_TAG, "UI loaded");
    }

    public void updateBluetoothCharacteristics(List<BluetoothGattCharacteristic> characteristics) {
        this.characteristics.clear();
        this.characteristics = characteristics;

        String[] names = new String[characteristics.size()];

        //C #### - S ####
        for (int i = 0; i < characteristics.size(); i++) {
            names[i] = "C " + characteristics.get(i).getUuid().toString().substring(4, 8)
                    + " - S " + characteristics.get(i).getService().getUuid().toString().substring(4, 8);
        }

        if (consoleSpinner != null)
             consoleSpinner.setAdapter(new ArrayAdapter<>(getActivity().getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, names));
    }

    public void setCurrentConfig(Config currentConfig) {
        this.currentConfig = currentConfig;

        if (currentConfig != null) {
            //Reset seekbar
            if (currentConfig.getCommand(R.id.chkResetSeek).equals("true")) {
                leftSeek.setResetOnUp(true);
                rightSeek.setResetOnUp(true);
            } else {
                leftSeek.setResetOnUp(false);
                rightSeek.setResetOnUp(false);
            }

            //Set char and seekbar delay
            if (!currentConfig.getCommand(R.id.char_delay).equals(""))
                charDelay = Integer.parseInt(currentConfig.getCommand(R.id.char_delay));
            if (!currentConfig.getCommand(R.id.seekbar_delay).equals(""))
                seekbarDelay = Integer.parseInt(currentConfig.getCommand(R.id.seekbar_delay));

        }
    }
    public Handler getSendHandler() {
        if (logHandler == null) {
            //Create console logging thread
            HandlerThread thread = new HandlerThread("ConsoleControls");
            thread.start();
            logHandler = new Handler(thread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    //If it's from bluetooth, use that tag, otherwise it comes from internal
                    if (msg.what == BTActivity.MESSAGE_READ)
                        log("Bluetooth", msg.obj.toString());
                    else log("App", msg.obj.toString());
                }
            };
        }
        return logHandler;
    }
    //Where to send commands to
    public void setWriteHandler(Handler handler) {
        writeHandler = handler;
    }

    //Send data / commands
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btn_clear_console:
                clearConsole();
                break;
            case R.id.btn_send_input:
                if (input == null) break;
                final String cmd = input.getText().toString();
                input.setText("");
                if (cmd.equals("")) break;

                Log.i(DEBUG_TAG, "input: " + cmd);


                if (writeHandler != null && consoleSpinner != null && characteristics != null && characteristics.size() > 0) {

                    //We can't use our message send method, we have custom command
                    final BluetoothGattCharacteristic bluetoothGattCharacteristic = characteristics.get(consoleSpinner.getSelectedItemPosition());
                    Message m = writeHandler.obtainMessage(BTActivity.MESSAGE_SEND, charDelay, 0, cmd);
                    Bundle b = m.getData();
                    b.putString("serviceUUID", bluetoothGattCharacteristic.getService().getUuid().toString());
                    b.putString("charUUID", bluetoothGattCharacteristic.getUuid().toString());
                    m.sendToTarget();
                }

                break;
            default:
                sendCommand(v.getId(), -1);
                break;
        }
    }

    //Seekbar tracking
    public void onStartTrackingTouch(final SeekBar seekBar) {
        switch (seekBar.getId()) {
            //We create Thread that send messages every seekbarDelay seconds

            case R.id.leftSeek:

                isLeftDown = true;
                Thread leftSeekThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isLeftDown) {
                            try {
                                Thread.sleep(seekbarDelay);
                            } catch (InterruptedException e) {
                                Log.w("ConsoleFragment", "InterruptedException in leftSeekThread");
                            }
                            sendCommand(R.id.leftSeek, seekBar.getProgress());
                        }
                    }
                });

                leftSeekThread.start();

                break;
            case R.id.rightSeek:

                isRightDown = true;
                Thread rightSeekThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isRightDown) {
                            try {
                                Thread.sleep(seekbarDelay);
                            } catch (InterruptedException e) {
                                Log.w("ConsoleFragment", "InterruptedException in rightSeekThread");
                            }
                            sendCommand(R.id.rightSeek, seekBar.getProgress());
                        }
                    }
                });

                rightSeekThread.start();
                break;
        }
    }
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { }
    public void onStopTrackingTouch(SeekBar seekBar) {
        switch (seekBar.getId()) {
            case R.id.leftSeek:
                isLeftDown = false;
                sendCommand(R.id.leftSeek, seekBar.getProgress()); //Send one final time
                break;

            case R.id.rightSeek:
                isRightDown = false;
                sendCommand(R.id.rightSeek, seekBar.getProgress()); //send one final time
                break;
        }
    }

    //If seekBarProgress > 0, then is seekbar and use number replace override
    private void sendCommand(int id, int seekBarProgress) {
        if (currentConfig != null && writeHandler != null && currentConfig.isValid(id)) {
            String command = currentConfig.getCommand(id);
            if (seekBarProgress >= 0)
                command = command.replace(getResources().getString(R.string.value_rep), ((seekBarProgress - 100) / (double) 100) + "");
            Message m = writeHandler.obtainMessage(BTActivity.MESSAGE_SEND, charDelay, 0, command);
            Bundle b = m.getData();
            b.putString("serviceUUID", currentConfig.getServiceUUID(id));
            b.putString("charUUID", currentConfig.getCharUUID(id));
            m.sendToTarget();
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (console != null) outState.putCharSequence("console", console.getText());
    }

    public void clearConsole() {
        if (console != null) console.setText("");
    }
    public void log(final String caption, final String message) {
        if (message.equals("") || console == null) return;

        if (getActivity() != null) getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                console.append("\n[" + getTimestamp() + "] " + caption.trim() + ": " + message.trim());
            }
        });
    }
    public String getTimestamp() {
        return new SimpleDateFormat("HH:mm:ss:SSS", Locale.US).format(new Date());
    }

}
