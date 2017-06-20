package com.roboticsftc.andi.arduinobluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.roboticsftc.andi.arduinobluetooth.fragments.FragmentTabManager;

import java.util.ArrayList;

/**
 * Created by andy on 4/21/17.
 *
 * Overarching UI activity for the app, while
 * all BT-related stuff is contained in BTFragment
 * Each tab has its own Fragment (4 in total)
 */

public class MainActivity extends AppCompatActivity implements BLFragment.BluetoothReporter {

    //Constant Strings, not defined in strings.xml (that's only for UI)
    private static final String DEBUG_TAG = "MainActivity";
    private static final String tab_tag = "tab";
    private static final String selectedConfig = "selected_config";

    //Related to the 4 tabs
    private FragmentTabManager tabs;
    private BLFragment blFragment;
    private TabLayout tabNavigator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tabs = new FragmentTabManager(getSupportFragmentManager());

        //Set up the tab viewport to see the tabs
        final ViewPager tabViewport = (ViewPager) this.findViewById(R.id.tab_viewport);
        tabViewport.setAdapter(tabs);
        tabViewport.setOffscreenPageLimit(tabs.getCount()); //Fragments won't be recreated
        tabViewport.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true;
            }
        });

        //Set up the tab navbar at the top
        tabNavigator = (TabLayout) findViewById(R.id.tab_navigation);
        tabNavigator.setTabGravity(TabLayout.GRAVITY_FILL);
        tabNavigator.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //When tab chosen at top, set tabViewport to go to that tab
                tabViewport.setCurrentItem(tabNavigator.getSelectedTabPosition());
            }
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 2) {
                    //When switching off of the Settings tab, get the current config to use
                    tabs.settingsFragment.saveEditedConfig();
                    tabs.controlsFragment.setCurrentConfig(tabs.settingsFragment.getSelectedConfig());
                }
            }
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        if (savedInstanceState != null && savedInstanceState.getInt(tab_tag, -1) != -1) {
            //App changed orientation, so we will restore what was set before

            Log.i(DEBUG_TAG, "Recovering previous tab selection and config selection");

            //Previously selected tab
            int selection = savedInstanceState.getInt(tab_tag);
            if (tabNavigator.getTabAt(selection) != null) tabNavigator.getTabAt(selection).select();

            //Previously selected config
            tabs.settingsFragment.setSelectedConfigNumerical(savedInstanceState.getInt(selectedConfig));
            tabs.controlsFragment.setCurrentConfig(tabs.settingsFragment.getSelectedConfig());
        }

        //blFragment is not destroyed when we change orientation, so we use FragmentManager to find it if we have one already
        blFragment = (BLFragment) getSupportFragmentManager().findFragmentByTag("BL_FRAGMENT");
        if (blFragment == null) {
            blFragment = new BLFragment();
            getSupportFragmentManager().beginTransaction().add(blFragment, "BL_FRAGMENT").commitAllowingStateLoss();
        }

        Log.i(DEBUG_TAG, "UI loaded");
    }

    protected void onSaveInstanceState(Bundle savedInstanceState) {
        //App changed orientation, so we will restore what was set before

        savedInstanceState.putInt(tab_tag, tabNavigator.getSelectedTabPosition());
        savedInstanceState.putInt(selectedConfig, tabs.settingsFragment.getSelectedConfigNumerical());
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void deviceConnected(BluetoothGatt device) {
        //Get valid characteristics - anything with "1800" in the service UUID can be ignored
        ArrayList<BluetoothGattCharacteristic> characteristicList = new ArrayList<>();
        if (device != null) {
            for (BluetoothGattService service : device.getServices()) {
                if (service.getUuid().toString().contains("1800")) continue; //1800 blacklist
                for (BluetoothGattCharacteristic c : service.getCharacteristics()) characteristicList.add(c);
            }
        }

        Log.i(DEBUG_TAG, "Updating UI & fragments with connected device");

        //Update UI with newly connected device
        tabs.controlsFragment.setWriteHandler(blFragment.getWriteHandler());
        tabs.connectionFragment.updateBluetooth(device);
        tabs.connectionFragment.updateCharacteristics(characteristicList);
        tabs.controlsFragment.updateBluetoothCharacteristics(characteristicList);
        tabs.settingsFragment.updateBluetoothCharacteristics(characteristicList);
    }

    @Override
    public void characteristicRead(String message) {
        //When message sent from BT to device
        tabs.controlsFragment.log("Bluetooth", message);
    }

    @Override
    public void updateDeviceList(ArrayList<BluetoothDevice> devices) {
        //For listing potential devices to connect to
        tabs.connectionFragment.updateDeviceList(devices);
    }

    public void onPause() {
        super.onPause();
        if (isFinishing()) {
            /*
             * All apps restart when orientation changes for whatever reason
             * We have to check if we're actually closing out of the app before saving
             */
            Log.i(DEBUG_TAG, "Finishing - closing configs");
            blFragment.closeConnection();
        }
    }

    //Redirect buttons on the Connection tab
    public void onConnectionsClick(View v) {
        switch(v.getId()) {
            case R.id.btn_bluetooth_connect:
                blFragment.connectToDevice(tabs.connectionFragment.getSelectedDevice());
                break;
            case R.id.btn_disconnect:
                blFragment.closeConnection();
                break;
            case R.id.btn_bluetooth_settings:
                blFragment.turnOnBTSettings();
                break;
            case R.id.btn_location_settings:
                blFragment.turnOnLocationServices();
                break;
            case R.id.btn_rescan:
                //This initiates scan
                if (blFragment.scanBTLEDevices()) tabs.connectionFragment.clearDevices();
                break;

        }
    }

    //Redirect buttons on the Controls and Settings tab (let the respective Fragment deal with it)
    public void onControlsClick(View v) {
        tabs.controlsFragment.onClick(v);
    }
    public void onSettingsClick(View v) {
        tabs.settingsFragment.onClick(v);
    }
}
