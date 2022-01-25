package com.silverback.carman.fragments;


import static android.content.Context.INPUT_METHOD_SERVICE;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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
import com.silverback.carman.threads.ThreadManager;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.threads.UploadBitmapTask;
import com.silverback.carman.threads.UploadPostTask;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.BoardImageSpanHandler;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.ImageViewModel;

import java.io.IOException;
import java.io.InputStream;
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
public class BoardWriteFragment extends DialogFragment implements
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
    private Uri imgUri;
    private List<Uri> imgUriList;
    //private ArrayList<String> autofilter;
    private SparseArray<Uri> downloadImages;
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
        imgUriList = new ArrayList<>();
        downloadImages = new SparseArray<>();
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
        imageAdapter = new BoardImageAdapter(getContext(), imgUriList, this);
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
        if(item.getItemId() == R.id.action_upload_post) prepareAttachedImages();
        return true;
    }

    // When fragments finish their lifecycle, the instance of a viewmodel exists in ViewModelStore,
    // until destruction of the parent activity. ViewModelStore has clear() method but it clears
    // all viewmodels in it.
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // BoardChooserDlgFragment notifies the viewmodel of which image media is selected.
        fragmentModel.getImageChooser().observe(getViewLifecycleOwner(), chooser -> {
            ((BoardActivity)requireActivity()).getImageFromChooser(chooser);
        });

        // The imgUri received as a result of startActivityForResult() is applied to applyGlideToImageSpan().
        // This method translates an image to an appropriate extent for fitting the imagespan.
        /*
        imgViewModel.getGlideBitmapTarget().observe(getViewLifecycleOwner(), bitmap -> {
            log.i("Bitmap received: %s, %s", uriImgList.size(), bitmap);
            ImageSpan imgSpan = new ImageSpan(requireContext(), bitmap);
            // Manage the image spans using BoardImageSpanHandler helper class.
            //this.imageSpan = imgSpan;
            //spanHandler.setImageSpanToPost(imgSpan);
            spanHandler.setImageSpan(imgSpan);
        });
         */

        // UploadBitmapTask compresses and uploads an image to Storage, the uri of which is notified
        // the viewmodel.
        imgViewModel.getDownloadBitmapUri().observe(getViewLifecycleOwner(), sparseArray -> {
            downloadImages.put(sparseArray.keyAt(0), sparseArray.valueAt(0));
            if(imgUriList.size() == downloadImages.size()) uploadPostToFirestore();
        });

        // UploadPostTask uploads the post to Firestore, the document id of which is notified the
        // viewmodel.
        /*
        fragmentModel.getNewPosting().observe(getViewLifecycleOwner(), docId -> {
            binding.progbarBoardWrite.setVisibility(View.GONE);
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
        imgUriList.add(position, imgUri);
        imageAdapter.notifyItemChanged(position);
    }

    @Override
    public void notifyRemovedImageSpan(int position) {
        imgUriList.remove(position);
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
    public void setAttachedImage(Uri imgUri) {
        this.imgUri = imgUri;
        ApplyImageResourceUtil.applyGlideToImageSpan(getContext(), imgUri, spanHandler);
    }

    // Event Handler for the image attachment button, which calls DialogFragment to choose a image
    // media.
    private void selectImageMedia() {
        ((InputMethodManager)(requireActivity().getSystemService(INPUT_METHOD_SERVICE)))
                .hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        // Pop up the dialog as far as the num of attached pics are no more than the fixed size.
        if(imgUriList.size() >= Constants.MAX_IMAGE_NUMS) {
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

    // Invoked when the upload menu in the toolbar is pressed.
    public void prepareAttachedImages() {
        ((InputMethodManager)requireActivity().getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);

        if(!doEmptyCheck()) return;
        // getChildFragmentManager().beginTransaction().add(android.R.id.content, pbFragment).commit();
        // No image posting makes an immediate uploading but postings with images attached
        // should take the uploading process that images starts uploading first and image
        // URIs would be sent back if uploading images is successful,then uploading gets
        // started with image URIs, adding it to a document of Firestore.
        if(imgUriList.size() == 0) uploadPostToFirestore();
        else {
            // Image Attachment button that starts UploadBitmapTask as many as the number of
            // images. In case the task starts with UploadBitmapTask multi-threading which
            // runs in ThreadManager, thread contentions may occur, replacing one with the
            // other which makes last image cover the other ones.
            // A download url from Storage each time when an attached image is successfully
            // downsized and scaled down, then uploaded to Storage is transferred via
            // ImageViewModel.getDownloadBitmapUri() as a live data of SparseArray.
            //binding.progbarBoardWrite.setVisibility(View.VISIBLE);
            for(int i = 0; i < imgUriList.size(); i++) {
                final Uri uri = imgUriList.get(i);
                bitmapTask = ThreadManager2.startBitmapUploadTask(getContext(), uri, i, imgViewModel);
            }

        }

        // As UploadBitmapTask has completed to optimize an attched image and upload it to Stroage,
        // the result is notified as SparseArray which indicates the position and uriString of image.
        /*
        imgViewModel.getDownloadBitmapUri().observe(requireActivity(), sparseArray -> {
            // Check if the number of attached images equals to the number of uris that are down
            // loaded from Storage.
            downloadImages.put(sparseArray.keyAt(0), sparseArray.valueAt(0));
            if(imgUriList.size() == downloadImages.size()) {
                // On completing optimization of attached images, start uploading a post.
                //pbFragment.dismiss();
                binding.progbarBoardWrite.setVisibility(View.GONE);
                uploadPostToFirestore();
            }

        });
         */
    }

    private void uploadPostToFirestore() {
        //if(!doEmptyCheck()) return;
        // UserId should be passed from the parent activity. If not, the process should end here.
        //if(TextUtils.isEmpty(userId)) return;

        // Cast SparseArray containing download urls from Storage to String array
        // Something wrong around here because a posting item contains an image despite no image
        // attached.
        List<String> downloadUriList = null;
        if(downloadImages.size() > 0) {
            downloadUriList = new ArrayList<>(downloadImages.size());
            for (int i = 0; i < downloadImages.size(); i++) {
                downloadUriList.add(downloadImages.keyAt(i), downloadImages.valueAt(i).toString());
            }
        }

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
        if(downloadImages.size() > 0) post.put("post_images",  downloadUriList);
        post.put("post_autoclub", tabPage == Constants.BOARD_AUTOCLUB);
        // To show or hide a post of the autoclub depends on the value of isGeneralPost. The default
        // value is set to true such that any post, no matter what is autoclub or general post,
        // it would be shown in every board. However, as long as an autoclub post set isGeneral value
        // to false, it would not be shown in the general board; only in the autoclub.
        // On the other hand, the autofilter values as Arrays should be turned into Map with autofilter
        // as key and timestamp as value, which avoid creating composite index
        boolean isGeneralPost;
        if(tabPage == Constants.BOARD_AUTOCLUB) {
            ArrayList<String> autofilter = ((BoardActivity)requireActivity()).getAutoFilterValues();
            isGeneralPost = ((BoardActivity)requireActivity()).checkGeneralPost();
            Map<String, Boolean> filters = new HashMap<>();
            for(String field : autofilter) filters.put(field, true);
            post.put("auto_filter", filters);
        } else isGeneralPost = true;
        post.put("post_general", isGeneralPost);

        // When uploading completes, the result is sent to BoardPagerFragment and the  notifes
        // BoardPagerFragment of a new posting. At the same time, the fragment dismisses.
        postTask = ThreadManager2.uploadPostTask(getContext(), post, fragmentModel);
        ((BoardActivity)requireActivity()).addViewPager();

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
