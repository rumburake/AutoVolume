package com.microntek.threecats.autovolume;

import android.os.Handler;
import android.util.Log;

import com.microntek.CarManager;

public class CanBusDriver {
    private static final String TAG = CanBusDriver.class.getSimpleName();

    private static final int POLL_DELAY = 500;

    private CarManager carManager;
    private Handler handler;
    private Runnable runnable;

    public CanBusDriver() {
        carManager = new CarManager();
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                requestCarData();
                handler.postDelayed(this, POLL_DELAY);
            }
        };
        handler.postDelayed(runnable, POLL_DELAY);
    }

    public void stop() {
        handler.removeCallbacks(runnable);
    }

    public void setParams(String str) {
        Log.d(TAG, "Set Params: [" + str + "]");
        this.carManager.setParameters(str);
    }

    public String getParams(String str) {
        Log.d(TAG, "Get Params: [" + str + "]");
        return this.carManager.getParameters(str);
    }

    void requestCarData() {
        jt((byte)65, (byte)2);
    }

    /** voodoo incantation starts here **/
    private void jt(byte b, byte b2) {
        ju((byte) -112, new byte[]{b, b2}, 2);
    }

    private void ju(byte b, byte[] bArr, int i) {
        int i2 = 0;
        byte[] bArr2 = new byte[(i + 4)];
        bArr2[0] = (byte) 46;
        bArr2[1] = b;
        bArr2[2] = (byte) (i & 255);
        int i3 = (short) (bArr2[1] + bArr2[2]);
        while (i2 < i) {
            bArr2[i2 + 3] = bArr[i2];
            i3 = (short) (i3 + bArr2[i2 + 3]);
            i2++;
        }
        bArr2[i + 3] = (byte) ((i3 & 255) ^ 255);
        jv(bArr2);
    }

    private void jv(byte[] bArr) {
        int length = bArr.length;
        String str = "";
        for (int i = 0; i < length; i++) {
            str = str + jx(bArr[i] & 255);
            if (i < length - 1) {
                str = str + ",";
            }
        }
        setParams("canbus_rsp=" + str);
    }

    private String jx(int i) {
        int i2 = i / 10;
        int i3 = i % 10;
        return i2 == 0 ? "" + i3 : "" + i2 + i3;
    }

    /** voodoo incantation end here **/
}
