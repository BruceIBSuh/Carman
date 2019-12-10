package com.silverback.carman2.fragments;


import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FieldValue;
import com.silverback.carman2.BoardActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.AttachImageAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.models.ImageViewModel;
import com.silverback.carman2.threads.UploadBitmapTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.threads.UploadPostTask;
import com.silverback.carman2.utils.BoardImageSpanHandler;
import com.silverback.carman2.utils.Constants;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardWriteDlgFragment extends DialogFragment implements
        CheckBox.OnCheckedChangeListener,
        AttachImageAdapter.OnBoardWriteListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardWriteDlgFragment.class);

    // Constants
    private static final int MENU_ITEM_ID = 1001;
    private static final int REQUEST_CODE_CAMERA = 1002;
    private static final int REQUEST_CODE_GALLERY = 1003;
    private static final int REQUEST_CODE_SAMSUNG = 1004;

    private static int imageTag;
    private static String markup;

    static final int GALLERY = 1;
    static final int CAMERA = 2;



    // Objects
    private SharedPreferences mSettings;
    private FragmentSharedModel fragmentModel;
    private AttachImageAdapter imageAdapter;
    private List<Uri> uriImageList;
    private List<String> strImgUriList;
    private ImageSpan[] arrImageSpan;
    private UploadBitmapTask bitmapTask;
    private UploadPostTask postTask;
    private ImageViewModel bitmapModel;
    //private FirestoreViewModel uploadPostModel;
    private SpannableStringBuilder ssb;
    private BoardImageSpanHandler spanHandler;
    private ProgressBar progressBar;


    // UIs
    private NestedScrollView nestedScrollView;
    private HorizontalScrollView hScrollView;
    private ConstraintLayout statusLayout, nestedLayout;
    private RecyclerView recyclerImageView;
    private EditText etPostTitle, etPostBody;

    // Fields
    private boolean isGeneral, isAutoMaker, isAutoType, isAutoModel, isAutoYear;

    // Constructor
    public BoardWriteDlgFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the soft input mode, which seems not working.
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_UNSPECIFIED);

        mSettings = ((BoardActivity)getActivity()).getSettings();
        uriImageList = new ArrayList<>();
        strImgUriList = new ArrayList<>();
        fragmentModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        bitmapModel = ViewModelProviders.of(this).get(ImageViewModel.class);
        //uploadPostModel = ViewModelProviders.of(getActivity()).get(FirestoreViewModel.class);
        ssb = new SpannableStringBuilder();



    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.dialog_board_write, container, false);

        nestedScrollView = localView.findViewById(R.id.nestedScrollView);
        hScrollView = localView.findViewById(R.id.scrollview_horizontal);
        //statusLayout = localView.findViewById(R.id.vg_constraint_status);
        nestedLayout = localView.findViewById(R.id.vg_constraint_body);

        CheckBox chkboxMaker = localView.findViewById(R.id.chkbox_maker);
        CheckBox chkboxType = localView.findViewById(R.id.chkbox_type);
        CheckBox chkboxModel = localView.findViewById(R.id.chkbox_model);
        CheckBox chkboxYear = localView.findViewById(R.id.chkbox_year);

        etPostTitle = localView.findViewById(R.id.et_board_title);
        etPostBody = localView.findViewById(R.id.et_board_body);

        recyclerImageView = localView.findViewById(R.id.vg_recycler_images);
        Button btnAttach = localView.findViewById(R.id.btn_attach_image);
        ImageButton btnDismiss = localView.findViewById(R.id.btn_dismiss);
        ImageButton btnUpload = localView.findViewById(R.id.btn_upload);

        RelativeLayout relativeLayout = localView.findViewById(R.id.vg_relative_attach);

        chkboxMaker.setText(mSettings.getString("pref_auto_maker", null));
        chkboxType.setText(mSettings.getString("pref_auto_type", null));
        chkboxModel.setText(mSettings.getString("pref_auto_model", null));
        chkboxYear.setText(mSettings.getString("pref_auto_year", null));


        // Set the event listener to the checkboxes
        chkboxMaker.setOnCheckedChangeListener(this);
        chkboxType.setOnCheckedChangeListener(this);
        chkboxModel.setOnCheckedChangeListener(this);
        chkboxYear.setOnCheckedChangeListener(this);



        log.i("Bottom height: %s", relativeLayout.getHeight());

        /*
        statusLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            float statusHeight = statusLayout.getHeight();
            log.i("statuslayout height: %s", statusHeight);
        });
        */

        // Animate the status bar
        TypedValue typedValue = new TypedValue();
        if(getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            float actionBarHeight = TypedValue.complexToDimensionPixelSize(
                    typedValue.data, getResources().getDisplayMetrics());

            ObjectAnimator animStatusView = ObjectAnimator.ofFloat(hScrollView, "Y", actionBarHeight);
            animStatusView.setDuration(1000);
            animStatusView.start();
        }



        // Create RecyclerView with attched pictures which are handled in onActivityResult()
        recyclerImageView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        //recyclerImageView.setHasFixedSize(true);//DO NOT SET THIS as far as notifyItemInserted may work.
        imageAdapter = new AttachImageAdapter(uriImageList, this);
        recyclerImageView.setAdapter(imageAdapter);



        // Set the event listeners to the buttons.
        btnDismiss.setOnClickListener(btn -> dismiss());

        // Upload button
        btnUpload.setOnClickListener(btn -> {
            if(!doEmptyCheck()) return;

            // Create ProbressBar
            /*
            RelativeLayout layout = new RelativeLayout(getContext());
            ProgressBar progressBar = new ProgressBar(getActivity(), null,android.R.attr.progressBarStyleLarge);
            progressBar.setIndeterminate(true);
            progressBar.setVisibility(View.VISIBLE);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100,100);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            layout.addView(progressBar,params);
            */


            // No attached image immediately makes uploading started.
            if(uriImageList.size() == 0) uploadPostToFirestore();
            else {

                // Downsize and compress attached images and upload them to Storage running in
                // the background, the result of which is notified to getUploadBitmap() of ImageViewModel
                // one by one and all of images has processed, start to upload the post to Firestore.
                for (Uri uri : uriImageList) {
                    bitmapTask = ThreadManager.startBitmapUploadTask(getContext(), uri, bitmapModel);
                }
            }

        });

        // Call the gallery or camera to capture images, the URIs of which are sent to an intent
        // of onActivityResult(int, int, Intent)
        btnAttach.setOnClickListener(btn -> {
            ((InputMethodManager)(getActivity().getSystemService(INPUT_METHOD_SERVICE)))
                    .hideSoftInputFromWindow(etPostTitle.getWindowToken(), 0);

            // Pop up the dialog as far as the num of attached pics are no more than 6.
            if(uriImageList.size() > Constants.MAX_ATTACHED_IMAGE) {
                log.i("Image count: %s", uriImageList.size());
                Snackbar.make(nestedLayout, getString(R.string.board_msg_image), Snackbar.LENGTH_SHORT).show();
            } else {
                // Pop up the dialog to select which media to use bewteen the camera and gallery, then
                // create an intent by the selection.
                DialogFragment dialog = new BoardChooserDlgFragment();
                dialog.show(getChildFragmentManager(), "@null");

                // Put a line feed into the EditTex when the image interleaves b/w the lines
                /*
                 * This works for both, inserting a text at the current position and replacing
                 * whatever text is selected by the user. The Math.max() is necessary in the first
                 * and second line because, if there is no selection or cursor in the EditText,
                 * getSelectionStart() and getSelectionEnd() will both return -1. The Math.min()
                 * and Math.max() in the third line is necessary because the user could have selected
                 * the text backwards and thus start would have a higher value than end which is not
                 * allowed for Editable.replace().
                 */
                int start = Math.max(etPostBody.getSelectionStart(), 0);
                int end = Math.max(etPostBody.getSelectionStart(), 0);
                etPostBody.getText().replace(Math.min(start, end), Math.max(start, end), "\n");
            }
        });

        // Create BoardImageSpanHandler, a helper class to handle the image span and the spanwatcher
        // to prevent the image span from deletion while editing.
        spanHandler = new BoardImageSpanHandler(etPostBody.getText());

        return localView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        // Notified of which chooser to select in BoardChooserDlgFragment
        fragmentModel.getImageChooser().observe(getActivity(), chooser -> {
            log.i("FragmentSharedModel chooser: %s", chooser);

            switch(chooser) {
                case GALLERY:
                    // Handle SAMSUNG for multi-selection
                    /*
                    if(Build.MANUFACTURER.equalsIgnoreCase("samsung")) {
                        Intent samsungIntent = new Intent("android.intent.action.MULTIPLE_PICK");
                        samsungIntent.setType("image/*");
                        PackageManager manager = getActivity().getApplicationContext().getPackageManager();
                        List<ResolveInfo> infos = manager.queryIntentActivities(samsungIntent, 0);
                        if(infos.size() > 0){
                            //samsungIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                            startActivityForResult(samsungIntent, REQUEST_CODE_SAMSUNG);
                        }
                        // General phones other than SAMSUNG
                    } else {
                        Intent galleryIntent = new Intent();
                        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                        galleryIntent.setType("image/*");
                        //galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                        startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY);
                    }
                    */

                    Intent galleryIntent = new Intent();
                    galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                    galleryIntent.setType("image/*");
                    //galleryIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                    startActivityForResult(galleryIntent, REQUEST_CODE_GALLERY);
                    break;

                case CAMERA: // Camera
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    Intent cameraChooser = Intent.createChooser(cameraIntent, "Choose camera");

                    if(cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                        log.i("Camera Intent");
                        startActivityForResult(cameraChooser, REQUEST_CODE_CAMERA);
                    }
                    break;
            }
        });

        // Notified of having attached images uploaded to Firebase Storage and retreive each uri
        // of uploaded images by ImageViewModel
        bitmapModel.getUploadBitmap().observe(getViewLifecycleOwner(), uriString -> {
            log.i("UploadedImageUri: %s", uriString);
            strImgUriList.add(uriString);

            // Start uploading only when attached images finised downsizing and uploading to Storage.
            // Otherwise, the image uris fail to upload to Firestore.
            if(strImgUriList.size() == uriImageList.size()) {
                uploadPostToFirestore();
                dismiss();
            }

        });

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode != RESULT_OK || data == null) return;
        Uri imgUri = null;

        switch(requestCode) {
            case REQUEST_CODE_GALLERY:
                if(data.getData() != null) {
                    imgUri = data.getData();
                    uriImageList.add(imgUri);
                }
                break;

            case REQUEST_CODE_CAMERA:
                break;
        }

        // Insert ImageSpan into SpannalbeStringBuilder
        //Bitmap bitmap = EditImageHelper.resizeBitmap(getContext(), imgUri, 50, 50);
        //ImageSpan imgSpan = new ImageSpan(getContext(), bitmap);
        // EditImageHelper is repalced w/ Glide.
        Glide.with(getContext().getApplicationContext()).asBitmap().override(80, 80).centerCrop()
                .load(imgUri)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        ImageSpan imgSpan = new ImageSpan(getContext(), resource);
                        // Manage the image spans using BoardImageSpanHandler helper class.
                        spanHandler.setImageSpanToPosting(imgSpan);
                        //spanHandler.setImageSpanInputFilter();

                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        log.i("onLoadCleard");
                        // this is called when imageView is cleared on lifecycle call or for
                        // some other reason.
                        // if you are referencing the bitmap somewhere else too other than this imageView
                        // clear it here as you can no longer have the bitmap

                    }
                });




        // Partial binding to show the image. RecyclerView.setHasFixedSize() is allowed to make
        // additional pics.
        final int position = uriImageList.size() - 1;
        //imageAdapter.notifyItemInserted(position);
        imageAdapter.notifyItemChanged(position);


        // Resize the image
        //handleAttachedBitmap(uriImageList.get(position));
        //bitmapTask = ThreadManager.startBitmapUploadTask(getContext(), uriImageList.get(position), bitmapModel);
        super.onActivityResult(requestCode, resultCode, data);

    }


    // Callback by Checkboxes
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch(buttonView.getId()) {
            case R.id.chkbox_maker:
                isAutoMaker = isChecked;
                break;

            case R.id.chkbox_model:
                isAutoModel = isChecked;
                break;

            case R.id.chkbox_type:
                isAutoType = isChecked;
                break;

            case R.id.chkbox_year:
                isAutoYear = isChecked;
                break;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void uploadPostToFirestore() {
        //if(!doEmptyCheck()) return;

        //FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String userId = null;
        try (FileInputStream fis = getActivity().openFileInput("userId");
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            userId = br.readLine();
            log.i("userID: %s", userId);
        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        }

        String[] arrUriString = new String[strImgUriList.size()];
        for (int i = 0; i < strImgUriList.size(); i++) arrUriString[i] = strImgUriList.get(i);

        //if(TextUtils.isEmpty(userId)) return;
        if(userId == null || userId.isEmpty()) return;

        Map<String, Object> post = new HashMap<>();
        post.put("user_id", userId);
        post.put("post_title", etPostTitle.getText().toString());
        post.put("timestamp", FieldValue.serverTimestamp());
        post.put("cnt_comment", 0);
        post.put("cnt_compathy", 0);
        post.put("post_content", etPostBody.getText().toString());
        post.put("post_images",  Arrays.asList(arrUriString));

        Map<String, Object> filter = new HashMap<>();
        filter.put("general", isGeneral);
        filter.put("auto_maker", isAutoMaker);
        filter.put("auto_type", isAutoType);
        filter.put("auto_model", isAutoModel);
        filter.put("auto_year", isAutoYear);

        post.put("post_filter", filter);

        // Alternative
        //post.put("post_filter", Arrays.asList(isGeneral, isAutoMaker, isAutoType, isAutoModel, isAutoYear));


        //postTask = ThreadManager.startUploadPostTask(getContext(), postTitle, content, strImgUriList);
        postTask = ThreadManager.startUploadPostTask(getContext(), post, fragmentModel);


        /*
        // In case attached images exist, upload the images as Array.
        if(uriUploadList.size() > 0) {
            String[] arrImageUri = new String[uriUploadList.size()];
            for(int i = 0; i < uriUploadList.size(); i++) {
                arrImageUri[i] = uriUploadList.get(i).toString();
            }
            post.put("images", Arrays.asList(arrImageUri));
        }

        // Query the user data with the retrieved user id.
        firestore.collection("users").document(userId).get().addOnSuccessListener(document -> {
            String userName = document.getString("user_name");
            String userPic = document.getString("user_pic");
            if(!userName.isEmpty()) post.put("user_name", userName);
            if(!userPic.isEmpty()) post.put("user_pic", userPic);

            // Upload the post along with the queried user data, which may prevent latency to load
            // the user data if the post retrieves the user data from different collection.
            firestore.collection("board_general").add(post)
                    .addOnSuccessListener(docref -> {
                        // Notify BoardPagerFragment of completing upload to upadte the fragment.
                        fragmentModel.getNewPosting().setValue(true);
                        dismiss();

                    })
                    .addOnFailureListener(e -> log.e("upload failed: %s", e.getMessage()));
        });

        */

    }

    private boolean doEmptyCheck() {
        if(TextUtils.isEmpty(etPostTitle.getText())) {
            Snackbar.make(nestedLayout, "Title is empty", Snackbar.LENGTH_SHORT).show();
            return false;
        } else if(TextUtils.isEmpty(etPostBody.getText())){
            Snackbar.make(nestedLayout, "Title is empty", Snackbar.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    // Callback invoked by AttachImageAdapter.OnBoardWriteListener when an image is removed from the list
    @Override
    public void removeGridImage(int position) {
        ImageSpan[] arrImageSpan = spanHandler.getImageSpan();
        log.i("position: %s, %s, %s", position, uriImageList.size(), arrImageSpan.length);
        imageAdapter.notifyItemRemoved(position);
        uriImageList.remove(position);
        spanHandler.removeImageSpan(position);


        imageTag -= 1;

        //ssb.removeSpan(arrImageSpan[position]);
        //log.i("ssb remove span: %s", ssb);

    }


    /*
    @SuppressWarnings("ConstantConditions")
    private void handleAttachedBitmap(Uri uri)  {

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        try (InputStream is = getContext().getApplicationContext().getContentResolver().openInputStream(uri)) {

            BitmapFactory.decodeStream(is, new Rect(10, 10, 10, 10), options);
            int imgWidth = options.outWidth;
            int imgHeight = options.outHeight;
            log.i("Dimension: %s, %s", imgWidth, imgHeight);
            options.inSampleSize = calculateInSampleSize(options, 100, 100);

        } catch(IOException e) {
            log.e("IOException: %s",e.getMessage());
        }
    }


    public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

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

    */

}
