package com.silverback.carman2.threads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Process;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.EditImageHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.List;

public class AttachedBitmapRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(AttachedBitmapRunnable.class);

    // Objects
    private Context context;
    private DownloadBitmapMethods mTask;
    private EditImageHelper imageHelper;
    private ExifInterface exifInterface;

    // Fields
    private Point screenSize;

    // Interface
    public interface DownloadBitmapMethods {
        void setDownloadBitmapThread(Thread thread);
        void setImageSpanArray(SparseArray<ImageSpan> spanArray);
        List<String> getImageUriList();
    }

    // Constructor
    AttachedBitmapRunnable(Context context, DownloadBitmapMethods task) {
        this.context = context;
        mTask = task;
        screenSize = calculateDeviceSize();
        imageHelper = new EditImageHelper(context);
    }


    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        mTask.setDownloadBitmapThread(Thread.currentThread());

        List<String> uriStringList = mTask.getImageUriList();
        SparseArray<ImageSpan> sparseSpanArray = new SparseArray<>();


        // What if the Glide fails to fetch an image?
        for(int i = 0; i < uriStringList.size(); i++) {
            int orientation = getAttachedImageOrientation(uriStringList.get(i));
            log.i("Orientation: %s", orientation);

            final int key = i;
            Glide.with(context.getApplicationContext())
                    .asBitmap()
                    .load(Uri.parse(uriStringList.get(i)))
                    //.centerCrop()
                    //.fitCenter()
                    .apply(new RequestOptions().override(screenSize.x))
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(
                                @NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                            ImageSpan imgSpan = new ImageSpan(context, resource);
                            sparseSpanArray.put(key, imgSpan);

                            if(sparseSpanArray.size() == uriStringList.size()) {
                                mTask.setImageSpanArray(sparseSpanArray);
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            log.i("onLoadCleared");
                        }
                    });
        }

    }

    // Calculate the screen size to fit the image to the size by Glide. Not sure why the size
    // should be divided by 2 for fitting.
    private Point calculateDeviceSize() {

        WindowManager windowManager = (WindowManager)(context.getSystemService(Context.WINDOW_SERVICE));
        Display display = windowManager.getDefaultDisplay();

        Point size = new Point();
        display.getSize(size);
        return size;
    }

    private int getAttachedImageOrientation(String url) {
        // Download images from Firebase Storage with URL provided by FireStore.
        int orientation = -1;
        try (InputStream in = new java.net.URL(url).openStream()) {
            exifInterface = new ExifInterface(in);
            orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            log.i("Image Orientation: %s", orientation);

            /*
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, options);

            float scale = (float)screenSize.x / (float)options.outWidth;

            int scaledWidth = screenSize.x;
            int scaledHeight = (int)(options.outHeight * scale);


            return new Point(scaledWidth, scaledHeight);
            */
            return orientation;

        } catch(MalformedURLException e) {
            log.e("MalFormedURLException: %s", e.getMessage());
        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        }

        return orientation;
    }


}
