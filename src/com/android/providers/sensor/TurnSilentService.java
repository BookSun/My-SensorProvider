/*
 * add by wangqiang 2013-10-21
 * function: turn phone switch silent when calling
 * 
 * */
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
import android.media.Ringtone;
import android.util.Log;
import android.net.Uri;
import android.media.RingtoneManager;
import android.util.androidAudioManagerHelper;
import android.provider.SensorProviderListener;
import android.provider.SensorProviderListener.OnTurnPhoneDownListener;

public class TurnSilentService extends Service{

    private TelephonyManager mTelephonyMgr;
    private SensorManager mSensorMgr;
    private AudioManager mAudioMgr;
    private Sensor mSensor;
    private int mRingerMode;
    private androidAudioManagerHelper mAudioManagerHelper = null;
    private SensorProviderListener mSensorProviderSer = null;
    private boolean mVibRinging;

    private boolean mSensorFlag = false;
    private boolean mUpwardFlag = false;

    private final String TAG = "TurnSilentService";
    private boolean DEBUG = true;
    private int mRingerCurrent;

    @Override
    public void onCreate() {
        super.onCreate();
        if (DEBUG) {Log.i(TAG, "onCreate");}
        mTelephonyMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
        mAudioMgr = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        mSensorMgr = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mAudioManagerHelper == null) {
            mAudioManagerHelper = new androidAudioManagerHelper(this);
        }
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
            mSensorProviderSer.unregisterSensorEventerListener(2);
            mSensorFlag = false;
        }
        mTelephonyMgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
        super.onDestroy();
    }


    public void TurnSilentRing() {
        mSensorProviderSer = new SensorProviderListener(TurnSilentService.this);
        mSensorProviderSer.registerSensorEventerListener(2);
        mSensorProviderSer.setOnTurnPhoneDownListener(new OnTurnPhoneDownListener() {
            @Override
            public void onTurnPhoneDown() {
                // TODO Auto-generated method stub
                Log.d(TAG,"TurnSilentRing:true");
                mAudioMgr.setStreamVolume(AudioManager.STREAM_RING,0,0);
                mSensorProviderSer.unregisterSensorEventerListener(2);
            }
        });
    }

    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    if (false == mSensorFlag) {
                        mRingerCurrent = mAudioMgr.getStreamVolume(AudioManager.STREAM_RING);
                        TurnSilentRing();
                        mSensorFlag = true;
                    }
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                case TelephonyManager.CALL_STATE_IDLE:
                    if (true == mSensorFlag) {
                        mSensorProviderSer.unregisterSensorEventerListener(2);
                        mAudioMgr.setStreamVolume(AudioManager.STREAM_RING,mRingerCurrent,0);
                        mSensorFlag = false;
                        mUpwardFlag = false;
                    }
                    break;
                default:
                    break;
            }
        }
    };
}
