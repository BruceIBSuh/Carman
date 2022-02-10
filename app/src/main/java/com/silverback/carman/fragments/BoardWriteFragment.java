package com.silverback.carman.fragments;


import static android.content.Context.INPUT_METHOD_SERVICE;

import static com.silverback.carman.BoardActivity.AUTOCLUB;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FieldValue;
import com.silverback.carman.BoardActivity;
import com.silverback.carman.R;
import com.silverback.carman.adapters.BoardImageAdapter;
import com.silverback.carman.databinding.FragmentBoardWriteBinding;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple {@link Fragment} subclass.
 * This Fragment is to wrtie a post, attach images, and upload the post to FireStore. Images are
 * uploaded to Storage first, which returns the image url and upload a post with the urls.
 *
 *
 */
public class BoardWriteFragment extends Fragment implements
        BoardImageSpanHandler.OnImageSpanListener,
        BoardImageAdapter.OnBoardAttachImageListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardWriteFragment.class);

    // Objects
    private FragmentBoardWriteBinding binding;
    private BoardImageSpanHandler spanHandler;
    private FragmentSharedModel fragmentModel;
    private ImageViewModel imgViewModel;
    private BoardImageAdapter imageAdapter;
    private UploadBitmapTask bitmapTask;
    private UploadPostTask postTask;
    // Fields
    private int tabPage;
    private String userId, userName;
    private Uri mImageUri;
    private List<Uri> uriImageList;
    //private ArrayList<String> autofilter;
    private SparseArray<Uri> sparseUriArray;
    //private boolean isGeneralPost;
    //private boolean isNetworkConnected;

    // Constructor
    public BoardWriteFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            userId = getArguments().getString("userId");
            userName = getArguments().getString("userName");
            tabPage = getArguments().getInt("tabPage");
        }
        // Required for Fragment
        setHasOptionsMenu(true);
        //isNetworkConnected = BaseActivity.notifyNetworkConnected(getContext());
        //ApplyImageResourceUtil applyImageResourceUtil = new ApplyImageResourceUtil(getContext());
        uriImageList = new ArrayList<>();
        sparseUriArray = new SparseArray<>();
        // ViewModels
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        imgViewModel = new ViewModelProvider(requireActivity()).get(ImageViewModel.class);
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBoardWriteBinding.inflate(inflater);

        // RecyclerView for displaying attached posting thumbnail images on the bottom side.
        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        linearLayout.setOrientation(LinearLayoutManager.HORIZONTAL);
        binding.recyclerImages.setLayoutManager(linearLayout);
        //recyclerImageView.setHasFixedSize(true);
        imageAdapter = new BoardImageAdapter(getContext(), uriImageList, this);
        binding.recyclerImages.setAdapter(imageAdapter);

        /* To scroll the edittext inside (nested)scrollview.
        binding.etBoardContent.setOnTouchListener((view, event) -> {
            if(view.hasFocus()) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                if((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_SCROLL) {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
            }
            return false;
        });
        */

        spanHandler = new BoardImageSpanHandler(binding.etBoardContent, this);
        // The event handler for the Image Attach button to select an image media(gallery or camera)
        binding.btnAttachImage.setOnClickListener(btn -> selectImageMedia());
        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.getItem(0).setVisible(false);
        menu.getItem(1).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_upload_post) {
            //binding.progbarBoardWrite.setVisibility(View.VISIBLE);
            binding.pbWriteContainer.setVisibility(View.VISIBLE);
            uploadImageToStorage();
            return true;
        } else return super.onOptionsItemSelected(item);

    }

    // When fragments finish their lifecycle, the instance of a viewmodel exists in ViewModelStore,
    // until destruction of the parent activity. ViewModelStore has clear() method but it clears
    // all viewmodels in it.
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // BoardChooserDlgFragment notifies the viewmodel of which image media is selected.
        fragmentModel.getImageChooser().observe(getViewLifecycleOwner(), chooser -> {
            log.i("chooser in BoardWrite");
            ((BoardActivity)requireActivity()).selectImageMedia(chooser);
        });

        // UploadBitmapTask compresses and uploads an image to Storage, the uri of which is notified
        // the viewmodel.
        imgViewModel.getDownloadBitmapUri().observe(getViewLifecycleOwner(), sparseArray -> {
            sparseUriArray.put(sparseArray.keyAt(0), sparseArray.valueAt(0));
            if(uriImageList.size() == sparseUriArray.size()) uploadPostToFirestore();
        });

        // UploadPostTask uploads the post to Firestore, the document id of which is notified the
        // viewmodel
        /*
        fragmentModel.getNewPosting().observe(getViewLifecycleOwner(), docId -> {
            log.i("new post notified: %s", docId);
            binding.pbWriteContainer.setVisibility(View.GONE);
            ((BoardActivity)requireActivity()).addViewPager();
        });

         */
    }

    @Override
    public void onPause() {
        super.onPause();
        if(bitmapTask != null) bitmapTask = null;
        if(postTask != null) postTask = null;
    }

    // Implement BoardImageSpanHandler.OnImageSpanListener which notifies that an new ImageSpan
    // has been added or removed. The position param is fetched from the markup using the regular
    // expression.
    @Override
    public void notifyAddImageSpan(ImageSpan imgSpan, int position) {
        uriImageList.add(position, mImageUri);
        imageAdapter.notifyItemChanged(position);
    }

    @Override
    public void notifyRemovedImageSpan(int position) {
        uriImageList.remove(position);
        imageAdapter.notifyItemRemoved(position);
    }

    // Implement BoardImageAdapter.OnBoardAttachImageListener when an image is removed from the
    // recyclerview.
    @Override
    public void removeImage(int position) {
        spanHandler.removeImageSpan(position);
    }

    // Invokded by the activity callback defined in the parent actvitiy. OnImageSpanListener defined
    // in BoardImageSpanHandler implements notifyAddImageSpan(), notifyRemoveImageSpan() when adding
    // or removing a thumbnail image.
    public void addImageThumbnail(Uri imgUri) {
        this.mImageUri = imgUri;
        ApplyImageResourceUtil.applyGlideToImageSpan(getContext(), imgUri, spanHandler);
    }

    // Event Handler for the image attachment button, which calls DialogFragment to choose a image
    // media.
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
        int start = binding.etBoardContent.getSelectionStart();
        int end = binding.etBoardContent.getSelectionEnd();
        binding.etBoardContent.getText().replace(start, end, "\n");
    }



    /*
     * UPLOAD PROCESS
     * The process of uploading the post consists of three steps.
     * First, upload attached images to Firebase Storage, if any.
     * Second, check whether the attached images safely completes uploading.
     * Third, start to upload the post to FireStore, then on notifying completion, dismiss.
     */
    public void uploadImageToStorage() {
        ((InputMethodManager)requireActivity().getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        if(!doEmptyCheck()) return;


        if(uriImageList.size() == 0) uploadPostToFirestore(); //no image attached
        else {
            binding.pbWriteContainer.setVisibility(View.VISIBLE);
            binding.tvPbMessage.setText("Image beging compressed...");
            for(int i = 0; i < uriImageList.size(); i++) {
                final Uri uri = uriImageList.get(i);
                bitmapTask = ThreadManager2.uploadBitmapTask(getContext(), uri, i, imgViewModel);
            }
        }
    }


    private void uploadPostToFirestore() {
        //if(!doEmptyCheck()) return;
        // UserId should be passed from the parent activity. If not, the process should end here.
        //if(TextUtils.isEmpty(userId)) return;
        binding.tvPbMessage.setText("Image Uploading...");
        Map<String, Object> post = new HashMap<>();
        post.put("user_id", userId);
        post.put("user_name", userName);
        post.put("post_title", binding.etBoardWriteTitle.getText().toString());
        post.put("timestamp", FieldValue.serverTimestamp());
        //post.put("timestamp", System.currentTimeMillis());
        post.put("cnt_comment", 0);
        post.put("cnt_compathy", 0);
        post.put("cnt_view", 0);
        post.put("post_content", binding.etBoardContent.getText().toString());
        // If the post has any images attached.
        if(sparseUriArray.size() > 0) {
            List<String> images = new ArrayList<>(sparseUriArray.size());
            for(int i = 0; i < sparseUriArray.size(); i++)
                images.add(sparseUriArray.keyAt(i), sparseUriArray.valueAt(i).toString());
            post.put("post_images",  images);
        }
        post.put("post_autoclub", tabPage == AUTOCLUB);

        if(tabPage == AUTOCLUB) {
            ArrayList<String> autofilter = ((BoardActivity)requireActivity()).getAutoFilterValues();
            Map<String, Boolean> filters = new HashMap<>();
            for(String field : autofilter) filters.put(field, true);
            //boolean isGeneralPost = ((BoardActivity)requireActivity()).checkGeneralPost();

            post.put("auto_filter", filters);
            //post.put("post_general", isGeneralPost);

        } else post.put("post_general", true);


        // When uploading completes, the result is sent to BoardPagerFragment and the  notifes
        // BoardPagerFragment of a new posting. At the same time, the fragment dismisses.
        postTask = ThreadManager2.uploadPostTask(getContext(), post, fragmentModel);
        //binding.progbarBoardWrite.setVisibility(View.GONE);
        //((BoardActivity)requireActivity()).addViewPager();


    }

    private boolean doEmptyCheck() {
        // If either userId or userName is empty, throw NullPointerException and retrn false;
        try {
            if(TextUtils.isEmpty(userId)||TextUtils.isEmpty(userName)) throw new NullPointerException();
        } catch(NullPointerException e) {
            e.printStackTrace();
            return false;
        }

        if(TextUtils.isEmpty(binding.etBoardWriteTitle.getText())) {
            Snackbar.make(binding.getRoot(), getString(R.string.board_msg_no_title), Snackbar.LENGTH_SHORT).show();
            return false;
        } else if(TextUtils.isEmpty(binding.etBoardContent.getText())){
            Snackbar.make(binding.getRoot(), getString(R.string.board_msg_no_content), Snackbar.LENGTH_SHORT).show();
            return false;
        } else return true;
    }
}
