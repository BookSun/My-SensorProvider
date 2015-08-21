package com.lewa.providers.sensor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public class LewaSensorReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if ((Intent.ACTION_BOOT_COMPLETED).equals(action)) {
            LewaSettingObserver settingObserver = new LewaSettingObserver(context, null);
            settingObserver.observe();
        }
    }
}
