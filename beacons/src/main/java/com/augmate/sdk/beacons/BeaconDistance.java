package com.augmate.sdk.beacons;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import com.augmate.sdk.logger.Log;
import com.augmate.sdk.logger.What;

import java.util.*;
import java.util.concurrent.*;

public class BeaconDistance implements BluetoothAdapter.LeScanCallback {
    private BluetoothAdapter bluetoothAdapter;
    private Context context;

    private Map<String, BeaconInfo> beaconInfos = new ConcurrentHashMap<>();
    private ScheduledFuture<?> scheduledFuture;

    public void configureFromContext(Context ctx) {
        context = ctx;
    }

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public void startListening() {
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // scan for 2 seconds, wait 1 second, repeat
        scheduledFuture = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                Log.debug("Starting Short Burst BLE Scan..");
                bluetoothAdapter.startLeScan(BeaconDistance.this);

                // stop 2 seconds later
                scheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        Log.debug("Stopping BLE Scan");
                        bluetoothAdapter.stopLeScan(BeaconDistance.this);
                    }
                }, 2, TimeUnit.SECONDS);
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    public void stopListening() {
        if(scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
    }

    /**
     * may be called from UI-thread or a periodic timer's thread
     * this was rewritten a while ago. it's fast. <1 msec for 6 beacons.
     */
    public List<BeaconInfo> getLatestBeaconDistances() {
        // deep-copy the beacons list and its history fifo queue
        // not pretty, but 30x faster than rits.cloning.cloner
        List<BeaconInfo> beacons = new ArrayList<>();
        for(BeaconInfo readOnlyBeacon : beaconInfos.values()) {
            beacons.add(readOnlyBeacon.duplicate());
        }

        if(beacons.size() == 0)
            return new ArrayList<>();

        long now = What.timey();

//        Log.debug("-------------------------------------------------");

        // process a local-thread copy of the latest beacon list
        for(BeaconInfo beacon : beacons) {

            // expire long unseen beacons
            beacon.numValidSamples = 0;
            for(int i = 0; i < beacon.NumHistorySamples; i ++) {
                if (beacon.history[i] != null) {

                    // gradual extinction. devalue aging samples quickly and smoothly.
                    beacon.history[i].life = 1 - (double) (now - beacon.history[i].timestamp) / 3000.0;

                    if (beacon.history[i].life < 0.01) {
                        beacon.history[i] = null;
                    } else {
                        beacon.numValidSamples++;
                    }
                }
            }

            // weighted average beacon samples
            double sum = 0;
            double energy = 0;
            final int biasRecentSamplesFactor = 3; // [1-10]

            for(int i = beacon.lastHistoryIdx; i != (beacon.lastHistoryIdx-1 + beacon.NumHistorySamples) % beacon.NumHistorySamples; i = (i+1) % beacon.NumHistorySamples) {
                if(beacon.history[i] == null)
                    continue;
                double weight = Math.pow(biasRecentSamplesFactor, 9.0 * beacon.history[i].life);
                double weightedSample = beacon.history[i].distance * weight;
                sum += weightedSample;
                energy += weight;
            }
            beacon.weightedAvgDistance = sum / energy;

//            Log.debug("beacon '%s %d' has weighted average distance: %.2f", beacon.beaconName, beacon.minor, beacon.weightedAvgDistance);
//            Log.debug("  recent samples: %s", TextUtils.join(",", beacon.history));
        }

        // expire long-unseen beacons
        for(Iterator<BeaconInfo> beaconsIter = beacons.iterator(); beaconsIter.hasNext(); ) {
            BeaconInfo beacon = beaconsIter.next();
            if(beacon.numValidSamples ==0) {
                beaconsIter.remove();
            }
        }

        List<BeaconInfo> sortedBeaconList = new ArrayList<>(beacons);

        Collections.sort(sortedBeaconList, new Comparator<BeaconInfo>() {
            @Override
            public int compare(BeaconInfo b1, BeaconInfo b2) {
                return b1.minor - b2.minor;
            }
        });

        return sortedBeaconList;
    }

    /* callback from BluetoothAdapter comes from its own thread */
    @Override
    public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
        // NOTE: this must be a unique device id within a region
        // the device may broadcast the same information as another device
        // eg: both can claim to be "Truck #5"
        // but their unique-ids must be distinct so we can track their distance properly
        String uniqueBeaconId = device.getAddress();

        // grab existing beacon-entry, else create a new one
        BeaconInfo beaconInfo = beaconInfos.get(uniqueBeaconId);
        if(beaconInfo == null)
            beaconInfo = new BeaconInfo();

        beaconInfo.uniqueBleDeviceId = uniqueBeaconId;
        beaconInfo.beaconName = device.getName() != null ? device.getName() : "<unnamed>";
        beaconInfo.lastSeen = What.timey();

        if(!beaconInfos.containsKey(beaconInfo.uniqueBleDeviceId)) {
            // the first time we locate a new device, parse it for useful information
            Log.debug("Found new device: " + device.getName() + " @ " + device.getAddress());

//            ParcelUuid[] uuids = device.getUuids();
//            for(ParcelUuid uuid : uuids) {
//                Log.debug("  uuid: " + uuid.toString());
//            }

            // try to parse beacon info
            EstimoteBeaconInfo estimoteBeaconInfo = EstimoteBeaconInfo.getFromScanRecord(scanRecord);
            if(estimoteBeaconInfo != null) {
                beaconInfo.major = estimoteBeaconInfo.major;
                beaconInfo.minor = estimoteBeaconInfo.minor;
                beaconInfo.measuredPower = estimoteBeaconInfo.measuredPower;
                beaconInfo.beaconType = BeaconInfo.BeaconType.Estimote;
            } else {
                Log.debug("Device not recognized. Treating as generic.");
                beaconInfo.beaconType = BeaconInfo.BeaconType.Unknown;
            }
        }

        // calculate approximate distance using rssi and power parameters
        beaconInfo.distance = computeAccuracy(rssi, beaconInfo.measuredPower);

        //Log.debug("Pinged device: type=" + beaconInfo.beaconType + " rssi=" + rssi + " power=" + beaconInfo.measuredPower + " dist=" + String.format("%.2f", beaconInfo.distance));

        // only commit recognized beacons
        if(beaconInfo.beaconType != BeaconInfo.BeaconType.Unknown) {
            onBeaconDiscovered(beaconInfo);
        }
    }



    /* will be called from BluetoothAdapter's own thread */
    private void onBeaconDiscovered(BeaconInfo beacon) {
        beaconInfos.put(beacon.uniqueBleDeviceId, beacon);

        HistorySample sample = new HistorySample();
        sample.distance = beacon.distance;
        sample.timestamp = What.timey();
        sample.life = 1;

        // adds to a fixed size FIFO queue
        beacon.addHistorySample(sample);
    }

    /* ripped out of Estimotes' SDK */
    private static double computeAccuracy(double rssi, double power) {
        if (rssi == 0) {
            return -1.0D;
        }
        double ratio = rssi / power;
        double rssiCorrection = 0.96D + Math.pow(Math.abs(rssi), 3.0D) % 10.0D / 150.0D;
        if (ratio <= 1.0D) {
            return Math.pow(ratio, 9.98D) * rssiCorrection;
        }
        return (0.103D + 0.89978D * Math.pow(ratio, 7.71D)) * rssiCorrection;
    }
}
