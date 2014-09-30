package com.augmate.apps.carloading;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.augmate.apps.R;
import com.augmate.apps.datastore.CarLoadingDataStore;
import com.augmate.sdk.data.InternetChecker;
import com.augmate.sdk.logger.Log;

public class UpsDataSyncActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ups_data_sync_activity);

        final TextView cnxView = (TextView) findViewById(R.id.internet_cnx_state);

        if (new InternetChecker().isConnected(this)) {
            Log.info("Connected to the internet");
            startUpsDataDownload();
        } else {
            cnxView.setVisibility(View.VISIBLE);
        }
    }

    private void startUpsDataDownload() {
        final ProgressBar progressBarView = (ProgressBar) findViewById(R.id.sync_progress_bar);
        final TextView downloadView = (TextView) findViewById(R.id.download_state);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                progressBarView.setVisibility(View.VISIBLE);
                downloadView.setText("Download started...");
                Log.info("Download started");

                final CarLoadingDataStore carLoadingDataStore = new CarLoadingDataStore(UpsDataSyncActivity.this);

                carLoadingDataStore.findLoadForTrackingNumber("1Z0098730367847638");
                carLoadingDataStore.findLoadForTrackingNumber("1Z2F57F00353700997");
                carLoadingDataStore.findLoadForTrackingNumber("1Z2967380390168881");
                carLoadingDataStore.findLoadForTrackingNumber("1Z1730820346813845");
            }
        }, 2000);
    }
}
