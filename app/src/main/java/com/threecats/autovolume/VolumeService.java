package com.threecats.autovolume;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.microntek.CarManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

public class VolumeService extends Service implements CanBusReceiver.Callback {
    private static final int MAX_REV = 5000; // RPM
    private static final int MAX_SPEED = 200; // km/h
    private static final int MAX_EFFECT = 9; // effect level

    private static final float REV_FACTOR = 2.0f;
    private static final float SPEED_FACTOR = 0.75f;
    private static final float EFFECT_AMP = 6.0f;
    private static final int TOLERANCE = 1; // tolerance in output volume

    private static final String VOLUME_PARAMETER_NAME = "av_volume=";
    private static final String MUTE_PARAMETER_NAME = "av_mute=";
    private static final String MAX_VOLUME_PARAMETER_NAME = "cfg_maxvolume=";
    private static final String VOLUME_CHANGED = "com.microntek.VOLUME_CHANGED";
    private static final String TAG = VolumeService.class.getSimpleName();

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
        resetHysteresis();
        setDynamicOutput(getVolume(), getVolumeMax());
    }

    int effect;

    public boolean getMonitor() {
        return monitor;
    }

    public void setMonitor(boolean monitor) {
        this.monitor = monitor;
        getSharedPreferences("main", 0).edit().putBoolean("monitor", monitor).apply();
    }

    boolean monitor;

    public int getEffectMax() {
        return MAX_EFFECT;
    }

    public boolean getMute() {
        return "true".equals(carManager.getParameters(MUTE_PARAMETER_NAME));
    }

    public int getVolume() {
        return Settings.System.getInt(getContentResolver(),VOLUME_PARAMETER_NAME, 0);
    }

    public int getVolumeMax() {
        return Settings.System.getInt(getContentResolver(),MAX_VOLUME_PARAMETER_NAME, 30);
    }

    public int getRev() {
        return rev;
    }

    public int getSpeed() {
        return speed;
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
            int revSteps = (int) (rev / (REV_FACTOR * MAX_REV / effect / EFFECT_AMP));
            int speedSteps = (int) (speed / (SPEED_FACTOR * MAX_SPEED / effect / EFFECT_AMP));
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
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (effect == 0) {
            stopSelf();
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Service is UP!");

        effect = getSharedPreferences("main", 0).getInt("effect", 0);
        monitor = getSharedPreferences("main", 0).getBoolean("monitor", false);

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

        initOVerlay();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Service is DOWN!");

        unregisterReceiver(volumeReceiver);

        CanBusReceiver.UnregisterCarReceiver(canBusReceiver);

        canBusDriver.stop();

        h.removeCallbacks(r);
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
        resetHysteresis();
        setDynamicOutput(getVolume(), getVolumeMax());
    }

    public void unregister() {
        muiCallback = null;
    }

    public static int mtcGetRealVolume(int vol, int maxVol)
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

    void setDynamicOutput(int volume, int volumeMax) {
        if (!getMute()) {
            int output = mtcGetRealVolume(volume, volumeMax);
            int gain = calculateGain();
            output += gain;
            if (output > 100) {
                output = 100;
            }
            setOutput(output);
        }
    }

    View overlayView;
    TextView overlayText;
    ImageView overlayUp;
    ImageView overlayDown;
    WindowManager.LayoutParams overlayParams;
    boolean overlayOn;
    WindowManager wm;
    Runnable r;
    Handler h = new Handler();
    private static final int OVERLAY_TIME = 1000;

    void initOVerlay() {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        overlayView = inflater.inflate(R.layout.overlay, null, false);
        overlayText = (TextView)overlayView.findViewById(R.id.overlayText);
        overlayUp = (ImageView)overlayView.findViewById(R.id.overlayUp);
        overlayDown = (ImageView)overlayView.findViewById(R.id.overlayDown);

        overlayParams = new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
        overlayParams.alpha = 0.7F;
        overlayParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
        overlayParams.x = 0;
        overlayParams.y = 0;
        overlayParams.width = 120;
        overlayParams.height = 120;

        wm = (WindowManager)getSystemService(WINDOW_SERVICE);

        r = new Runnable() {
            @Override
            public void run() {
                if (overlayOn) {
                    wm.removeView(overlayView);
                    overlayOn = false;
                }
            }

        };

    }

    void showOverlay() {
        if (!overlayOn) {
            overlayOn = true;
            wm.addView(overlayView, overlayParams);
        }
        overlayText.setText("" + currentOutput + "%");
        if (lastOutputDelta == 0) {
            overlayUp.setVisibility(View.INVISIBLE);
            overlayDown.setVisibility(View.INVISIBLE);
        } else if (lastOutputDelta > 0){
            overlayUp.setVisibility(View.VISIBLE);
            overlayDown.setVisibility(View.INVISIBLE);
        } else {
            overlayUp.setVisibility(View.INVISIBLE);
            overlayDown.setVisibility(View.VISIBLE);
        }
        h.removeCallbacks(r);
        h.postDelayed(r, OVERLAY_TIME);
    }

    private int currentOutput = -1;
    private int lastOutputDelta = 0;

    void resetHysteresis() {
        currentOutput = -1;
        lastOutputDelta = 0;
    }

    private void setOutput(int output) {
        if (output != currentOutput) {
            boolean absorb = false;
            if (lastOutputDelta > 0 && output < currentOutput && output >= currentOutput - TOLERANCE) {
                absorb = true;
            } else if (lastOutputDelta < 0 && output > currentOutput && output <= currentOutput + TOLERANCE) {
                absorb = true;
            }
            if (!absorb) {
                if (currentOutput != -1) {
                    lastOutputDelta = output - currentOutput;
                }
                currentOutput = output;
                if (monitor) {
                    showOverlay();
                }

                if (muiCallback != null) {
                    muiCallback.updateOutput(output);
                }
                carManager.setParameters(VOLUME_PARAMETER_NAME + output);
            }
        }
    }
}
