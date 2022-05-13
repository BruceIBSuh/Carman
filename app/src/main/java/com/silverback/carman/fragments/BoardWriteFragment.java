package com.silverback.carman.fragments;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.silverback.carman.BoardActivity.AUTOCLUB;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.Transaction;
import com.silverback.carman.BoardActivity;
import com.silverback.carman.R;
import com.silverback.carman.adapters.BoardImageAdapter;
import com.silverback.carman.databinding.FragmentBoardWriteTempBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.threads.UploadBitmapTask;
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

public class BoardWriteFragment extends DialogFragment implements
        BoardImageSpanHandler.OnImageSpanListener, CompoundButton.OnCheckedChangeListener,
        BoardImageAdapter.OnBoardAttachImageListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardWriteFragment.class);

    private FirebaseFirestore mDB;
    private FragmentBoardWriteTempBinding binding;
    private BoardImageAdapter imageAdapter;
    private BoardImageSpanHandler spanHandler;
    private UploadBitmapTask bitmapTask;

    private FragmentSharedModel fragmentModel;
    private ImageViewModel imgViewModel;

    private List<Uri> uriImageList;
    private SparseArray<Uri> sparseUriArray;
    private List<String> cbAutoFilter;
    private JSONArray jsonAutoArray;
    private Uri imageUri;
    private String userId;
    private String userName;
    private String autofilter;
    private int marginPlus;
    private int page;
    private boolean isGeneral;

    public BoardWriteFragment() {
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
            if(page == AUTOCLUB) autofilter = getArguments().getString("autofilter");
        }

        mDB = FirebaseFirestore.getInstance();
        uriImageList = new ArrayList<>();
        sparseUriArray = new SparseArray<>();
        cbAutoFilter = new ArrayList<>();
        imageAdapter = new BoardImageAdapter(getContext(), uriImageList, this);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentBoardWriteTempBinding.inflate(inflater);
        binding.toolbarBoardWrite.setTitle(getString(R.string.board_write_toolbar_title));
        binding.toolbarBoardWrite.setNavigationOnClickListener(view -> dismiss());
        binding.tvWriteGuide.setText(getString(R.string.board_posting_guide));
        createPostWriteMenu();

        // RecyclerView to display attached thumbnail images on the bottom
        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        linearLayout.setOrientation(LinearLayoutManager.HORIZONTAL);
        binding.recyclerImages.setLayoutManager(linearLayout);
        //recyclerImageView.setHasFixedSize(true);

        spanHandler = new BoardImageSpanHandler(binding.etPostContent, this);
        binding.btnImage.setOnClickListener(view -> {
            binding.recyclerImages.setAdapter(imageAdapter);
            selectImageMedia();
        });

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
    public void onResume() {
        super.onResume();
        // The autoclub page needs extra space which is occupied by the autofilter, the height of
        // which should be measured after the
        if(page == AUTOCLUB) {
            setWriteAutofilter(autofilter);
            binding.scrollAutofilter.setVisibility(View.VISIBLE);
            ViewTreeObserver vto = binding.scrollAutofilter.getViewTreeObserver();
            vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    marginPlus = binding.scrollAutofilter.getMeasuredHeight();
                    binding.scrollAutofilter.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    animAutoFilter();
                }
            });

        } else binding.scrollAutofilter.setVisibility(View.GONE);

    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        imgViewModel = new ViewModelProvider(requireActivity()).get(ImageViewModel.class);

        fragmentModel.getImageChooser().observe(getViewLifecycleOwner(), chooser -> {
            ((BoardActivity)requireActivity()).chooseImageMedia(chooser, binding.getRoot());
        });

        imgViewModel.getDownloadBitmapUri().observe(getViewLifecycleOwner(), sparseArray -> {
            sparseUriArray.put(sparseArray.keyAt(0), sparseArray.valueAt(0));
            if(uriImageList.size() == sparseUriArray.size()) uploadPostToFirestore();
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if(bitmapTask != null) bitmapTask = null;
    }

    @Override
    public void onDestroyView() {
        log.i("onDestroyView");
        //requireActivity().getViewModelStore().clear();
        super.onDestroyView();
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
        float height = 0;
        TypedValue tv = new TypedValue();
        if(requireActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)){
            height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
        }

        ObjectAnimator filter = ObjectAnimator.ofFloat(binding.scrollAutofilter, "translationY", height);
        filter.setDuration(1000);
        filter.start();

        CollapsingToolbarLayout.LayoutParams params = new CollapsingToolbarLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = (int)height + marginPlus;
        binding.tvWriteGuide.setLayoutParams(params);

        /*
        Animation animMargin = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                params.setMargins(0, (int) (newMargin * interpolatedTime), 0, 0);
                binding.tvWriteGuide.setLayoutParams(params);
            }
        };
        animMargin.setDuration(1000);
        animMargin.start();

         */
    }

    //Create the autofilter checkbox
    private void setWriteAutofilter(String json) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginStart(10);

        try { jsonAutoArray = new JSONArray(json);}
        catch(JSONException e) {e.printStackTrace();}
        if(TextUtils.isEmpty(json) || jsonAutoArray.isNull(0)) return;

        CheckBox cbGeneral = new CheckBox(getContext());
        cbGeneral.setTag(0);
        cbGeneral.setText(getString(R.string.board_filter_chkbox_general));
        cbGeneral.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        cbGeneral.setTextColor(Color.WHITE);
        cbGeneral.setChecked(false);
        cbGeneral.setOnCheckedChangeListener((chkbox, isChecked) -> isGeneral = isChecked);
        binding.layoutAutofilter.addView(cbGeneral, params);

        jsonAutoArray.remove(2); //exclue the auto type
        for(int i = 0; i < jsonAutoArray.length(); i++) { // Exclude the auto type.
            CheckBox cbType = new CheckBox(getContext());
            cbType.setTag(i);
            cbType.setOnCheckedChangeListener(this);
            cbType.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            cbType.setTextColor(Color.WHITE);
            //if(jsonAutoArray.optString(i).equals("null")) { // conditions should be reconsidered.
            if(jsonAutoArray.isNull(i)) {
                switch(i) {
                    case 1: cbType.setText(R.string.pref_auto_model);break;
                    case 2: cbType.setText(R.string.pref_engine_type);break;
                    case 3: cbType.setText(R.string.board_filter_year);break;
                }
                cbType.setEnabled(false);
            } else {
                cbType.setText(jsonAutoArray.optString(i));
                if(i == 0) {
                    cbType.setChecked(true); // automatically invoke the listener.
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
        post.put("cnt_comment", 0);
        post.put("cnt_compathy", 0);
        post.put("cnt_view", 0);
        post.put("isAutoclub", page == AUTOCLUB);
        post.put("post_content", binding.etPostContent.getText().toString());
        // If the post has any images attached.
        if(sparseUriArray.size() > 0) {
            List<String> images = new ArrayList<>(sparseUriArray.size());
            for(int i = 0; i < sparseUriArray.size(); i++) {
                images.add(sparseUriArray.keyAt(i), sparseUriArray.valueAt(i).toString());
            }
            post.put("post_images",  images);
        }

        if(page == AUTOCLUB) {
            List<String> filterList = new ArrayList<>(cbAutoFilter);
            post.put("auto_filter", filterList);
            post.put("isGeneral", isGeneral);
        } else post.put("isGeneral", true);


        DocumentReference docRef = mDB.collection("users").document(userId);
        mDB.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot doc = transaction.get(docRef);
            if(doc.exists()) {
                post.put("user_pic", doc.getString("user_pic"));
                mDB.collection("user_post").add(post).addOnSuccessListener(postRef -> {
                    fragmentModel.getNewPosting().setValue(postRef);
                    dismiss();
                    /*
                    postRef.get().addOnSuccessListener(snapshot -> {
                        log.i("add a new post");
                        fragmentModel.getNewPosting().setValue(snapshot);
                        dismiss();
                    });

                     */

                }).addOnFailureListener(Throwable::printStackTrace);
            }
            return null;
        }).addOnFailureListener(e -> {log.e("transaction failed: %s", e);});

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

    private static class UserNames {
        @PropertyName("user_names")
        private List<String> userNames;

        public UserNames () {}
        public UserNames(List<String> userNames) {
            this.userNames = userNames;
        }
        @PropertyName("user_names")
        public List<String> getUserNames() {
            return userNames;
        }
        @PropertyName("usre_names")
        public void setUserNames(List<String> userNames) {
            this.userNames = userNames;
        }
    }

}
