package com.android.providers.sensor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.os.SystemClock;
import android.os.ServiceManager;
import android.os.Vibrator;
import com.android.internal.telephony.ITelephony;
import android.util.Log;

public class SmartAnswerService extends Service{

    private TelephonyManager mTelephonyMgr;
    private SensorManager mSensorMgr;
    private Sensor mAccelerometerSensor;
    private Sensor mProximitySensor;
    private Sensor mOrientationSensor;

    //private boolean mmAccelerometerActive;
    private boolean mProximityActive;
    private boolean mPreviousActive;
    private boolean mCalling = false;
    private boolean mAnswerState = false;
    private final String TAG = "SmartAnswerService";
    private boolean DEBUG = true;
    private float mLast_oy = 0.0f;
    private float mLast_oz = 0.0f;
    private float mLast_gx = 0.0f;
    private float mLast_gy = 0.0f;
    private float mLast_gz = 0.0f;
    private Vibrator mVibrator = null;
    private static final int VIBRATE_LENGTH = 100;
    private static final int SHAKE_SPEED = 19;
    private boolean mAleadyVibrate = false;
    private boolean mPhoneIsUsing;
    private boolean mRegisterAccele = false;
    private boolean mRegisterOrient = false;
    @Override
    public void onCreate() {
        super.onCreate();

        if (DEBUG) {Log.i(TAG, "onCreate");}
        mTelephonyMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        mSensorMgr = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometerSensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mProximitySensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mOrientationSensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mVibrator = (Vibrator)this.getSystemService(Service.VIBRATOR_SERVICE);

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) {Log.i(TAG, "onDestroy");}
        if (true == mCalling) {
            //mSensorMgr.unregisterListener(mAccelerometerEventListener);
            mSensorMgr.unregisterListener(mProximityEventListener);
            mSensorMgr.unregisterListener(mOrientationEventListener);
        }
        if (mVibrator != null) {
            mVibrator.cancel();
        }
        mTelephonyMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        super.onDestroy();
    }

    private void removeListener() {
            mSensorMgr.unregisterListener(mProximityEventListener);
            mSensorMgr.unregisterListener(mOrientationEventListener);
            mSensorMgr.unregisterListener(mAccelerometerListener);
            mCalling = false;
            mRegisterOrient = false;
            mRegisterAccele = false;
    }

    private void answer() {
        try {
            ITelephony iTelephony = ITelephony.Stub.asInterface(ServiceManager.getService(Context.TELEPHONY_SERVICE));
            if (!mAnswerState) {
                iTelephony.answerRingingCall();
                if (mVibrator != null) {
                    mVibrator.vibrate(VIBRATE_LENGTH);
                }
                removeListener();
                mAnswerState = true;
                if (DEBUG) Log.i(TAG,"answerRingingCall........");
            }
        } catch (android.os.RemoteException e) {
            if(DEBUG) Log.i(TAG, "phone answer failed by smart answer");
            mAnswerState = false;
        }
    }
/*///android BEGIN 
    //Add by SL
    private boolean phoneIsInUse() {
        boolean phoneInUse = false;
        try {
        ITelephony phone = ITelephony.Stub.asInterface(ServiceManager.checkService("phone"));
        if (phone != null)
            phoneInUse = !phone.isIdle();
        } catch (android.os.RemoteException e) {
            Log.w(TAG, "phone.isIdle() failed", e);
        }
        return phoneInUse;
    }
    //Add end
///android END*/
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    if (false == mCalling) {
                        if(DEBUG) Log.i(TAG, "have a calling");
                        /*mSensorMgr.registerListener(mProximityEventListener,
                            mProximitySensor,
                            SensorManager.SENSOR_DELAY_NORMAL);*/
                        initSensorRegister();
                        mCalling = true;
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (true == mCalling) {
                        if (DEBUG) Log.i(TAG,"CALL_STATE_OFFHOOK:calling is true");
                        removeListener();
                    }
                    mAnswerState = false;
                    if (DEBUG) Log.i(TAG,"mAnswerState:"+mAnswerState);
                    break;
                default:
                    break;
            }
        }
    };

    // private final SensorEventListener mAccelerometerEventListener = new SensorEventListener() {

    //     @Override
    //     public void onSensorChanged(SensorEvent event) {
    //         if (DEBUG) {Log.i(TAG, "Acceleromete onSensorChanged");}
    //         if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
    //             float gx = event.values[SensorManager.DATA_X];
    //             float gy = event.values[SensorManager.DATA_Y];
    //             float gz = event.values[SensorManager.DATA_Z];
    //             if (mAnswerState == false && false == mProximityActive) {
    //                     if (Math.abs(gx) > SHAKE_SPEED && mAnswerState == false) {
    //                         answer();
    //                         if (DEBUG) {Log.i(TAG, "The answer third success...");}
    //                     }
    //             }
    //         }
    //     }

    //     @Override
    //     public void onAccuracyChanged(Sensor sensor, int accuracy) {
    //     }
    // };
    private final SensorEventListener mAccelerometerListener = new SensorEventListener() {
        boolean proximityOpen = false;
        float gy = 0.0f;
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (DEBUG) {Log.i(TAG, "Acceleromete onSensorChanged");}
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float gx = event.values[SensorManager.DATA_X];
                gy = event.values[SensorManager.DATA_Y];
                float gz = event.values[SensorManager.DATA_Z];
            }
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                float distance = event.values[0];
                proximityOpen = (distance >= 0.0f && distance < mProximitySensor.getMaximumRange());
                if (proximityOpen && gy > 8.0f) {
                    answer();
                    if (DEBUG) {Log.i(TAG, "The answer third success...");}
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private final SensorEventListener mProximityEventListener = new SensorEventListener() {
        boolean proximityOn = false;
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (DEBUG) {Log.i(TAG, "proximity onSensorChanged");}
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                float distance = event.values[0];
                proximityOn = (distance >= 0.0f && distance < mProximitySensor.getMaximumRange());
                if (proximityOn == false) {
                    if (mOrientationSensor != null) {
                        if (!mRegisterOrient) {
                            mRegisterOrient = mSensorMgr.registerListener(mOrientationEventListener,
                                mOrientationSensor,SensorManager.SENSOR_DELAY_NORMAL);
                            mSensorMgr.registerListener(mOrientationEventListener,mProximitySensor,
                                SensorManager.SENSOR_DELAY_NORMAL);
                            mSensorMgr.registerListener(mOrientationEventListener,mAccelerometerSensor,
                                SensorManager.SENSOR_DELAY_NORMAL);
                            if(DEBUG) Log.i(TAG, "mOrientationEventListener registerListener...");
                        }
                    } else {
                        if (!mRegisterAccele) {
                            mRegisterAccele = mSensorMgr.registerListener(mAccelerometerListener,mAccelerometerSensor,
                                SensorManager.SENSOR_DELAY_NORMAL);
                            mSensorMgr.registerListener(mAccelerometerListener,mProximitySensor,
                                SensorManager.SENSOR_DELAY_NORMAL);
                            if(DEBUG) Log.i(TAG, "mAccelerometerListener registerListener...");
                        }
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private final SensorEventListener mOrientationEventListener = new SensorEventListener() {
        boolean proximityActive = false;
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (DEBUG) {Log.i(TAG, "mOrientationSensor onSensorChanged");}
            if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
                float ox = event.values[SensorManager.DATA_X];
                float oy = event.values[SensorManager.DATA_Y];
                float oz = event.values[SensorManager.DATA_Z];
                mLast_oz = oz;
                mLast_oy = oy;
            }
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float gx = event.values[SensorManager.DATA_X];
                float gy = event.values[SensorManager.DATA_Y];
                float gz = event.values[SensorManager.DATA_Z];
                mLast_gx = gx;
                mLast_gy = gy;
                mLast_gz = gz;
            }
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                float distance = event.values[0];
                Log.i(TAG, "TYPE_PROXIMITY:"+distance);
                proximityActive = (distance >= 0.0f && distance < mProximitySensor.getMaximumRange());
                Log.i(TAG, "TYPE_PROXIMITY:"+proximityActive);
            }
            Log.i(TAG, "mAnswerState:"+mAnswerState);
            Log.i(TAG, "proximityActive:"+proximityActive);
            if(mAnswerState == false && proximityActive) {
                if (DEBUG) {Log.i(TAG, "The answer...");}
                if(Math.abs(mLast_gx) > 3.0f && mLast_gy > 8.0f && Math.abs(mLast_gz) < 3.0f) {
                    answer();
                    if (DEBUG) {Log.i(TAG, "The answer first success...");}
                } else {
                    if (DEBUG) {Log.i(TAG, "mLast_oy:"+mLast_oy+";mLast_oz:"+mLast_oz);}
                    if (Math.abs(mLast_oz) < 45.0f && mLast_oy > -130.0f && mLast_oy < -45.0f) {
                        answer();
                        if (DEBUG) {Log.i(TAG, "The answer second success...");}
                    }
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };


    private void initSensorRegister () {

            if (!mRegisterAccele) {
                    mRegisterAccele = mSensorMgr.registerListener(mAccelerometerListener,mAccelerometerSensor,
                                SensorManager.SENSOR_DELAY_NORMAL);
                    mSensorMgr.registerListener(mAccelerometerListener,mProximitySensor,
                                SensorManager.SENSOR_DELAY_NORMAL);
                if(DEBUG) Log.i(TAG, "mAccelerometerListener registerListener...");
            }
    }
}
