package com.roboticsftc.andi.arduinobluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.roboticsftc.andi.arduinobluetooth.fragments.FragmentTabManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by andy on 4/21/17.
 *
 * Main UI activity for the app
 * All BT-related stuff is contained in BTActivity,
 * which this Activity extends
 */

public class MainActivity extends BTActivity {

    private static final String DEBUG_TAG = "MainActivity";

    //Each of the four tabs are held in here
    private FragmentTabManager tabManager;

    private List<BluetoothGattCharacteristic> characteristicList = new ArrayList<>();

    @Override
    protected void activityCreated(Bundle savedInstanceState) {

        setContentView(R.layout.activity_main);

        //Set up the tab viewport to see the tabs
        final ViewPager tabViewport = (ViewPager) this.findViewById(R.id.tab_viewport);
        tabManager = new FragmentTabManager(getSupportFragmentManager());
        tabViewport.setAdapter(tabManager);
        tabViewport.setOffscreenPageLimit(tabManager.getCount()); //Fragments won't be recreated
        tabViewport.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        //Set up the tab navbar at the top
        final TabLayout tabNavigator = (TabLayout) findViewById(R.id.tab_navigation);
        tabNavigator.setTabGravity(TabLayout.GRAVITY_FILL);
        tabNavigator.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //When tab chosen at top, set tabViewport to go to that tab
                tabViewport.setCurrentItem(tabNavigator.getSelectedTabPosition());
            }
            public void onTabUnselected(TabLayout.Tab tab) {

                if (tab.getPosition() == 2) {
                    //Settings tab - save and set config
                    Log.i(DEBUG_TAG, "Auto-saving configuration");
                    tabManager.settingsFragment.saveEditedConfig();
                    tabManager.controlsFragment.setCurrentConfig(tabManager.settingsFragment.getSelectedConfig());
                }

            }
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        //TODO make tabViewport tab == tabNavigator selection
        try {
            tabNavigator.getTabAt(tabViewport.getCurrentItem()).select();
        } catch (NullPointerException npe) {}

        //Don't forget this - for logging to console
        addHandler(tabManager.controlsFragment.getSendHandler());

        Log.i(DEBUG_TAG, "UI loaded");
    }

    @Override
    protected void deviceConnected(BluetoothGatt device) {
        //Get valid characteristics
        characteristicList.clear();
        if (device != null) {
            for (BluetoothGattService service : device.getServices()) {
                if (service.getUuid().toString().contains("1800")) continue; //1800 blacklist
                for (BluetoothGattCharacteristic c : service.getCharacteristics()) characteristicList.add(c);
            }
        }

        Log.i(DEBUG_TAG, "Updating UI & fragments with connected device");
        //Update the UI

        //Configure connection in other fragments
        tabManager.controlsFragment.setWriteHandler(getWriteHandler());
        tabManager.connectionFragment.updateBluetooth(device);
        tabManager.connectionFragment.updateCharacteristics(characteristicList);
        tabManager.controlsFragment.updateBluetoothCharacteristics(characteristicList);
        tabManager.settingsFragment.updateBluetoothCharacteristics(characteristicList);
    }

    @Override
    protected void updateDeviceList(BluetoothDevice device) {
        //For connecting
        tabManager.connectionFragment.addDevice(device);
    }

    public void onDestroy() {
        //Close connection on destroy
        super.onDestroy();
        if (isFinishing()) {
            /*
             * All apps restart when orientation changes for whatever reason
             * We have to check if we're actually closing out of the app before saving
             */
            Log.i(DEBUG_TAG, "Finishing - commiting configs to memory");
            closeConnection();
            tabManager.settingsFragment.commitConfigs();
        }
    }

    //Buttons on the Connection tab
    public void onConnectionsClick(View v) {
        switch(v.getId()) {
            case R.id.btn_bluetooth_connect:
                connectToDevice(tabManager.connectionFragment.getSelectedDevice());
                break;
            case R.id.btn_disconnect:
                closeConnection();
                break;
            case R.id.btn_bluetooth_settings:
                turnOnBTSettings(); //In BTActivity
                break;
            case R.id.btn_location_settings:
                turnOnLocationServices(); //In BTActivity
                break;
            case R.id.btn_rescan:
                if (scanBTLEDevices()) tabManager.connectionFragment.clearDevices();
                break;

        }
    }

    //Buttons on the Controls tab - direct reroute
    public void onControlsClick(View v) {
        tabManager.controlsFragment.onClick(v);
    }
    //Buttons on the Settings tab - direct reroute
    public void onSettingsClick(View v) {
        tabManager.settingsFragment.onClick(v);
    }

}
