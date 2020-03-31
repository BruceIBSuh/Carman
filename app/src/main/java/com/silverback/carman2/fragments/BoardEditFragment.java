package com.silverback.carman2.fragments;

import android.graphics.Bitmap;
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
import android.widget.ImageView;

import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardImageAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.ApplyImageResourceUtil;
import com.silverback.carman2.utils.BoardImageSpanHandler;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.viewmodels.ImageViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Context.INPUT_METHOD_SERVICE;

public class BoardEditFragment extends BoardBaseFragment implements
        BoardImageSpanHandler.OnImageSpanRemovedListener,
        BoardImageAdapter.OnBoardAttachImageListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardEditFragment.class);

    private static final String REGEX_MARKUP = "\\[image_\\d]\\n";

    // Objects
    private Matcher m;
    private BoardImageSpanHandler spanHandler;
    private ApplyImageResourceUtil imgUtil;
    private ImageViewModel imgModel;

    // UIs
    private EditText etPostContent;
    private RecyclerView recyclerView;
    private BoardImageAdapter imgAdapter;
    private ImageView imageView;

    // Fields
    private SpannableStringBuilder ssb;
    private String title, content;
    private List<String> imgUriList;
    private List<Uri> uriImages;
    private List<ImageSpan> spanList;

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
        // Create RecyclerView for holding attched pictures which are handled in onActivityResult()
        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        linearLayout.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayout);


        // If the post includes attached image(s), make a type cast of String to Uri, then pass it
        // to the adapter. Otherwise, display the post as it is.
        if(imgUriList != null && imgUriList.size() > 0) {
            for (String uriString : imgUriList) uriImages.add(Uri.parse(uriString));
            imgAdapter = new BoardImageAdapter(uriImages, this);
            recyclerView.setAdapter(imgAdapter);
            ssb = new SpannableStringBuilder(content);

        } else etPostContent.setText(content);


        // To scroll edittext inside (nested)scrollview. More research should be done.
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

                // Put linefeeder into the edittext when the image interleaves b/w the lines
                int start = Math.max(etPostContent.getSelectionStart(), 0);
                int end = Math.max(etPostContent.getSelectionStart(), 0);
                etPostContent.getText().replace(Math.min(start, end), Math.max(start, end), "\n");
            }
        });



        return localView;
    }


    // Implement BoardImageADapter.OnAttachImageListener which has the following overriding methods
    @SuppressWarnings("ConstantConditions")
    @Override
    public void attachImage(Bitmap bmp, int pos) {
        log.i("Attache image test: %s, %s", bmp, pos);
        // Create ImageSpan list to set it to the post.
        ImageSpan imgspan = new ImageSpan(getContext(), bmp);
        spanList.add(imgspan);
        // ImageSpans are done with all images
        if(spanList.size() == imgAdapter.getItemCount()) {
            int index = 0;
            while(m.find()) {
                ssb.setSpan(spanList.get(index), m.start(), m.end(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                index++;
            }

            etPostContent.setText(ssb);
            spanHandler = new BoardImageSpanHandler(ssb, this);
            spanHandler.setImageSpan(spanList);
        }
    }

    @Override
    public void removeImage(int position) {
        log.i("removeImage: %s", position);
        spanHandler.removeImageSpan(position);
        uriImages.remove(position);
        // notifyItemRemoved(), weirdly does not work here.
        imgAdapter.notifyDataSetChanged();

        etPostContent.setText(ssb);
    }

    @Override
    public void notifyAddImageSpan(int position) {

    }

    // Implement BoardImageSpanHandler.OnImageSpanRemovedListener
    @Override
    public void notifyRemovedImageSpan(int position) {
        log.i("Removed Span: %s", position);
        //uriImages.remove(position);
        //imgAdapter.notifyDataSetChanged();
    }
}

