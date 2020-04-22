package com.silverback.carman2.fragments;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman2.BoardActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardImageAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.ApplyImageResourceUtil;
import com.silverback.carman2.utils.BoardImageSpanHandler;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
import com.silverback.carman2.viewmodels.ImageViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * This fragment calls in an existing post to edit.
 */
public class BoardEditFragment extends BoardBaseFragment implements
        BoardImageSpanHandler.OnImageSpanListener,
        BoardImageAdapter.OnBoardAttachImageListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardEditFragment.class);

    private static final String REGEX_MARKUP = "\\[image_\\d]\\n";

    // Objects
    private Matcher m;
    private BoardImageAdapter imgAdapter;
    private BoardImageSpanHandler spanHandler;
    private ApplyImageResourceUtil imgUtil;
    private ImageViewModel imgModel;
    private FragmentSharedModel sharedModel;
    private Uri imgUri;
    private List<Uri> uriImages;
    private List<ImageSpan> spanList;

    // UIs
    private EditText etPostContent;

    // Fields
    private String title, content;
    private List<String> imgUriList;
    private int index;


    // Default Constructor
    public BoardEditFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            title = getArguments().getString("postTitle");
            content = getArguments().getString("postContent");
            imgUriList = getArguments().getStringArrayList("uriImgList");
        }

        m = Pattern.compile(REGEX_MARKUP).matcher(content);
        spanList = new ArrayList<>();
        uriImages = new ArrayList<>();

        imgUtil = new ApplyImageResourceUtil(getContext());
        imgModel = new ViewModelProvider(requireActivity()).get(ImageViewModel.class);
        sharedModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
    }

    @SuppressWarnings({"ConstantConditions", "ClickableViewAccessibility"})
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_board_edit, container, false);

        EditText etPostTitle = localView.findViewById(R.id.et_board_write_title);
        Button btnAttach = localView.findViewById(R.id.btn_attach_image);
        RecyclerView recyclerView = localView.findViewById(R.id.vg_recycler_images);
        etPostContent = localView.findViewById(R.id.et_edit_content);

        etPostTitle.setText(title);
        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        linearLayout.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayout);

        imgAdapter = new BoardImageAdapter(uriImages, this);
        recyclerView.setAdapter(imgAdapter);

        etPostContent.setText(content);
        spanHandler = new BoardImageSpanHandler(etPostContent, this);

        // Post contains any image that
        if(imgUriList != null && imgUriList.size() > 0) {

            for(String uriString : imgUriList) uriImages.add(Uri.parse(uriString));

            // Set the thumbnail size
            final float scale = getResources().getDisplayMetrics().density;
            int size = (int)(Constants.IMAGESPAN_THUMBNAIL_SIZE * scale + 0.5f);
            // Match the markup to set the imagespan to it.
            index = 0;
            while(m.find()) {
                Uri uri = Uri.parse(imgUriList.get(index));
                Glide.with(this).asBitmap().override(size).fitCenter().load(uri).into(new CustomTarget<Bitmap>(){
                    @Override
                    public void onResourceReady(@NonNull Bitmap res, @Nullable Transition<? super Bitmap> transition) {
                        ImageSpan imgSpan = new ImageSpan(getContext(), res);
                        spanList.add(imgSpan);
                        if(spanList.size() == imgUriList.size()) spanHandler.setImageSpanList(spanList);
                        index++;
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
            }
        }

        // Scroll the edittext inside (nested)scrollview. More research should be done.
        // warning message is caused by onPermClick not implemented. Unless the method is requried
        // to implement, the warning can be suppressed by "clickableVieweaccessibility".
        etPostContent.setOnTouchListener((view, event) ->{
            if(etPostContent.hasFocus()) {
                view.getParent().requestDisallowInterceptTouchEvent(true);
                switch(event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_SCROLL:
                        view.getParent().requestDisallowInterceptTouchEvent(false);
                        return true;
                }
            }
            return false;
        });

        // Call the gallery or camera to capture images, the URIs of which are sent to an intent
        // of onActivityResult(int, int, Intent)
        btnAttach.setOnClickListener(btn -> {
            ((InputMethodManager)(getActivity().getSystemService(INPUT_METHOD_SERVICE)))
                    .hideSoftInputFromWindow(localView.getWindowToken(), 0);

            // Pop up the dialog as far as the num of attached pics are no more than 6.
            if(uriImages.size() > Constants.MAX_ATTACHED_IMAGE_NUMS) {
                log.i("Image count: %s", uriImages.size());
                Snackbar.make(localView, getString(R.string.board_msg_image), Snackbar.LENGTH_SHORT).show();

            } else {
                // Pop up the dialog to select which media to use bewteen the camera and gallery, then
                // create an intent by the selection.
                DialogFragment dialog = new BoardChooserDlgFragment();
                dialog.show(getActivity().getSupportFragmentManager(), "chooser");

                // Put the line breaker into the edittext when the image interleaves b/w the lines
                int start = etPostContent.getSelectionStart();
                int end = etPostContent.getSelectionEnd();
                log.i("insert image: %s, %s, %s", etPostContent.getText(), start, end);
                etPostContent.getText().replace(start, end, "\n");
            }
        });

        return localView;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        // Notified of which media(camera or gallery) to select in BoardChooserDlgFragment, according
        // to which startActivityForResult() is invoked by the parent activity and the result will be
        // notified to the activity and it is, in turn, sent back here by calling
        sharedModel.getImageChooser().observe(getViewLifecycleOwner(), chooser -> {
            log.i("FragmentViwModel: %s", sharedModel);
            ((BoardActivity)getActivity()).getImageFromChooser(chooser);
        });


        // The imgUri received as a result of startActivityForResult() is applied to applyGlideToImageSpan().
        // This util method translates an image to an appropriate extent for fitting the imagespan and
        // the result is provided
        imgModel.getGlideBitmapTarget().observe(requireActivity(), bitmap -> {
            ImageSpan imgSpan = new ImageSpan(getContext(), bitmap);
            spanHandler.setImageSpan(imgSpan);
        });
    }


    @Override
    public void removeImage(int position) {
        log.i("removeImage: %s", position);
        spanHandler.removeImageSpan(position);
        /*
        uriImages.remove(position);
        // notifyItemRemoved(), weirdly does not work here.
        imgAdapter.notifyDataSetChanged();
        etPostContent.setText(ssb);
         */
    }

    // Implement BoardImageSpanHandler.OnImageSpanListener which notifies that an new ImageSpan
    // has been added or removed. The position param is fetched from the markup using the regular
    // expression.
    @Override
    public void notifyAddImageSpan(ImageSpan imgSpan, int position) {
        if(imgUri != null) uriImages.add(position, imgUri);
        imgAdapter.notifyDataSetChanged();
    }

    // Implement BoardImageSpanHandler.OnImageSpanListener
    @Override
    public void notifyRemovedImageSpan(int position) {
        log.i("Removed Span: %s", position);
        uriImages.remove(position);
        imgAdapter.notifyDataSetChanged();
    }


    // Invokde by OnActivityResult() in the parent activity that passes an intent data(URI) as to
    // an image picked in the media which has been selected by BoardChooserDlgFragment
    public void setUriFromImageChooser(Uri uri) {
        log.d("setUriFromImageChooser");
        int x = Constants.IMAGESPAN_THUMBNAIL_SIZE;
        int y = Constants.IMAGESPAN_THUMBNAIL_SIZE;
        imgUtil.applyGlideToImageSpan(uri, x, y, imgModel);
        // Partial binding to show the image. RecyclerView.setHasFixedSize() is allowed to make
        // additional pics.
        imgUri = uri;
    }
}

