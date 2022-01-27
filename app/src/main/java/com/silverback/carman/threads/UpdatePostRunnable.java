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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UpdatePostRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(UpdatePostRunnable.class);

    private final StorageReference storageReference;
    private final UploadPostCallback callback;
    private final Context context;
    private final ApplyImageResourceUtil imgUtil;
    private List<Uri> updateImages;

    public interface UploadPostCallback {
        void setUploadPostThread(Thread thread);
        List<String> getRemovedImages();
        List<Uri> getNewImages();
        Map<String, Object> getUpdatePost();
    }

    public UpdatePostRunnable(Context context, UploadPostCallback callback) {
        this.context = context;
        this.callback = callback;
        storageReference = FirebaseStorage.getInstance().getReference();
        imgUtil = new ApplyImageResourceUtil(context);
    }

    @Override
    public void run() {
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        callback.setUploadPostThread(Thread.currentThread());
        log.i("current thread:%s", Thread.currentThread());

        // Delete removed images from Storage
        List<String> removedImages = callback.getRemovedImages();
        for(String url : removedImages) {
            log.i("removed images: %s", url);
        }

        Map<String, Object> updatePost = callback.getUpdatePost();
        String docId = (String) updatePost.get("documentId");
        log.i("document id: %s", docId);
        updateImages = callback.getNewImages();
        for(int i = 0; i < updateImages.size(); i++) {
            if(Objects.equals(updateImages.get(i).getScheme(), "content")) {
                compressImage(updateImages.get(i), i);
            }
        }
    }

    private void compressImage(Uri uri, final int pos) {
        int orientation;
        try(InputStream is = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true; // just out_fields are set w/o returning bitmap
            BitmapFactory.decodeStream(is, null, options);
            orientation = imgUtil.getImageOrientation(uri);
            options.inSampleSize = imgUtil.calculateInSampleSize(options, orientation);

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

                byte[] bmpByteArray = imgUtil.compressBitmap(resizedBitmap, Constants.MAX_IMAGE_SIZE);

                // Upload the compressed image(less than 1 MB) to Firebase Storage
                //uploadBitmapToStorage(bmpByteArray, pos);
            }
        } catch(IOException | NullPointerException e) {e.printStackTrace();}
    }

    private void uploadBitmapToStorage(byte[] bytes, final int pos) {
        log.i("uploadBitmapToStorage thread: %s", Thread.currentThread());
        // Create the storage reference of an image uploading to Firebase Storage
        final String filename = System.currentTimeMillis() + ".png";
        final StorageReference uploadReference = storageReference.child(filename);

        UploadTask uploadTask = uploadReference.putBytes(bytes);
        uploadTask.addOnProgressListener(listener -> log.i("upload progressing"))
                .addOnSuccessListener(snapshot -> log.i("File metadata: %s", snapshot.getMetadata()))
                .addOnFailureListener(e -> log.e("UploadFailed: %s", e.getMessage()));

        uploadTask.continueWithTask(task -> {
            log.i("Firebase Storage Thread: %s", Thread.currentThread());
            return uploadReference.getDownloadUrl();

        }).addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                Uri downloadUri = task.getResult();
                log.i("Storage downloaded url: %s", downloadUri);
                //if(downloadUri != null) callback.setDownloadBitmapUri(position, downloadUri);
                //callback.handleUploadBitmapState(UPLOAD_BITMAP_COMPLETE);
                updateImages.add(pos, downloadUri);
            } //else callback.handleUploadBitmapState(UPLOAD_BITMAP_FAIL);
        });
    }
}
