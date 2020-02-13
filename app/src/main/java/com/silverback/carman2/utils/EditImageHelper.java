package com.silverback.carman2.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ImageViewModel;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class EditImageHelper {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(EditImageHelper.class);

    // Objects
    private Context mContext;
    //private BitmapTypeRequest<ModelType> bitmapTypeReq;

    public EditImageHelper(Context context) {
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
    public int getImageOrientation(Uri uri) {

        Cursor cursor = mContext.getContentResolver().query(uri,
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

    // Uris coming from cameras will mostly have "file://" scheme whereas uris coming from gallery or
    // other content providers usually have "content://" which is a type of FileProvider.
    public int getExifImageOrientation(Uri uri)  {


        //Cursor cursor = null;
        String[] projection = { MediaStore.Images.Media.DATA };
        try(Cursor cursor = mContext.getContentResolver().query(uri, projection, null, null, null)){
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            log.i("decoded uri: %s", cursor.getString(column_index));
            ExifInterface exif = new ExifInterface(cursor.getString(column_index));
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            log.i("orientation: %s", orientation);

        } catch(IOException e) {
            e.printStackTrace();
        }
        //int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        //log.i("Current Rotation: %s", rotation);
        return -1;

    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public Uri rotateBitmapUri(Uri uri, int orientation) {
        Bitmap rotatedBitmap;
        Matrix matrix = new Matrix();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        try {
            BitmapFactory.decodeStream(mContext.getContentResolver().openInputStream(uri), null, options);
        } catch(IOException e) {
            e.printStackTrace();
        }

        switch(orientation) {
            case 90: matrix.postRotate(90); break;
            case 180: matrix.postRotate(180); break;
            case 270: matrix.postRotate(270); break;
        }

        // Temporarily save the rotated image in the cache directory, which will be deleted on exiting
        // the app.
        File imagePath = new File(mContext.getCacheDir(), "images/");
        if(!imagePath.exists()) imagePath.mkdir();
        File fRotated = new File(imagePath, "tmpRotated.jpg");

        // try-resources statement
        try (FileOutputStream fos = new FileOutputStream(fRotated) ){
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
            rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            return FileProvider.getUriForFile(mContext, Constants.FILE_IMAGES, fRotated);

        } catch(IOException e) {
            e.printStackTrace();
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

    // Resize the image by calculating BitmapFactory.Options.inSampleSize.
    public Bitmap resizeBitmap(Context context, Uri uri, int reqWidth, int reqHeight) {

        try (InputStream is = context.getApplicationContext().getContentResolver().openInputStream(uri)){
            log.i("InputStream: %s", is);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);

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

            options.inSampleSize = inSampleSize;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;

            // Must create a new InputStream for the actual sampling bitmap
            try(InputStream in = context.getApplicationContext().getContentResolver().openInputStream(uri)){
                options.inJustDecodeBounds = false;
                log.i("Bitmap: %s, %s", inSampleSize, BitmapFactory.decodeStream(is, null, options));

                return BitmapFactory.decodeStream(in, null, options);

            }
        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        }

        return null;
    }

    public void setUserImageToIcon(String uriString, int size, ImageViewModel model) {

        if(TextUtils.isEmpty(uriString)) return;
        //if(editImageHelper == null) editImageHelper = new EditImageHelper(this);

        // The float of 0.5f makes the scale round as it is cast to int. For exmaple, let's assume
        // the scale is between 1.5 and 2.0. When casting w/o the float, it will be cast to 1.0. By
        // adding the float, it will be round up to 2.0.
        final float scale = mContext.getResources().getDisplayMetrics().density;
        int px_x = (int)(size * scale + 0.5f);
        int px_y = (int)(size * scale + 0.5f);

        Glide.with(mContext).load(Uri.parse(uriString)).override(px_x, px_y)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .fitCenter()
                .circleCrop()
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        model.getGlideTarget().setValue(resource);
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });

    }


    // Convert Bitmap to Base64 which translates bitmaps to String format or vice versa.
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
