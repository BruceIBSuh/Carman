package com.silverback.carman2.fragments;


import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.DynamicDrawableSpan;
import android.text.style.ImageSpan;
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
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FirestoreViewModel;
import com.silverback.carman2.threads.DownloadBitmapTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.EditImageHelper;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
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
        firestoreModel = ViewModelProviders.of(getActivity()).get(FirestoreViewModel.class);
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
            for(String uriString : imgUriList) {
                bitmapTask = ThreadManager.startDownloadBitmapTask(context, uriString, firestoreModel);
            }
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
        /*
        firestore.collection("users").document(userId).get().addOnSuccessListener(document -> {
            log.i("auto data: %s", document.getString("auto_data"));
            if(document.getString("auto_data") == null || document.getString("auto_data").isEmpty())
                return;

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
         */



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
        //tvContent.setText(postContent);
        tvDate.setText(getArguments().getString("timestamp"));

        attachedImage = localView.findViewById(R.id.img_attached);

        Uri uriUserPic = Uri.parse(userPic);
        Glide.with(getContext())
                .asBitmap()
                .load(uriUserPic)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .fitCenter()
                .circleCrop()
                .into(imgUserPic);

        /*
        int count = 0;
        while(m.find()) {
            log.i("matched: %s, %s", m.start(), m.end());
            Uri imgUri = Uri.parse(imgUriList.get(count));
            //Bitmap bitmap = EditImageHelper.resizeBitmap(getContext(), imgUri, 100, 100);


            FutureTarget<Drawable> futureImage = Glide.with(getContext()).asDrawable().load(imgUri).submit();
            new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        log.i("futureImage: %s", futureImage.get());


                    } catch(ExecutionException e) {

                    } catch(InterruptedException e) {

                    }
                }
            }).start();


            Glide.with(getContext()).load(imgUri).into(new CustomTarget<Drawable>(){
                @Override
                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                    resource.setBounds(0, 0, resource.getIntrinsicWidth(), resource.getIntrinsicHeight());
                    ImageSpan imgSpan = new ImageSpan(resource, DynamicDrawableSpan.ALIGN_BASELINE);
                    ssb.setSpan(imgSpan, m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {

                }
            });


            count++;
        }
        */




        SpannableStringBuilder ssb = doImageSpanString(imgUriList);
        tvContent.setText(ssb);




        /*
        // In case a single image is attached, the image is displayed at the end of the content.
        } else if(imgUriList.size() == 1) {
            tvContent.setText(postContent);

            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT);
            ConstraintSet set = new ConstraintSet();

            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(params);
            imageView.setId(View.generateViewId());
            imageView.setBackgroundColor(Color.parseColor("#E0E0E0"));
            constraintLayout.addView(imageView);
            set.clone(constraintLayout);

            set.connect(imageView.getId(), ConstraintSet.START, R.id.constraint_posting, ConstraintSet.START);
            set.connect(imageView.getId(), ConstraintSet.END, R.id.constraint_posting, ConstraintSet.END);
            set.connect(imageView.getId(), ConstraintSet.TOP, R.id.tv_posting_body, ConstraintSet.BOTTOM, 50);

        // Multiple images should be interleaved b/w split strings according to the markups.
        } else if(imgUriList.size() > 1) {
            log.i("imgUriList: %s, %s", imgUriList.get(0), Uri.parse(imgUriList.get(0)));
            addTextImageView(postContent);

        }
        */
        // Attached Image(s) dynamically using LayoutParams for layout_width and height and
        // ConstraintSet to set the layout positioned in ConstraintLayout
        /*
        List<Integer> idList = new ArrayList<>();
        for(int i = 0; i < imgUriList.size(); i++) {
            ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT);
            ConstraintSet set = new ConstraintSet();

            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(layoutParams);
            imageView.setId(View.generateViewId());
            imageView.setBackgroundColor(Color.parseColor("#E0E0E0"));
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
        */

        return localView;
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    /*
    @SuppressWarnings("ConstantConditions")
    private void addTextImageView(String content) {

        List<Integer> viewIdList = new ArrayList<>();

        final String REGEX = "image_\\d";
        Pattern p = Pattern.compile(REGEX);
        String trimContent = content.trim();
        List<String> splitContent =  Arrays.asList(p.split((trimContent)));



        SpannableStringBuilder ssb = new SpannableStringBuilder(content);
        Matcher m = p.matcher(ssb);


        int count = 0;
        while(m.find()) {
            log.i("matched: %s, %s", m.start(), m.end());
            Uri imgUri = Uri.parse(imgUriList.get(count));
            log.i("Image Uri: %s", imgUri);
            Bitmap bitmap = EditImageHelper.resizeBitmap(getActivity().getApplicationContext(), imgUri, 50, 50);
            log.i("Bitmap: %s", bitmap);
            ImageSpan imgSpan = new ImageSpan(getContext(), bitmap);
            ssb.setSpan(imgSpan, m.start(), m.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            count++;
        }

        tvContent.setText(ssb);


        log.i("Size: %s, %s", splitContent.size(), imgUriList.size());
        for(int i = 0; i < splitContent.size(); i++) {

            ConstraintLayout.LayoutParams tvParams = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT);

            ConstraintLayout.LayoutParams imgParams = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_CONSTRAINT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT);


            ConstraintSet set = new ConstraintSet();

            TextView textView = new TextView(getContext());
            textView.setLayoutParams(tvParams);
            textView.setId(View.generateViewId());
            viewIdList.add(textView.getId());
            log.i("TextView ID: %s", textView.getId());

            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(imgParams);
            imageView.setId(View.generateViewId());
            log.i("ImageView Id: %s", imageView.getId());
            viewIdList.add(imageView.getId());

            constraintLayout.addView(textView);
            constraintLayout.addView(imageView);
            set.clone(constraintLayout);

            set.connect(textView.getId(), ConstraintSet.START, R.id.constraint_posting, ConstraintSet.START);
            set.connect(textView.getId(), ConstraintSet.END, R.id.constraint_posting, ConstraintSet.END);

            set.connect(imageView.getId(), ConstraintSet.START, R.id.constraint_posting, ConstraintSet.START);
            set.connect(imageView.getId(), ConstraintSet.END, R.id.constraint_posting, ConstraintSet.END);



            //if(imgUriList.get(i) == null) return;
            final int pos = i;
            Glide.with(getContext()).asBitmap().load(Uri.parse(imgUriList.get(i)))
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .fitCenter()
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                            if(pos == 0) {
                                set.connect(textView.getId(), ConstraintSet.TOP, R.id.tv_posting_body, ConstraintSet.BOTTOM);
                                set.connect(imageView.getId(), ConstraintSet.TOP, textView.getId(), ConstraintSet.BOTTOM);
                                textView.setText(splitContent.get(pos));
                                imageView.setImageBitmap(resource);

                            } else {
                                // The second text view is placed below the first image
                                log.i("View id: %s, %s", textView.getId(), imageView.getId());
                                set.connect(textView.getId(), ConstraintSet.TOP, viewIdList.get(pos), ConstraintSet.BOTTOM);
                                set.connect(imageView.getId(), ConstraintSet.TOP, viewIdList.get(pos -1), ConstraintSet.BOTTOM);
                                textView.setText(splitContent.get(pos));
                                imageView.setImageBitmap(resource);
                            }

                            log.i("split string: %s", splitContent.get(pos));


                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });

            set.applyTo(constraintLayout);

        }
    }
    */

    @SuppressWarnings("ConstantConditiosn")
    private SpannableStringBuilder doImageSpanString(List<String> imgUriList) {



        cntImages = 0;
        SpannableStringBuilder ssb = new SpannableStringBuilder(postContent);

        // Find the tag from the posting String.
        final String REGEX = "\\[image_\\d\\]";
        final Pattern p = Pattern.compile(REGEX);
        final Matcher m = p.matcher(ssb);

        // No tags exist and insert markup
        /*
        if(!m.lookingAt()) {
            log.i("no markup exists");
            for(int i = 0; i < imgCount; i++) {
                ssb.append("\n").append("image_").append(String.valueOf(i + 1));
                //ssb.append(System.getProperty("line.separator")); // Line Separator using System
            }

            //m = p.matcher(ssb);
        }
        */
        List<FutureTarget<Bitmap>> futureBitmapList = new ArrayList<>();
        for(String uriString : imgUriList) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    FutureTarget<Bitmap> futureBitmap = Glide.with(context.getApplicationContext())
                            .asBitmap()
                            .load(Uri.parse(uriString))
                            .submit();

                    futureBitmapList.add(futureBitmap);
                    log.i("futureBitmapList: %s", futureBitmapList.size());

                }
            }).start();

        }


        while(m.find()) {

            /*
            Glide.with(context.getApplicationContext()).asBitmap()
                    .load(Uri.parse(imgUriList.get(cntImages)))
                    .into(new CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {

                            ImageSpan imageSpan = new ImageSpan(context, resource);
                            log.i("image span: %s", imageSpan);

                            ssb.setSpan(imageSpan, m.start(), m.end(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                            //log.i("image span: %s, %s, %s", imageSpan, m.start(), m.end());
                            cntImages++;
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });

             */


        }

        return ssb;
    }


}
