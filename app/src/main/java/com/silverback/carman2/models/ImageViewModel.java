package com.silverback.carman2.models;

import android.graphics.Bitmap;
import android.util.SparseArray;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class ImageViewModel extends ViewModel {

    private final MutableLiveData<SparseArray> downloadImage = new MutableLiveData<>();
    private MutableLiveData<Boolean> resizeBitmap;

    public MutableLiveData<SparseArray> getDownloadImage() {
        return downloadImage;
    }

    public void setDownloadImage(int key, RoundedBitmapDrawable drawable) {
        SparseArray<RoundedBitmapDrawable> sparseArray = new SparseArray<>();
        sparseArray.put(key, drawable);
        downloadImage.postValue(sparseArray); // Background Thread!!

    }

    public MutableLiveData<Boolean> getResizeBitmap() {
        if(resizeBitmap == null) resizeBitmap = new MutableLiveData<>();
        return resizeBitmap;
    }
}
