package com.silverback.carman2.models;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.style.ImageSpan;
import android.util.SparseArray;

import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

/**
 * This class subclasses ViewModel to instantiate LiveData concerning image resources.
 */
public class ImageViewModel extends ViewModel {

    private final MutableLiveData<SparseArray> downloadImage = new MutableLiveData<>();

    private MutableLiveData<SparseArray> downloadBitmapUri;
    private MutableLiveData<SparseArray<ImageSpan>> sparseImageSpanArray;
    private MutableLiveData<Bitmap> glideBitmapTarget;
    private MutableLiveData<Drawable> glideDrawableTarget;

    public void setDownloadImage(int key, RoundedBitmapDrawable drawable) {
        SparseArray<RoundedBitmapDrawable> sparseArray = new SparseArray<>();
        sparseArray.put(key, drawable);
        downloadImage.postValue(sparseArray); // Background Thread!!

    }

    public MutableLiveData<SparseArray> getDownloadBitmapUri() {
        if(downloadBitmapUri == null) downloadBitmapUri = new MutableLiveData<>();
        return downloadBitmapUri;
    }

    public MutableLiveData<SparseArray<ImageSpan>> getImageSpanArray() {
        if(sparseImageSpanArray == null) sparseImageSpanArray = new MutableLiveData<>();
        return sparseImageSpanArray;
    }

    // Glide creates CustomTarget which has bitmap as resource set it generally to views.
    public MutableLiveData<Bitmap> getGlideBitmapTarget() {
        if(glideBitmapTarget == null) glideBitmapTarget = new MutableLiveData<>();
        return glideBitmapTarget;
    }

    // Glide creates CustomTarget which has drawable as resource and set it mostly to icons.
    public MutableLiveData<Drawable> getGlideDrawableTarget() {
        if(glideDrawableTarget == null) glideDrawableTarget = new MutableLiveData<>();
        return glideDrawableTarget;
    }
}
