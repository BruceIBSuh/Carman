package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.LruCache;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FirestoreViewModel;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardPostDialogFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPostDialogFragment.class);

    // Constants
    private static final int IMAGE_CACHE_SIZE = 1024 * 1024 * 4;


    // Objects
    private FirebaseFirestore firestore;
    private FirestoreViewModel firestoreModel;
    private SimpleDateFormat sdf;
    private String postTitle, postContent, userName, userPic;
    private List<String> imgUriList;
    private LruCache<String, Bitmap> memCache;

    // UIs
    private ConstraintLayout constraintLayout;
    private ConstraintSet set;
    private ImageView imgView;
    private ConstraintLayout.LayoutParams layoutParams;

    // Fields
    private List<Integer> imgIdList;
    private int prevImageViewId;

    public BoardPostDialogFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firestore = FirebaseFirestore.getInstance();
        firestoreModel = ViewModelProviders.of(getActivity()).get(FirestoreViewModel.class);
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());

        if(getArguments() != null) {
            postTitle = getArguments().getString("postTitle");
            postContent = getArguments().getString("postContent");
            userName = getArguments().getString("userName");
            userPic = getArguments().getString("userPic");
            imgUriList = getArguments().getStringArrayList("imageUriList");
        }

        memCache = new LruCache<String, Bitmap>(IMAGE_CACHE_SIZE) {
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };


    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.dialog_board_post, container, false);
        constraintLayout = localView.findViewById(R.id.root_constraint);
        ImageButton btn = localView.findViewById(R.id.imgbtn_dismiss);
        TextView tvTitle = localView.findViewById(R.id.tv_post_title);
        TextView tvUserName = localView.findViewById(R.id.tv_username);
        TextView tvAutoInfo = localView.findViewById(R.id.tv_autoinfo);
        TextView tvDate = localView.findViewById(R.id.tv_posting_date);
        TextView tvContent = localView.findViewById(R.id.tv_posting_body);
        ImageView imgUserPic = localView.findViewById(R.id.img_userpic);
        btn.setOnClickListener(view -> dismiss());

        tvTitle.setText(postTitle);
        tvUserName.setText(userName);
        tvContent.setText(postContent);
        tvDate.setText(getArguments().getString("timestamp"));

        Uri uriUserPic = Uri.parse(userPic);
        Glide.with(getContext())
                .asBitmap()
                .load(uriUserPic)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .fitCenter()
                .circleCrop()
                .into(imgUserPic);

        // If no images are transferred, just return localview not displaying any images.
        log.i("imagUriList: %s, %s", imgUriList, imgUriList.size());
        if(imgUriList == null || imgUriList.size() == 0) return localView;

        for(String uri : imgUriList) {

            Glide.with(getContext())
                    .asBitmap()
                    .load(Uri.parse(uri))
                    //.diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            memCache.put(uri, resource);
                            log.i("Image Uri: %s", memCache.get(uri));

                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
        }


            // Attached Image(s) dynamically using LayoutParams for layout_width and height and
        // ConstraintSet to set the layout positioned in ConstraintLayout
        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT);

        ConstraintSet set = new ConstraintSet();
        imgIdList = new ArrayList<>();
        prevImageViewId = -1;

        for(int i = 0; i < imgUriList.size(); i++) {

            ImageView imgView = new ImageView(getContext());
            imgView.setId(View.generateViewId());
            imgView.setLayoutParams(layoutParams);
            log.i("image view id: %s", imgView.getId());
            constraintLayout.addView(imgView);
            set.clone(constraintLayout);

            set.connect(imgView.getId(), ConstraintSet.START, R.id.root_constraint, ConstraintSet.START, 50);
            set.connect(imgView.getId(), ConstraintSet.END, R.id.root_constraint, ConstraintSet.END, 50);
            //set.connect(imgView.getId(), ConstraintSet.TOP, R.id.tv_posting_body, ConstraintSet.BOTTOM, 50);
            /*
            int constrained = (i == 0)? R.id.tv_posting_body : prevImageViewId;
            set.connect(imgView.getId(), ConstraintSet.TOP, constrained, ConstraintSet.BOTTOM, 50);
            set.applyTo(constraintLayout);
            */

            String uri = imgUriList.get(i);

            Glide.with(getContext())
                    .asBitmap()
                    .load(Uri.parse(imgUriList.get(i)))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                            Bitmap image = memCache.get(uri);
                            log.i("Image in cache: %s", image);

                            if(uri.equals(imgUriList.get(0))) {
                                set.connect(imgView.getId(), ConstraintSet.TOP, R.id.tv_posting_body, ConstraintSet.BOTTOM, 50);
                                set.applyTo(constraintLayout);


                            } else {
                                log.i("additional images");
                                set.connect(imgView.getId(), ConstraintSet.TOP, prevImageViewId, ConstraintSet.BOTTOM, 50);
                                set.applyTo(constraintLayout);
                            }

                            if(image != null) {
                                imgView.setImageBitmap(image);
                                prevImageViewId = imgView.getId();
                                log.i("prev id: %s", prevImageViewId);
                            } else {
                                Glide.with(getContext())
                                        .load(uri)
                                        .into(imgView);

                            }



                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {
                            log.i("onLoadCleared");
                        }

                    });

            log.i("prevImageViewId: %s", prevImageViewId);
        }

        return localView;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

}
