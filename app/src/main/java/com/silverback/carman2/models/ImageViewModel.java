package com.silverback.carman2.models;

import android.net.Uri;
import android.util.SparseArray;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ImageViewModel extends ViewModel {

    private final MutableLiveData<SparseArray> downloadImage = new MutableLiveData<>();
    private MutableLiveData<Uri> uploadBitmap;

    public MutableLiveData<SparseArray> getDownloadImage() {
        return downloadImage;
    }

    public void setDownloadImage(int key, RoundedBitmapDrawable drawable) {
        SparseArray<RoundedBitmapDrawable> sparseArray = new SparseArray<>();
        sparseArray.put(key, drawable);
        downloadImage.postValue(sparseArray); // Background Thread!!

    }

    public MutableLiveData<Uri> getUploadBitmap() {
        if(uploadBitmap == null) uploadBitmap = new MutableLiveData<>();
        return uploadBitmap;
    }
}
