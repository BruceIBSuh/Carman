package com.silverback.carman2.camera;

import android.hardware.camera2.CameraManager;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class CameraHelper {

    // Objects
    private final LoggingHelper log;

    public CameraHelper(CameraManager camManager) {
        this.log = LoggingHelperFactory.create(CameraHelper.class);
    }
}
