package com.augmate.apps.common.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.ViewFlipper;

import com.augmate.apps.common.ErrorPrompt;
import com.augmate.apps.common.FlowUtils;
import com.augmate.apps.scanner.ScannerActivity;
import com.augmate.sdk.logger.Log;

import java.io.Serializable;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static com.augmate.apps.common.FlowUtils.TRANSITION_TIMEOUT;

public class BaseActivity extends Activity {
    public static final int REQUEST_BARCODE_SCAN = 0x01;
    public static final int REQUEST_PROMPT = 0x02;
    protected Handler mHandler = new Handler();
    PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        float brightness = getSharedPreferences(getApplication().getPackageName(),MODE_PRIVATE).getFloat("BRIGHTNESS",0.5f);
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness =brightness;
        getWindow().setAttributes(params);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK , "");
        wakeLock.acquire(FlowUtils.SCREEN_ON_TIME);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            wakeLock.release();
        }catch (Throwable ignored) {

        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(new CalligraphyContextWrapper(newBase));
    }

    public Handler getHandler() {
        return mHandler;
    }

    public void rescan() {
        getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startScanner();
            }
        }, TRANSITION_TIMEOUT);
    }

    public void startScanner() {
        Log.debug("Starting scanner activity..");
        Intent intent = new Intent(this, ScannerActivity.class);
        startActivityForResult(intent, REQUEST_BARCODE_SCAN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_BARCODE_SCAN) {
            String value = data.getStringExtra(ScannerActivity.BARCODE);
            boolean exited = data.getBooleanExtra(ScannerActivity.EXITED,false);
            boolean timeout = data.getBooleanExtra(ScannerActivity.TIMEOUT,false);
            processBarcodeScanning(value, exited, resultCode == Activity.RESULT_OK, timeout);
        } else if (requestCode == REQUEST_PROMPT) {
            if (resultCode == RESULT_CANCELED) {
                handlePromptReturn();
            }
        }
        super.onActivityResult(requestCode,resultCode, data);
    }

    public void processBarcodeScanning(String barcodeString, boolean wasExited,
                                       boolean wasSuccessful, boolean wasTimedOut) {
        throw new RuntimeException("This method should be implemented if you need to scan");
    }

    public void handlePromptReturn() {
        throw new RuntimeException("This method should be implemented if you need to scan");
    }

    protected Animation.AnimationListener getAnimationListener(final ViewFlipper flipper){
        return new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                if (flipper != null) {
                    if (flipper.getDisplayedChild() == flipper.getChildCount() - 1) {
                        flipper.stopFlipping();
                    }
                }
            }
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationRepeat(Animation animation) {}
        };
    }

    public void showError(ErrorPrompt prompt) {
        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra(MessageActivity.ERROR, true);
        intent.putExtra(MessageActivity.MESSAGE, prompt.getString());
        startActivityForResult(intent, REQUEST_PROMPT);
    }

    public void showConfirmation(String confirmationText,Class clazz, Serializable data) {
        Intent intent = new Intent(this, MessageActivity.class);
        intent.putExtra(MessageActivity.MESSAGE, confirmationText);
        if (clazz != null) {
            intent.putExtra(MessageActivity.CLASS, clazz.getName());
        }
        if (data != null) {
            intent.putExtra(MessageActivity.DATA, data);
        }
        startActivityForResult(intent, REQUEST_PROMPT);
    }
}