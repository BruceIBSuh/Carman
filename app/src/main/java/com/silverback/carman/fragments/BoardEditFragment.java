package com.silverback.carman.fragments;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.silverback.carman.BoardActivity;
import com.silverback.carman.R;
import com.silverback.carman.adapters.BoardImageAdapter;
import com.silverback.carman.databinding.FragmentBoardEditBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.threads.UploadBitmapTask;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.BoardImageSpanHandler;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.ImageViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BoardEditFragment extends DialogFragment implements
        BoardImageSpanHandler.OnImageSpanListener,
        BoardImageAdapter.OnBoardAttachImageListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardEditFragment.class);
    private static final String REGEX_MARKUP = "\\[image_\\d]\\n";

    // Objects
    private FragmentBoardEditBinding binding;

    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private UploadBitmapTask bitmapTask;

    private BoardImageAdapter imgAdapter;
    private BoardImageSpanHandler spanHandler;
    private SparseArray<ImageSpan> sparseSpanArray;

    private FragmentSharedModel sharedModel;
    private ImageViewModel imgModel;

    private Uri mImageUri;
    private ArrayList<String> uriStringList;
    private ArrayList<Uri> uriEditList;

    // Fields
    private String documentId;
    private String title, content;
    private int position;
    private int numImgAdded;

    // Default Constructor
    public BoardEditFragment() {
        //Empty constructor which might be referenced by FragmentFactory
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if(getArguments() != null) {
            documentId = getArguments().getString("documentId");
            position = getArguments().getInt("position");
            title = getArguments().getString("postTitle");
            content = getArguments().getString("postContent");
            uriStringList = getArguments().getStringArrayList("uriImgList");
        }

        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        //imgUtil = new ApplyImageResourceUtil(getContext());
        imgModel = new ViewModelProvider(requireActivity()).get(ImageViewModel.class);
        sharedModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);

        sparseSpanArray = new SparseArray<>();
        //sparseImageArray = new SparseArray<>();
        uriEditList = new ArrayList<>();


        // If the post contains any image, the http url should be typecast to uri.
        if(uriStringList != null && uriStringList.size() > 0) {
            for(String uriString : uriStringList) uriEditList.add(Uri.parse(uriString));
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentBoardEditBinding.inflate(inflater);
        binding.toolbarBoardEdit.setTitle("POST EDITING");
        binding.toolbarBoardEdit.setNavigationOnClickListener(view -> dismiss());
        createEditMenu();

        binding.etBoardEditTitle.setText(title);
        binding.btnAttachImage.setOnClickListener(button -> selectImageMedia());

        // Horizontal recycleview for displaying attached images
        if(uriStringList != null && uriStringList.size() > 0) {
            LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
            linearLayout.setOrientation(LinearLayoutManager.HORIZONTAL);
            binding.editRecyclerImages.setLayoutManager(linearLayout);
            imgAdapter = new BoardImageAdapter(getContext(), uriEditList, this);
            binding.editRecyclerImages.setAdapter(imgAdapter);
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder(content);
        binding.etEditContent.setText(ssb);
        spanHandler = new BoardImageSpanHandler(binding.etEditContent, this);

        // If the post contains any image, find the markup in the content using Matcher.find() and
        // at the same time, images should be downsized by Glide and create imagespans only when it
        // has finished to do it. At this point, a sync issue may occur because Glide works on
        // the async basis. Thus, with while-loop index value made final, bitmaps from Glide
        // should be put into SparseArray. It seems that List.add(int, obj) does not work here.
        // Once the sparsearray completes to hold all imagespans, it should be converted to spanList
        // to pass to BoardImageSpanHander.setImageSpanList().
        if(uriEditList != null && uriEditList.size() > 0) setThumbnailImages();

        // Scroll the edittext inside (nested)scrollview.
        // warning message is caused by onPermClick not implemented. Unless the method is requried
        // to implement, the warning can be suppressed by "clickableVieweaccessibility".
        /*
        binding.etEditContent.setOnTouchListener((view, event) ->{
            if(binding.etEditContent.hasFocus()) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                if((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_SCROLL) {
                    view.getParent().requestDisallowInterceptTouchEvent(false);
                    return true;
                }
                switch(event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_SCROLL:
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        return true;
                }
            }
            return false;
        });
         */


        return binding.getRoot();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }
    /*
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.getItem(0).setVisible(false);
        menu.getItem(1).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_upload_post) {
            binding.pbContainer.setVisibility(View.VISIBLE);
            updateImageToStorage();//uploadPostUpdate();
        }
        return true;
    }

     */

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Notified of which media(camera or gallery) to select in ImageChooserFragment, according
        // to which startActivityForResult() is invoked by the parent activity and the result will be
        // notified to the activity and it is, in turn, sent back here by calling
        sharedModel.getImageChooser().observe(getViewLifecycleOwner(), chooser -> {
            ((BoardActivity)requireActivity()).selectImageMedia(chooser, binding.getRoot());
        });

        // As UploadBitmapTask has completed to optimize an attched image and upload it to Stroage,
        // the result is notified as SparseArray which indicates the position and uriString of image.
        imgModel.getDownloadBitmapUri().observe(getViewLifecycleOwner(), sparseArray -> {
            uriEditList.set(sparseArray.keyAt(0), sparseArray.valueAt(0));
            numImgAdded--;
            if(numImgAdded == 0) updatePost();
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if(bitmapTask != null) bitmapTask = null;
    }

    // Implement BoardImageAdapter.OnBoardAttachImageListener when clicking the deletion button
    // in the recyclerview thumbnail
    @Override
    public void removeImage(int position) {
        spanHandler.removeImageSpan(position);
    }

    // Implement BoardImageSpanHandler.OnImageSpanListener which notifies that an new ImageSpan
    // has been added or removed. The position param is fetched from the markup using the regular
    // expression.
    @Override
    public void notifyAddImageSpan(ImageSpan imgSpan, int position) {
        log.i("added image uri: %s", mImageUri);
        imgAdapter.notifyItemChanged(position);
        if(mImageUri != null) uriEditList.add(position, mImageUri);
    }

    // Implement BoardImageSpanHandler.OnImageSpanListener
    @Override
    public void notifyRemovedImageSpan(int position) {
        imgAdapter.notifyItemRemoved(position);
        if(uriEditList.get(position) != null) uriEditList.remove(position);
    }

    private void createEditMenu() {
        binding.toolbarBoardEdit.inflateMenu(R.menu.options_board_write);
        binding.toolbarBoardEdit.setOnMenuItemClickListener(item -> {
            updateImageToStorage();
            return true;
        });
    }

    // Initially, insert thumbnail images in the content with the urlImgList transferred from
    // BoardReadFragment
    private void setThumbnailImages() {
        Matcher m = Pattern.compile(REGEX_MARKUP).matcher(content);
        List<ImageSpan> spanList = new ArrayList<>();
        final float scale = getResources().getDisplayMetrics().density;
        int size = (int)(Constants.IMAGESPAN_THUMBNAIL_SIZE * scale + 0.5f);
        int index = 0;
        while(m.find()) {
            final int pos = index;
            final Uri uri = uriEditList.get(pos);
            Glide.with(this).asBitmap().placeholder(R.drawable.ic_image_holder)
                    .override(size)
                    .fitCenter()
                    .load(uri)
                    .into(new CustomTarget<Bitmap>(){
                        @Override
                        public void onResourceReady(
                                @NonNull Bitmap res, @Nullable Transition<? super Bitmap> transition) {
                            ImageSpan imgspan = new ImageSpan(requireContext(), res);
                            sparseSpanArray.put(pos, imgspan);
                            // No guarantee to get bitmaps sequentially because Glide handles
                            // images on an async basis. Thus, SparseArray<ImageSpan> should be
                            // used to keep the position of an image.
                            if(sparseSpanArray.size() == uriStringList.size()) {
                                for(int i = 0; i < sparseSpanArray.size(); i++)
                                    spanList.add(i, sparseSpanArray.get(i));
                                spanHandler.setImageSpanList(spanList);
                            }

                        }
                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {}
                    });
            index++;
        }
    }

    // The attach button event handler to call the imge media chooser.
    private void selectImageMedia() {
        ((InputMethodManager)(requireActivity().getSystemService(INPUT_METHOD_SERVICE)))
                .hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        // Pop up the dialog as far as the num of attached pics are no more than the fixed size.
        if(uriEditList.size() >= Constants.MAX_IMAGE_NUMS) {
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
        int start = binding.etEditContent.getSelectionStart();
        int end = binding.etEditContent.getSelectionEnd();
        binding.etEditContent.getText().replace(start, end, "\n");
    }

    // Add a thumbnail in the content, which is invoked by getAttachedImageUri(), ActivityResult
    // callback by ActivityResultContract.GetContent
    public void addImageThumbnail(Uri uri) {
        mImageUri = uri;
        ApplyImageResourceUtil.applyGlideToImageSpan(getContext(), uri, spanHandler);
    }

    // If any images is removed, delete them from Firebase Storage.
    public void updateImageToStorage() {
        // Hide the soft input if it is visible.
        ((InputMethodManager)requireActivity().getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        if(!doEmptyCheck()) return;
        binding.tvPbMessage.setText("Image being compressed and uploading...");

        //get removed images
        List<String> tempList = new ArrayList<>();
        for(Uri uri : uriEditList) tempList.add(uri.toString());
        uriStringList.removeAll(tempList);
        if(uriStringList != null && uriStringList.size() > 0) {
            for(String url : uriStringList) {
                storage.getReferenceFromUrl(url).delete();//.addOnSuccessListener(aVoid -> {});
            }
            uriStringList.clear();
        }

        numImgAdded = 0;
        for(int i = 0; i < uriEditList.size(); i++) {
            if(Objects.equals(uriEditList.get(i).getScheme(), "content")) {
                numImgAdded++;
                final Uri imgUri = uriEditList.get(i);
                bitmapTask = ThreadManager2.uploadBitmapTask(getContext(), imgUri, i, imgModel);
            }
        }

        if(bitmapTask == null) updatePost();
    }

    private void updatePost() {
        if (documentId == null || TextUtils.isEmpty(documentId)) return;

        binding.tvPbMessage.setText("Post is uploading...");

        //final DocumentReference docref = firestore.collection("board_general").document(docId);
        final DocumentReference docref = firestore.collection("user_post").document(documentId);
        Map<String, Object> updatePost = new HashMap<>();
        updatePost.put("post_title", binding.etBoardEditTitle.getText().toString());
        updatePost.put("post_content", binding.etEditContent.getText().toString());
        updatePost.put("timestamp", FieldValue.serverTimestamp());

        // Update the post_image field, if any.
        List<String> postImages = new ArrayList<>();
        if (uriEditList.size() > 0) {
            docref.update("post_images", FieldValue.delete());
            for (Uri uri : uriEditList) postImages.add(uri.toString());
            updatePost.put("post_images", postImages);
        }

        docref.update(updatePost).addOnSuccessListener(aVoid -> {
            binding.pbContainer.setVisibility(View.GONE);
            sharedModel.getEditedPosting().setValue(position);
            dismiss();
        }).addOnFailureListener(Exception::printStackTrace);
    }

    private boolean doEmptyCheck() {
        if(TextUtils.isEmpty(binding.etBoardEditTitle.getText())) {
            Snackbar.make(binding.getRoot(), getString(R.string.board_msg_no_title), Snackbar.LENGTH_SHORT).show();
            return false;
        } else if(TextUtils.isEmpty(binding.etEditContent.getText())){
            Snackbar.make(binding.getRoot(), getString(R.string.board_msg_no_content), Snackbar.LENGTH_SHORT).show();
            return false;
        } else return true;

    }
}

