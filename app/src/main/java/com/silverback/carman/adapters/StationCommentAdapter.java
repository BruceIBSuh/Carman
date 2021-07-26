package com.silverback.carman.adapters;

import android.content.ClipData;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.R;
import com.silverback.carman.databinding.CardviewCommentsBinding;
import com.silverback.carman.databinding.InclStnmapInfoBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import org.w3c.dom.Document;

import java.util.List;

public class StationCommentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationCommentAdapter.class);

    // Define Types
    private final int TYPE_HEADER = 0;
    private final int TYPE_ITEM = 1;
    private final int TYPE_FOOTER = 2;


    // Objects
    private Context context;
    private final DocumentReference docRef;
    private final List<DocumentSnapshot> snapshotList;
    private InclStnmapInfoBinding stnBinding;
    private CardviewCommentsBinding commentBinding;

    public StationCommentAdapter(String stnId, List<DocumentSnapshot> snapshotList) {
        log.i("station id: %s", stnId);
        this.snapshotList = snapshotList;
        docRef = FirebaseFirestore.getInstance().collection("gas_station").document(stnId);
    }

    // ViewHolder
    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        public ItemViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        public HeaderViewHolder(View headerView){
            super(headerView);
        }
    }

//    public static class FooterViewHolder extends RecyclerView.ViewHolder {
//        public FooterViewHolder(View footerView){
//            super(footerView);
//        }
//    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        RecyclerView.ViewHolder holder;

        if(viewType == TYPE_HEADER) {
            //if(viewType == 0) {
            log.i("Station Info");
            stnBinding = InclStnmapInfoBinding.inflate(inflater, parent, false);
            View headerView = stnBinding.getRoot();
            holder = new HeaderViewHolder(headerView);

        } else {
            commentBinding = CardviewCommentsBinding.inflate(inflater, parent, false);
            View commentView = commentBinding.getRoot();
            holder = new ItemViewHolder(commentView);
        }

        return holder;

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        /*
        if(position == 0) dispStationInfo();
        else bindCommentToView(snapshotList.get(position));

        final String userId = snapshotList.get(position).getId();
        firestore.collection("users").document(userId).get().addOnSuccessListener(snapshot -> {
            if(snapshot != null && snapshot.exists()) {
                String strUserPic = snapshot.getString("user_pic");
                if (!TextUtils.isEmpty(strUserPic)) holder.bindImage(Uri.parse(strUserPic));
            }
        }).addOnFailureListener(e -> {});

        holder.bindToComments(snapshotList.get(position));

         */

        if(holder instanceof HeaderViewHolder) dispStationInfo();
        //else if(holder instanceof FooterViewHolder) dispStationInfo();
        else bindCommentToView(snapshotList.get(position - 1));
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(holder instanceof HeaderViewHolder) {
            dispStationInfo();
        } else if(holder instanceof ItemViewHolder) {
            //super.onBindViewHolder(holder, position, payloads);
            if (payloads.isEmpty()) {
                super.onBindViewHolder(holder, position, payloads);
            } else {
                for (Object obj : payloads) {
                    //drawable = (RoundedBitmapDrawable)obj;
                    //holder.imgProfile.setImageDrawable((RoundedBitmapDrawable)obj);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return snapshotList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0) return TYPE_HEADER;
        //else if(position == snapshotList.size() + 1) return TYPE_FOOTER;
        else return TYPE_ITEM;
    }

    // ViewHolder class
    /*
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
            log.i("viewholder");

        }

        @SuppressWarnings("ConstantConditions")
        void bindToComments(DocumentSnapshot snapshot) {
            log.i("document: %s", snapshot);
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

     */
    @SuppressWarnings("ConstantConditions")
    private void bindCommentToView(DocumentSnapshot doc) {
        log.i("bind comment");
        commentBinding.tvNickname.setText(doc.getString("name"));
        commentBinding.tvComments.setText(doc.getString("comments"));
        commentBinding.tvCommentTimestamp.setText(doc.getTimestamp("timestamp").toDate().toString());
        float rating = (float)doc.getLong("rating");
        commentBinding.rbCommentsRating.setRating(rating);
        /*
        final String userId = snapshotList.get(position).getId();
        firestore.collection("users").document(userId).get().addOnSuccessListener(snapshot -> {
            if(snapshot != null && snapshot.exists()) {
                String strUserPic = snapshot.getString("user_pic");
                if (!TextUtils.isEmpty(strUserPic)) {
                    Uri imgUri = Uri.parse(strUserPic);
                    RequestOptions options = new RequestOptions().fitCenter().override(50, 50).circleCrop();
                    Glide.with(context)
                            .asBitmap()
                            .load(imgUri)
                            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                            .apply(options)
                            .into(commentBinding.imgUserpic);
                }
            }
        }).addOnFailureListener(e -> {});

         */
    }

    @SuppressWarnings("ConstantConditions")
    private void dispStationInfo() throws NullPointerException {
        docRef.get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                DocumentSnapshot doc = task.getResult();
                stnBinding.tvStnName.setText(doc.get("stn_name", String.class));
                stnBinding.tvStnAddrs.setText(doc.get("new_addrs", String.class));
                boolean isWash = (boolean)doc.get("carwash");
                boolean isCvs = (boolean)doc.get("cvs");
                boolean isSvc = (boolean)doc.get("service");
                String wash = (isWash)?context.getString(R.string.map_value_ok):context.getString(R.string.map_value_not_ok);
                String cvs = (isCvs)?context.getString(R.string.map_value_yes):context.getString(R.string.map_value_no);
                String svc = (isSvc)?context.getString(R.string.map_value_yes):context.getString(R.string.map_value_no);
                stnBinding.tvWash.setText(wash);
                stnBinding.tvService.setText(cvs);
                stnBinding.tvCvs.setText(svc);
            }
        });




    }
}
