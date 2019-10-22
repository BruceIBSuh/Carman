package com.silverback.carman2.models;

import android.graphics.Bitmap;
import android.util.SparseArray;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class LoadImageViewModel extends ViewModel {

    private static final LoggingHelper log = LoggingHelperFactory.create(LoadImageViewModel.class);

    private final MutableLiveData<SparseArray> downloadImage = new MutableLiveData<>();

    public MutableLiveData<SparseArray> getDownloadImage() {
        return downloadImage;
    }

    public void setDownloadImage(int key, RoundedBitmapDrawable drawable) {
        SparseArray<RoundedBitmapDrawable> sparseArray = new SparseArray<>();
        sparseArray.put(key, drawable);
        downloadImage.postValue(sparseArray); // Background Thread!!

    }
}
