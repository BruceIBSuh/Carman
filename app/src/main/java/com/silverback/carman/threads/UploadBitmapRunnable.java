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

    // Objects
    private Context context;
    private ApplyImageResourceUtil imageUtil;
    private BitmapResizeMethods callback;
    private FirebaseStorage firestorage;

    // Interface
    public interface BitmapResizeMethods {
        Uri getAttachedImageUri();
        int getImagePosition();
        void setBitmapTaskThread(Thread thread);
        void setDownloadBitmapUri(int position, String uri);
        void handleUploadBitmapState(int state);
    }

    // Constructor
    UploadBitmapRunnable(Context context, BitmapResizeMethods task) {
        this.context = context;
        this.callback = task;
        firestorage = FirebaseStorage.getInstance();
        imageUtil = new ApplyImageResourceUtil(context);
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        callback.setBitmapTaskThread(Thread.currentThread());

        final Uri uri = callback.getAttachedImageUri();
        final int position = callback.getImagePosition();
        int orientation;

        log.i("Image uri: %s", uri);
        File imgFile = new File(uri.getPath());
        log.i("ImageFile check: %s, %s", imgFile.exists(), imgFile);
        // Create the storage reference of an image uploading to Firebase Storage
        /*
        final StorageReference imgReference = firestorage.getReference().child("images");
        final String filename = System.currentTimeMillis() + ".jpg";
        final StorageReference uploadReference = imgReference.child(filename);
        */

        // Set BitmapFactory.Options
        try(InputStream is = context.getContentResolver().openInputStream(uri)) {
            /*
            Cursor cursor = context.getContentResolver().query(
                    uri, new String[]{ MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null)){
            */
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            orientation = imageUtil.getImageOrientation(uri);
            options.inSampleSize = imageUtil.calculateInSampleSize(options, orientation);


            // Get the image orientation. Unless it is 0, rotate the image
            /*
            if(cursor.getCount() >= 1) {
                cursor.moveToFirst();
                orientation = cursor.getInt(0);
                log.i("orientation: %s", orientation);
            }
            */

            // Recall InputStream once again b/c it is auto closeable. Otherwise, it returns null.
            try(InputStream in = context.getContentResolver().openInputStream(uri)) {
                options.inJustDecodeBounds = false;
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;//default value. no need to define.

                // Compress the Bitmap which already resized down by calculating the inSampleSize.
                Bitmap resizedBitmap = BitmapFactory.decodeStream(in, null, options);
                log.i("Resized Bitmap: %s", resizedBitmap);

                if(orientation > 0) {
                    log.i("Orientation of image is not 0");
                    Matrix matrix = new Matrix();
                    matrix.postRotate(orientation);
                    resizedBitmap = Bitmap.createBitmap(
                            resizedBitmap, 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight(), matrix, true);
                }

                byte[] bmpByteArray = imageUtil.compressBitmap(resizedBitmap, Constants.MAX_IMAGE_SIZE);
                // Upload the compressed image(less than 1 MB) to Firebase Storage
                uploadBitmapToStorage(bmpByteArray, position);
            }

        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        }

    }

    /*
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

        log.i("scale: %s", inSampleSize);

        return inSampleSize;
    }


    //private byte[] compressBitmap(InputStream in, BitmapFactory.Options options)  {
    private byte[] compressBitmap(final Bitmap resizedBitmap, BitmapFactory.Options options)  {
        final int MAX_IMAGE_SIZE = 1024 * 1024;
        //Bitmap resizedBitmap = BitmapFactory.decodeStream(in, null, options);
        //Bitmap resizedBitmap = bitmap;
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
    */

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void uploadBitmapToStorage(byte[] bitmapByteArray, int position) {
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
                Uri downloadUri = task.getResult();
                if(downloadUri != null) callback.setDownloadBitmapUri(position, downloadUri.toString());
                callback.handleUploadBitmapState(UploadBitmapTask.UPLOAD_BITMAP_COMPLETE);

            } else {
                log.w("No uri fetched");
                callback.handleUploadBitmapState(UploadBitmapTask.UPLOAD_BITMAP_FAIL);
            }
        });
    }
}
