package com.silverback.carman2.fragments;


import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;

import android.net.Uri;
import android.os.Bundle;
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
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FieldValue;
import com.silverback.carman2.BoardActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardAttachImageAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.models.ImageViewModel;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.threads.UploadBitmapTask;
import com.silverback.carman2.threads.UploadPostTask;
import com.silverback.carman2.utils.ApplyImageResourceUtil;
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
        BoardAttachImageAdapter.OnBoardWriteListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardWriteDlgFragment.class);

    // Constants
    private static final int MENU_ITEM_ID = 1001;
    private static final int REQUEST_CODE_CAMERA = 1002;
    private static final int REQUEST_CODE_GALLERY = 1003;
    private static final int REQUEST_CODE_SAMSUNG = 1004;

    //private static int imageTag;
    //private static String markup;

    static final int GALLERY = 1;
    static final int CAMERA = 2;



    // Objects
    private ApplyImageResourceUtil applyImageResourceUtil;
    private SharedPreferences mSettings;
    private FragmentSharedModel shardModel;
    private BoardAttachImageAdapter imageAdapter;
    private List<Uri> uriImageList;
    private List<String> strImgUriList;
    private ImageSpan[] arrImageSpan;
    private UploadBitmapTask bitmapTask;
    private UploadPostTask postTask;
    private ImageViewModel imgViewModel;
    //private FirestoreViewModel uploadPostModel;
    private SpannableStringBuilder ssb;
    private BoardImageSpanHandler spanHandler;
    private ProgressBar progressBar;


    // UIs
    private View localView;
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
        getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        //setHasOptionsMenu(true);

        applyImageResourceUtil = new ApplyImageResourceUtil(getContext());
        mSettings = ((BoardActivity)getActivity()).getSettings();
        uriImageList = new ArrayList<>();
        strImgUriList = new ArrayList<>();
        shardModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);
        imgViewModel = new ViewModelProvider(this).get(ImageViewModel.class);
        //uploadPostModel = ViewModelProviders.of(getActivity()).get(FirestoreViewModel.class);
        ssb = new SpannableStringBuilder();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        localView = inflater.inflate(R.layout.dialog_board_write, container, false);
        Toolbar toolbar = localView.findViewById(R.id.toolbar_board_write);
        //nestedScrollView = localView.findViewById(R.id.nestedScrollView);
        hScrollView = localView.findViewById(R.id.scrollview_horizontal);
        nestedLayout = localView.findViewById(R.id.vg_constraint_body);

        CheckBox chkboxMaker = localView.findViewById(R.id.chkbox_maker);
        CheckBox chkboxType = localView.findViewById(R.id.chkbox_type);
        CheckBox chkboxModel = localView.findViewById(R.id.chkbox_model);
        CheckBox chkboxYear = localView.findViewById(R.id.chkbox_year);

        etPostTitle = localView.findViewById(R.id.et_board_title);
        etPostBody = localView.findViewById(R.id.et_board_body);

        recyclerImageView = localView.findViewById(R.id.vg_recycler_images);
        Button btnAttach = localView.findViewById(R.id.btn_attach_image);
        //ImageButton btnDismiss = localView.findViewById(R.id.btn_dismiss);
        //ImageButton btnUpload = localView.findViewById(R.id.btn_upload);

        chkboxMaker.setText(mSettings.getString(Constants.AUTO_MAKER, null));
        chkboxType.setText(mSettings.getString(Constants.AUTO_TYPE, null));
        chkboxModel.setText(mSettings.getString(Constants.AUTO_MODEL, null));
        chkboxYear.setText(mSettings.getString(Constants.AUTO_YEAR, null));


        // Set the event listener to the checkboxes
        chkboxMaker.setOnCheckedChangeListener(this);
        chkboxType.setOnCheckedChangeListener(this);
        chkboxModel.setOnCheckedChangeListener(this);
        chkboxYear.setOnCheckedChangeListener(this);

        /*
        statusLayout.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            float statusHeight = statusLayout.getHeight();
            log.i("statuslayout height: %s", statusHeight);
        });
        */

        // Animate the status bar up to the actionbar height.
        TypedValue typedValue = new TypedValue();
        if(getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            log.i("animation filter bar");
            float actionBarHeight = TypedValue.complexToDimensionPixelSize(
                    typedValue.data, getResources().getDisplayMetrics());

            ObjectAnimator animStatusView = ObjectAnimator.ofFloat(hScrollView, "y", actionBarHeight);
            animStatusView.setDuration(1000);
            animStatusView.start();
        }

        // Create RecyclerView with attched pictures which are handled in onActivityResult()
        recyclerImageView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        //recyclerImageView.setHasFixedSize(true);//DO NOT SET THIS as far as notifyItemInserted may work.
        imageAdapter = new BoardAttachImageAdapter(uriImageList, this);
        recyclerImageView.setAdapter(imageAdapter);


        // DialogFragment requires Toolbar to create the menu as like the following methods, which is
        // different from general Fragment in which the menu is created by overriding onCreateOptions
        // menu and onOptionSelectedItem().
        toolbar.inflateMenu(R.menu.menu_board_write);
        toolbar.setNavigationOnClickListener(view -> dismiss());
        // Upload the post to FireStore
        toolbar.setOnMenuItemClickListener(item -> {

            if(item.getItemId() == R.id.action_board_upload) {
                ((InputMethodManager)(getActivity().getSystemService(INPUT_METHOD_SERVICE)))
                        .hideSoftInputFromWindow(localView.getWindowToken(), 0);

                // Posting with no attached image immediately starts to upload. Otherwise, takes
                // the uploading process that starts image uploading, then receives uris, and
                // finally, upload the post.
                if(uriImageList.size() == 0) {
                    log.i("upload clicked");
                    uploadPostToFirestore();
                } else {
                    // Downsize and compress attached images and upload them to Storage running in
                    // the background, the result of which is notified to getUploadBitmap() of ImageViewModel
                    // one by one and all of images has processed, start to upload the post to Firestore.
                    for (Uri uri : uriImageList) {
                        bitmapTask = ThreadManager.startBitmapUploadTask(getContext(), uri, imgViewModel);
                    }
                }

                return true;
            }

            return false;
        });


        // Call the gallery or camera to capture images, the URIs of which are sent to an intent
        // of onActivityResult(int, int, Intent)
        btnAttach.setOnClickListener(btn -> {

            ((InputMethodManager)(getActivity().getSystemService(INPUT_METHOD_SERVICE)))
                    .hideSoftInputFromWindow(localView.getWindowToken(), 0);

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

        // When uploading the post, check if any attached image exists.
        /*
        btnUpload.setOnClickListener(btn -> {
            if(!doEmptyCheck()) return;

            ((InputMethodManager)(getActivity().getSystemService(INPUT_METHOD_SERVICE)))
                    .hideSoftInputFromWindow(localView.getWindowToken(), 0);

            // No attached image immediately makes uploading started.
            if(uriImageList.size() == 0) uploadPostToFirestore();
            else {

                // Downsize and compress attached images and upload them to Storage running in
                // the background, the result of which is notified to getUploadBitmap() of ImageViewModel
                // one by one and all of images has processed, start to upload the post to Firestore.
                for (Uri uri : uriImageList) {
                    bitmapTask = ThreadManager.startBitmapUploadTask(getContext(), uri, imgViewModel);
                }
            }

            //dismiss();

        });
        */

        // Create BoardImageSpanHandler implementing SpanWatcher, which is a helper class to handle
        // SpannableStringBuilder in order to protect image spans from while editing.
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

        // ViewModel to be Notified of which media(camera or gallery) to select in BoardChooserDlgFragment
        shardModel.getImageChooser().observe(getActivity(), chooser -> {

            switch(chooser) {
                case GALLERY:
                    // MULTI-SELECTION: special handling of Samsung phone.
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

        imgViewModel.getGlideBitmapTarget().observe(getViewLifecycleOwner(), bitmap -> {
            log.i("Bitmap received");
            ImageSpan imgSpan = new ImageSpan(getContext(), bitmap);
            // Manage the image spans using BoardImageSpanHandler helper class.
            spanHandler.setImageSpanToPosting(imgSpan);
            //spanHandler.setImageSpanInputFilter();
        });

        /*
        imgViewModel.getGlideDrawableTarget().observe(getViewLifecycleOwner(), drawable -> {
            log.i("Drawable received");
        });
         */


        // Upload process consists of three steps, which should be considered to refactor.
        // First, upload attached images to Firebase Storage, if any.
        // Second, check whether the attached images safely completes uploading.
        // Third, start to upload the post to FireStore, then on notifying completion, dismiss.

        // Notified of having attached images uploaded to Firebase Storage and retreive each uri
        // of uploaded images by ImageViewModel
        imgViewModel.getUploadBitmap().observe(getViewLifecycleOwner(), uriString -> {
            log.i("UploadedImageUri: %s", uriString);
            // Receive the uris of uploaded images sequentially.
            strImgUriList.add(uriString);

            // Start uploading only when attached images finised downsizing and uploading to Storage.
            // Otherwise, the image uris fail to upload to Firestore.
            if(strImgUriList.size() == uriImageList.size()) {
                log.i("Image numbers: %s", strImgUriList.size());
                uploadPostToFirestore();
            }

        });
    }

    // Receive the uri of an selected image from BoardChooserDlgFragment as the resulf of
    // startActivityForResult().
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(resultCode != RESULT_OK || data == null) return;
        Uri imgUri = null;

        switch(requestCode) {
            case REQUEST_CODE_GALLERY:
                imgUri = data.getData();
                uriImageList.add(imgUri);
                break;

            case REQUEST_CODE_CAMERA:
                break;
        }

        // Glide creates a processed bitmap with the uri which the result intent from MediaStore
        // contains and as the process completes, the bitmap is sent to ImageViewModel for putting
        // it to the imagespan, which is defined in getGlideBitmapTarget() of onActivityCreated().
        applyImageResourceUtil.applyGlideToBitmap(imgUri, 80, imgViewModel);

        // Partial binding to show the image. RecyclerView.setHasFixedSize() is allowed to make
        // additional pics.
        final int position = uriImageList.size() - 1;
        //imageAdapter.notifyItemInserted(position);
        imageAdapter.notifyItemChanged(position);


        // Resize the image: TEST CODING!!!!!
        //bitmapTask = ThreadManager.startBitmapUploadTask(getContext(), uriImageList.get(position), imgViewModel);
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

    // Callback invoked by BoardAttachImageAdapter.OnBoardWriteListener when an image is removed from the list
    @Override
    public void removeGridImage(int position) {

        spanHandler.removeImageSpan(position);
        //ImageSpan[] arrImageSpan = spanHandler.getImageSpan();
        imageAdapter.notifyItemRemoved(position);
        uriImageList.remove(position);
    }

    @SuppressWarnings("ConstantConditions")
    private void uploadPostToFirestore() {

        if(!doEmptyCheck()) return;

        String userId = null;
        try (FileInputStream fis = getActivity().openFileInput("userId");
             BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            userId = br.readLine();

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
        post.put("cnt_view", 0);
        post.put("post_content", etPostBody.getText().toString());
        post.put("post_images",  Arrays.asList(arrUriString));

        // Nested fields to filter the post by category
        Map<String, Object> filter = new HashMap<>();
        filter.put("general", isGeneral);
        filter.put("auto_maker", isAutoMaker);
        filter.put("auto_type", isAutoType);
        filter.put("auto_model", isAutoModel);
        filter.put("auto_year", isAutoYear);

        post.put("post_filter", filter);

        // When uploading completes, the result is sent to BoardPagerFragment and the  notifes
        // BoardPagerFragment of a new posting. At the same time, the fragment dismisses.
        postTask = ThreadManager.startUploadPostTask(getContext(), post, shardModel);
        dismiss();
    }


    private boolean doEmptyCheck() {

        if(TextUtils.isEmpty(etPostTitle.getText())) {
            Snackbar.make(localView, "Title is empty", Snackbar.LENGTH_SHORT).show();
            return false;

        } else if(TextUtils.isEmpty(etPostBody.getText())){
            Snackbar.make(localView, "Title is empty", Snackbar.LENGTH_SHORT).show();
            return false;

        } else return true;

    }
}
