package com.threecats.autovolume;

import android.app.Activity;
import android.content.Intent;
import android.microntek.CarManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.w3c.dom.Text;

public class TestVolActivity extends Activity {
    private static final String TAG = TestVolActivity.class.getName();

    TextView textView;
    EditText editText;
    CarManager carManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_vol);
        textView = (TextView)findViewById(R.id.textView);
        editText = (EditText)findViewById(R.id.editText);

        carManager = new CarManager();
    }

    int inputVal() {
        return Integer.parseInt(editText.getText().toString());
    }


    public void getVolume(View view) {
        int i = android.provider.Settings.System.getInt(getContentResolver(), "av_volume=", -1);
        textView.setText("Get Vol: " + i);
    }

    public void getMaxVol(View view) {
        Log.d(TAG, "getMaxVol()");
        int i = android.provider.Settings.System.getInt(getContentResolver(), "cfg_maxvolume", -1);
        textView.setText("Get Max Vol: " + i);
    }


    public void testBroadcastVol(View view) {
        Log.d(TAG, "testBroadcastVol()");
        Intent bi = new Intent();
        bi.setAction("com.microntek.VOLUME_CHANGED");
        bi.putExtra("maxvolume", 30);
        bi.putExtra("volume", inputVal());
        sendBroadcast(bi);
    }

    public void testBroadcastCommandSet(View view) {
        Log.d(TAG, "testBroadcastCommandSet()");
        Intent bi = new Intent();
        bi.setAction("com.microntek.VOLUME_SET");
        bi.putExtra("volume", inputVal());
        sendBroadcast(bi);
    }

    public void testBroadcastCommandUp(View view) {
        Log.d(TAG, "testBroadcastCommandUp()");
        Intent bi = new Intent();
        bi.setAction("com.microntek.VOLUME_SET");
        bi.putExtra("type", "add");
        sendBroadcast(bi);
    }

    public void testBroadcastCommandDown(View view) {
        Log.d(TAG, "testBroadcastCommandDown()");
        Intent bi = new Intent();
        bi.setAction("com.microntek.VOLUME_SET");
        bi.putExtra("type", "sub");
        sendBroadcast(bi);
    }

    public void testSetVolSys(View view) {
        Log.d(TAG, "testSetVolSys()");
        android.provider.Settings.System.putInt(getContentResolver(), "av_volume=", inputVal());
    }

    public void testSetVolMtcd(View view) {
        Log.d(TAG, "testSetVolMtcd()");
        int mtcVol = mtcGetRealVolume(inputVal(), 30);
        carManager.setParameters("av_volume=" + mtcVol);
    }

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


}
