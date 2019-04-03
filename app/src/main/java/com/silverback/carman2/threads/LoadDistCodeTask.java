package com.silverback.carman2.threads;

import android.content.Context;

import com.silverback.carman2.adapters.DistrictSpinnerAdapter;
import com.silverback.carman2.fragments.SpinnerPrefDlgFragment;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Opinet;

import java.lang.ref.WeakReference;
import java.util.List;

public class LoadDistCodeTask extends ThreadTask implements
        LoadDistCodeRunnable.DistCodeMethods {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(LoadDistCodeTask.class);

    // Objects
    private Context context;
    private WeakReference<SpinnerPrefDlgFragment> mWeakSpinnerDialogFragment;
    private WeakReference<DistrictSpinnerAdapter> mWeakSpinnerAdapter;
    private LoadDistCodeRunnable loadDistCodeRunnable;
    private int sidoCode;
    private List<Opinet.DistrictCode> distCodeList;


    // Constructor
    LoadDistCodeTask(Context context) {
        this.context = context;
        loadDistCodeRunnable = new LoadDistCodeRunnable(context, this);
    }

    void initSpinnerDistCodeTask(ThreadManager threadManager, SpinnerPrefDlgFragment fm, int code) {
        sThreadManager = threadManager;
        sidoCode = code;
        mWeakSpinnerDialogFragment = new WeakReference<>(fm);
        mWeakSpinnerAdapter = new WeakReference<>(fm.getSigunAdapter());
    }

    Runnable getLoadDistCodeRunnable() {
        return loadDistCodeRunnable;
    }

    public void recycle() {
        if(mWeakSpinnerDialogFragment != null) {
            mWeakSpinnerDialogFragment.clear();
            mWeakSpinnerDialogFragment = null;
        }

        if(mWeakSpinnerAdapter != null) {
            mWeakSpinnerAdapter.clear();
            mWeakSpinnerAdapter = null;
        }
    }

    @Override
    public void setSpinnerDistCodeThread(Thread currentThread) {
        log.i("Current Thread: " + currentThread);
        // Inheritedd from the parent class of ThreadTask
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
    public DistrictSpinnerAdapter getSpinnerAdapter() {
        return mWeakSpinnerAdapter.get();
    }

    public List<Opinet.DistrictCode> getSigunList() {
        return distCodeList;
    }

    public SpinnerPrefDlgFragment getPrefDlgFragment() {
        if(mWeakSpinnerDialogFragment != null) return mWeakSpinnerDialogFragment.get();
        return null;
    }
}
