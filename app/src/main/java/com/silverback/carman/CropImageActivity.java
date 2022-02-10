package com.silverback.carman;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;

import com.google.firebase.appcheck.interop.BuildConfig;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.views.DrawEditorView;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;


/**
 * This activity is an image editor that crops an specific area of images to the shape of circle
 * or rectangle(not working now). DrawEditorView makes a drawing of the shape to crop. The cropped
 * image should be scaled down using scaledownBitmap(), then send the uri of the crooped and down
 * scaled image back to the caller.
 */
public class CropImageActivity extends AppCompatActivity implements View.OnClickListener{

    private static final LoggingHelper log = LoggingHelperFactory.create(CropImageActivity.class);
    // Objects
    private Uri mUri;
    private DrawEditorView drawView;
    // UIs
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);

        Toolbar cropImageToolbar = findViewById(R.id.toolbar_crop_Image);
        setSupportActionBar(cropImageToolbar);

        if(getIntent() != null) mUri = getIntent().getData();

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        mImageView = findViewById(R.id.img_userpic);
        drawView = findViewById(R.id.view_custom_drawEditorView);
        Button btnConfirm = findViewById(R.id.btn_editor_confirm);
        Button btnCancel = findViewById(R.id.btn_editor_cancel);
        btnConfirm.setOnClickListener(this);
        btnCancel.setOnClickListener(this);


        drawView.setToolbarHeight(cropImageToolbar.getMinimumHeight() + 100);
        mImageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                drawView.setScaleMatrix(metrics, mUri, mImageView);
                mImageView.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });

    }

    // Save the cropped image
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btn_editor_confirm) {
            // Get the cropped image from DrawView.getCroppedBitmap
            Bitmap croppedBitmap = drawView.getCroppedBitmap();
            if(croppedBitmap == null) return;

            // Save the cropped image in the internal storage with the path of "images/",
            // then return its uri using FileProvder
            File imagePath = new File(getFilesDir(), "images/");
            if(!imagePath.exists()) imagePath.mkdir();

            SimpleDateFormat sdf = new SimpleDateFormat("hhmmss", Locale.US);
            String filename = sdf.format(Calendar.getInstance().getTimeInMillis());

            File imgFile = new File(imagePath, filename + ".jpg");
            if(imgFile.exists()) imgFile.delete();

            // Scale down the cropped image to the default size(50kb)
            byte[] byteUserImage = scaleDownCroppedBitmap(croppedBitmap);

            try(FileOutputStream fos = new FileOutputStream(imgFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                bos.write(byteUserImage);
            } catch(IOException e) {
                e.printStackTrace();
            } finally {
                // FileProvider which convert the file format to the content uri.  It defines
                // the file path in file_path.xml as a meta data.
                Uri croppedUri = FileProvider.getUriForFile(
                        getApplicationContext(), Constants.FILE_IMAGES, imgFile);

                Intent resultIntent = new Intent();
                resultIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                resultIntent.putExtra("croppedUri", croppedUri.toString());
                resultIntent.setData(croppedUri);
                setResult(RESULT_OK, resultIntent);
                finish();
            }

        } else finish();

    }

    // Compress the bitmap based on a given compress density, then repeat to compress with
    // a decresing density until the length of the byte array is larger than the preset one.
    private byte[] scaleDownCroppedBitmap(final Bitmap bitmap)  {
        int compressDensity = 100;
        int streamLength;
        ByteArrayOutputStream baos;
        byte[] bmpByteArray;
        // Compress the raw image down to the MAX_IMAGE_SIZE
        do{
            baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, compressDensity, baos);
            bmpByteArray = baos.toByteArray();
            streamLength = bmpByteArray.length;
            compressDensity -= 5;
            log.i("compress density: %s", streamLength / 1024 + " kb");

        } while(streamLength >= Constants.MAX_ICON_SIZE);

        return bmpByteArray;
    }
}
