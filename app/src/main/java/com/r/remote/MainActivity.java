package com.r.remote;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.HashMap;

public class MainActivity extends Activity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final byte LEFT_STICK = 1;
    private static final byte RIGHT_STICK = 2;

    private ConnectionManager.ConnectedThread connectedThread;

    private final View.OnTouchListener leftOnTouchListener = new View.OnTouchListener() {
        private int[] originalViewPosition = new int[2];
        private int[] previousViewPosition = new int[2];
        private int[] previousEventPosition = new int[2];
        private HashMap<Object, Integer> currentSpeed = new HashMap<>(2);
        private HashMap<Object, Integer> pointerIds = new HashMap<>();
        private int parentTop;
        private int parentBottom;
        private int workingLength;
        private final int SPEED_STEPS = 25;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            Object stick = v.getTag();
            String TAG = "STICK/" + stick.toString() + "/" + event.getAction();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    ConstraintLayout parent = (ConstraintLayout)v.getParent();
                    parentTop = parent.getTop();
                    parentBottom = parent.getBottom();
                    workingLength = (int)(v.getY() - parentTop);
                    try {
                        int pointerId = event.getPointerId(0);
                        pointerIds.put(stick, pointerId);
                        previousEventPosition[pointerId] = (int)(event.getY() + v.getY());
                        originalViewPosition[pointerId] = previousViewPosition[pointerId] = v.getTop();
                        Log.d(TAG, String.format("Pointer ID: %d; Event Y: %d; Stick Y: %d",
                                pointerIds.get(stick),
                                previousEventPosition[pointerId],
                                previousViewPosition[pointerId]));
                    } catch (Exception exc) {
                        Log.e(TAG, exc.getMessage());
                        exc.printStackTrace();
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    try {
                        int pointerId = pointerIds.get(stick);
                        int pointerIndex = event.findPointerIndex(pointerId);
                        /// absolute position of the event is a sum of event position relative to
                        /// view where it was fired and position of view itself
                        int currentEventPosition = (int)(event.getY(pointerIndex) + v.getY());
                        int currentViewPosition = previousViewPosition[pointerId] - previousEventPosition[pointerId] + currentEventPosition;
                        Log.d(TAG, String.format("previousEventPosition: %d; previousViewPosition: %d",
                                previousEventPosition[pointerId], previousViewPosition[pointerId]));
                        Log.d(TAG, String.format("currentEventPosition: %d; currentViewPosition: %d",
                                currentEventPosition, currentViewPosition));
                        if (currentViewPosition < parentTop) {
                            currentViewPosition = parentTop;
                        }
                        if (currentViewPosition > parentBottom - v.getHeight()) {
                            currentViewPosition = parentBottom - v.getHeight();
                        }

                        v.setY(currentViewPosition);
                        int newSpeed = Math.abs(Math.round(SPEED_STEPS*(originalViewPosition[pointerId] - currentViewPosition) / workingLength));
                        Log.d(TAG, String.format("Speed is %d", newSpeed));
                        if (newSpeed != currentSpeed.getOrDefault(stick, 0)) {
                            currentSpeed.put(stick, newSpeed);
                            //connectedThread.sendSpeed(stick.toString(), stickMovesUp.get(stick), newSpeed);
                        }                        previousEventPosition[pointerId] = currentEventPosition;
                        previousViewPosition[pointerId] = currentViewPosition;
                    } catch (Exception exc) {
                        Log.e(TAG, exc.getMessage());
                        exc.printStackTrace();
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    v.setY(originalViewPosition[pointerIds.get(stick)]);
                    currentSpeed.put(stick, 0);
                    //connectedThread.sendSpeed(stick.toString(), false, 0);
                    break;
            }
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //enableBluetooth();

        View leftStick = findViewById(R.id.leftArea);
        leftStick.setOnTouchListener(leftOnTouchListener);
        View rightStick = findViewById(R.id.rightArea);
        rightStick.setOnTouchListener(leftOnTouchListener);
    }

    private void enableBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        while (!bluetoothAdapter.isEnabled()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        connectedThread = ConnectionManager.connect(bluetoothAdapter, new Handler());
    }

    private int getThrust(float position) {
        return 0;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            Log.v("The BT enabling result is ", String.valueOf(resultCode));
        }
    }
}
