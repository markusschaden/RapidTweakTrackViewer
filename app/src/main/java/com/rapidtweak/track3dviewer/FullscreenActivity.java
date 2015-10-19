package com.rapidtweak.track3dviewer;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.rapidtweak.track3dviewer.algo.MadgwickAHRS;
import com.rapidtweak.track3dviewer.algo.MadgwickAHRSIMU;
import com.rapidtweak.track3dviewer.algo.Quaternion;
import com.rapidtweak.track3dviewer.algo.ReadCSV;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class FullscreenActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;

    //private View mContentView;
    private View mControlsView;
    private boolean mVisible;
    private TrackSurfaceView mGLSurfaceView;
    Double[] lastCoordinate = {0d, 0d, 0d};
    List<Double[]> raceTrack = new ArrayList<>();
    MadgwickAHRS madgwickAHRS = null;
    private Quaternion quat = null;
    double[] Z = {0d, 0d, 1d};

    private double[][] values = {
            //0-2 acce
            //3-5 gyro
            //6-8 mag
            //9 time
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);

        mGLSurfaceView = (TrackSurfaceView) findViewById(R.id.gl_surface_view);
        mGLSurfaceView.setPoints(raceTrack);
        raceTrack.add(lastCoordinate);

        madgwickAHRS = new MadgwickAHRSIMU(0.1d, new double[]{1, 0, 0, 0}, 50d);

        new Thread() {
            @Override
            public void run() {
                try {
                    values = new ReadCSV(FullscreenActivity.this).getData();
                    Log.i("Read CSV", "complete");
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("Read CSV", "failed");
                }

                for (int i = 0; i < values.length; i++) {
                    onData(values[i]);
                    try {
                        Thread.sleep(50);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mGLSurfaceView.invalidate();
                            }
                        });

                    } catch (InterruptedException e2) {
                        e2.printStackTrace();
                    }
                }
            }
        }.start();



        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);


        // Set up the user interaction to manually show or hide the system UI.
        mGLSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        findViewById(R.id.dummy_button).setOnTouchListener(mDelayHideTouchListener);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mGLSurfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mGLSurfaceView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };

    private final Handler mHideHandler = new Handler();
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    public void onData(double[] valuesIn) {

		/*Calculate and set quaternion!!*/
        /*Scale the values*/
        double[] imuData = new double[9];

        imuData[0] = ((double) valuesIn[0]) / 256d;
        imuData[1] = ((double) valuesIn[1]) / 256d;
        imuData[2] = ((double) valuesIn[2]) / 256d;
        imuData[3] = ((((double) valuesIn[3]) / 14.375d) * (Math.PI / 180.0));
        imuData[4] = ((((double) valuesIn[4]) / 14.375d) * (Math.PI / 180.0));
        imuData[5] = ((((double) valuesIn[5]) / 14.375d) * (Math.PI / 180.0));
        imuData[6] = ((double) valuesIn[6]);
        imuData[7] = ((double) valuesIn[7]);
        imuData[8] = ((double) valuesIn[8]);

        double initTheta;
        double[] rotAxis;
        /*The initial round*/
        if (quat == null) {
            //Set the initial orientation according to first sample of accelerometry
            System.out.println("X " + Double.toString(imuData[0]) + " Y " + Double.toString(imuData[1]) + " Z " + Double.toString(imuData[2]));
            initTheta = Math.acos(dot(normalize(new double[]{imuData[0], imuData[1], imuData[2]}), Z));
            rotAxis = cross(new double[]{imuData[0], imuData[1], imuData[2]}, Z);
            //System.out.println("X "+Double.toString(rotAxis[0]) +" Y "+Double.toString(rotAxis[1])+" Z "+Double.toString(rotAxis[2])+" norm "+Double.toString(norm(rotAxis))+" cos "+Double.toString(Math.cos(initTheta/2d))+" "+Double.toString(initTheta));
            if (norm(rotAxis) != 0) {
                rotAxis = normalize(rotAxis);
                //quat = new Quaternion(Math.cos(initTheta/2d),-Math.sin(initTheta/2d)*rotAxis[0],-Math.sin(initTheta/2d)*rotAxis[1],-Math.sin(initTheta/2d)*rotAxis[2]);
                quat = new Quaternion(Math.cos(initTheta / 2d), Math.sin(initTheta / 2d) * rotAxis[0], Math.sin(initTheta / 2d) * rotAxis[1], Math.sin(initTheta / 2d) * rotAxis[2]);
            } else {
                quat = new Quaternion(1d, 0d, 0d, 0d);
            }
            madgwickAHRS.setOrientationQuaternion(quat.getDouble());
            //System.out.println(Double.toString(initTheta) +" "+Double.toString(Math.cos(initTheta/2d))+" "+Double.toString(-Math.sin(initTheta/2d)*rotAxis[0])+" "+Double.toString(-Math.sin(initTheta/2d)*rotAxis[1])+" "+Double.toString(-Math.sin(initTheta/2d)*rotAxis[2]) );
            System.out.println(quat.toString());
        } else {
            /*Use Madgwick AHRS IMU algorithm*/
            madgwickAHRS.AHRSUpdate(new double[]{imuData[3], imuData[4], imuData[5], imuData[0], imuData[1], imuData[2], imuData[6], imuData[7], imuData[8]});
            double[] tempQ = madgwickAHRS.getOrientationQuaternion();
            quat = new Quaternion(tempQ[0], tempQ[1], tempQ[2], tempQ[3]);
        }

        if (quat != null) {

            //Calculated rotated values
            //System.out.println(quat.getFloat()[0] + " " + quat.getFloat()[1] + " " + quat.getFloat()[2] + " " + quat.getFloat()[3]);
            //System.out.println("");

            //Quaternion grf = new Quaternion(0d, imuData[0], imuData[1], imuData[2]);
            float axisRotation[] = {-90f,1f,0f,0f};
            double rotAngle = axisRotation[0]/180.0*Math.PI;
            Quaternion grf = new Quaternion(Math.cos(rotAngle/2.0),Math.sin(rotAngle/2.0)*axisRotation[1],Math.sin(rotAngle/2.0)*axisRotation[2],Math.sin(rotAngle/2)*axisRotation[3]);
            //Quaternion rotatedQ = ((quat.conjugate()).times(grf)).times(quat);
            Quaternion rotatedQ = (quat.times(grf)).times(quat.conjugate());
            double[] rotatedVals = rotatedQ.getAxis();
            //System.out.println("Got to rotating data X " + rotatedVals[0] + " Y " + rotatedVals[1] + " Z " + rotatedVals[2]);
            Double[] newCoords = {(lastCoordinate[0] + rotatedVals[0]), (lastCoordinate[1] + rotatedVals[1]), (lastCoordinate[2] + rotatedVals[2])};
            float scaleFactor = 5f;
            Double[] tempCoords = {newCoords[0]*scaleFactor, newCoords[1]*scaleFactor, newCoords[2]*scaleFactor};
            //Log.i("new Coords: ",""+tempCoords[0] + ", " + tempCoords[1] + ", " + tempCoords[2]);
            raceTrack.add(tempCoords);
            mGLSurfaceView.addCoordinate(tempCoords[0], tempCoords[1]);
            lastCoordinate = newCoords;

        }


    }

    private double[] normalize(double[] a) {
        double magnitude = norm(a);
        for (int i = 0; i < a.length; ++i) {
            a[i] = (a[i] / magnitude);
        }
        return a;
    }
    private double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }
    private double[] cross(double[] a, double[] b) {
        double[] c = new double[3];
        c[0] = (a[1] * b[2] - a[2] * b[1]);
        c[1] = (a[2] * b[0] - a[0] * b[2]);
        c[2] = (a[0] * b[1] - a[1] * b[0]);
        return c;
    }
    private double norm(double[] a) {
        double b = 0;
        for (int i = 0; i < a.length; ++i) {
            b += a[i] * a[i];
        }
        return Math.sqrt(b);
    }


}
