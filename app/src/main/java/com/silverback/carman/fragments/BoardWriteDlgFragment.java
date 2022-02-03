package com.silverback.carman.fragments;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static com.silverback.carman.BoardActivity.AUTOCLUB;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.DialogInterface;
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
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman.BoardActivity;
import com.silverback.carman.R;
import com.silverback.carman.adapters.BoardImageAdapter;
import com.silverback.carman.databinding.FragmentBoardWriteTempBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.BoardImageSpanHandler;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.ImageViewModel;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class BoardWriteDlgFragment extends DialogFragment implements
        BoardImageSpanHandler.OnImageSpanListener,
        BoardImageAdapter.OnBoardAttachImageListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardWriteDlgFragment.class);

    private FragmentBoardWriteTempBinding binding;
    private BoardImageAdapter imageAdapter;
    private BoardImageSpanHandler spanHandler;
    private FragmentSharedModel fragmentModel;
    private ImageViewModel imgViewModel;

    private List<Uri> uriImageList;
    private SparseArray<Uri> sparseUriArray;

    private Uri imageUri;
    private String userId;
    private String userName;
    private String autofilter;
    private int page;

    public BoardWriteDlgFragment() {
        // empty constructor which might be referenced by FragmentFactory
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

        uriImageList = new ArrayList<>();
        sparseUriArray = new SparseArray<>();
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        imgViewModel = new ViewModelProvider(requireActivity()).get(ImageViewModel.class);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

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
            autofilter = requireArguments().getString("autofilter");
            try {setAutoFilter(autofilter); }
            catch (JSONException e) {e.printStackTrace();}
        } else binding.scrollviewAutofilter.setVisibility(View.GONE);

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
        fragmentModel.getImageChooser().observe(getViewLifecycleOwner(), chooser -> {
            ((BoardActivity)requireActivity()).getImageFromChooser(chooser);
        });

        imgViewModel.getDownloadBitmapUri().observe(getViewLifecycleOwner(), sparseArray -> {
            sparseUriArray.put(sparseArray.keyAt(0), sparseArray.valueAt(0));
            if(uriImageList.size() == sparseUriArray.size()) uploadPostToFirestore();
        });


    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        log.i("onDismiss");
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

    public void addImageThumbnail(Uri uri) {
        log.i("attached Image Uri: %s", uri);
        this.imageUri = uri;
        ApplyImageResourceUtil.applyGlideToImageSpan(getContext(), imageUri, spanHandler);
    }

    private void createPostWriteMenu() {
        binding.toolbarBoardWrite.inflateMenu(R.menu.options_board_write);
        binding.toolbarBoardWrite.setOnMenuItemClickListener(item -> {
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

        CheckBox cbAutoOnly = new CheckBox(getContext());
        cbAutoOnly.setText(getString(R.string.board_filter_chkbox_general));
        cbAutoOnly.setTextColor(Color.WHITE);
        cbAutoOnly.setChecked(true);
        binding.layoutAutofilter.addView(cbAutoOnly, params);

        for(int i = 0; i < jsonAuto.length() - 1; i++) { // Exclude the auto type.
            CheckBox cb = new CheckBox(getContext());
            cb.setTag(i);
            cb.setTextColor(Color.WHITE);
            if(jsonAuto.optString(i).equals("null")) {
                switch(i) {
                    case 1: cb.setText(R.string.pref_auto_model);break;
                    case 2: cb.setText(R.string.pref_engine_type);break;
                    case 3: cb.setText(R.string.board_filter_year);break;
                }
                cb.setEnabled(false);
            } else {
                // The automaker is required to be checked as far as jsonAuto has been set not to be
                // null. Other autofilter values depends on whether it is the locked mode
                // which retrieves its value from SharedPreferences.
                cb.setText(jsonAuto.optString(i));
                if(i == 0) {
                    cb.setChecked(true);
                    cb.setEnabled(true);
                }
            }
            binding.layoutAutofilter.addView(cb, params);
            //chkboxList.add(cb);
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
        DialogFragment dialog = new BoardChooserDlgFragment();
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

    private void uploadPostToFirestore(){

    }



}
