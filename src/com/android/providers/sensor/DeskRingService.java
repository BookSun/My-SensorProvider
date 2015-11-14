/*
 * add by liuwenshuai 2014-01-17
 * function: The ring from 0 to max,if phone in the desk when calling
 * 
 */
package com.android.providers.sensor;

import android.app.Service;
import android.content.Context;
import android.content.ContentResolver;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemProperties;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.provider.Settings;
import android.provider.ExtraSettings;
import android.provider.SensorProviderListener;
import android.provider.SensorProviderListener.OnAdjustVolumeListener;

public class DeskRingService extends Service {
    private TelephonyManager mTelephonyMgr;
    private AudioManager mAudioManager;
    private SensorManager mSensorManager;
    private Sensor mProximitySensor;
    private int mRingerMode;
    private int mVibRinging;
    private int mRingerCurrent;
    private boolean mSensorFlag =false;
    private boolean DEBUG = true;
    private boolean onScreenOn = false;
    private boolean mRegisterSensor;
    private int mCurrent;
    private Handler mHandler;
    private Runnable mRunNable;
    private Runnable mRunnable;
    private Context mContext;
    private int mMaxVolume;
    private int mCount = 0;
    private final int REGISTER_DESK = 8;
    private final String TAG = "DeskRingService";
    private SensorProviderListener mSensorProviderListener = null;

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) {Log.i(TAG, "onCreate");}
        mTelephonyMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        mSensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        mSensorProviderListener = new SensorProviderListener(DeskRingService.this);
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
    public void AdjustStreamVolume() {
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING,1,0);
        mRunNable = new Runnable() {
            public void run() {
                mCurrent = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
                if (mCurrent <= 0) {
                    mHandler.removeCallbacks(mRunNable);
                    return;
                }
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING,AudioManager.ADJUST_RAISE,0);
                if (mCurrent < mRingerCurrent) {
                    mHandler.postDelayed(mRunNable,2200);
                } else {
                    mHandler.removeCallbacks(mRunNable);
                }
            }
        };
        mHandler.postDelayed(mRunNable,200);
    }
    public void DeskRingVolume() {
        int deskRing = Settings.System.getInt(getContentResolver(),ExtraSettings.System.PACKET_RING, 0);
        if (deskRing == 0) {
            Log.d(TAG,"deskRing:0");
            AdjustStreamVolume();
        }
        if (deskRing == 1) {
            mSensorProviderListener.registerSensorEventerListener(REGISTER_DESK);
            Log.d(TAG,"deskRing:1");
            mSensorProviderListener.setOnAdjustVolumeListener(new OnAdjustVolumeListener() {
                @Override
                public void onVolume() {
                    AdjustStreamVolume();
                    mSensorProviderListener.unregisterSensorEventerListener(REGISTER_DESK);
                    Log.d(TAG,"onProximity:true");
                }
            });
        }
    }
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    mRingerCurrent = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);
                    if (mAudioManager.isWiredHeadsetOn() || mAudioManager.isBluetoothA2dpOn()) {
                        Log.d(TAG,"Have a calling...isWiredHeadsetOn:"+mAudioManager.isWiredHeadsetOn());
                    } else {
                        if (!mAudioManager.isSilentMode()) {
                            if (false == mSensorFlag && mRingerCurrent != 0) {
                                if (DEBUG) {Log.i(TAG, "it's a calling.......mVibRinging:"+mRingerCurrent);}
                                DeskRingVolume();
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
                            mSensorProviderListener.unregisterSensorEventerListener(REGISTER_DESK);
                        mAudioManager.setStreamVolume(AudioManager.STREAM_RING,mRingerCurrent,0);
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
