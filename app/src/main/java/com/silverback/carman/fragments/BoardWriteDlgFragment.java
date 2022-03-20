package com.silverback.carman.fragments;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.silverback.carman.BoardActivity.AUTOCLUB;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import com.silverback.carman.BoardActivity;
import com.silverback.carman.R;
import com.silverback.carman.adapters.BoardImageAdapter;
import com.silverback.carman.databinding.FragmentBoardWriteTempBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.threads.UploadBitmapTask;
import com.silverback.carman.threads.UploadPostTask;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.BoardImageSpanHandler;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.ImageViewModel;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoardWriteDlgFragment extends DialogFragment implements
        BoardImageSpanHandler.OnImageSpanListener, CompoundButton.OnCheckedChangeListener,
        BoardImageAdapter.OnBoardAttachImageListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardWriteDlgFragment.class);

    private FirebaseFirestore mDB;
    private FragmentBoardWriteTempBinding binding;
    private BoardImageAdapter imageAdapter;
    private BoardImageSpanHandler spanHandler;
    private UploadBitmapTask bitmapTask;
    private UploadPostTask postTask;

    private FragmentSharedModel fragmentModel;
    private ImageViewModel imgViewModel;

    private List<Uri> uriImageList;
    private SparseArray<Uri> sparseUriArray;
    private List<String> cbAutoFilter;

    private Uri imageUri;
    private String userId;
    private String userName;
    private int page;
    private boolean isGeneralPost;

    public BoardWriteDlgFragment() {
        //empty constructor which might be referenced by FragmentFactory
    }

    @Override
    public void onStart(){
        super.onStart();
        //for rererernce only
        /*
        Dialog dialog = getDialog();
        if (dialog != null){
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            Objects.requireNonNull(dialog.getWindow()).setLayout(width, height);
        }
         */
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if(getArguments() != null) {
            userId = getArguments().getString("userId");
            userName = getArguments().getString("userName");
            page = getArguments().getInt("page");
        }

        mDB = FirebaseFirestore.getInstance();
        uriImageList = new ArrayList<>();
        sparseUriArray = new SparseArray<>();
        cbAutoFilter = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentBoardWriteTempBinding.inflate(inflater);
        binding.toolbarBoardWrite.setTitle("POST WRITING");
        binding.toolbarBoardWrite.setNavigationOnClickListener(view -> dismiss());
        createPostWriteMenu();

        //RecyclerView to display attached images
        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        linearLayout.setOrientation(LinearLayoutManager.HORIZONTAL);
        binding.recyclerImages.setLayoutManager(linearLayout);
        //recyclerImageView.setHasFixedSize(true);
        imageAdapter = new BoardImageAdapter(getContext(), uriImageList, this);
        binding.recyclerImages.setAdapter(imageAdapter);

        spanHandler = new BoardImageSpanHandler(binding.etPostContent, this);
        binding.btnImage.setOnClickListener(view -> selectImageMedia());

        //Create the autofilter
        if(page == AUTOCLUB) {
            animAutoFilter();
            String jsonAutoFilter = requireArguments().getString("autofilter");
            try {setAutoFilter(jsonAutoFilter); }
            catch (JSONException e) {e.printStackTrace();}
        } //else binding.scrollviewAutofilter.setVisibility(View.GONE);

        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        imgViewModel = new ViewModelProvider(requireActivity()).get(ImageViewModel.class);

        fragmentModel.getImageChooser().observe(getViewLifecycleOwner(), chooser -> {
            ((BoardActivity)requireActivity()).selectImageMedia(chooser, binding.getRoot());
        });

        imgViewModel.getDownloadBitmapUri().observe(getViewLifecycleOwner(), sparseArray -> {
            log.i("Storage download url:%s,  %s", sparseArray.keyAt(0), sparseArray.valueAt(0));
            sparseUriArray.put(sparseArray.keyAt(0), sparseArray.valueAt(0));
            if(uriImageList.size() == sparseUriArray.size()) uploadPostToFirestore();
        });
    }


    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        log.i("onDismiss");
        if(bitmapTask != null) bitmapTask = null;
        if(postTask != null) postTask = null;
    }


    //Implement BoardImageSpanHandler.OnImageSpanListener
    @Override
    public void notifyAddImageSpan(ImageSpan imgSpan, int position) {
        log.i("image uri: %s", imageUri);
        uriImageList.add(position, imageUri);
        imageAdapter.notifyItemChanged(position);
    }
    @Override
    public void notifyRemovedImageSpan(int position) {
        uriImageList.remove(position);
        imageAdapter.notifyItemRemoved(position);
    }

    //Implement BoardImageAdapter.OnBoardAttachImageListener
    @Override
    public void removeImage(int position) {
        spanHandler.removeImageSpan(position);
    }

    @Override
    public void onCheckedChanged(CompoundButton chkbox, boolean isChecked) {
        final int index = (int)chkbox.getTag();
        log.i("checkbox index: %s", index);
        if(isChecked) cbAutoFilter.add(chkbox.getText().toString());
        else cbAutoFilter.remove(chkbox.getText().toString());
    }



    private void createPostWriteMenu() {
        binding.toolbarBoardWrite.inflateMenu(R.menu.options_board_write);
        binding.toolbarBoardWrite.setOnMenuItemClickListener(item -> {
            uploadImageToStorage();
            return true;
        });
    }

    //Slide down the autofilter when it comes to AUTOCLUB
    private void animAutoFilter() {
        binding.scrollviewAutofilter.setVisibility(View.VISIBLE);
        float height = 0;
        TypedValue tv = new TypedValue();
        if(requireActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)){
            height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }

        ObjectAnimator animTab = ObjectAnimator.ofFloat(binding.scrollviewAutofilter, "translationY", height);
        animTab.setDuration(1000);
        animTab.start();
    }

    //Create the autofilter checkbox
    private void setAutoFilter(String json) throws JSONException {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(5);

        JSONArray jsonAuto = new JSONArray(json);
        if(TextUtils.isEmpty(json) || jsonAuto.isNull(0)) {
            throw new NullPointerException("no auto data");
        }

        CheckBox cbGeneral = new CheckBox(getContext());
        cbGeneral.setTag(0);
        cbGeneral.setText(getString(R.string.board_filter_chkbox_general));
        cbGeneral.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        //cbAutoOnly.setTextColor(Color.WHITE);
        cbGeneral.setChecked(true);
        isGeneralPost = true;
        cbGeneral.setOnCheckedChangeListener((chkbox, isChecked) -> isGeneralPost = isChecked);
        binding.layoutAutofilter.addView(cbGeneral, params);

        jsonAuto.remove(2); //exclue the auto type
        for(int i = 0; i < jsonAuto.length(); i++) { // Exclude the auto type.
            CheckBox cbType = new CheckBox(getContext());
            cbType.setTag(i);
            cbType.setOnCheckedChangeListener(this);
            cbType.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            //cb.setTextColor(Color.WHITE);
            if(jsonAuto.optString(i).equals("null")) {
                switch(i) {
                    case 1: cbType.setText(R.string.pref_auto_model);break;
                    case 2: cbType.setText(R.string.pref_engine_type);break;
                    case 3: cbType.setText(R.string.board_filter_year);break;
                }
                cbType.setEnabled(false);
            } else {
                // The automaker is required to be checked as far as jsonAuto has been set not to be
                // null. Other autofilter values depends on whether it is the locked mode
                // which retrieves its value from SharedPreferences.
                cbType.setText(jsonAuto.optString(i));
                if(i == 0) {
                    cbType.setChecked(true);
                    cbAutoFilter.add(jsonAuto.optString(0));
                    cbType.setEnabled(false);
                }
            }

            binding.layoutAutofilter.addView(cbType, params);
        }
    }

    private void selectImageMedia() {
        ((InputMethodManager)(requireActivity().getSystemService(INPUT_METHOD_SERVICE)))
                .hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        // Pop up the dialog as far as the num of attached pics are no more than the fixed size.
        if(uriImageList.size() >= Constants.MAX_IMAGE_NUMS) {
            String msg = String.format(getString(R.string.board_msg_image), Constants.MAX_IMAGE_NUMS);
            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
            return;
        }
        // Pop up the dialog to select which media to use bewteen the camera and gallery, the viewmodel
        // of which passes the value to getImageChooser().
        DialogFragment dialog = new ImageChooserFragment();
        dialog.show(getChildFragmentManager(), "ImageMediaChooser");
        /*
         * This works for both, inserting a text at the current position and replacing
         * any text which is selected by the user. The Math.max() is necessary in the first
         * and second line because, if there is no selection or focus in the edittext,
         * getSelectionStart() and getSelectionEnd() will both return -1. On the other hand,
         * The Math.min() and Math.max() in the third line is necessary because the user
         * could have selected the text backwards and thus start would have a higher value
         * than the end value which is not allowed for Editable.replace().
         */
        //int start = Math.max(etPostBody.getSelectionStart(), 0);
        //int end = Math.max(etPostBody.getSelectionEnd(), 0);
        int start = binding.etPostContent.getSelectionStart();
        int end = binding.etPostContent.getSelectionEnd();
        binding.etPostContent.getText().replace(start, end, "\n");
    }

    // Invoked in the parent activity to set an image uri which has received from the image media.
    public void addImageThumbnail(Uri uri) {
        log.i("attached Image Uri: %s", uri);
        this.imageUri = uri;
        ApplyImageResourceUtil.applyGlideToImageSpan(getContext(), imageUri, spanHandler);
    }

    //If any image is attached, compress images and upload them to Storage using a worker thread.
    private void uploadImageToStorage() {
        ((InputMethodManager)requireActivity().getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        if(!doEmptyCheck()) return;

        if(uriImageList.size() == 0) uploadPostToFirestore(); //no image attached
        else {
            //binding.pbWriteContainer.setVisibility(View.VISIBLE);
            //binding.tvPbMessage.setText("Image beging compressed...");
            for(int i = 0; i < uriImageList.size(); i++) {
                final Uri uri = uriImageList.get(i);
                bitmapTask = ThreadManager2.uploadBitmapTask(getContext(), uri, i, imgViewModel);
            }
        }
    }

    private void uploadPostToFirestore() {
        //binding.tvPbMessage.setText("Image Uploading...");
        Map<String, Object> post = new HashMap<>();
        post.put("user_id", userId);
        post.put("user_name", userName);
        post.put("post_title", binding.etPostTitle.getText().toString());
        post.put("timestamp", FieldValue.serverTimestamp());
        //post.put("timestamp", System.currentTimeMillis());
        post.put("cnt_comment", 0);
        post.put("cnt_compathy", 0);
        post.put("cnt_view", 0);
        post.put("post_content", binding.etPostContent.getText().toString());
        // If the post has any images attached.
        if(sparseUriArray.size() > 0) {
            List<String> images = new ArrayList<>(sparseUriArray.size());
            for(int i = 0; i < sparseUriArray.size(); i++)
                images.add(sparseUriArray.keyAt(i), sparseUriArray.valueAt(i).toString());
            post.put("post_images",  images);
        }

        post.put("post_autoclub", page == AUTOCLUB);
        if(page == AUTOCLUB) {
            Map<String, Boolean> filters = new HashMap<>();
            for(String field : cbAutoFilter) filters.put(field, true);
            post.put("auto_filter", filters);
            post.put("post_general", isGeneralPost);
        } else post.put("post_general", true);


        final DocumentReference docRef = mDB.collection("users").document(userId);
        mDB.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot doc = transaction.get(docRef);
            if(doc.exists()) {
                post.put("user_name", doc.getString("user_name"));
                post.put("user_pic", doc.getString("user_pic"));

                mDB.collection("user_post").add(post).addOnSuccessListener(postRef -> {
                    fragmentModel.getNewPosting().setValue(postRef);
                    dismiss();
                }).addOnFailureListener(Throwable::printStackTrace);
            }

            return null;
        });


        // When uploading completes, the result is sent to BoardPagerFragment and the  notifes
        // BoardPagerFragment of a new posting. At the same time, the fragment dismisses.
        //postTask = ThreadManager2.uploadPostTask(getContext(), post, fragmentModel);
        //dismiss();
    }

    private boolean doEmptyCheck() {
        // If either userId or userName is empty, throw NullPointerException and retrn false;
        try {
            if(TextUtils.isEmpty(userId)||TextUtils.isEmpty(userName)) throw new NullPointerException();
        } catch(NullPointerException e) {
            e.printStackTrace();
            return false;
        }

        if(TextUtils.isEmpty(binding.etPostTitle.getText())) {
            Snackbar.make(binding.getRoot(), getString(R.string.board_msg_no_title), Snackbar.LENGTH_SHORT).show();
            return false;
        } else if(TextUtils.isEmpty(binding.etPostContent.getText())){
            Snackbar.make(binding.getRoot(), getString(R.string.board_msg_no_content), Snackbar.LENGTH_SHORT).show();
            return false;
        } else return true;
    }



}
