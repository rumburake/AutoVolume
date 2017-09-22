package com.threecats.autovolume;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.microntek.CarManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;

public class VolumeService extends Service implements CanBusReceiver.Callback {
    private static final int MAX_REV = 5000; // RPM
    private static final int MAX_SPEED = 200; // km/h
    private static final int MAX_EFFECT = 33; // effect level

    private static final float REV_FACTOR = 1.0f;
    private static final float SPEED_FACTOR = 0.75f;

    private static final String VOLUME_PARAMETER_NAME = "av_volume=";
    private static final String MUTE_PARAMETER_NAME = "av_mute=";
    private static final String MAX_VOLUME_PARAMETER_NAME = "cfg_maxvolume=";
    private static final String VOLUME_CHANGED = "com.microntek.VOLUME_CHANGED";

    private CarManager carManager = null;
    private BroadcastReceiver volumeReceiver = null;
    private CanBusReceiver canBusReceiver;
    private CanBusDriver canBusDriver;

    private int rev;
    private int speed;
    private boolean revBarChanging;
    private boolean speedBarChanging;



    public int getEffect() {
        return effect;
    }

    public void setEffect(int effect) {
        this.effect = effect;
        getSharedPreferences("main", 0).edit().putInt("effect", effect).apply();
        setDynamicOutput(getVolume(), getVolumeMax());
    }

    int effect;

    public int getEffectMax() {
        return MAX_EFFECT;
    }

    public int getVolume() {
        return Settings.System.getInt(getContentResolver(),VOLUME_PARAMETER_NAME, 0);
    }

    public int getVolumeMax() {
        return Settings.System.getInt(getContentResolver(),MAX_VOLUME_PARAMETER_NAME, 30);
    }

    public int getRevMax() {
        return MAX_REV;
    }

    public int getSpeedMax() {
        return MAX_SPEED;
    }

    private int calculateGain() {
        if (effect == 0) {
            return 0;
        } else {
            int revSteps = (int) (rev / (REV_FACTOR * MAX_REV / effect));
            int speedSteps = (int) (speed / (SPEED_FACTOR * MAX_SPEED / effect));
            return revSteps + speedSteps;
        }
    }

    @Override
    public void receiveCanBus(Bundle bundle) {
        int rev = bundle.getInt(CanBusReceiver.REV, Integer.MIN_VALUE);
        if (rev != Integer.MIN_VALUE) {
            if (!revBarChanging) {
                if (muiCallback != null) {
                    muiCallback.updateRev(this.rev);
                }
                setRev(rev);
            }
        }
        double speed = bundle.getDouble(CanBusReceiver.SPEED, Double.MIN_VALUE);
        if (speed != Double.MIN_VALUE) {
            if (!speedBarChanging) {
                if (muiCallback != null) {
                    muiCallback.updateSpeed(this.speed);
                }
                setSpeed((int) speed);
            }
        }
    }

    @Override
    public Context getContext() {
        return this;
    }

    public void setRevBarChanging(boolean revBarChanging) {
        this.revBarChanging = revBarChanging;
    }

    public void setSpeedBarChanging(boolean speedBarChanging) {
        this.speedBarChanging = speedBarChanging;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
        setDynamicOutput(getVolume(), getVolumeMax());
    }

    public void setRev(int rev) {
        this.rev = rev;
        setDynamicOutput(getVolume(), getVolumeMax());
    }

    class VolumeBinder extends Binder {
        VolumeService getService() {
            return VolumeService.this;
        }
    }

    private final IBinder mBinder = new VolumeBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        effect = getSharedPreferences("main", 0).getInt("effect", 0);

        volumeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int volume = intent.getIntExtra("volume", 0);
                int volumeMax = intent.getIntExtra("volumemax", 30);
                if (muiCallback != null) {
                    muiCallback.updateVolume(volume, volumeMax);
                }
                setDynamicOutput(volume, volumeMax);
            }
        };
        registerReceiver(volumeReceiver, new IntentFilter(VOLUME_CHANGED));

        carManager = new CarManager();

        canBusReceiver = CanBusReceiver.RegisterCarReceiver(this);

        canBusDriver = new CanBusDriver();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(volumeReceiver);

        CanBusReceiver.UnregisterCarReceiver(canBusReceiver);


        canBusDriver.stop();
    }


    interface UICallback {
        void updateVolume(int volume, int volumeMax);
        void updateOutput(int output);
        void updateSpeed(int speed);
        void updateRev(int rev);
    }

    UICallback muiCallback = null;

    public void register(UICallback uiCallback) {
        muiCallback = uiCallback;
    }

    public void unregister() {
        muiCallback = null;
    }

    void setDynamicOutput(int volume, int volumeMax) {
        int output = (int)(100F * volume / volumeMax);
        int gain = calculateGain();
        output += gain;
        if (output > 100) {
            output = 100;
        }
        setOutput(output);
    }

    private void setOutput(int output) {
        if (muiCallback != null) {
            muiCallback.updateOutput(output);
        }
        carManager.setParameters(VOLUME_PARAMETER_NAME + output);
    }
}
