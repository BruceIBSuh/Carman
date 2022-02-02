package com.silverback.carman.threads;

import android.content.Context;

import androidx.fragment.app.Fragment;

import java.lang.ref.WeakReference;

public class UploadPostImageTask {
    private Context context;
    private WeakReference<Fragment> weakFragmentRef;
    public UploadPostImageTask(Fragment fragment) {
        weakFragmentRef = new WeakReference<>(fragment);
    }

    public void initTask(int position) {

    }
}
