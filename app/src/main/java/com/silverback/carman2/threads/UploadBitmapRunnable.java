package com.silverback.carman2.threads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Process;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UploadBitmapRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(UploadBitmapRunnable.class);


    // Objects
    private Context context;
    private BitmapResizeMethods callback;
    private FirebaseStorage firestorage;

    // Interface
    public interface BitmapResizeMethods {
        Uri getImageUri();
        void setBitmapTaskThread(Thread thread);
        void setBitmapUri(String uriString);
    }

    // Constructor
    UploadBitmapRunnable(Context context, BitmapResizeMethods task) {
        this.context = context;
        this.callback = task;
        firestorage = FirebaseStorage.getInstance();
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        callback.setBitmapTaskThread(Thread.currentThread());

        final Uri uri = callback.getImageUri();
        final int MAX_IMAGE_SIZE = 1024 * 1024;
        log.i("uri: %s", uri);

        // Create the storage reference of an image uploading to Firebase Storage
        /*
        final StorageReference imgReference = firestorage.getReference().child("images");
        final String filename = System.currentTimeMillis() + ".jpg";
        final StorageReference uploadReference = imgReference.child(filename);
        */

        // Set BitmapFactory.Options
        try(InputStream is = context.getApplicationContext().getContentResolver().openInputStream(uri)){
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);

            options.inSampleSize = calculateInSampleSize(options, 800, 800);
            options.inJustDecodeBounds = false;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            // Recall InputStream once again b/c it is auto closeable. Otherwise, it returns null.
            try(InputStream in = context.getApplicationContext().getContentResolver().openInputStream(uri)) {
                // Compress the Bitmap which already resized down by calculating the inSampleSize.
                byte[] bmpByteArray = compressBitmap(in, options);

                // Upload the compressed image(less than 1 MB) to Firebase Storage
                uploadBitmapToStorage(bmpByteArray);

                /*
                Bitmap resizedBitmap = BitmapFactory.decodeStream(in, null, options);
                log.i("Resized Bitmap: %s", resizedBitmap);

                int compressDensity = 100;
                int streamLength;
                ByteArrayOutputStream baos;
                byte[] bmpByteArray;
                // Compress the raw image down to the MAX_IMAGE_SIZE
                do{
                    baos = new ByteArrayOutputStream();
                    resizedBitmap.compress(Bitmap.CompressFormat.JPEG, compressDensity, baos);
                    bmpByteArray = baos.toByteArray();
                    streamLength = bmpByteArray.length;
                    compressDensity -= 5;
                    log.i("compress density: %s", streamLength / 1024 + " kb");

                } while(streamLength >= MAX_IMAGE_SIZE);

                */

                /*
                UploadTask uploadTask = uploadReference.putBytes(bmpByteArray);
                uploadTask.addOnProgressListener(listener -> log.i("upload progressing"))
                        .addOnSuccessListener(snapshot -> log.i("File metadata: %s", snapshot.getMetadata()))
                        .addOnFailureListener(e -> log.e("UploadFailed: %s", e.getMessage()));

                uploadTask.continueWithTask(task -> {
                    if(!task.isSuccessful()) {
                        task.getException();
                    }

                    return uploadReference.getDownloadUrl();

                }).addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        Uri uriUploaded = task.getResult();
                        callback.setBitmapUri(uriUploaded);

                    } else log.w("No uri fetched");
                });
                */

            }

        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        }

    }


    @SuppressWarnings("ConstantConditions")
    private byte[] compressBitmap(InputStream in, BitmapFactory.Options options)  {
        final int MAX_IMAGE_SIZE = 1024 * 1024;
        Bitmap resizedBitmap = BitmapFactory.decodeStream(in, null, options);
        log.i("Resized Bitmap: %s", resizedBitmap);

        int compressDensity = 100;
        int streamLength;
        ByteArrayOutputStream baos;
        byte[] bmpByteArray;
        // Compress the raw image down to the MAX_IMAGE_SIZE
        do{
            baos = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, compressDensity, baos);
            bmpByteArray = baos.toByteArray();
            streamLength = bmpByteArray.length;
            compressDensity -= 5;
            log.i("compress density: %s", streamLength / 1024 + " kb");

        } while(streamLength >= MAX_IMAGE_SIZE);

        return bmpByteArray;

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

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void uploadBitmapToStorage(byte[] bitmapByteArray) {

        // Create the storage reference of an image uploading to Firebase Storage
        final StorageReference imgReference = firestorage.getReference().child("images");
        final String filename = System.currentTimeMillis() + ".jpg";
        final StorageReference uploadReference = imgReference.child(filename);

        UploadTask uploadTask = uploadReference.putBytes(bitmapByteArray);
        uploadTask.addOnProgressListener(listener -> log.i("upload progressing"))
                .addOnSuccessListener(snapshot -> log.i("File metadata: %s", snapshot.getMetadata()))
                .addOnFailureListener(e -> log.e("UploadFailed: %s", e.getMessage()));

        uploadTask.continueWithTask(task -> {
            if(!task.isSuccessful()) {
                task.getException();
            }

            return uploadReference.getDownloadUrl();

        }).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Uri uriUploaded = task.getResult();
                callback.setBitmapUri(uriUploaded.toString());

            } else log.w("No uri fetched");
        });
    }
}
