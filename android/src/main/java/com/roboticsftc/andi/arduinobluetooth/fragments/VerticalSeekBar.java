package com.roboticsftc.andi.arduinobluetooth.fragments;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by andy on 4/24/17.
 *
 * Custom Vertical SeekBar for the UI
 * Can set whether to reset value on up
 */
public class VerticalSeekBar extends AppCompatSeekBar {

    private boolean resetOnUp = false;
    private OnSeekBarChangeListener listener = null; //We keep a copy of the listener

    //Overridden constructors and method
    public VerticalSeekBar(Context context) {
        super(context);
    }
    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }
    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    protected void onSizeChanged(int w, int h, int oldw, int oldh) { super.onSizeChanged(h, w, oldh, oldw); }

    public void setResetOnUp(boolean resetOnUp) {
        this.resetOnUp = resetOnUp;
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        super.setOnSeekBarChangeListener(listener);
        this.listener = listener;
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //Flip horizontal and vertical measurements to get vertical
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    @Override
    protected void onDraw(Canvas c) {
        //Refactor before sending to super for drawing onto
        c.rotate(-90);
        c.translate(-getHeight(), 0);
        super.onDraw(c);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        //Returning true/false means we have dealt with it

        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                //Update self every time moved
                setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
                onSizeChanged(getWidth(), getHeight(), 0, 0);
                if (event.getAction() == MotionEvent.ACTION_DOWN && listener != null) listener.onStartTrackingTouch(this);
                break;
            case MotionEvent.ACTION_UP:
                //If resetOnUp, 0 the spinner, else keep it there
                if (resetOnUp) {
                    setProgress(getMax() / 2);
                    onSizeChanged(getWidth(), getHeight(), 0, 0);
                } else {
                    setProgress(getMax() - (int) (getMax() * event.getY() / getHeight()));
                    onSizeChanged(getWidth(), getHeight(), 0, 0);
                }
                if (listener != null) listener.onStopTrackingTouch(this);
                break;
        }
        return true;
    }
}

