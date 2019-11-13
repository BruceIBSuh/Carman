package com.silverback.carman2;

import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman2.adapters.AttachImagesAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;

public class BoardWritingActivity extends BaseActivity {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardWritingActivity.class);

    // Constants
    private static final int MENU_ITEM_ID = 1001;
    private static final int REQUEST_CODE_CAMERA = 1002;
    private static final int REQUEST_CODE_GALLERY = 1003;

    // Objects
    private List<Uri> uriImageList;
    private AttachImagesAdapter imageAdapter;

    // UIs
    private CoordinatorLayout root;
    private ConstraintLayout statusLayout;
    private View statusView, titleView;
    private TextView tvAutoMaker, tvAutoModel, tvAutoYear;
    private TextView tvBoardTitle, tvClubStatus;
    private EditText etBoardTitle;
    private RecyclerView recyclerImageView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board_writing);

        root = findViewById(R.id.coordinatorLayout);
        Toolbar toolbar = findViewById(R.id.toolbar_board_write);
        statusLayout = findViewById(R.id.vg_status);
        //statusView = findViewById(R.id.tv_status_automaker);
        //titleView = findViewById(R.id.view_title);
        //tvAutoMaker = findViewById(R.id.tv_status_automaker);
        //tvAutoModel = findViewById(R.id.tv_status_model);
        //tvAutoYear = findViewById(R.id.tv_status_year);
        TextView tvAttach = findViewById(R.id.btn_attach_image);
        EditText etTitle = findViewById(R.id.et_board_title);
        EditText etContent = findViewById(R.id.et_board_body);
        recyclerImageView = findViewById(R.id.vg_recycler_images);

        etTitle.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        // Call the gallery or camera to capture images, the URIs of which are sent to an intent
        // of onActivityResult(int, int, Intent)
        tvAttach.setOnClickListener(view -> {
            ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(etTitle.getWindowToken(), 0);

            attachImage();
        });

        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        // Instantiate the objects
        uriImageList = new ArrayList<>();


        animateStatusTitleViews(getActionbarHeight());


        recyclerImageView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerImageView.setHasFixedSize(true);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, MENU_ITEM_ID, Menu.NONE, R.string.exp_menuitem_title_save);
        MenuItem item = menu.findItem(MENU_ITEM_ID);
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setIcon(R.drawable.ic_toolbar_save);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            log.i("onOptionsItemSelected in SettingPreferenceActivity");
            Intent resultIntent = new Intent();
            resultIntent.putExtra("result_msg", "OK");
            setResult(1001, resultIntent);
            finish();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void animateStatusTitleViews(float actionbarHeight) {
        ObjectAnimator animStatusView = ObjectAnimator.ofFloat(statusLayout, "Y", actionbarHeight);
        animStatusView.setDuration(1000);
        animStatusView.start();
    }

    private void attachImage() {
        log.i("Attach Image clicked");

        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY);

        /*
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Intent chooser = Intent.createChooser(cameraIntent, "Choose camera");

        if(cameraIntent.resolveActivity(getPackageManager()) != null) {
            log.i("Camera Intent");
            startActivityForResult(chooser, REQUEST_CODE_CAMERA);
        }

         */
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {


        log.i("intent data: %s", data);
        try {
            if(requestCode == REQUEST_CODE_GALLERY && resultCode == RESULT_OK && data != null) {

                String[] filePathColumns = {MediaStore.Images.Media.DATA};
                List<String> imgEncodedList = new ArrayList<>();
                String imgEncoded = null;

                // Single selection out of the gallery
                if(data.getData() != null) {
                    log.i("getData");
                    Uri imageUri = data.getData();
                    uriImageList.add(imageUri);

                    Cursor cursor = getContentResolver().query(imageUri, filePathColumns, null, null, null);
                    if(cursor == null) return;
                    cursor.moveToFirst();


                    int columnIndex = cursor.getColumnIndex(filePathColumns[0]);
                    imgEncoded  = cursor.getString(columnIndex);
                    cursor.close();

                // Multiple selection out of the gallery
                } else {
                    if(data.getClipData() == null) return;
                    ClipData clipData = data.getClipData();

                    log.i("ClipData: %s", clipData.getItemCount());
                    for(int i = 0; i < clipData.getItemCount(); i++) {
                        ClipData.Item item = clipData.getItemAt(i);
                        Uri imageUri = item.getUri();
                        log.i("Multiple Uris: %s", imageUri);
                        uriImageList.add(imageUri);

                        Cursor cursor = getContentResolver().query(imageUri, filePathColumns, null, null, null);
                        if(cursor == null) return;
                        cursor.moveToFirst();

                        int colIndex = cursor.getColumnIndex(filePathColumns[0]);
                        imgEncoded = cursor.getString(colIndex);
                        imgEncodedList.add(imgEncoded);
                        cursor.close();

                    }
                }


                imageAdapter = new AttachImagesAdapter(uriImageList);
                recyclerImageView.setAdapter(imageAdapter);



            } else {
                Snackbar.make(root, " Selected no images", Snackbar.LENGTH_SHORT).show();
            }

        } catch(Exception e) {
            log.e("Exception occurred: %s", e.getMessage());
        }


        super.onActivityResult(requestCode, resultCode, data);

    }
}
