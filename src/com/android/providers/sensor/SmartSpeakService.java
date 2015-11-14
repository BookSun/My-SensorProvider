/*
 * add by liuwenshuai 2014-02-11
 * function: Turn off speaker,if phone's speaker is on when calling
 * 
 */
package com.android.providers.sensor;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.media.AudioManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.provider.SensorProviderListener;
import android.provider.SensorProviderListener.OnProximityListener;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

public class SmartSpeakService extends Service {
    private TelephonyManager mTelephonyMgr;
    private AudioManager mAudioManager = null;
    private Sensor mProximitySensor;
    private final String TAG = "SmartSpeakService";
    private boolean DEBUG = false;
    private boolean mSensorFlag = false;
    private SensorProviderListener mSensorProvider = null;
    private PowerManager mPowerManager = null;
    PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate() {
        if (DEBUG) {Log.i(TAG, "onCreate");}
        super.onCreate();
        registerReceiver(mHeadStateReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        mTelephonyMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
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
        if (true == mSensorFlag) {
            mSensorProvider.unregisterSensorEventerListener(4);
            mSensorFlag = false;
            //releaseWakeLock();
        }
        unregisterReceiver(mHeadStateReceiver);
        mTelephonyMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        super.onDestroy();
    }
    //get lock
    private void acquireWakeLock() {
          if (mWakeLock != null && !mWakeLock.isHeld()) {
              mWakeLock.acquire();
              Log.d(TAG,"acquire");
        }
    }
    //release lock
    private void releaseWakeLock() {
         if (mWakeLock != null && mWakeLock.isHeld()) {
             mWakeLock.release();
             mWakeLock = null;
             Log.d(TAG,"release");
       }
    }
    private void setPhoneSpeaker() {
        mSensorProvider.registerSensorEventerListener(4);
        mSensorProvider.setOnProximityListener(new OnProximityListener() {
            @Override
            public void onProximity() {
                if (mAudioManager.isSpeakerphoneOn()) {
                    Intent intent = new Intent();
                    intent.setAction("com.android.providers.smartspeak");
                    SmartSpeakService.this.sendBroadcast(intent);
                    mAudioManager.setSpeakerphoneOn(!mAudioManager.isSpeakerphoneOn());
                    Log.d(TAG,"isSpeakerphoneOn is true");
                }
            }
        });
    }

    private BroadcastReceiver mHeadStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                if (DEBUG) Log.d(TAG,"getAction.....");
                handleHeadStateChanged(intent);
            }
        }
    };

    private void handleHeadStateChanged(Intent intent) {
        if(intent.hasExtra("state")) {
            if (DEBUG) Log.d(TAG,"state:"+intent.getIntExtra("state", 0));
            int headState = intent.getIntExtra("state", 0);
            if (headState == 0) {
                if (mSensorFlag && mSensorProvider != null) {
                     mSensorProvider.registerSensorEventerListener(4);
                }
            } else if (headState == 1) {
                if (mSensorFlag && mSensorProvider != null) {
                    mSensorProvider.unregisterSensorEventerListener(4);
                }
            }
        }
    }

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    if (mSensorFlag == false) {
                        if (mAudioManager.isWiredHeadsetOn() || mAudioManager.isBluetoothA2dpOn()) {
                        Log.d(TAG,"Have a calling...isWiredHeadsetOn:"+mAudioManager.isWiredHeadsetOn());
                        } else {
                            Log.d(TAG,"CALL_STATE_OFFHOOK is true");
                            //mWakeLock = mPowerManager.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "SmartSpeakService.WakeLock");
                            mSensorFlag = true;
                            mSensorProvider = new SensorProviderListener(SmartSpeakService.this);
                            //acquireWakeLock();
                            setPhoneSpeaker();
                        }
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    if (mSensorFlag) {
                        mSensorProvider.unregisterSensorEventerListener(4);
                        //releaseWakeLock();
                        Log.d(TAG,"CALL_STATE_IDLE is true");
                        mSensorFlag = false;
                    }
                    break;
                default:
                    break;
            }
        }
    };
}