package com.silverback.carman.threads;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Process;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/*
 * FIREBASE STORAGE SECURITY RULE OF UPLOADING IMAGE SIZE
 *
 * service firebase.storage {
 *   match /b/<bucket>/o {
 *     match /files/{fileName} {
 *       allow read;
 *       allow write: if request.resource.size < 10 * 1024 * 1024; // 10MB limit for instance
 *     }
 *   }
 * }
 *
 */

public class UploadBitmapRunnable implements Runnable {
    private static final LoggingHelper log = LoggingHelperFactory.create(UploadBitmapRunnable.class);
    // Constants
    static final int UPLOAD_BITMAP_COMPLETE = 1;
    static final int UPLOAD_BITMAP_FAIL = -1;
    // Objects
    private final Context context;
    private final ApplyImageResourceUtil imageUtil;
    private final BitmapResizeMethods callback;
    private final StorageReference imgReference;

    // Interface
    public interface BitmapResizeMethods {
        Uri getAttachedImageUri();
        int getImagePosition();
        void setBitmapTaskThread(Thread thread);
        void setDownloadBitmapUri(int position, Uri uri);
        void handleUploadBitmapState(int state);
    }

    // Constructor
    UploadBitmapRunnable(Context context, BitmapResizeMethods task) {
        this.context = context;
        this.callback = task;
        imageUtil = new ApplyImageResourceUtil(context);
        imgReference = FirebaseStorage.getInstance().getReference().child("post_images");
    }


    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        callback.setBitmapTaskThread(Thread.currentThread());

        final Uri uri = callback.getAttachedImageUri();
        final int position = callback.getImagePosition();
        log.i("image position in UploadBitmapTask: %s", position);
        int orientation;

        //File imgFile = new File(Objects.requireNonNull(uri).getPath());
        //log.i("ImageFile check: %s, %s", imgFile.exists(), imgFile);
        // Set BitmapFactory.Options
        try(InputStream is = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true; // just out_fields are set w/o returning bitmap
            BitmapFactory.decodeStream(is, null, options);
            orientation = imageUtil.getImageOrientation(uri);
            options.inSampleSize = imageUtil.calculateInSampleSize(options, orientation);

            // Recall InputStream once again b/c it is auto closeable. Otherwise, it returns null.
            try(InputStream in = context.getContentResolver().openInputStream(uri)) {
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;//default value. no need to define.
                // Compress the Bitmap which already resized down by calculating the inSampleSize.
                Bitmap resizedBitmap = BitmapFactory.decodeStream(in, null, options);
                if(resizedBitmap == null) throw new NullPointerException();
                if(orientation > 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(orientation);
                    resizedBitmap = Bitmap.createBitmap(resizedBitmap, 0, 0,
                            resizedBitmap.getWidth(), resizedBitmap.getHeight(), matrix, true);
                }
                byte[] bmpByteArray = imageUtil.compressBitmap(resizedBitmap, Constants.MAX_IMAGE_SIZE);

                // Upload the compressed image(less than 1 MB) to Firebase Storage
                uploadBitmapToStorage(bmpByteArray, position);
            }
        } catch(IOException | NullPointerException e) {e.printStackTrace();}
    }

    private void uploadBitmapToStorage(byte[] bitmapByteArray, final int position) {
        log.i("uploadBitmapToStorage thread: %s", Thread.currentThread());
        // Create the storage reference of an image uploading to Firebase Storage
        final String filename = System.currentTimeMillis() + ".png";
        //final StorageReference uploadReference = imgReference.child("filename");
        final StorageReference uploadReference = imgReference.child(filename);

        UploadTask uploadTask = uploadReference.putBytes(bitmapByteArray);
        uploadTask.addOnProgressListener(listener -> log.i("upload progressing"))
                .addOnSuccessListener(snapshot -> log.i("File metadata: %s", snapshot.getMetadata()))
                .addOnFailureListener(e -> log.e("UploadFailed: %s", e.getMessage()));

        uploadTask.continueWithTask(task -> {
            log.i("Firebase Storage Thread: %s", Thread.currentThread());
            if(!task.isSuccessful()) {
                log.e("upload bitmap task failed"); // refaactor required!
            }
            return uploadReference.getDownloadUrl();

        }).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                if(downloadUri != null) callback.setDownloadBitmapUri(position, downloadUri);
                callback.handleUploadBitmapState(UPLOAD_BITMAP_COMPLETE);
            } else callback.handleUploadBitmapState(UPLOAD_BITMAP_FAIL);
        });
    }
}
