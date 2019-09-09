package com.silverback.carman2.threads;

import android.content.Context;

import androidx.fragment.app.Fragment;

import com.silverback.carman2.adapters.DistrictSpinnerAdapter;
import com.silverback.carman2.fragments.SpinnerPrefDlgFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.SpinnerDistrictModel;

import java.lang.ref.WeakReference;
import java.util.List;

public class LoadDistCodeTask extends ThreadTask implements
        LoadDistCodeRunnable.DistCodeMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(LoadDistCodeTask.class);

    // Objects
    private SpinnerDistrictModel model;
    private LoadDistCodeRunnable loadDistCodeRunnable;
    private int sidoCode;


    // Constructor
    LoadDistCodeTask(Context context) {
        loadDistCodeRunnable = new LoadDistCodeRunnable(context, this);
    }

    void initSpinnerDistCodeTask(SpinnerDistrictModel model, int code) {
        sidoCode = code;
        this.model = model;
    }

    Runnable getLoadDistCodeRunnable() {
        return loadDistCodeRunnable;
    }

    public void recycle() {}

    @Override
    public void setSpinnerDistCodeThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    @Override
    public void handleSpinnerDistCodeTask(int state) {
        int outState = -1;

        switch(state) {
            case LoadDistCodeRunnable.SPINNER_DIST_CODE_COMPLETE:
                outState = ThreadManager.LOAD_SPINNER_DIST_CODE_COMPLETE;
                break;

            case LoadDistCodeRunnable.SPINNER_DIST_CODE_FAIL:
                outState = ThreadManager.LOAD_SPINNER_DIST_CODE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outState);

    }

    @Override
    public int getSidoCode() {
        return sidoCode;
    }

    @Override
    public SpinnerDistrictModel getSpinnerDistrictModel() {
        return model;
    }

}
