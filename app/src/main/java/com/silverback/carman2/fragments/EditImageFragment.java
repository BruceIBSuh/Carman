package com.silverback.carman2.fragments;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;

import com.silverback.carman2.R;
import com.silverback.carman2.views.DrawImageView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class EditImageFragment extends Fragment implements View.OnClickListener {


    // Objects
    private DisplayMetrics metrics;
    private Uri mUri;

    // UIs
    private ImageView mImageView;
    private DrawImageView drawView;

    public EditImageFragment() {

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null) mUri = Uri.parse(getArguments().getString("uri"));
        metrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View localView = inflater.inflate(R.layout.fragment_setting_editimg, container, false);
        mImageView = localView.findViewById(R.id.img_profile);
        drawView = localView.findViewById(R.id.drawView);

        localView.findViewById(R.id.btn_editor_confirm).setOnClickListener(this);
        localView.findViewById(R.id.btn_editor_cancel).setOnClickListener(this);


        return localView;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onClick(View v) {

        switch(v.getId()) {

            case R.id.btn_editor_confirm:
                // Get the cropped image from DrawView.getCroppedBitmap
                Bitmap croppedBitmap = drawView.getCroppedBitmap();
                if(croppedBitmap == null) return;

                // Save the cropped image in the cache directory, which is removed on exiting the app.
                File imagePath = new File(getContext().getCacheDir(), "images/");
                if(!imagePath.exists()) imagePath.mkdir();

                SimpleDateFormat sdf = new SimpleDateFormat("hhmmss", Locale.US);
                Calendar calendar = Calendar.getInstance();
                String filename = sdf.format(calendar.getTimeInMillis());

                File fCropImage = new File(imagePath, filename + ".jpg");
                if(fCropImage.exists()) {
                    fCropImage.delete();
                }

                try(FileOutputStream fos = new FileOutputStream(fCropImage);
                    BufferedOutputStream bos = new BufferedOutputStream(fos)) {
                    croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);

                } catch(IOException e) {
                    //Log.e(LOG_TAG, "IOException: " + e.getMessage());

                } finally {
                    /*
                    Uri cropUri = getUriForFile(this, "com.hjkim.soccerplan.fileprovider", fCropImage);
                    //Log.i(LOG_TAG, "contentUri: " + cropUri);

                    Intent resultIntent = new Intent();
                    resultIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    resultIntent.setData(cropUri);
                    setResult(RESULT_OK, resultIntent);

                    finish();

                     */
                }


                break;

            case R.id.btn_editor_cancel:
                if(getActivity() != null) getActivity().getSupportFragmentManager().popBackStackImmediate();
                break;
        }
    }
}
