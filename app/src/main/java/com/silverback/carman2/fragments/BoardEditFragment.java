package com.silverback.carman2.fragments;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardAttachImgAdapter;
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

public class BoardEditFragment extends BoardBaseFragment implements
        BoardAttachImgAdapter.OnBoardAttachImageListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardEditFragment.class);

    private static final String REGEX_MARKUP = "\\[image_\\d]\\n";

    // Objects
    private BoardImageSpanHandler spanHandler;
    private ApplyImageResourceUtil imgUtil;
    private ImageViewModel imgModel;

    // UIs
    private ConstraintLayout root;
    private View header;
    private EditText etPostTitle, etContent;
    private RecyclerView recyclerView;
    private BoardAttachImgAdapter imgAdapter;
    private ImageView imageView;

    // Fields
    private String title, content;
    private List<String> imgUriList;
    private List<Uri> uriImages;


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

        imgUtil = new ApplyImageResourceUtil(getContext());
        imgModel = new ViewModelProvider(requireActivity()).get(ImageViewModel.class);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_board_edit, container, false);

        root = localView.findViewById(R.id.root_board_edit);
        header = localView.findViewById(R.id.view_edit_header);
        etPostTitle = localView.findViewById(R.id.et_board_title);
        recyclerView = localView.findViewById(R.id.vg_recycler_images);

        etContent = localView.findViewById(R.id.et_edit_content);

        etPostTitle.setText(title);

        // Create RecyclerView for holding attched pictures which are handled in onActivityResult()
        LinearLayoutManager linearLayout = new LinearLayoutManager(getContext());
        linearLayout.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayout);

        // If the image list is passed here as string type, set it to the adapter after translating
        // it to Uri list.
        if(imgUriList != null && imgUriList.size() > 0) {
            uriImages = new ArrayList<>();
            for (String uriString : imgUriList) uriImages.add(Uri.parse(uriString));
            imgAdapter = new BoardAttachImgAdapter(uriImages, this);
            recyclerView.setAdapter(imgAdapter);
        }


        etContent.setText(content);
        spanHandler = new BoardImageSpanHandler(etContent.getText());



        return localView;
    }


    @Override
    public void removeGridImage(int position) {


        final Matcher m = Pattern.compile(REGEX_MARKUP).matcher(content);
        removeContentView();

        int index = 0;
        while(m.find()) {
            if(index == position) {
                content = content.replaceFirst(REGEX_MARKUP, "");
                uriImages.remove(position);
                imgAdapter.notifyItemRemoved(position);

                editContentView(content, uriImages);

                break;
            }

            index++;
        }
    }

    private void removeContentView() {
        final Matcher m = Pattern.compile(REGEX_MARKUP).matcher(content);
        int index = 0;

        while(m.find()) {
            root.removeView(root.findViewWithTag(index));
            index++;
        }
    }

    private void editContentView(String content, List<Uri> images) {

        final Matcher m = Pattern.compile(REGEX_MARKUP).matcher(content);
        int index = 0;
        int start = 0;
        int constraintId = root.getId();
        int topConstraint;
        int prevImageId = 0;

        // Create LayoutParams using LinearLayout(RelativeLayout).LayoutParams, not using Constraint
        // Layout.LayoutParams. WHY?
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
               LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);


        // If the content contains images, which means the markup(s) exists in the content, the content
        // is split into parts of texts and images and respectively connected to ConstraintSet.

        while(m.find()) {
           // Check if the content starts w/ text or image, which depends on the value of start and
           // add the
            String paragraph = content.substring(start, m.start());
            EditText et = createEditText(paragraph, index);
            et.setTag(index);
            root.addView(et, params);
            topConstraint = (start == 0) ? header.getId() : prevImageId;

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(root);
            tvSet.connect(et.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 16);
            tvSet.connect(et.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 16);
            tvSet.connect(et.getId(), ConstraintSet.TOP, topConstraint, ConstraintSet.BOTTOM, 16);
            tvSet.applyTo(root);

            // Even if no content exists, ConstrainSet.TOP should be tv.getId() b/c a line is inserted
            // when attaching an image.
            ImageView imgView = new ImageView(getContext());
            imgView.setId(View.generateViewId());
            imgView.setTag(index);
            prevImageId = imgView.getId();
            root.addView(imgView, params);

            ConstraintSet imgSet = new ConstraintSet();
            imgSet.clone(root);
            imgSet.connect(imgView.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 0);
            imgSet.connect(imgView.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 0);
            imgSet.connect(imgView.getId(), ConstraintSet.TOP, et.getId(), ConstraintSet.BOTTOM, 0);
            imgSet.applyTo(root);

            // Consider to apply Glide thumbnail() method.
            Glide.with(this).asBitmap().override(100).load(images.get(index))
                   .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).fitCenter().into(imgView);

            start = m.end();
            index++;
        }


        // Coordinate the position b/w the last part, no matter what is image or text in the content,
        // and the following recycler view by the patterns.

        // No image attached b/c m.find() is false
        if(start == 0) {
            EditText noImageText = createEditText(content, index);
            root.addView(noImageText, params);

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(root);
            tvSet.connect(noImageText.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 16);
            tvSet.connect(noImageText.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 16);
            tvSet.connect(noImageText.getId(), ConstraintSet.TOP, header.getId(), ConstraintSet.BOTTOM, 0);


            tvSet.applyTo(root);

        // Text exists after the last image. The last textview is constrained to the previous imageview
        // and the recyclerview constrained to the textview.
        } else if(start < content.length()) {
            String lastParagraph = content.substring(start);
            EditText lastView = createEditText(lastParagraph, index);
            root.addView(lastView, params);

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(root);
            tvSet.connect(lastView.getId(), ConstraintSet.START, constraintId, ConstraintSet.START, 16);
            tvSet.connect(lastView.getId(), ConstraintSet.END, constraintId, ConstraintSet.END, 16);
            tvSet.connect(lastView.getId(), ConstraintSet.TOP, prevImageId, ConstraintSet.BOTTOM, 0);
            tvSet.connect(recyclerView.getId(), ConstraintSet.TOP, lastView.getId(), ConstraintSet.BOTTOM, 64);
            tvSet.applyTo(root);

           // In case no text exists after the last image, the recyclerView is constrained to the last
           // ImageView.
        }
    }


    private EditText createEditText(String text, int tag) {

        EditText etText = new EditText(getContext());
        etText.setId(View.generateViewId());
        etText.setTag(tag);
        etText.setText(text);
        //etText.setTextSize(14);
        etText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        etText.setTextColor(Color.BLUE);
        etText.setBackground(null);

        return etText;

   }
}

