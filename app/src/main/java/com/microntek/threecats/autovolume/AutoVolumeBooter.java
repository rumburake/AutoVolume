package com.microntek.threecats.autovolume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoVolumeBooter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, VolumeService.class);
        context.startService(i);
    }
}
