package com.silverback.carman2.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CropImageHelper {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(CropImageHelper.class);

    // Objects
    private Context mContext;
    //private BitmapTypeRequest<?> bitmapTypeReq;

    public CropImageHelper(Context context) {
        mContext = context;
    }

    /*
    public void applyGlideForCroppedImage(Uri uri, byte[] byteArray, ImageView view) {

        if(uri != null) {
            bitmapTypeReq = Glide.with(mContext).load(uri).asBitmap();
        } else if(byteArray.length > 0) {
            bitmapTypeReq = Glide.with(mContext).load(byteArray).asBitmap();
        }


        bitmapTypeReq.into(new BitmapImageViewTarget(view){
            @Override
            protected void setResource(Bitmap resource) {
                RoundedBitmapDrawable circularBitmapDrawable =
                        RoundedBitmapDrawableFactory.create(mContext.getResources(), resource);
                circularBitmapDrawable.setCircular(true);
                view.setImageDrawable(circularBitmapDrawable);
            }
        });
    }
    */

    // Make the cropped image be circular.
    public RoundedBitmapDrawable drawRoundedBitmap(Uri uri) throws IOException {

        Bitmap srcBitmap;

        if(android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // This method was deprecated in API level 29.
            // Loading of images should be performed through ImageDecoder#createSource(ContentResolver, Uri)
            srcBitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
        } else {
            ImageDecoder.Source source = ImageDecoder.createSource(mContext.getContentResolver(), uri);
            srcBitmap = ImageDecoder.decodeBitmap(source);
        }

        RoundedBitmapDrawable roundedBitmap = RoundedBitmapDrawableFactory.create(mContext.getResources(), srcBitmap);
        roundedBitmap.setCircular(true);

        return roundedBitmap;
    }

    // Rotate Bitmap as appropriate when taken by camera.
    // Use ExifInterface to get meta-data on image taken by camera and Metrix to rotate
    // it vertically if necessary.
    public int getImageOrientation(Uri cameraUri) {
        Cursor cursor = mContext.getContentResolver().query(cameraUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION },
                null, null, null);

        if(cursor == null || cursor.getCount() != 1) {
            //cursor.close();
            return -1;
        }

        cursor.moveToFirst();
        int orientation = cursor.getInt(0);
        cursor.close();

        return orientation;

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public Uri rotateBitmapUri(Uri uri, int orientation) {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        try {
            BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(uri), null, options);
        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        }

        log.i("Bitmap Size: %s, %s", options.outWidth, options.outHeight);


        Matrix matrix = new Matrix();
        switch(orientation) {
            case 90: matrix.postRotate(90); break;
            case 180: matrix.postRotate(180); break;
            case 270: matrix.postRotate(270); break;
        }

        // Save the rotated image in the cache directory, which will be erased on exiting
        // programatically in onBackPressed in MainActivity.
        File imagePath = new File(mContext.getCacheDir(), "images/");
        if(!imagePath.exists()) imagePath.mkdir();
        File fRotated = new File(imagePath, "tmpRotated.jpg");
        Bitmap rotatedBitmap;

        // try-resources statement
        try (FileOutputStream fos = new FileOutputStream(fRotated) ){
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
            rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            return FileProvider.getUriForFile(mContext, "com.silverback.carman2.fileprovider", fRotated);

        } catch(IOException e) {
            //Log.e(LOG_TAG, "IOException: " + e.getMessage());
        }

        return null;
    }


    private void setCameraDispOrientation() throws CameraAccessException {
        /*
        CameraManager cameraManager = mContext.getSystemService(CameraManager.class);
        String cameraId = null;
        int result;
        int degrees = 0;
        int rotation = mContext().getWindowManager().getDefaultDisplay().getRotation();

        switch(rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        for(String id : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
            int orientation = characteristics.get(CameraCharacteristics.LENS_FACING);


            if(orientation == CameraCharacteristics.LENS_FACING_FRONT) {
                cameraId = id;
                result = (orientation + degrees) % 360;
                result = (360 - result) % 360; // compensate the mirror;

                break;
            } else {
                cameraId = id;
                result = (orientation - degrees + 360) % 360;

                break;
            }
        }
         */

    }

    public String encodeBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        return android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT);
    }

    public Bitmap decodeBase64ToBitmap(String input) {
        byte[] decodeBytes = android.util.Base64.decode(input, android.util.Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodeBytes, 0, decodeBytes.length);
    }
}
