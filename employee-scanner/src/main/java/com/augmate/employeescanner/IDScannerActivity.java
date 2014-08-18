package com.augmate.employeescanner;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.augmate.sdk.logger.Log;
import com.augmate.sdk.scanner.ScannerFragmentBase;

/**
 * Created by premnirmal on 8/18/14.
 */
public class IDScannerActivity extends Activity implements ScannerFragmentBase.OnScannerResultListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_idscan);

        Log.debug("Created activity that uses barcode scanner");
    }

    @Override
    public void onBarcodeScanSuccess(String result) {
        Log.debug("Got scanning result: [%s]", result);

        Intent resultIntent = new Intent();
        resultIntent.putExtra("barcodeString", result);
        setResult(RESULT_OK, resultIntent);
        finish();
    }
}