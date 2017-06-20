package com.roboticsftc.andi.arduinobluetooth.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;


/**
 * Created by andy on 4/21/17.
 *
 * Manages the tab navbar and holds the 4 tab fragments
 */

public class FragmentTabManager extends FragmentPagerAdapter {
    public ConnectionFragment connectionFragment;
    public ControlsFragment controlsFragment;
    public SettingsFragment settingsFragment;
    public InfoFragment infoFragment;

    public FragmentTabManager(FragmentManager fragmentManager) {
        super(fragmentManager);

        //This will only be called once actually
        connectionFragment = new ConnectionFragment();
        controlsFragment = new ControlsFragment();
        settingsFragment = new SettingsFragment();
        infoFragment = new InfoFragment();
    }

    // Here we can finally safely save a reference to the created
    // Fragment, no matter where it came from (either getItem() or
    // FragmentManger). Simply save the returned Fragment from
    // super.instantiateItem() into an appropriate reference depending
    // on the ViewPager position.

    /*
     * During app orientation change, entire app restarts, losing variables, but
     * Fragments stored in this class are kept somehow and are just recreated magically
     * When they're reinstantiated automatically, we have to get them through this override
     */
    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment createdFragment = (Fragment) super.instantiateItem(container, position);

        switch (position) {
            case 0:
                connectionFragment = (ConnectionFragment) createdFragment;
                break;
            case 1:
                controlsFragment = (ControlsFragment) createdFragment;
                break;
            case 2:
                settingsFragment = (SettingsFragment) createdFragment;
                break;
            case 3:
                infoFragment = (InfoFragment) createdFragment;
                break;
        }
        return createdFragment;
    }

    @Override
    public Fragment getItem(int position) {

        // Only called initially so this is fine
        switch (position) {
            case 0:
                return connectionFragment;
            case 1:
                return controlsFragment;
            case 2:
                return settingsFragment;
            case 3:
                return infoFragment;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 4;
    }
}