package com.silverback.carman2.adapters;

import android.content.Context;
import android.net.Uri;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

public class CommentRecyclerAdapter extends RecyclerView.Adapter<CommentRecyclerAdapter.CommentListHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(CommentRecyclerAdapter.class);

    // Objects
    private Context context;
    private FirebaseFirestore firestore;
    private FirebaseStorage storage;
    //private LoadImageViewModel viewModel;
    private StorageReference storageRef;
    private List<DocumentSnapshot> snapshotList;
    //private RoundedBitmapDrawable drawable;

    // UIs
    //private TextView tvComment;
    //private ImageView imgProfile;

    // Constructor
    public CommentRecyclerAdapter(Context context, List<DocumentSnapshot> snapshotList) {
            //Context context, List<DocumentSnapshot> snapshotList, LoadImageViewModel viewModel){

        this.context = context;
        this.snapshotList = snapshotList;
        //this.viewModel = viewModel;
        //firestore = FirebaseFirestore.getInstance();
        //storage = FirebaseStorage.getInstance();
        //storageRef = storage.getReference();

    }


    @NonNull
    @Override
    public CommentListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_comments, parent, false);

        //imgProfile = cardView.findViewById(R.id.img_profile);

        return new CommentListHolder(cardView);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentListHolder holder, int position) {

        final String userid = snapshotList.get(position).getId();
        final String imagePath = "images/" + userid + "/profile.jpg";

        //storage.getReference().child("images/" + userid + "/profile.jpg").getDownloadUrl()
        FirebaseStorage.getInstance().getReference().child(imagePath).getDownloadUrl()
                .addOnSuccessListener(uri -> {
                    log.i("Image Uri: %s", uri);
                    //ThreadManager.downloadImageTask(context, position, uri.toString(), viewModel);
                    /*
                    RequestOptions myOptions = new RequestOptions().fitCenter().override(50, 50).circleCrop();
                    Glide.with(context)
                            .asBitmap()
                            .load(uri)
                            .apply(myOptions)
                            .into(imgProfile);
                    */
                    holder.bindImage(uri);


                }).addOnFailureListener(e -> log.e("Download failed"));

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
                holder.imgProfile.setImageDrawable((RoundedBitmapDrawable)obj);
            }
        }

         */
    }

    @Override
    public int getItemCount() {
        //log.i("snapshotlist: %s", snapshotList.size());
        return snapshotList.size();
    }

    // ViewHolder class
    class CommentListHolder extends RecyclerView.ViewHolder {

        TextView tvNickname, tvComments, tvTimestamp;
        ImageView imgProfile;
        RatingBar ratingBar;

        CommentListHolder(CardView cardView) {
            super(cardView);
            imgProfile = cardView.findViewById(R.id.img_profile);
            tvNickname = cardView.findViewById(R.id.tv_nickname);
            tvComments = cardView.findViewById(R.id.tv_comments);
            tvTimestamp = cardView.findViewById(R.id.tv_timestamp);
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
