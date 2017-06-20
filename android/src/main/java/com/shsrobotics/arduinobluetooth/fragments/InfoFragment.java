package com.shsrobotics.arduinobluetooth.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.roboticsftc.andi.arduinobluetooth.R;

/**
 * Created by andy on 4/21/17.
 *
 * Layout for info display (tab 4)
 */

public class InfoFragment extends Fragment {

    private TextView textInfo;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.info_layout, container, false);

        //Get our info view
        textInfo = (TextView) v.findViewById(R.id.text_info);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        //Set up scrollable text
        textInfo.setText(getResources().getString(R.string.info_all));
        textInfo.setMovementMethod(new ScrollingMovementMethod());

    }
}
