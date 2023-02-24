package com.microntek.threecats.autovolume;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity implements CanBusReceiver.Callback, CarVolume.Callback {

    public static final int MAX_REV = 5000; // RPM
    public static final int MAX_SPEED = 200; // km/h
    public static final int MAX_EFFECT = 11; // effect level

    private static final float REV_FACTOR = 1.0f;
    private static final float SPEED_FACTOR = 0.75f;

    private TextView debugReceiverView;
    private TextView debugDriverView;
    private TextView debugVolView;

    TextView revView;
    TextView speedView;
    TextView battView;
    TextView tempView;
    TextView distView;

    CheckBox checkBoxSend;
    CheckBox checkBoxReceive;
    CheckBox checkBoxParamVol;
    CheckBox checkBoxIntentVol;

    CanBusReceiver canBusReceiver;
    CanBusDriver canBusDriver;
    CarVolume carVolume;

    SeekBar revBar;
    boolean revBarChanging;
    SeekBar speedBar;
    boolean speedBarChanging;

    SeekBar effectBar;
    SeekBar volumeBar;
    boolean volumeBarChanging;
    ProgressBar staticVolumeBar;

    private int speed;
    private int rev;
    private int effect;
    private int volume;
    private int staticVolume;

    private int volMax;

    TextView effectView;
    TextView volumeView;
    TextView staticVolumeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        carVolume = new CarVolume(this);

        debugReceiverView = (TextView) findViewById(R.id.debugReceiverView);
        debugDriverView = (TextView) findViewById(R.id.debugDriverView);
        revView = (TextView) findViewById(R.id.revView);
        speedView = (TextView) findViewById(R.id.speedView);
        battView = (TextView) findViewById(R.id.battView);
        tempView = (TextView) findViewById(R.id.tempView);
        distView = (TextView) findViewById(R.id.distView);

        checkBoxSend = (CheckBox) findViewById(R.id.checkBoxSend);
        checkBoxSend.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                switchSend(b);
            }
        });

        checkBoxReceive = (CheckBox) findViewById(R.id.checkBoxReceive);
        checkBoxReceive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                switchReceive(b);
            }
        });

        checkBoxParamVol = (CheckBox) findViewById(R.id.checkBoxParamVol);
        checkBoxParamVol.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                carVolume.paramVol = b;
            }
        });
        carVolume.paramVol = checkBoxParamVol.isChecked();

        checkBoxIntentVol = (CheckBox) findViewById(R.id.checkBoxIntentVol);
        checkBoxIntentVol.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                carVolume.intentVol = b;
            }
        });
        carVolume.intentVol = checkBoxIntentVol.isChecked();

        revBar = (SeekBar) findViewById(R.id.revBar);
        revBar.setMax(MAX_REV);
        revBar.setProgress(rev);
        revBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setRev(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                revBarChanging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                revBarChanging = false;
            }
        });

        speedBar = (SeekBar) findViewById(R.id.speedBar);
        speedBar.setMax(MAX_SPEED);
        speedBar.setProgress(speed);
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setSpeed(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                speedBarChanging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                speedBarChanging = false;
            }
        });

        effect = getSharedPreferences("main", 0).getInt("effect", 0);

        effectBar = (SeekBar) findViewById(R.id.effectBar);
        effectBar.setMax(MAX_EFFECT);
        effectBar.setProgress(effect);
        effectBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setEffect(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                getSharedPreferences("main", 0).edit().putInt("effect", getEffect()).apply();
            }
        });

        effectView = (TextView) findViewById(R.id.effectView);


        volumeBar = (SeekBar) findViewById(R.id.volumeBar);
        volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    setVolume(progress, true);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                volumeBarChanging = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                volumeBarChanging = false;
            }
        });

        volumeView = (TextView) findViewById(R.id.volumeView);

        staticVolumeBar = (ProgressBar) findViewById(R.id.staticVolumeBar);

        staticVolumeView = (TextView) findViewById(R.id.staticVolumeView);

        volMax = carVolume.getMaxVolume();
        volume = carVolume.getVolume();

        volumeBar.setMax(volMax);
        volumeBar.setProgress(volume);
        volumeView.setText("Volume: " + volume + " / " + volMax);

        staticVolumeBar.setMax(volMax);
        staticVolumeBar.setProgress(staticVolume);
        staticVolumeView.setText("Static Volume: " + staticVolume);

        debugVolView = (TextView) findViewById(R.id.debugVolView);

        this.startService(new Intent(this, VolumeService.class));
    }

    private void switchSend(boolean on) {
        if (on) {
            if (canBusDriver == null) {
                canBusDriver = new CanBusDriver();
            }
        } else {
            if (canBusDriver != null) {
                canBusDriver.stop();
                canBusDriver = null;
            }
        }
    }

    private void switchReceive(boolean on) {
        if (on) {
            if (canBusReceiver == null) {
                canBusReceiver = CanBusReceiver.RegisterCarReceiver(this);
            }
        } else {
            if (canBusReceiver != null) {
                CanBusReceiver.UnregisterCarReceiver(canBusReceiver);
                canBusReceiver = null;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        switchSend(checkBoxSend.isChecked());
        switchReceive(checkBoxReceive.isChecked());
        carVolume.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        switchSend(false);
        switchReceive(false);
        carVolume.stop();
    }

    @Override
    public void receiveCanBus(Bundle bundle) {
        String debug = bundle.getString(CanBusReceiver.DEBUG);
        if (debug != null) {
            debugReceiverView.setText(debug);
        }
        int rev = bundle.getInt(CanBusReceiver.REV, Integer.MIN_VALUE);
        if (rev != Integer.MIN_VALUE) {
            if (!revBarChanging) {
                setRev(rev);
            }
        }
        double speed = bundle.getDouble(CanBusReceiver.SPEED, Double.MIN_VALUE);
        if (speed != Double.MIN_VALUE) {
            if (!speedBarChanging) {
                setSpeed((int) speed);
            }
        }
        double batt = bundle.getDouble(CanBusReceiver.BATT, Double.MIN_VALUE);
        if (batt != Double.MIN_VALUE) {
            battView.setText("Battery: " + String.format("%.2f", batt) + " V");
        }
        double temp = bundle.getDouble(CanBusReceiver.TEMP, Double.MIN_VALUE);
        if (temp != Double.MIN_VALUE) {
            tempView.setText("Temperature: " + String.format("%.1f", temp) + " C");
        }
        int dist = bundle.getInt(CanBusReceiver.DIST, Integer.MIN_VALUE);
        if (dist != Integer.MIN_VALUE) {
            distView.setText("Total Distance: " + dist + " km");
        }
    }

    @Override
    public Context getContext() {
        return this;
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void receiveVolume(int volume) {
        debugVolView.setText("Received Volume: " + volume);
        setVolume(volume, false);
    }

    @Override
    public CanBusDriver getCanBusDriver() {
        return canBusDriver;
    }

    public void testBroadcastCan(View view) {
        Intent bi = new Intent();
        bi.setAction("com.microntek.sync");
        byte[] data = new byte[20];
        for (int i = 0; i < 20; ++i) {
            if (i == 2) {
                data[i] = 2;
            } else {
                data[i] = (byte) (Math.random() * 256 - 128);
            }
        }
        bi.putExtra("syncdata", data);
        sendBroadcast(bi);
    }

    int volCycle = 3;

    public void testBroadcastVol(View view) {
        if (volCycle++ > 5) {
            volCycle = 3;
        }
        Intent bi = new Intent();
        bi.setAction("com.microntek.VOLUME_CHANGED");
        bi.putExtra("volumemax", 30);
        bi.putExtra("volume", volCycle);
        sendBroadcast(bi);
    }

    public void setSpeed(int speed) {
        this.speed = speed;
        speedView.setText("Speed: " + speed + " km/h");
        speedBar.setProgress(speed);
        setDynamicVolume();
    }

    public int getSpeed() {
        return speed;
    }

    public void setRev(int rev) {
        this.rev = rev;
        revView.setText("Engine Revs: " + rev + " RPM");
        revBar.setProgress(rev);
        setDynamicVolume();
    }

    public int getRev() {
        return rev;
    }

    public void setEffect(int effect) {
        this.effect = effect;
        effectView.setText("Effect Level: " + effect);
        setDynamicVolume();
    }

    public int getEffect() {
        return effect;
    }

    public void setVolume(int volume, boolean toCar) {
        if (volume != this.volume) {
            this.volume = volume;
            volumeView.setText("Volume: " + volume + " / " + volMax);
            volumeBar.setProgress(volume);
            if (toCar) {
                carVolume.setVolume(volume);
            }
            calculateStaticVolume();
        }
    }

    public void setDynamicVolume() {
        int revSteps = 0;
        int speedSteps = 0;
        int newVolume = 0;
        if (effect != 0) {
            revSteps = (int) (rev / (REV_FACTOR * MAX_REV / effect));
            speedSteps = (int) (speed / (SPEED_FACTOR * MAX_SPEED / effect));
            newVolume = staticVolume + revSteps + speedSteps;
            if (newVolume > volMax) {
                newVolume = volMax;
            }
        } else {
            newVolume = staticVolume;
        }
        setVolume(newVolume, true);
    }

    public void calculateStaticVolume() {
        int revSteps = 0;
        int speedSteps = 0;
        if (effect != 0) {
            revSteps = (int) (rev / (REV_FACTOR * MAX_REV / effect));
            speedSteps = (int) (speed / (SPEED_FACTOR * MAX_SPEED / effect));
            staticVolume = volume - revSteps - speedSteps;
            if (staticVolume < 0) {
                staticVolume = 0;
            }
        } else {
            staticVolume = volume;
        }
        staticVolumeView.setText("Static Volume: " + staticVolume + " Rev: " + revSteps + " Speed: " + speedSteps);
        staticVolumeBar.setProgress(staticVolume);
    }
}
