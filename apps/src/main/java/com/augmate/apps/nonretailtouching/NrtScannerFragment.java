package com.augmate.apps.nonretailtouching;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.*;
import com.augmate.apps.R;
import com.augmate.sdk.scanner.ScannerFragmentBase;

public class NrtScannerFragment extends ScannerFragmentBase {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nrt_scanner, container, false);
        SurfaceView sv = (SurfaceView) view.findViewById(R.id.camera_preview);
        setupScannerActivity(sv, null);

        view.findViewById(R.id.nrt_scan_indicator).startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.nrt_indicator_anim));

        return view;
    }

    public void onSuccess() {

        Animation scan_indicator_animation = AnimationUtils.loadAnimation(getActivity(), R.anim.nrt_indicator_success_anim);
        scan_indicator_animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                getView().findViewById(R.id.nrt_scan_indicator).startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.nrt_indicator_anim));
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        getView().findViewById(R.id.nrt_scan_overlay).startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.nrt_overlay_success_anim));
        getView().findViewById(R.id.nrt_scan_indicator).startAnimation(scan_indicator_animation);
    }
}
