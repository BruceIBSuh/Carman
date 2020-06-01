package com.silverback.carman2.adapters;

import android.content.Context;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman2.R;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.ApplyImageResourceUtil;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.viewmodels.ImageViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * This RecyclerView.Adapter is to display posting items of
 */
public class BoardPostingAdapter extends RecyclerView.Adapter<BoardPostingAdapter.PostViewHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPostingAdapter.class);

    // Objects
    private Context context;
    private List<DocumentSnapshot> snapshotList;
    private OnRecyclerItemClickListener mListener;
    private SimpleDateFormat sdf;
    private ApplyImageResourceUtil imgUtil;
    private ImageViewModel imgModel;

    // Fields
    private int type;

    // Interface to notify BoardPagerFragment of pressing a recyclerview item.
    public interface OnRecyclerItemClickListener {
        void onPostItemClicked(DocumentSnapshot snapshot, int position);
    }

    // Constructor
    public BoardPostingAdapter(List<DocumentSnapshot> snapshots, OnRecyclerItemClickListener listener) {
        //super();
        mListener = listener;
        snapshotList = snapshots;
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        imgUtil = new ApplyImageResourceUtil(context);

        CardView cardView = (CardView) LayoutInflater.from(context)
                    .inflate(R.layout.cardview_board_post, parent, false);
        return new PostViewHolder(cardView);
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, final int position) {

        // Retreive an board item queried in and passed from BoardPagerFragment
        //DocumentSnapshot document = querySnapshot.getDocuments().get(position);
        //DocumentSnapshot document = snapshotList.get(position);
        final DocumentSnapshot snapshot = snapshotList.get(position);
        log.i("Snapshot: %s", snapshot.getString("post_title"));

        holder.tvPostTitle.setText(snapshot.getString("post_title"));
        holder.tvNumber.setText(String.valueOf(position + 1));
        holder.tvPostingDate.setText(sdf.format(snapshot.getDate("timestamp")));
        holder.tvUserName.setText(snapshot.getString("user_name"));
        holder.tvViewCount.setText(String.valueOf(snapshot.getLong("cnt_view")));
        holder.tvCommentCount.setText(String.valueOf(snapshot.getLong("cnt_comment")));

        // Set the user image
        if(!TextUtils.isEmpty(snapshot.getString("user_pic"))) {
            holder.bindUserImage(Uri.parse(snapshot.getString("user_pic")));
        } else {
            holder.bindUserImage(Uri.parse(Constants.imgPath + "ic_user_blank_white"));
        }

        // Set the thumbnail. When Glide applies, async issue occurs so that Glide.clear() should be
        // invoked and the imageview is made null to prevent images from having wrong positions.
        if(snapshot.get("post_images") != null) {
            String thumb = ((ArrayList<String>)snapshot.get("post_images")).get(0);
            holder.bindAttachedImage(Uri.parse(thumb));
        } else {
            Glide.with(context).clear(holder.imgAttached);
            holder.imgAttached.setImageDrawable(null);
        }

        // Set the listener for clicking the item with position
        holder.itemView.setOnClickListener(view -> {
            log.i("position: %s, %s", position, holder.getAdapterPosition());
            if(mListener != null) mListener.onPostItemClicked(snapshot, position);
        });

    }


    // Partial binding when the count is increased in terms of the view count and comment count.
    @Override
    public void onBindViewHolder(
            @NonNull PostViewHolder holder, int position, @NonNull List<Object> payloads) {

        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            holder.tvViewCount.setText(String.valueOf(payloads.get(0)));
            holder.tvCommentCount.setText(String.valueOf(payloads.get(1)));
        }
    }


    @Override
    public int getItemCount() {
        //return snapshotList == null ? 0 : snapshotList.size();
        return snapshotList.size();
    }

    // Guess this will be useful to apply plug-in ads.
    @Override
    public int getItemViewType(int position) {
        return -1;
    }

    // ViewHolders
    class PostViewHolder extends RecyclerView.ViewHolder {

        TextView tvPostTitle, tvUserName, tvNumber, tvViewCount, tvCommentCount, tvPostingDate;
        ImageView imgUser;
        ImageView imgAttached;

        PostViewHolder(CardView cardview){
            super(cardview);
            tvNumber = cardview.findViewById(R.id.tv_number);
            tvPostTitle = cardview.findViewById(R.id.tv_post_title);
            tvPostingDate = cardview.findViewById(R.id.tv_posting_date);
            tvUserName = cardview.findViewById(R.id.tv_post_owner);
            tvViewCount = cardview.findViewById(R.id.tv_count_views);
            tvCommentCount = cardview.findViewById(R.id.tv_count_comment);
            imgUser = cardview.findViewById(R.id.img_user);
            imgAttached = cardview.findViewById(R.id.img_attached);
        }

        void bindUserImage(Uri uri) {
            int size = Constants.ICON_SIZE_POSTING_LIST;
            imgUtil.applyGlideToImageView(uri, imgUser, size, size, true);
        }


        void bindAttachedImage(Uri uri) {
            int x = imgAttached.getWidth();
            int y = imgAttached.getHeight();
            imgUtil.applyGlideToImageView(uri, imgAttached, x, y, false);
        }

    }


}
