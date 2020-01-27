package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.SpinnerDistrictModel;

import java.util.List;

public class DistCodeSpinnerTask extends ThreadTask implements
        DistCodeSpinnerRunnable.DistCodeMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(DistCodeSpinnerTask.class);

    // Objects
    private SpinnerDistrictModel model;
    private DistCodeSpinnerRunnable distCodeSpinnerRunnable;
    private int sidoCode;


    // Constructor
    DistCodeSpinnerTask(Context context) {
        distCodeSpinnerRunnable = new DistCodeSpinnerRunnable(context, this);
    }

    void initSpinnerDistCodeTask(SpinnerDistrictModel model, int position) {
        sidoCode = position;
        this.model = model;
    }

    Runnable getDistCodeSpinnerRunnable() {
        return distCodeSpinnerRunnable;
    }

    public void recycle() {
        sidoCode = -1;
    }

    @Override
    public void setSpinnerDistCodeThread(Thread currentThread) {
        setCurrentThread(currentThread);
    }

    /*
    @Override
    public void handleSpinnerDistCodeTask(int state) {
        int outState = -1;

        switch(state) {
            case DistCodeSpinnerRunnable.SPINNER_DIST_CODE_COMPLETE:
                outState = ThreadManager.LOAD_SPINNER_DIST_CODE_COMPLETE;
                break;

            case DistCodeSpinnerRunnable.SPINNER_DIST_CODE_FAIL:
                outState = ThreadManager.LOAD_SPINNER_DIST_CODE_FAILED;
                break;
        }

        sThreadManager.handleState(this, outState);
    }
     */

    @Override
    public int getSidoCode() {
        return sidoCode;
    }

    /*
    @Override
    public SpinnerDistrictModel getSpinnerDistrictModel() {
        return model;
    }
     */

    // Post(Set) value in SpinnerDistriceModel, which is notified to the parent fragment,
    // SettingSpinnerDlgFragment as LiveData.
    @Override
    public void setSigunCode(List<Opinet.DistrictCode> distCode) {
        model.getSpinnerDataList().postValue(distCode);
    }


}
