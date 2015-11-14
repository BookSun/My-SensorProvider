package com.android.providers.sensor;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.content.Intent;
import android.provider.ExtraSettings;
import android.provider.Settings;

import android.util.Log;

public class SettingObserver extends ContentObserver {

    private Context mContext;

    private int mTurnSilentValue = 0;
    private int mSmartAnswerValue = 0;
    private int mPacketRingValue = 0;//Add by SL
    private int mDeskRingValue = 0;//Add by SL
    private int mSmartSpeakValue = 0;

    private boolean DEBUG = true;
    private final String TAG = "androidSettingObserver";

    SettingObserver(Context context, Handler handler) {
        super(handler);
        mContext = context;
        if(DEBUG)Log.i(TAG, "androidSettingObserver");
    }

    void observe() {
        if(DEBUG)Log.i(TAG, "observe");
        ContentResolver resolver = mContext.getContentResolver();
        resolver.registerContentObserver(Settings.System.getUriFor(
            ExtraSettings.System.TURN_SILENT_WHEN_CALLING), false, this);
        resolver.registerContentObserver(Settings.System.getUriFor(
            ExtraSettings.System.SMART_ANSWER), false, this);
        resolver.registerContentObserver(Settings.System.getUriFor(
            ExtraSettings.System.PACKET_RING), false, this);
        resolver.registerContentObserver(Settings.System.getUriFor(
            ExtraSettings.System.DESK_RING),false,this);
        resolver.registerContentObserver(Settings.System.getUriFor(
            ExtraSettings.System.SMART_SPEAK),false,this);
        updateService();
    }

    @Override public void onChange(boolean selfChange) {
        if(DEBUG)Log.i(TAG, "onChange");
        updateService();
    }

    private void updateService() {
        if(DEBUG)Log.i(TAG, "updateService");
        ContentResolver resolver = mContext.getContentResolver();

        int turnSilent = Settings.System.getInt(resolver,
                ExtraSettings.System.TURN_SILENT_WHEN_CALLING, 0);
        if (mTurnSilentValue != turnSilent) {
            if (turnSilent == 1) {
                mContext.startService(new Intent(mContext, TurnSilentService.class));
            }else if (turnSilent == 0) {
                mContext.stopService(new Intent(mContext, TurnSilentService.class));
            }
            mTurnSilentValue = turnSilent;
        }

        int smartAnswer = Settings.System.getInt(resolver,
                ExtraSettings.System.SMART_ANSWER, 0);
        if (mSmartAnswerValue != smartAnswer) {
            if (smartAnswer == 1) {
                mContext.startService(new Intent(mContext, SmartAnswerService.class));
            }else if (smartAnswer == 0) {
                mContext.stopService(new Intent(mContext, SmartAnswerService.class));
            }
            mSmartAnswerValue = smartAnswer;
        }
        int packetRing = Settings.System.getInt(resolver,
                ExtraSettings.System.PACKET_RING, 0);
        if (mPacketRingValue != packetRing) {
            if (packetRing == 1) {
                mContext.startService(new Intent(mContext, PacketRingService.class));
            }else if (packetRing == 0) {
                mContext.stopService(new Intent(mContext, PacketRingService.class));
            }
            mPacketRingValue = packetRing;
        }

        int deskRing = Settings.System.getInt(resolver,
            ExtraSettings.System.DESK_RING, 0);
        if (mDeskRingValue != deskRing) {
            if (deskRing == 1) {
                mContext.startService(new Intent(mContext, DeskRingService.class));
            }else if (deskRing == 0) {
                mContext.stopService(new Intent(mContext, DeskRingService.class));
            }
            mDeskRingValue = deskRing;
        }

        int smartSpeak = Settings.System.getInt(resolver,
            ExtraSettings.System.SMART_SPEAK, 0);
        if (mSmartSpeakValue != smartSpeak) {
            if (smartSpeak == 1) {
                mContext.startService(new Intent(mContext, SmartSpeakService.class));
            }else if (smartSpeak == 0) {
                mContext.stopService(new Intent(mContext, SmartSpeakService.class));
            }
            mSmartSpeakValue = smartSpeak;
        }
    }
}
