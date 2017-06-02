package com.roboticsftc.andi.arduinobluetooth.fragments;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;


/**
 * Created by andy on 4/21/17.
 *
 * Manages the tab navbar by redirecting to new fragments
 * Also redirects methods to each fragment
 */

public class FragmentTabManager extends FragmentStatePagerAdapter {

    private static final String
        connection_tag = "Connection",
        controls_tag = "Controls",
        settings_tag = "Settings",
        info_tag = "Info";
    
    public ConnectionFragment connectionFragment;
    public ControlsFragment controlsFragment;
    public SettingsFragment settingsFragment;
    public InfoFragment infoFragment;

    public FragmentTabManager(FragmentManager fragmentManager) {
        super(fragmentManager);

        connectionFragment = (ConnectionFragment) fragmentManager.findFragmentByTag(connection_tag);
        if (connectionFragment == null) {
            connectionFragment = new ConnectionFragment();
            connectionFragment.setRetainInstance(true);
            fragmentManager.beginTransaction().add(connectionFragment, connection_tag).commit();
        }
        controlsFragment = (ControlsFragment) fragmentManager.findFragmentByTag(controls_tag);
        if (controlsFragment == null) {
            controlsFragment = new ControlsFragment();
            controlsFragment.setRetainInstance(true);
            fragmentManager.beginTransaction().add(controlsFragment, controls_tag).commit();
        }
        settingsFragment = (SettingsFragment) fragmentManager.findFragmentByTag(settings_tag);
        if (settingsFragment == null) {
            settingsFragment = new SettingsFragment();
            settingsFragment.setRetainInstance(true);
            fragmentManager.beginTransaction().add(settingsFragment, settings_tag).commit();
        }
        infoFragment = (InfoFragment) fragmentManager.findFragmentByTag(info_tag);
        if (infoFragment == null) {
            infoFragment = new InfoFragment();
            infoFragment.setRetainInstance(true);
            fragmentManager.beginTransaction().add(infoFragment, info_tag).commit();
        }
    }

    public void onDestroy(FragmentManager manager) {
        //Get rid of these saves - memory leaks

        manager.beginTransaction().remove(connectionFragment).remove(controlsFragment)
                .remove(settingsFragment).remove(infoFragment).commit();

    }

    @Override
    public Fragment getItem(int position) {

        // Only called initially
        switch (position) {
            case 0:
                connectionFragment = new ConnectionFragment();
                connectionFragment.setRetainInstance(true);
                return connectionFragment;
            case 1:
                controlsFragment = new ControlsFragment();
                controlsFragment.setRetainInstance(true);
                return controlsFragment;
            case 2:
                settingsFragment = new SettingsFragment();
                settingsFragment.setRetainInstance(true);
                return settingsFragment;
            case 3:
                return new InfoFragment();
            default:
                return null;
        }
    }
/*
    @Override
    public Object instantiateItem(ViewGroup vg, int position) {
        Object fragment = super.instantiateItem(vg, position);

        if (fragment instanceof ConnectionFragment) connectionFragment = (ConnectionFragment) fragment;
        if (fragment instanceof ControlsFragment) controlsFragment = (ControlsFragment) fragment;
        if (fragment instanceof SettingsFragment) settingsFragment = (SettingsFragment) fragment;
        if (fragment instanceof InfoFragment) infoFragment = (InfoFragment) fragment;

        return fragment;
    }
*/
    @Override
    public int getCount() {
        return 4;
    }
}