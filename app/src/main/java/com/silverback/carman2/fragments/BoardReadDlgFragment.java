package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.silverback.carman2.BoardActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.ImageViewModel;
import com.silverback.carman2.threads.AttachedBitmapTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardReadDlgFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardReadDlgFragment.class);

    // Constants
    //private static final int IMAGE_CACHE_SIZE = 1024 * 1024 * 4;


    // Objects
    private Context context;
    private SpannableStringBuilder ssb;
    private ImageViewModel imageModel;
    private String postTitle, postContent, userName, userPic;
    private List<String> imgUriList;
    //private LruCache<String, Bitmap> memCache;
    private List<Integer> viewIdList;
    private AttachedBitmapTask bitmapTask;
    private List<Bitmap> bmpList;
    private SharedPreferences mSettings;

    // UIs
    private ConstraintLayout constraintLayout;
    //private ConstraintSet set;
    //private ImageView imgView;
    //private ConstraintLayout.LayoutParams layoutParams;
    private TextView tvAutoInfo;
    private TextView tvContent;

    private ImageView attachedImage;

    // Fields
    private StringBuilder autoData;
    private String userId;
    private int cntImages;

    public BoardReadDlgFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        imageModel = ViewModelProviders.of(this).get(ImageViewModel.class);
        //sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());

        if(getArguments() != null) {
            postTitle = getArguments().getString("postTitle");
            postContent = getArguments().getString("postContent");
            userName = getArguments().getString("userName");
            userPic = getArguments().getString("userPic");
            imgUriList = getArguments().getStringArrayList("imageUriList");
            userId = getArguments().getString("userId");
        }

        if(imgUriList != null && imgUriList.size() > 0) {
            bitmapTask = ThreadManager.startAttachedBitmapTask(context, imgUriList, imageModel);
        }

        // Get the auto data from SharedPreferences to display the post header.
        if(getActivity() != null) mSettings = ((BoardActivity)getActivity()).getSettings();
        String json = mSettings.getString(Constants.VEHICLE, null);

        try {
            JSONArray jsonArray = new JSONArray(json);
            autoData = new StringBuilder();

            // Refactor required
            autoData.append(jsonArray.get(0)).append(" ")
                    .append(jsonArray.get(2)).append(" ")
                    .append(jsonArray.get(3));

        } catch(JSONException e) {
            log.e("JSONException: %s", e.getMessage());
        }

        // If no images are transferred, just return localview not displaying any images.
        /*
        if(imgUriList != null && imgUriList.size() > 0) {
            for(int i = 0 ; i < imgUriList.size(); i++) {
                final String imgUri = imgUriList.get(i);
                bitmapTask = ThreadManager.startAttachedBitmapTask(context, imgUri, i, imageModel);
            }

        }

         */
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.dialog_board_read, container, false);
        constraintLayout = localView.findViewById(R.id.constraint_posting);
        ImageButton btn = localView.findViewById(R.id.imgbtn_dismiss);
        TextView tvTitle = localView.findViewById(R.id.tv_post_title);
        TextView tvUserName = localView.findViewById(R.id.tv_username);
        tvAutoInfo = localView.findViewById(R.id.tv_autoinfo);
        TextView tvDate = localView.findViewById(R.id.tv_posting_date);
        tvContent = localView.findViewById(R.id.tv_posting_body);
        ImageView imgUserPic = localView.findViewById(R.id.img_userpic);
        btn.setOnClickListener(view -> dismiss());

        tvTitle.setText(postTitle);
        tvUserName.setText(userName);
        tvAutoInfo.setText(autoData.toString());
        //tvContent.setText(postContent);
        tvContent.setText(postContent);
        tvDate.setText(getArguments().getString("timestamp"));

        //attachedImage = localView.findViewById(R.id.img_attached);

        // Set the user image
        Uri uriUserPic = Uri.parse(userPic);
        Glide.with(getContext())
                .asBitmap()
                .load(uriUserPic)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .circleCrop()
                .into(imgUserPic);

        // TEST CODING
        createImageSpanStringBuilder();


        return localView;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        // Set the image span to the post content as the image span instances are retrieved from
        // AttachedBitmapTask.
        imageModel.getImageSpanArray().observe(getViewLifecycleOwner(), spanArray -> {
            log.i("ImageSpan: %s", spanArray.keyAt(0));
            SpannableString spannable = createImageSpanString(spanArray);
            //tvContent.setText(spannable);



        });
    }

    @SuppressWarnings("ConstantConditiosn")
    private SpannableString createImageSpanString(SparseArray<ImageSpan> spanArray) {

        SpannableString spannable = new SpannableString(postContent);
        // Find the tag from the posting String.
        final String REGEX = "\\[image_\\d]";
        final Pattern p = Pattern.compile(REGEX);
        final Matcher m = p.matcher(spannable);

        int key = 0;
        while(m.find()) {
            if(spanArray.get(key) != null) {
                spannable.setSpan(spanArray.get(key), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                log.i("Failed to set Span");
            }

            key++;
        }

        return spannable;
    }

    private SpannableStringBuilder createImageSpanStringBuilder() {
        Spannable text = new SpannableString(postContent);
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        final String REGEX = "\\[image_\\d]";
        final Pattern p = Pattern.compile(REGEX);
        final Matcher m = p.matcher(text);
        int start = 0;

        while(m.find()) {
            CharSequence s = text.subSequence(start, m.start());
            TextView tv = new TextView(context);
            tv.setId(View.generateViewId());
            ConstraintSet set = new ConstraintSet();
            set.constrainWidth(tv.getId(), ConstraintSet.MATCH_CONSTRAINT);
            set.constrainHeight(tv.getId(), ConstraintSet.WRAP_CONTENT);
            //if(start == 0) set.connect(R.id.view_underline_header, ConstraintSet.TOP, null, null);


            //ssb.setSpan(new ImageSpan(context, R.drawable.arrow_left), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = m.end();
        }

        log.i("ssb: %s", ssb.length());

        tvContent.setText(ssb);

        return ssb;
    }


}
