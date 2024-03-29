package com.augmate.apps.datastore;

import android.content.Context;
import com.augmate.sdk.data.AugmateData;
import com.augmate.sdk.data.PackageCarLoad;
import com.augmate.sdk.logger.Log;
import com.parse.DeleteCallback;
import com.parse.SaveCallback;

public class CarLoadingDataStore {
    private final AugmateData<PackageCarLoad> augmateData;

    public CarLoadingDataStore(Context context) {
        augmateData = new AugmateData<>(context);
    }

    // HACK: bypassing slow and crashy parseobject local datastore
    public String findLoadForTrackingNumberHack(String trackingNumber) {
        return AugmateData.getCarLoadPositionFromPackageTrackingNumber(trackingNumber);
    }

    public PackageCarLoad findLoadForTrackingNumber(String trackingNumber) {
        PackageCarLoad packageCarLoad = augmateData.cacheFind(PackageCarLoad.class, PackageCarLoad.TRACKING_NUMBER_KEY, trackingNumber);

        if(packageCarLoad != null) {
            Log.info("load information for %s available in cache", packageCarLoad.getLoadPosition());
            Log.info("tracking number %s has load position %s", packageCarLoad.getTrackingNumber(), packageCarLoad.getLoadPosition());
        }else{
            Log.warn("Car load info not found in cache for %s", trackingNumber);
        }

        return packageCarLoad;
    }

    public void downloadCarLoadDataToCache(final String carLoad, SaveCallback callback) {
        augmateData.pullToCache(PackageCarLoad.class, PackageCarLoad.LOAD_POSITION_KEY, carLoad, callback);
    }

    public void wipeLocalCache(DeleteCallback callback){
        augmateData.clearCache(PackageCarLoad.class, callback);
    }
    public int numberOfPackages() {
        return augmateData.countCached(PackageCarLoad.class);
    }

    public int numberOfPackages(String load) {
        //return augmateData.countCached(PackageCarLoad.class, PackageCarLoad.LOAD_POSITION_KEY, load);
        return AugmateData.countCachedHack(load);
    }
}
