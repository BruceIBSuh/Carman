package com.silverback.carman2.adapters;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

public class StationCommentAdapter extends RecyclerView.Adapter<StationCommentAdapter.CommentListHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationCommentAdapter.class);

    // Objects
    private Context context;
    private FirebaseFirestore firestore;
    private List<DocumentSnapshot> snapshotList;

    // Constructor
    public StationCommentAdapter(List<DocumentSnapshot> snapshotList) {
        this.snapshotList = snapshotList;
        firestore = FirebaseFirestore.getInstance();
    }


    @NonNull
    @Override
    public CommentListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();

        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_comments, parent, false);

        return new CommentListHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentListHolder holder, int position) {

        final String userId = snapshotList.get(position).getId();
        firestore.collection("users").document(userId).get().addOnSuccessListener(snapshot -> {
            if(snapshot != null && snapshot.exists()) {
                String strUserPic = snapshot.getString("user_pic");
                if (!TextUtils.isEmpty(strUserPic)) holder.bindImage(Uri.parse(strUserPic));
            }
        }).addOnFailureListener(e -> {});

        holder.bindToComments(snapshotList.get(position));
    }

    @Override
    public void onBindViewHolder(@NonNull CommentListHolder holder, int position, @NonNull List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);

        /*
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);

        }else{
            for(Object obj : payloads) {
                log.i("Partial Binding");
                drawable = (RoundedBitmapDrawable)obj;
                holder.imgUser.setImageDrawable((RoundedBitmapDrawable)obj);
            }
        }

         */
    }

    @Override
    public int getItemCount() {
        return snapshotList.size();
    }

    // ViewHolder class
    class CommentListHolder extends RecyclerView.ViewHolder {

        TextView tvNickname, tvComments, tvTimestamp;
        ImageView imgProfile;
        RatingBar ratingBar;

        CommentListHolder(CardView cardView) {
            super(cardView);
            imgProfile = cardView.findViewById(R.id.img_userpic);
            tvNickname = cardView.findViewById(R.id.tv_nickname);
            tvComments = cardView.findViewById(R.id.tv_comments);
            tvTimestamp = cardView.findViewById(R.id.tv_comment_timestamp);
            ratingBar = cardView.findViewById(R.id.rb_comments_rating);
        }

        @SuppressWarnings("ConstantConditions")
        void bindToComments(DocumentSnapshot snapshot) {
            tvNickname.setText(snapshot.getString("name"));
            tvComments.setText(snapshot.getString("comments"));
            tvTimestamp.setText(snapshot.getTimestamp("timestamp").toDate().toString());

            float rating = (float)snapshot.getLong("rating");
            ratingBar.setRating(rating);
        }

        // Required to make caching in Glide!!
        void bindImage(Uri uri) {
            // Apply Glide with options.
            RequestOptions myOptions = new RequestOptions().fitCenter().override(50, 50).circleCrop();
            Glide.with(context)
                    .asBitmap()
                    .load(uri)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .apply(myOptions)
                    .into(imgProfile);

        }
    }



}