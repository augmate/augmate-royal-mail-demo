package com.augmate.apps;

import android.content.Context;
import com.augmate.sdk.logger.Log;
import com.augmate.sdk.scanner.DecoderManager;
import com.augmate.sdk.scanner.decoder.scandit.Configuration;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class AugmateApplication extends android.app.Application {

    private int activityStackSize = 0;
    private static Context sContext;
    public static Context getContext() {
        return sContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        CalligraphyConfig.initDefault("fonts/GothamNarrow-Book.otf", R.attr.fontPath);
        sContext = this;
        Log.start(this);
        Log.debug("Application started");
        // if using the scandit scanner, configure it here
        DecoderManager.ScanditConfiguration = Configuration.createFromContext(getBaseContext());
    }

    // never called in production
    @Override
    public void onTerminate() {
        super.onTerminate();
        Log.debug("Application ended");
        Log.shutdown();
    }

    public int getActivityStackSize() {
        return activityStackSize;
    }
}
