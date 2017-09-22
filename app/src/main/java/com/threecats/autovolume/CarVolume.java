package com.threecats.autovolume;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class CarVolume {

    private static final String VOLUME_PARAMETER_NAME = "av_volume=";
    private static final String MUTE_PARAMETER_NAME = "av_mute=";
    private static final String MAX_VOLUME_PARAMETER_NAME = "cfg_maxvolume";

    // public static final int DELAY = 500;

    boolean emulation = false;

    int emulatedVolume = 10;
    int emulatedMaxVolume = 35;

    /*
    Handler handler;
    Runnable runnable;
    */

    BroadcastReceiver br;

    interface Callback {
        Activity getActivity();
        void receiveVolume(int volume);
        CanBusDriver getCanBusDriver();
    }

    Callback callback;

    public CarVolume(Callback callback) {
        this.callback = callback;
    }

    private void pollVolume() {
        int vol = getVolume();
        callback.receiveVolume(vol);
    }

    public int getVolume() {
        if (emulation) {
            return emulatedVolume;
        }
        return android.provider.Settings.System.getInt(callback.getActivity().getContentResolver(),VOLUME_PARAMETER_NAME, emulatedVolume);
    }

    public int getMaxVolume() {
        if (emulation) {
            return emulatedMaxVolume;
        }
        return android.provider.Settings.System.getInt(callback.getActivity().getContentResolver(), MAX_VOLUME_PARAMETER_NAME, emulatedMaxVolume);
    }

    public void setVolume(int volume) {
        int maxVol = getMaxVolume();
        if (volume > maxVol) {
            volume = maxVol;
        }
        if (volume < 0) {
            volume = 0;
        }
        if (emulation) {
            emulatedVolume = volume;
            return;
        }
        android.provider.Settings.System.putInt(callback.getActivity().getContentResolver(),VOLUME_PARAMETER_NAME, volume);

        if (paramVol) {
            if (callback.getCanBusDriver() != null) {
                int mtcVol = mtcGetRealVolume(volume, maxVol);
                callback.getCanBusDriver().setParams(VOLUME_PARAMETER_NAME + mtcVol);
            }
        }

        if (intentVol) {
            Intent intent = new Intent("com.microntek.VOLUME_CHANGED");
            intent.putExtra("maxvolume", getMaxVolume());
            intent.putExtra("volume", volume);
            intent.putExtra("threecats", true);
            callback.getActivity().sendBroadcast(intent);
        }
    }

    // ôóíêöèÿ èç android.microntek.service.MicrontekServer
    private int mtcGetRealVolume(int vol, int maxVol)
    {
        float perc = 100.0F * vol / maxVol;
        float att;
        if (perc < 20.0F) {
            att = perc * 3.0F / 2.0F;
        } else if (perc < 50.0F) {
            att = perc + 10.0F;
        } else {
            att = 20.0F + perc * 4.0F / 5.0F;
        }
        return (int)att;
    }

    public boolean paramVol = false;
    public boolean intentVol = false;

    public void start() {
        if (br == null) {
            br = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!intent.getBooleanExtra("threecats", false)) {
                        int volume = intent.getIntExtra("volume", 5);
                        callback.receiveVolume(volume);
                    }
                }
            };
            callback.getActivity().registerReceiver(br, new IntentFilter("com.microntek.VOLUME_CHANGED"));
        }

        /*
        if (handler == null) {
            handler = new Handler();
            runnable = new Runnable() {
                @Override
                public void run() {
                    pollVolume();
                    handler.postDelayed(this, DELAY);
                }
            };
            handler.postDelayed(runnable, DELAY);
        }
        */
    }

    public void stop() {
        if (br != null) {
            callback.getActivity().unregisterReceiver(br);
            br = null;
        }

        /*
        if (handler != null) {
            handler.removeCallbacks(runnable);
            handler = null;
        }
        */
    }

}
