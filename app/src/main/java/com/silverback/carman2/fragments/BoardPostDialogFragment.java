package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardPostDialogFragment extends DialogFragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPostDialogFragment.class);

    // Constants
    //private static final int IMAGE_CACHE_SIZE = 1024 * 1024 * 4;


    // Objects
    //private FirestoreViewModel firestoreModel;
    //private SimpleDateFormat sdf;
    private String postTitle, postContent, userName, userPic;
    private List<String> imgUriList;
    //private LruCache<String, Bitmap> memCache;

    // UIs
    //private ConstraintLayout constraintLayout;
    //private ConstraintSet set;
    //private ImageView imgView;
    //private ConstraintLayout.LayoutParams layoutParams;
    private TextView tvAutoInfo;

    // Fields
    private String userId;

    public BoardPostDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        //firestoreModel = ViewModelProviders.of(getActivity()).get(FirestoreViewModel.class);
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

        /*
        memCache = new LruCache<String, Bitmap>(IMAGE_CACHE_SIZE) {
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
         */

        // Auto information is retrived from Firestore based upon transferred user id and set it
        // to the view.
        firestore.collection("users").document(userId).get().addOnSuccessListener(document -> {
            log.i("auto data: %s", document.getString("auto_data"));
            try {
                JSONArray jsonArray = new JSONArray(document.getString("auto_data"));
                StringBuilder sb = new StringBuilder();
                for(int i = 0; i < jsonArray.length(); i++) {
                    if(!jsonArray.getString(i).isEmpty()) sb.append(jsonArray.getString(i));
                    sb.append(" ");
                }
                tvAutoInfo.setText(sb.toString());

            } catch(JSONException e) {
                log.e("JSONException: %s", e.getMessage());
            }
        });



    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.dialog_board_post, container, false);
        ConstraintLayout constraintLayout = localView.findViewById(R.id.constraint_posting);
        ImageButton btn = localView.findViewById(R.id.imgbtn_dismiss);
        TextView tvTitle = localView.findViewById(R.id.tv_post_title);
        TextView tvUserName = localView.findViewById(R.id.tv_username);
        tvAutoInfo = localView.findViewById(R.id.tv_autoinfo);
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
        if(imgUriList == null || imgUriList.size() == 0) return localView;

        // Create ImageView dynamically
        // Attached Image(s) dynamically using LayoutParams for layout_width and height and
        // ConstraintSet to set the layout positioned in ConstraintLayout
        List<Integer> idList = new ArrayList<>();
        for(int i = 0; i < imgUriList.size(); i++) {
            ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT);
            ConstraintSet set = new ConstraintSet();

            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(layoutParams);
            imageView.setId(View.generateViewId());
            idList.add(imageView.getId());
            constraintLayout.addView(imageView, i);
            set.clone(constraintLayout);

            set.connect(imageView.getId(), ConstraintSet.START, R.id.constraint_posting, ConstraintSet.START);
            set.connect(imageView.getId(), ConstraintSet.END, R.id.constraint_posting, ConstraintSet.END);

            // Create first Imageview
            if(i == 0) {
                set.connect(imageView.getId(), ConstraintSet.TOP, R.id.tv_posting_body, ConstraintSet.BOTTOM, 50);
            // Create last Imageview
            } else if(i == imgUriList.size() - 1) {
                set.connect(imageView.getId(), ConstraintSet.TOP, idList.get(i - 1), ConstraintSet.BOTTOM, 30);
                // Last image should be in postion to the top of RecyclerView with a margin.
                set.connect(imageView.getId(), ConstraintSet.BOTTOM, R.id.vg_recycler_comments, ConstraintSet.TOP, 100);
            } else {
                set.connect(imageView.getId(), ConstraintSet.TOP, idList.get(i -1), ConstraintSet.BOTTOM, 30);
            }

            set.applyTo(constraintLayout);

            Glide.with(getContext())
                    .asBitmap()
                    .load(Uri.parse(imgUriList.get(i)))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            imageView.setImageBitmap(resource);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });

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
