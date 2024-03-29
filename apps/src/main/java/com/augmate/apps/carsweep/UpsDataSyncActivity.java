package com.augmate.apps.carsweep;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.augmate.apps.R;
import com.augmate.apps.datastore.CarLoadingDataStore;
import com.augmate.sdk.data.InternetChecker;
import com.augmate.sdk.data.callbacks.CacheCallback;
import com.augmate.sdk.logger.Log;
import com.parse.ParseException;
import roboguice.activity.RoboActivity;
import roboguice.inject.InjectExtra;
import roboguice.inject.InjectView;

import java.util.ArrayList;
import java.util.List;


public class UpsDataSyncActivity extends RoboActivity {
    public static final String EXTRA_CAR_LOAD = "EXTRA_CAR_LOAD";

    private List<String> carLoads = new ArrayList<>();
    private List<String> loadsDone = new ArrayList<>();
    private CarLoadingDataStore carLoadingDataStore;

    @InjectView(R.id.sync_progress_bar)
    ProgressBar progressBarView;

    @InjectView(R.id.download_state)
    TextView downloadView;

    @InjectView(R.id.internet_cnx_state)
    TextView cnxView;

    @InjectExtra(EXTRA_CAR_LOAD) String carLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.ups_data_sync_activity);

        carLoadingDataStore = new CarLoadingDataStore(UpsDataSyncActivity.this);

        carLoads.add(carLoad);
    }

    @Override
    protected void onStart() {
        super.onStart();
        startUpsDataDownload();

        if (new InternetChecker().isConnected(this)) {
            Log.info("Connected to the internet");
        } else {
            Log.info("Not connected to the internet!");
            cnxView.setVisibility(View.VISIBLE);
            findViewById(R.id.download_state).setVisibility(View.INVISIBLE);
        }
    }

    private void startUpsDataDownload() {
        downloadView.setText("Downloading Package List..");
        progressBarView.setVisibility(View.VISIBLE);
        Log.info("Download started");

        for (final String load : carLoads) {
            carLoadingDataStore.downloadCarLoadDataToCache(load, new CacheCallback() {
                @Override
                public void success() {
                    Log.info("Done downloading car load data for %s", load);
                    loadFinished(load);
                }

                @Override
                public void error(ParseException e) {
                    Log.info("Could not download car load data for %s", load);
                }
            });
        }
    }

    //Thread safe due to SaveCallback being called on main thread.
    private void loadFinished(String load) {
        loadsDone.add(load);

        if (loadsDone.size() == carLoads.size()) {
            progressBarView.setVisibility(View.INVISIBLE);

            String loadStr = "";
            for (String l : loadsDone) {
                loadStr += String.format("%s [%d]\n", l, carLoadingDataStore.numberOfPackages(l));
            }

            downloadView.setText("Download car load \n\n" + loadStr);

            delayedFinish();
        }
    }

    private void delayedFinish() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.info("UPS Download complete");
                setResult(Activity.RESULT_OK);
                finish();
            }
        }, 1500);
    }
}
