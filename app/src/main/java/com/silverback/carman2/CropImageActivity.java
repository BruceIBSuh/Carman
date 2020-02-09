package com.silverback.carman2;

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

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.views.DrawEditorView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static androidx.core.content.FileProvider.getUriForFile;


/**
 * This activity is an image editor that crops an specific area of images to the shape of circle
 * or rectangle(not working now). DrawEditorView makes a drawing of the shape to crop. The cropped
 * image
 */
public class CropImageActivity extends AppCompatActivity implements View.OnClickListener{

    private static final LoggingHelper log = LoggingHelperFactory.create(CropImageActivity.class);

    // Objects
    private Uri mUri;
    //private EditImageHelper editImage;
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
        //EditImageHelper editImage = new EditImageHelper(this);

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

        switch(v.getId()) {
            case R.id.btn_editor_confirm:
                // Get the cropped image from DrawView.getCroppedBitmap
                Bitmap croppedBitmap = drawView.getCroppedBitmap();
                if(croppedBitmap == null) return;

                // Save the cropped image in the internal storage with the path of "/images/fimename",
                // then return its uri using FileProvder
                File imagePath = new File(getFilesDir(), "images/");
                if(!imagePath.exists()) imagePath.mkdir();

                SimpleDateFormat sdf = new SimpleDateFormat("hhmmss", Locale.US);
                String filename = sdf.format(Calendar.getInstance().getTimeInMillis());

                File fCropImage = new File(imagePath, filename + ".jpg");
                if(fCropImage.exists()) fCropImage.delete();

                try(FileOutputStream fos = new FileOutputStream(fCropImage);
                    BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);

                } catch(IOException e) {
                    e.printStackTrace();

                } finally {
                    // FileProvider which convert the file format to Uri and defines the file path
                    // in file_path.xml.
                    Uri cropUri = FileProvider.getUriForFile(this, Constants.FILE_IMAGES, fCropImage);

                    Intent resultIntent = new Intent();
                    resultIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    resultIntent.setData(cropUri);
                    setResult(RESULT_OK, resultIntent);

                    finish();
                }

                break;

            case R.id.btn_editor_cancel:
                finish();
                break;
        }

    }
}
