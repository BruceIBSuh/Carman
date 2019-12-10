package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
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
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FirestoreViewModel;
import com.silverback.carman2.threads.DownloadBitmapTask;
import com.silverback.carman2.threads.ThreadManager;

import java.util.List;
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
    private FirestoreViewModel firestoreModel;
    //private SimpleDateFormat sdf;
    private String postTitle, postContent, userName, userPic;
    private List<String> imgUriList;
    //private LruCache<String, Bitmap> memCache;
    private List<Integer> viewIdList;
    private DownloadBitmapTask bitmapTask;
    private List<Bitmap> bmpList;

    // UIs
    private ConstraintLayout constraintLayout;
    //private ConstraintSet set;
    //private ImageView imgView;
    //private ConstraintLayout.LayoutParams layoutParams;
    private TextView tvAutoInfo;
    private TextView tvContent;

    private ImageView attachedImage;

    // Fields
    private String userId;
    private int cntImages;

    public BoardReadDlgFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();
        //FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestoreModel = ViewModelProviders.of(this).get(FirestoreViewModel.class);
        //sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());

        if(getArguments() != null) {
            postTitle = getArguments().getString("postTitle");
            postContent = getArguments().getString("postContent");
            userName = getArguments().getString("userName");
            userPic = getArguments().getString("userPic");
            imgUriList = getArguments().getStringArrayList("imageUriList");
            userId = getArguments().getString("userId");
            //autoData = getArguments().getString("autoData");
        }

        // If no images are transferred, just return localview not displaying any images.
        if(imgUriList != null && imgUriList.size() > 0) {
            bitmapTask = ThreadManager.startDownloadBitmapTask(context, imgUriList, firestoreModel);
        }
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.dialog_board_post, container, false);
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
        tvContent.setText(postContent);
        tvDate.setText(getArguments().getString("timestamp"));

        attachedImage = localView.findViewById(R.id.img_attached);

        // Set the user image
        Uri uriUserPic = Uri.parse(userPic);
        Glide.with(getContext())
                .asBitmap()
                .load(uriUserPic)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .fitCenter()
                .circleCrop()
                .into(imgUserPic);


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
        // DownloadBitmapTask.
        firestoreModel.getAttachedImageSpanList().observe(getViewLifecycleOwner(), spanArray -> {
            SpannableStringBuilder ssb = doImageSpanString(spanArray);
            tvContent.setText(ssb);

        });
    }

    @SuppressWarnings("ConstantConditiosn")
    private SpannableStringBuilder doImageSpanString(SparseArray<ImageSpan> spanArray) {

        SpannableStringBuilder ssb = new SpannableStringBuilder(postContent);

        // Find the tag from the posting String.
        final String REGEX = "\\[image_\\d\\]";
        final Pattern p = Pattern.compile(REGEX);
        final Matcher m = p.matcher(ssb);


        int key = 0;
        while(m.find()) {
            if(spanArray.get(key) != null) {
                ssb.setSpan(spanArray.get(key), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                log.i("Failed to sete Span");
            }

            key++;
        }

        return ssb;
    }


}
