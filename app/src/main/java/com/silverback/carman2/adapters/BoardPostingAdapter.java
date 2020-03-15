package com.silverback.carman2.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * This RecyclerView.Adapter is to display posting items of
 */
public class BoardPostingAdapter extends RecyclerView.Adapter<BoardPostingAdapter.BoardItemHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPostingAdapter.class);

    // Constants

    // Objects
    private Context context;
    //private List<DocumentSnapshot> snapshotList;
    private QuerySnapshot querySnapshot;
    private OnRecyclerItemClickListener mListener;
    private SimpleDateFormat sdf;

    // Interface for RecyclerView item click event
    public interface OnRecyclerItemClickListener {
        void onPostItemClicked(DocumentSnapshot snapshot, int position);
    }

    // Constructor
    /*
    public BoardPostingAdapter(List<DocumentSnapshot> snapshots, OnRecyclerItemClickListener listener) {
        super();

        log.i("BoarePostingAdapter");
        this.snapshotList = snapshots;
        mListener = listener;
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
    }

     */

    public BoardPostingAdapter(QuerySnapshot snapshots, OnRecyclerItemClickListener listener) {
        super();
        mListener = listener;
        querySnapshot = snapshots;
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
    }


    @NonNull
    @Override
    public BoardItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        CardView cardView = (CardView)LayoutInflater.from(parent.getContext())
                .inflate(R.layout.cardview_board_post, parent, false);


        return new BoardItemHolder(cardView);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(@NonNull BoardItemHolder holder, int position) {

        // Retreive an board item queried in and passed from BoardPagerFragment
        //DocumentSnapshot document = querySnapshot.getDocuments().get(position);
        //DocumentSnapshot document = snapshotList.get(position);
        DocumentSnapshot snapshot = querySnapshot.getDocuments().get(position);
        log.i("User Profile Pic: %s", snapshot.getString("user_pic"));

        holder.tvPostTitle.setText(snapshot.getString("post_title"));
        holder.tvNumber.setText(String.valueOf(position + 1));
        holder.tvPostingDate.setText(sdf.format(snapshot.getDate("timestamp")));
        holder.tvUserName.setText(snapshot.getString("user_name"));
        holder.tvViewCount.setText(String.valueOf(snapshot.getLong("cnt_view")));
        holder.tvCommentCount.setText(String.valueOf(snapshot.getLong("cnt_comment")));

        //
        if(!TextUtils.isEmpty(snapshot.getString("user_pic"))) {
            holder.bindProfileImage(Uri.parse(snapshot.getString("user_pic")));
        } else holder.bindProfileImage(null);

        List<String> imgList = (ArrayList<String>)snapshot.get("post_images");
        if(imgList != null && imgList.size() > 0) {
            holder.bindAttachedImage(Uri.parse(imgList.get(0)));
        }

        // Set the listener for clicking the item with position
        holder.itemView.setOnClickListener(view -> {
            if(mListener != null) mListener.onPostItemClicked(snapshot, position);
        });

    }


    @Override
    public void onBindViewHolder(@NonNull BoardItemHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            holder.tvViewCount.setText(String.valueOf(payloads.get(0)));
            holder.tvCommentCount.setText(String.valueOf(payloads.get(1)));

        }
    }


    @Override
    public int getItemCount() {
        return querySnapshot.size();
    }

    @Override
    public int getItemViewType(int position) {
        return -1;
    }


    // Set new snapshot list to the apdater for updating
    public void updatePostingAdapdter(QuerySnapshot snapshots) {
        this.querySnapshot = snapshots;
    }


    class BoardItemHolder extends RecyclerView.ViewHolder {

        TextView tvPostTitle, tvUserName, tvNumber, tvViewCount, tvCommentCount, tvPostingDate;
        ImageView imgProfile;
        ImageView imgAttached;

        BoardItemHolder(CardView cardview){
            super(cardview);
            tvNumber = cardview.findViewById(R.id.tv_number);
            tvPostTitle = cardview.findViewById(R.id.tv_post_title);
            tvPostingDate = cardview.findViewById(R.id.tv_posting_date);
            tvUserName = cardview.findViewById(R.id.tv_post_owner);
            tvViewCount = cardview.findViewById(R.id.tv_count_views);
            tvCommentCount = cardview.findViewById(R.id.tv_count_comment);
            imgProfile = cardview.findViewById(R.id.img_user);
            imgAttached = cardview.findViewById(R.id.img_attached);

        }

        // Null check of the uri shouldn't be needed b/c Glide handles it on its own.
        void bindProfileImage(Uri uri) {
            RequestOptions myOptions = new RequestOptions()
                    .fitCenter()
                    .override(Constants.ICON_SIZE_POSTING_LIST, Constants.ICON_SIZE_POSTING_LIST)
                    .circleCrop();

            if(uri == null) {
                Glide.with(context).load(R.drawable.ic_user_blank_white)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .apply(myOptions)
                        .into(imgProfile);
            } else {
                Glide.with(context).asBitmap()
                        //.placeholder(new ColorDrawable(Color.BLUE))
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                        .apply(myOptions)
                        .into(imgProfile);
            }
        }

        void bindAttachedImage(Uri uri) {
            Glide.with(context)
                    .asBitmap()
                    //.placeholder(new ColorDrawable(Color.BLUE))
                    .load(uri)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(imgAttached);
        }

    }

}
