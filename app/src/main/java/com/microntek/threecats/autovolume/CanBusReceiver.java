package com.microntek.threecats.autovolume;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

public class CanBusReceiver extends BroadcastReceiver {

    private static final String TAG = CanBusReceiver.class.getSimpleName();

    public static final String REV = "rev"; // engine RPM - int
    public static final String SPEED = "speed"; // speed km/h - double
    public static final String BATT = "batt"; // battery - double
    public static final String TEMP = "temp"; // temperature - double
    public static final String DIST = "dist"; // total tistance km - int
    public static final String DEBUG = "debug"; // debugging info - string

    int received = 0;

    interface Callback {
        void receiveCanBus(Bundle bundle);
        Context getContext();
    }

    Callback callback;

    private CanBusReceiver(Callback callback) {
        this.callback = callback;
    }

    public static CanBusReceiver RegisterCarReceiver(Callback callback) {
        Context context = callback.getContext();
        CanBusReceiver newCanBusReceiver = new CanBusReceiver(callback);
        context.registerReceiver(newCanBusReceiver, new IntentFilter("com.microntek.sync"));
        return newCanBusReceiver;
    }

    public static void UnregisterCarReceiver(CanBusReceiver oldCanBusReceiver) {
        oldCanBusReceiver.callback.getContext().unregisterReceiver(oldCanBusReceiver);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = new Bundle();

        received++;
        Log.d(TAG, "Received: " + received);

        byte[] data = intent.getByteArrayExtra("syncdata");

        StringBuilder debugStr = new StringBuilder();
        debugStr.append("Received: ").append(received).append("\n");
        debugStr.append("Action: ").append(intent.getAction()).append("\n");
        if (data != null) {
            debugStr.append("Data Len: ").append(data.length).append("\n");
            if (data.length > 0) {
                debugStr.append("Data: ");
                int i;
                for (i = 0; i < data.length - 1; ++i) {
                    debugStr.append(data[i]).append(",");
                }
                debugStr.append(data[i]);
            }
        }
        bundle.putString(DEBUG, debugStr.toString());

        if (data != null) {
            if (data.length > 13 && data[2] == 2) {

                int rev = (0xFF & data[3]) * 256 + (0xFF & data[4]);
                bundle.putInt(REV, rev);

                // don't allow erroneous high values when car is stopped
                // if RPM is 0 then speed will be set to 0 too so no loud surprises
                double speed = 0;
                if (rev > 0) {
                    speed = ((0xFF & data[5]) * 256 + (0xFF & data[6])) * 0.01d;
                    
                    // some cars are jumping to 327km/h when stopped, set to 0 too
                    if (rev < 3000 && speed == 327) {
                        speed = 0;
                    }
                }
                bundle.putDouble(SPEED, speed);
                
                double battery = ((0xFF & data[7]) * 256 + (0xFF & data[8])) * 0.01d;
                bundle.putDouble(BATT, battery);

                double temp = (0xFF & data[9]) * 256 + (0xFF & data[10]);
                if (temp >= 32768) {
                    temp -= 65536;
                }
                temp /= 10;
                bundle.putDouble(TEMP, temp);

                int distance = (0xFF & data[11]) * 65536 + (0xFF & data[12]) * 256 + (0xFF & data[13]);
                bundle.putInt(DIST, distance);

            }
        }

        callback.receiveCanBus(bundle);
    }
}
