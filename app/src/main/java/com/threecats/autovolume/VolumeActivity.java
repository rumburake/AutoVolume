package com.threecats.autovolume;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

public class VolumeActivity extends Activity implements VolumeService.UICallback {

    VolumeService mService = null;

    SeekBar seekBarEffect;
    TextView textViewEffect;
    TextView textViewEffectMax;

    ProgressBar progressBarVolume;
    TextView textViewVolume;
    TextView textViewVolumeMax;

    ProgressBar progressBarOutput;
    TextView textViewOutput;
    TextView textViewOutputMax;

    SeekBar seekBarRev;
    TextView textViewRev;
    TextView textViewRevMax;

    SeekBar seekBarSpeed;
    TextView textViewSpeed;
    TextView textViewSpeedMax;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.volume_activity);
    }

    void setupRevUI() {
        seekBarRev = (SeekBar)findViewById(R.id.seekBarRev);
        textViewRev = (TextView)findViewById(R.id.textViewRev);
        textViewRevMax = (TextView)findViewById(R.id.textViewRevMax);

        int revMax = mService.getRevMax();

        seekBarRev.setMax(revMax);
        seekBarRev.setProgress(0);
        textViewRev.setText("" + 0 + " RPM");
        textViewRevMax.setText("" + revMax + " RPM");

        seekBarRev.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    textViewRev.setText("" + progress + " RPM");
                    mService.setRev(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mService.setRevBarChanging(true);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mService.setRevBarChanging(false);
            }
        });
    }

    void setupSpeedUI() {
        seekBarSpeed = (SeekBar)findViewById(R.id.seekBarSpeed);
        textViewSpeed = (TextView)findViewById(R.id.textViewSpeed);
        textViewSpeedMax = (TextView)findViewById(R.id.textViewSpeedMax);

        int speedMax = mService.getSpeedMax();

        seekBarSpeed.setMax(speedMax);
        seekBarSpeed.setProgress(0);
        textViewSpeed.setText("" + 0 + " km/h");
        textViewSpeedMax.setText("" + speedMax + "km/h");

        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    textViewSpeed.setText("" + progress + " km/h");
                    mService.setSpeed(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mService.setSpeedBarChanging(true);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mService.setSpeedBarChanging(false);
            }
        });
    }

    void setupVolumeUI() {
        progressBarVolume = (ProgressBar)findViewById(R.id.progressBarVolume);
        textViewVolume = (TextView)findViewById(R.id.textViewVolume);
        textViewVolumeMax = (TextView)findViewById(R.id.textViewVolumeMax);

        int volume = mService.getVolume();
        int volumeMax = mService.getVolumeMax();

        progressBarVolume.setMax(volumeMax);
        progressBarVolume.setProgress(volume);
        textViewVolume.setText("" + volume);
        textViewVolumeMax.setText("" + volumeMax);
    }

    void setupOutputUI() {
        progressBarOutput = (ProgressBar)findViewById(R.id.progressBarOutput);
        textViewOutput = (TextView)findViewById(R.id.textViewOutput);
        textViewOutputMax = (TextView)findViewById(R.id.textViewOutputMax);

        int volume = mService.getVolume();
        int volumeMax = mService.getVolumeMax();

        int output = (int) (100F * volume / volumeMax);

        progressBarOutput.setMax(100);
        progressBarOutput.setProgress(output);
        textViewOutput.setText("" + output + " %");
        textViewOutputMax.setText("100 %");
    }

    void setupEffectUI() {
        seekBarEffect = (SeekBar)findViewById(R.id.seekBarEffect);
        textViewEffect = (TextView)findViewById(R.id.textViewEffect);
        textViewEffectMax = (TextView)findViewById(R.id.textViewEffectMax);

        seekBarEffect.setMax(mService.getEffectMax());
        seekBarEffect.setProgress(mService.getEffect());
        textViewEffect.setText("" + mService.getEffect());
        textViewEffectMax.setText("" + mService.getEffectMax());

        seekBarEffect.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mService.setEffect(progress);
                textViewEffect.setText("" + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, VolumeService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mService != null) {
            mService.unregister();
            unbindService(mConnection);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mConnection = null;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mService = ((VolumeService.VolumeBinder)binder).getService();

            setupEffectUI();
            setupVolumeUI();
            setupOutputUI();
            setupRevUI();
            setupSpeedUI();

            mService.register(VolumeActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @Override
    public void updateVolume(int volume, int volumeMax) {
        progressBarVolume.setMax(volumeMax);
        progressBarVolume.setProgress(volume);
        textViewVolume.setText("" + volume);
        textViewVolumeMax.setText("" + volumeMax);
    }

    @Override
    public void updateOutput(int output) {
        progressBarOutput.setProgress(output);
        textViewOutput.setText("" + output + " %");
    }

    @Override
    public void updateSpeed(int speed) {
        seekBarSpeed.setProgress(speed);
        textViewSpeed.setText("" + speed + " km/h");
    }

    @Override
    public void updateRev(int rev) {
        seekBarRev.setProgress(rev);
        textViewRev.setText("" + rev + " RPM");
    }

    /////////// ----- TEST HERE

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



}
