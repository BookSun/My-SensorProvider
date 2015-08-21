/*
 * add by liuwenshuai 2014-01-17
 * function: The ring is max and vibrator,if phone in the packet when calling
 * 
 */
package com.lewa.providers.sensor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.os.Bundle;
import android.os.Handler;
import lewa.util.PlatformHelper;
import android.os.SystemProperties;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import lewa.provider.SensorProviderListener;
import lewa.provider.SensorProviderListener.OnOnlyProximityListener;

public class LewaPacketRingService extends Service {
    private TelephonyManager mTelephonyMgr;
    private AudioManager mAudioManager;
    private SensorManager mSensorManager;
    private Sensor mProximitySensor;
    private int mRingerMode;
    private int mVibRinging;
    private boolean mProximiryActive;
    private final String TAG = "LewaPacketRingService";
    private boolean DEBUG = true;
    private boolean mSensorFlag = false;
    private boolean mRegisterSensor;
    private int mRingerCurrent;
    private int mRingCurrent;
    private Handler mHandler;
    private Runnable mRunNable;
    private int mMaxVolume;
    private int REGISTER_PACKET = 10;
    private SensorProviderListener mSensorProviderListener = null;

    @Override
    public void onCreate() {
        if (DEBUG) {Log.i(TAG, "onCreate");}
        super.onCreate();
        mTelephonyMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        mSensorProviderListener = new SensorProviderListener(LewaPacketRingService.this);
        mHandler = new Handler();
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
        mTelephonyMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        super.onDestroy();
    }
    private void AdjustVolume() {
        mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,AudioManager.VIBRATE_SETTING_ON);
        mRunNable = new Runnable() {
            public void run() {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING,AudioManager.ADJUST_RAISE,0);
                mRingCurrent = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
                if (DEBUG) {Log.i(TAG, "mRingCurrent is:"+mRingCurrent);}
                if (mRingCurrent < mMaxVolume) {
                    mHandler.postDelayed(mRunNable,1000);
                } else {
                    mHandler.removeCallbacks(mRunNable);
                    if (DEBUG) {Log.i(TAG, "removeCallbacks");}
                }
            }
        };
        mHandler.postDelayed(mRunNable,1000);
    }
    private void PacketRingMax() {

    }
    private final SensorEventListener mProximityEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (DEBUG) {Log.i(TAG, "proximity onSensorChanged");}
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                float distance = event.values[0];
                if (distance == 0.0f) {
                    AdjustVolume();
                    if (DEBUG) {Log.i(TAG, "packageProximity");}
                    mSensorManager.unregisterListener(mProximityEventListener);
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    if (mAudioManager.isWiredHeadsetOn() || mAudioManager.isBluetoothA2dpOn()) {
                        Log.d(TAG,"Have a calling...isWiredHeadsetOn:"+mAudioManager.isWiredHeadsetOn());
                    } else {
                        if (!mAudioManager.isSilentMode()) {
                            if (false == mSensorFlag) {
                                mRingerMode = mAudioManager.getRingerMode();
                                mRingerCurrent = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
                                mVibRinging = mAudioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER);
                                if (DEBUG) {Log.i(TAG, "it's a calling.......mVibRinging:"+mVibRinging);}
                                if (PlatformHelper.isQcom()) {
                                    mRegisterSensor = mSensorManager.registerListener(mProximityEventListener,mProximitySensor,
                                        SensorManager.SENSOR_DELAY_NORMAL);
                                } else {
                                    mSensorProviderListener.registerSensorEventerListener(REGISTER_PACKET);
                                    mSensorProviderListener.setOnOnlyProximityListener(new OnOnlyProximityListener() {
                                        @Override
                                        public void onOnlyProximity() {
                                            AdjustVolume();
                                            mSensorProviderListener.unregisterSensorEventerListener(REGISTER_PACKET);
                                        }
                                    });
                                }
                                mSensorFlag = true;
                            }
                        }
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                case TelephonyManager.CALL_STATE_IDLE:
                    if (true == mSensorFlag) {
                        if (DEBUG) {Log.i(TAG, "End calling........");}
                        if (mSensorProviderListener != null)
                            mSensorProviderListener.unregisterSensorEventerListener(REGISTER_PACKET);
                        if (mRegisterSensor)
                            mSensorManager.unregisterListener(mProximityEventListener);
                        mAudioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER,mVibRinging);
                        mSensorFlag = false;
                        mHandler.removeCallbacks(mRunNable);
                    }
                    break;
                default:
                    break;
            }
        }
    };
}