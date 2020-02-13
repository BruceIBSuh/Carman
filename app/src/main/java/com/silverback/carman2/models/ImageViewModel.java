package com.silverback.carman2.models;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.style.ImageSpan;
import android.util.SparseArray;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ImageViewModel extends ViewModel {

    private final MutableLiveData<SparseArray> downloadImage = new MutableLiveData<>();
    private MutableLiveData<String> uploadBitmap;
    private MutableLiveData<SparseArray<ImageSpan>> sparseImageSpanArray;

    public MutableLiveData<Drawable> glideTarget;

    public void setDownloadImage(int key, RoundedBitmapDrawable drawable) {
        SparseArray<RoundedBitmapDrawable> sparseArray = new SparseArray<>();
        sparseArray.put(key, drawable);
        downloadImage.postValue(sparseArray); // Background Thread!!

    }

    public MutableLiveData<String> getUploadBitmap() {
        if(uploadBitmap == null) uploadBitmap = new MutableLiveData<>();
        return uploadBitmap;
    }

    public MutableLiveData<SparseArray<ImageSpan>> getImageSpanArray() {
        if(sparseImageSpanArray == null) sparseImageSpanArray = new MutableLiveData<>();
        return sparseImageSpanArray;
    }

    public MutableLiveData<Drawable> getGlideTarget() {
        if(glideTarget == null) glideTarget = new MutableLiveData<>();
        return glideTarget;
    }
}
