package com.silverback.carman2.fragments;


import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.SparseArray;
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

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.app.Activity.RESULT_OK;
import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * A simple {@link Fragment} subclass.
 * This fragment is to upload any writing to post in the board with images attached.
 */
public class BoardWriteDlgFragment extends DialogFragment implements
        CheckBox.OnCheckedChangeListener,
        BoardAttachImageAdapter.OnBoardWriteListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardWriteDlgFragment.class);

    // Constants
    private static final int MENU_ITEM_ID = 1001;

    private static final int REQUEST_CODE_SAMSUNG = 1004;

    //private static int imageTag;
    //private static String markup;
    static final int GALLERY = 1;
    static final int CAMERA = 2;

    // Objects
    private ProgbarDialogFragment progbarFragment;
    private ApplyImageResourceUtil applyImageResourceUtil;
    private SharedPreferences mSettings;
    private FragmentSharedModel shardModel;
    private ImageViewModel imgViewModel;
    private BoardAttachImageAdapter imageAdapter;
    private List<Uri> attachedImages;
    private SparseArray<String> downloadImages;
    private UploadBitmapTask bitmapTask;
    private UploadPostTask postTask;
    private BoardImageSpanHandler spanHandler;


    // UIs
    private View localView;
    //private NestedScrollView nestedScrollView;
    private ConstraintLayout nestedLayout;
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

        attachedImages = new ArrayList<>();
        //strImgList = new ArrayList<>();
        downloadImages = new SparseArray<>();

        shardModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);
        imgViewModel = new ViewModelProvider(getActivity()).get(ImageViewModel.class);
        //uploadPostModel = ViewModelProviders.of(getActivity()).get(FirestoreViewModel.class);
        //ssb = new SpannableStringBuilder();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        localView = inflater.inflate(R.layout.dialog_board_write, container, false);
        Toolbar toolbar = localView.findViewById(R.id.toolbar_board_write);
        //nestedScrollView = localView.findViewById(R.id.nestedScrollView);
        HorizontalScrollView hScrollView = localView.findViewById(R.id.scrollview_horizontal);
        nestedLayout = localView.findViewById(R.id.vg_constraint_body);

        CheckBox chkboxMaker = localView.findViewById(R.id.chkbox_maker);
        CheckBox chkboxType = localView.findViewById(R.id.chkbox_type);
        CheckBox chkboxModel = localView.findViewById(R.id.chkbox_model);
        CheckBox chkboxYear = localView.findViewById(R.id.chkbox_year);

        etPostTitle = localView.findViewById(R.id.et_board_title);
        etPostBody = localView.findViewById(R.id.et_board_body);

        RecyclerView recyclerImageView = localView.findViewById(R.id.vg_recycler_images);
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

        // Animate the status bar up to the actionbar height which may be calculated by TypeValue
        TypedValue typedValue = new TypedValue();
        if(getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, typedValue, true)) {
            float actionBarHeight = TypedValue.complexToDimensionPixelSize(
                    typedValue.data, getResources().getDisplayMetrics());

            ObjectAnimator animStatusView = ObjectAnimator.ofFloat(hScrollView, "y", actionBarHeight);
            animStatusView.setDuration(1000);
            animStatusView.start();
        }

        // Create RecyclerView for holding attched pictures which are handled in onActivityResult()
        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        linearLayout.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerImageView.setLayoutManager(linearLayout);
        //recyclerImageView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        //recyclerImageView.setHasFixedSize(true);//DO NOT SET THIS as far as notifyItemInserted may work.
        imageAdapter = new BoardAttachImageAdapter(attachedImages, this);
        recyclerImageView.setAdapter(imageAdapter);


        // DialogFragment requires Toolbar to create the menu as like the following methods, which
        // appears different from other general fragments in which the menu is created by overriding
        // onCreateOptions menu and onOptionSelectedItem().
        toolbar.inflateMenu(R.menu.menu_board_write);
        toolbar.setNavigationOnClickListener(view -> dismiss());
        toolbar.setOnMenuItemClickListener(item -> {
            // Upload button event
            if(item.getItemId() == R.id.action_board_upload) {
                ((InputMethodManager)(getActivity().getSystemService(INPUT_METHOD_SERVICE)))
                        .hideSoftInputFromWindow(localView.getWindowToken(), 0);

                progbarFragment = new ProgbarDialogFragment();
                // No image posting makes an immediate uploading but postings with images attached
                // should take the uploading process that images starts uploading first and image
                // URIs would be sent back if uploading images is successful,then uploading gets
                // started with image URIs which should be added in a document of Firestore.
                if(attachedImages.size() == 0) {
                    log.i("upload clicked");
                    // start ProgressDialog in DialogFragment with the message of "uploading"
                    uploadPostToFirestore();
                } else {
                    // start ProgressDialog in DialogFragment w/ the message of "optimizing"
                    progbarFragment.setProgressMsg(getString(R.string.board_msg_optimize_image));
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .add(android.R.id.content, progbarFragment).commit();
                    // Image Attachment button that starts UploadBitmapTask as many as the number of
                    // images. In case the task starts with UploadBitmapTask multi-threading which
                    // runs in ThreadManager, thread contentions may occur, replacing one with the
                    // other which makes last image cover the other ones.
                    // A download url from Storage each time when an attached image is successfully
                    // downsized and scaled down, then uploaded to Storage is transferred via
                    // ImageViewModel.getDownloadBitmapUri() as a live data of SparseArray.
                    for(int i = 0; i < attachedImages.size(); i++) {
                        bitmapTask = ThreadManager.startBitmapUploadTask(
                                getContext(), attachedImages.get(i), i, imgViewModel);
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
            if(attachedImages.size() > Constants.MAX_ATTACHED_IMAGE_NUMS) {
                log.i("Image count: %s", attachedImages.size());
                Snackbar.make(nestedLayout, getString(R.string.board_msg_image), Snackbar.LENGTH_SHORT).show();

            } else {
                // Pop up the dialog to select which media to use bewteen the camera and gallery, then
                // create an intent by the selection.
                DialogFragment dialog = new BoardChooserDlgFragment();
                // BUGS FREQUENTLY OCCURRED!!!!
                //java.lang.IllegalStateException: Fragment BoardWriteDlgFragment{984f0c5}
                // (4b73b12d-9e70-4e32-95bb-52f209a6b8a1)} not attached to Activity
                //dialog.show(getChildFragmentManager(), "@null");
                dialog.show(getParentFragmentManager(), "chooserDialog");

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

                    // The result should go to the parent activity
                    getActivity().startActivityForResult(galleryIntent, BoardActivity.REQUEST_CODE_GALLERY);
                    break;

                case CAMERA: // Camera
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    Intent cameraChooser = Intent.createChooser(cameraIntent, "Choose camera");

                    if(cameraIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                        log.i("Camera Intent");
                        getActivity().startActivityForResult(cameraChooser, BoardActivity.REQUEST_CODE_CAMERA);
                    }
                    break;
            }


        });

        // The result of startActivityForResult() invoked in the parent activity should be notified
        // to the activity and it is, in turn, sent back here via a viewmodel livedata with the image
        // uri, with which the image span and the recyclerview are displayed with a new image.
        imgViewModel.getUriFromImageChooser().observe(getViewLifecycleOwner(), imgUri -> {
            // Glide creates a processed bitmap with the uri which the result intent from MediaStore
            // contains and as the process completes, the bitmap is sent to ImageViewModel for putting
            // it to the imagespan, which is defined in getGlideBitmapTarget() of onActivityCreated().
            applyImageResourceUtil.applyGlideToBitmap(imgUri, Constants.IMAGESPAN_THUMBNAIL_SIZE, imgViewModel);

            // Partial binding to show the image. RecyclerView.setHasFixedSize() is allowed to make
            // additional pics.
            attachedImages.add(imgUri);
            final int position = attachedImages.size() - 1;
            //imageAdapter.notifyItemInserted(position);
            imageAdapter.notifyItemChanged(position);
        });


        // The imgUri received as a result of startActivityForResult() is applied to applyGlideToBitmap().
        // This util method translates an image to an appropriate extent for fitting the imagespan and
        // the result is provided
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


        /*
         * The process of uploading the post consists of three steps.
         * First, upload attached images to Firebase Storage, if any.
         * Second, check whether the attached images safely completes uploading.
         * Third, start to upload the post to FireStore, then on notifying completion, dismiss.
         */
        // As UploadBitmapTask has completed to optimize an attched image and upload it to Stroage,
        // the result is notified as SparseArray which indicates the position and uriString of image.
        imgViewModel.getDownloadBitmapUri().observe(getViewLifecycleOwner(), sparseArray -> {
            // Check if the number of attached images equals to the number of uris that are down
            // loaded from Storage.
            downloadImages.put(sparseArray.keyAt(0), sparseArray.valueAt(0).toString());
            if(attachedImages.size() == downloadImages.size()) {
                // On completing optimization of attached images, start uploading a post.
                uploadPostToFirestore();
            }

        });
    }

    @Override
    public void onPause() {
        super.onPause();
        //if(bitmapTask != null) bitmapTask = null;
        //if(postTask != null) postTask = null;
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
        attachedImages.remove(position);
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


        // String.isEmpty() throws NullPointerException when the value is null but TextUtils.isEmpty()
        // always returns a boolean value, no matter what value is.
        //if(userId == null || userId.isEmpty()) return;
        if(TextUtils.isEmpty(userId)) return;

        // Start ProgbarDialogFragment to show the progressdialog and message
        progbarFragment.setProgressMsg(getString(R.string.board_msg_uploading));
        getActivity().getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, progbarFragment).commit();

        // Cast SparseArray containing download urls from Storage to String array
        List<String> downlaodUriList = new ArrayList<>(downloadImages.size());
        for(int i = 0; i < downloadImages.size(); i++) {
            downlaodUriList.add(downloadImages.keyAt(i), downloadImages.valueAt(i));
        }

        Map<String, Object> post = new HashMap<>();
        post.put("user_id", userId);
        post.put("post_title", etPostTitle.getText().toString());
        post.put("timestamp", FieldValue.serverTimestamp());
        post.put("cnt_comment", 0);
        post.put("cnt_compathy", 0);
        post.put("cnt_view", 0);
        post.put("post_content", etPostBody.getText().toString());
        post.put("post_images",  downlaodUriList);

        // Nested fields to filter the post by category
        /*
        Map<String, Object> filter = new HashMap<>();
        filter.put("auto_maker", isAutoMaker);
        filter.put("auto_type", isAutoType);
        filter.put("auto_model", isAutoModel);
        filter.put("auto_year", isAutoYear);
        post.put("post_filter", filter);
        */



        // When uploading completes, the result is sent to BoardPagerFragment and the  notifes
        // BoardPagerFragment of a new posting. At the same time, the fragment dismisses.
        postTask = ThreadManager.startUploadPostTask(getContext(), post, shardModel);
        progbarFragment.dismiss();
        dismiss();
    }


    private boolean doEmptyCheck() {
        if(TextUtils.isEmpty(etPostTitle.getText())) {
            Snackbar.make(localView, getString(R.string.board_msg_no_title), Snackbar.LENGTH_SHORT).show();
            return false;

        } else if(TextUtils.isEmpty(etPostBody.getText())){
            Snackbar.make(localView, getString(R.string.board_msg_no_content), Snackbar.LENGTH_SHORT).show();
            return false;

        } else return true;

    }
}
