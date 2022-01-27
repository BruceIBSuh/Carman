package com.silverback.carman.fragments;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
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
import com.silverback.carman.threads.UpdatePostTask;
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


public class BoardEditFragment extends Fragment implements
        BoardImageSpanHandler.OnImageSpanListener,
        BoardImageAdapter.OnBoardAttachImageListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardEditFragment.class);
    private static final String REGEX_MARKUP = "\\[image_\\d]\\n";

    // Objects
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    private Bundle bundle;
    private BoardImageAdapter imgAdapter;
    private BoardImageSpanHandler spanHandler;
    private ApplyImageResourceUtil imgUtil;
    private ImageViewModel imgModel;
    private FragmentSharedModel sharedModel;
    private Uri imgUri;
    private List<String> imgUriStringList; // Url(http://) transferred from BoardReadFragment
    private List<Uri> imgEditUriList; // Uri(content://)
    private SparseArray<ImageSpan> sparseSpanArray;
    private SparseArray<Uri> sparseImageArray;

    private UploadBitmapTask bitmapTask;
    private UpdatePostTask updatePostTask;

    private FragmentBoardEditBinding binding;
    // Fields
    private String title, content;
    private int cntUpdateImages;

    // Default Constructor
    public BoardEditFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        if(getArguments() != null) {
            bundle = getArguments();
            title = getArguments().getString("postTitle");
            content = getArguments().getString("postContent");
            imgUriStringList = getArguments().getStringArrayList("urlImgList");
        }

        firestore = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        imgUtil = new ApplyImageResourceUtil(getContext());
        imgModel = new ViewModelProvider(requireActivity()).get(ImageViewModel.class);
        sharedModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);

        sparseSpanArray = new SparseArray<>();
        sparseImageArray = new SparseArray<>();
        imgEditUriList = new ArrayList<>();

        // If the post contains any image, the http url should be typecast to uri.
        if(imgUriStringList != null && imgUriStringList.size() > 0) {
            for(String uriString : imgUriStringList) imgEditUriList.add(Uri.parse(uriString));
        }


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBoardEditBinding.inflate(inflater);
        binding.etBoardEditTitle.setText(title);

        // Horizontal recycleview for displaying attached images
        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        linearLayout.setOrientation(LinearLayoutManager.HORIZONTAL);
        binding.editRecyclerImages.setLayoutManager(linearLayout);
        imgAdapter = new BoardImageAdapter(getContext(), imgEditUriList, this);
        binding.editRecyclerImages.setAdapter(imgAdapter);

        binding.etEditContent.setText(new SpannableStringBuilder(content));
        spanHandler = new BoardImageSpanHandler(binding.etEditContent, this);

        // If the post contains any image, find the markup in the content using Matcher.find() and
        // at the same time, images should be downsized by Glide and create imagespans only when it
        // has finished to do it. At this point, a sync issue may occur because Glide works on
        // the async basis. Thus, with while-loop index value made final, bitmaps from Glide
        // should be put into SparseArray. It seems that List.add(int, obj) does not work here.
        // Once the sparsearray completes to hold all imagespans, it should be converted to spanList
        // to pass to BoardImageSpanHander.setImageSpanList().
        if(imgEditUriList != null && imgEditUriList.size() > 0) setThumbnailImages();

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

        // Call the gallery or camera to capture images, the URIs of which are sent to an intent
        // of onActivityResult(int, int, Intent)
        binding.btnAttachImage.setOnClickListener(button -> selectImageMedia());
        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.getItem(0).setVisible(false);
        menu.getItem(1).setVisible(true);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_upload_post)
            updateImageToStorage();//uploadPostUpdate();
        return true;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Notified of which media(camera or gallery) to select in BoardChooserDlgFragment, according
        // to which startActivityForResult() is invoked by the parent activity and the result will be
        // notified to the activity and it is, in turn, sent back here by calling
        sharedModel.getImageChooser().observe(requireActivity(), chooser ->
            ((BoardActivity)requireActivity()).getImageFromChooser(chooser)
        );

        // On completing to apply Glide with an image selected from the chooser, set it to the span.
        /*
        imgModel.getGlideBitmapTarget().observe(getViewLifecycleOwner(), bitmap -> {
            ImageSpan imgSpan = new ImageSpan(view.getContext(), bitmap);
            spanHandler.setImageSpan(imgSpan);
        });
        */
        // As UploadBitmapTask has completed to optimize an attched image and upload it to Stroage,
        // the result is notified as SparseArray which indicates the position and uriString of image.
        imgModel.getDownloadBitmapUri().observe(getViewLifecycleOwner(), sparseArray -> {
            // Check if the number of attached images equals to the number of uris that are down
            // loaded from Storage.
            log.i("sparseArray: %s, %s", sparseArray.keyAt(0), sparseArray.valueAt(0));
            sparseImageArray.put(sparseArray.keyAt(0), sparseArray.valueAt(0));
            imgEditUriList.set(sparseArray.keyAt(0), Uri.parse(sparseArray.valueAt(0).toString()));

            if(sparseImageArray.size() == cntUpdateImages) updatePost();
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
        log.i("removeImage: %s", position);
        spanHandler.removeImageSpan(position);
    }

    // Implement BoardImageSpanHandler.OnImageSpanListener which notifies that an new ImageSpan
    // has been added or removed. The position param is fetched from the markup using the regular
    // expression.
    @Override
    public void notifyAddImageSpan(ImageSpan imgSpan, int position) {
        log.i("notifyAddImageSpan");
        if(imgUri != null) imgEditUriList.add(position, imgUri);
        imgAdapter.notifyItemChanged(position);
    }

    // Implement BoardImageSpanHandler.OnImageSpanListener
    @Override
    public void notifyRemovedImageSpan(int position) {
        log.i("notifyRemovedImageSpan");
        imgAdapter.notifyItemRemoved(position);
        imgEditUriList.remove(position);
        // On deleting an image by pressing the handle in a recyclerview thumbnail, the image file
        // is deleted from Storage as well.
        /* Not good b/c it cannot turn it back when calcelling.
        storage.getReferenceFromUrl(urlImgList.get(position)).delete()
                .addOnSuccessListener(aVoid ->log.i("delete image from Storage"))
                .addOnFailureListener(Exception::printStackTrace);
        */
    }

    // The attach button event handler to call the imge media chooser.
    private void selectImageMedia() {
        ((InputMethodManager)(requireActivity().getSystemService(INPUT_METHOD_SERVICE)))
                .hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        // Pop up the dialog as far as the num of attached pics are no more than the fixed size.
        if(imgEditUriList.size() >= Constants.MAX_IMAGE_NUMS) {
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
        int start = binding.etEditContent.getSelectionStart();
        int end = binding.etEditContent.getSelectionEnd();
        log.i("select range; %s, %s", start, end);
        binding.etEditContent.getText().replace(start, end, "\n");
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
            final Uri uri = imgEditUriList.get(pos);
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
                            if(sparseSpanArray.size() == imgUriStringList.size()) {
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

    // Add a thumbnail in the content. It is invoked by getAttachedImageUri(), ActivityResult
    // callback by ActivityResultContract.GetContent
    public void addImageThumbnail(Uri uri) {
        imgUri = uri;
        ApplyImageResourceUtil.applyGlideToImageSpan(getContext(), uri, spanHandler);
    }


    // If any images is removed, delete them from Firebase Storage.
    public void updateImageToStorage() {
        // Hide the soft input if it is visible.
        log.i("updateImageToStorage");
        ((InputMethodManager)requireActivity().getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(binding.getRoot().getWindowToken(), 0);
        if(!doEmptyCheck()) return;

        // If no images are attached, upload the post w/o processing images. Otherwise, beofore-editing
        // images should be deleted and new images be processed with downsize and rotation if necessary.
        if(imgEditUriList.size() == 0) updatePost(); // The post originally contains no images.
        else {
            if(imgUriStringList != null && imgUriStringList.size() > 0) {
                List<String> removedImages = new ArrayList<>();
                for (Uri uri : imgEditUriList) removedImages.add(uri.toString());
                imgUriStringList.removeAll(removedImages);
                log.i("Image: %s:", imgUriStringList.size());

                // Delete removed images in the post from Storage.
                for (String url : imgUriStringList) {
                    storage.getReferenceFromUrl(url).delete()
                            .addOnSuccessListener(aVoid -> log.i("delete image from Storage"))
                            .addOnFailureListener(Exception::printStackTrace);
                }
            }


            // Newly added images, the uri of which should have the scheme of "content://" instead
            // of "http://".
            log.i("uploadbitmaptask started");
            cntUpdateImages = 0;
            for(int i = 0; i < imgEditUriList.size(); i++) {
                if(Objects.equals(imgEditUriList.get(i).getScheme(), "content")) {
                    cntUpdateImages++;
                    log.i("count: %s", cntUpdateImages);
                    final Uri imgUri = imgEditUriList.get(i);
                    bitmapTask = ThreadManager2.uploadBitmapTask(getContext(), imgUri, i, imgModel);
                }
            }
        }
    }



    private void updatePost(){
        String docId = bundle.getString("documentId");
        if(docId == null || TextUtils.isEmpty(docId)) return;

        //final DocumentReference docref = firestore.collection("board_general").document(docId);
        final DocumentReference docref = firestore.collection("user_post").document(docId);
        docref.update("post_images", FieldValue.delete());

        Map<String, Object> updatePost = new HashMap<>();
        updatePost.put("post_title", binding.etBoardEditTitle.getText().toString());
        updatePost.put("post_content", binding.etEditContent.getText().toString());
        updatePost.put("timestamp", FieldValue.serverTimestamp());

        // Once deleting the existing image list, then upload a new image url list.
        if(imgEditUriList.size() > 0) {
            List<String> downloadList = new ArrayList<>();
            for(Uri uri : imgEditUriList) downloadList.add(uri.toString());
            updatePost.put("post_images", downloadList);
        }

        docref.update(updatePost).addOnSuccessListener(aVoid -> {
            sharedModel.getNewPosting().setValue(docId);
            ((BoardActivity)requireActivity()).addViewPager();
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

