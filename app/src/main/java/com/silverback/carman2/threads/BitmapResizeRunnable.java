package com.silverback.carman2.threads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Process;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class BitmapResizeRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(BitmapResizeRunnable.class);

    // Constants
    private final int MAX_IMAGE_SIZE = 1024 * 1024;

    // Objects
    private Context context;
    private BitmapResizeMethods task;


    // Interface
    public interface BitmapResizeMethods {
        Uri getImageUri();
        void setBitmapTaskThread(Thread thread);
    }

    // Constructor
    public BitmapResizeRunnable(Context context,BitmapResizeMethods task) {
        this.context = context;
        this.task = task;
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public void run() {
        task.setBitmapTaskThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        final Uri uri = task.getImageUri();
        log.i("uri: %s", uri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        try(InputStream is = context.getApplicationContext().getContentResolver().openInputStream(uri)){
            BitmapFactory.decodeStream(is, null, options);
            int imgWidth = options.outWidth;
            int imgHeight = options.outHeight;
            log.i("Dimension: %s, %s", imgWidth, imgHeight);

            options.inSampleSize = calculateInSampleSize(options, 800, 800);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            // Recall InputStream once again b/c it is auto closeable. Otherwise, it returns null.
            try(InputStream in = context.getApplicationContext().getContentResolver().openInputStream(uri)) {
                Bitmap resizedBitmap = BitmapFactory.decodeStream(in, null, options);
                log.i("Resized Bitmap: %s", resizedBitmap);

                int compressDensity = 100;
                int streamLength;

                do{
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, compressDensity, byteArrayOutputStream);
                    byte[] bmpByteArray = byteArrayOutputStream.toByteArray();
                    streamLength = bmpByteArray.length;
                    compressDensity -= 5;
                    log.i("compress density: %s", streamLength / 1024 + " kb");
                } while(streamLength >= MAX_IMAGE_SIZE);
            }

        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        }

    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        // Raw dimension of the image
        final int rawHeight = options.outHeight;
        final int rawWidth = options.outWidth;
        int inSampleSize = 1;

        if(rawHeight > reqHeight || rawWidth > reqWidth) {
            final int halfHeight = rawHeight / 2;
            final int halfWidth = rawWidth / 2;

            while((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;

    }
}
