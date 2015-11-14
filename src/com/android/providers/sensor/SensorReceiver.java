package com.android.providers.sensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public class SensorReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ((Intent.ACTION_BOOT_COMPLETED).equals(action)) {
            SettingObserver settingObserver = new SettingObserver(context, null);
            settingObserver.observe();
        }
    }
}
