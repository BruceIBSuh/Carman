package com.silverback.carman.adapters;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.SparseLongArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.DocumentSnapshot;
import com.silverback.carman.BoardActivity;
import com.silverback.carman.R;
import com.silverback.carman.databinding.BoardRecyclerviewPostBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


public class BoardPostingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPostingAdapter.class);

    // Constants
    private final int CONTENT_VIEW_TYPE = 0;
    private final int AD_VIEW_TYPE = 1;

    // Objects
    //private final List<BoardPagerFragment.MultiTypeItem> multiTypeItemList;

    private Context context;
    private final OnRecyclerItemClickListener mListener;
    private final SimpleDateFormat sdf;
    private ApplyImageResourceUtil imgUtil;
    private BoardRecyclerviewPostBinding postBinding;
    private final List<DocumentSnapshot> snapshotList;
    private int category;

    // Interface to notify BoardPagerFragment of pressing a recyclerview item.
    public interface OnRecyclerItemClickListener {
        void onPostItemClicked(DocumentSnapshot snapshot, int position);
    }

    // Multi-type viewholder:PostViewHolder and AdViewHolder
    private static class PostViewHolder extends RecyclerView.ViewHolder {
        final TextView tvTitle;
        final TextView tvNumber;
        final TextView tvPostingDate;
        final TextView tvPostingOwner;
        final TextView tvCountViews;
        final TextView tvCountComment;
        final ImageView imgAttached;

        PostViewHolder(BoardRecyclerviewPostBinding binding) {
            super(binding.getRoot());
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(itemView.getLayoutParams());
            params.setMargins(0, 0, 0, Constants.DIVIDER_HEIGHT_POSTINGBOARD);
            binding.getRoot().setLayoutParams(params);

            tvTitle = binding.tvPostTitle;
            tvNumber = binding.tvNumber;
            tvPostingDate = binding.tvPostingDate;
            tvPostingOwner = binding.tvPostOwner;
            tvCountViews = binding.tvCountViews;
            tvCountComment = binding.tvCountComment;
            imgAttached = binding.imgAttached;
        }
    }

    private static class AdViewHolder extends RecyclerView.ViewHolder {
        public AdViewHolder(View view) {
            super(view);
        }
    }

    // Constructor
    public BoardPostingAdapter(List<DocumentSnapshot> snapshots, OnRecyclerItemClickListener listener) {
    //public BoardPostingAdapter(List<BoardPagerFragment.MultiTypeItem> multiTypeItemList, OnRecyclerItemClickListener listener) {
        super();
        mListener = listener;
        snapshotList = snapshots;
        //this.multiTypeItemList = multiTypeItemList;
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
        setHasStableIds(true);
    }

    // Create 2 difference viewholders, one of which is to display the general post content and
    // the other is to display the plug-in content which will be used for
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        this.context = viewGroup.getContext();
        imgUtil = new ApplyImageResourceUtil(context);
        switch(category) {
            case CONTENT_VIEW_TYPE:
                postBinding = BoardRecyclerviewPostBinding.inflate(
                        LayoutInflater.from(context), viewGroup, false);
                return new PostViewHolder(postBinding);

            case AD_VIEW_TYPE:
            default:
                CardView bannerView = (CardView)LayoutInflater.from(context)
                        .inflate(R.layout.cardview_board_banner, viewGroup, false);
                return new AdViewHolder(bannerView);

        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        switch(category) {
            case CONTENT_VIEW_TYPE:
                PostViewHolder postHolder = (PostViewHolder)holder;
                DocumentSnapshot snapshot = snapshotList.get(position);
                //DocumentSnapshot snapshot = multiTypeItemList.get(position).getItemSnapshot();
                // Calculate the index number by taking the plugin at the end of the pagination
                // into account.
                //int index = multiTypeItemList.get(position).getItemIndex();
                postHolder.tvTitle.setText(snapshot.getString("post_title"));
                //postHolder.tvNumber.setText(String.valueOf(index + 1));
                postHolder.tvNumber.setText(String.valueOf(position + 1));

                // Refactor considered: day based format as like today, yesterday format, 2 days ago.
                //Timestamp timeStamp = (Timestamp)snapshot.get("timestamp");
                //long postingTime = timeStamp.getSeconds() * 1000;
                //log.i("timestamp: %s", postingTime);
                if(snapshot.getDate("timestamp") != null) {
                    Date date = snapshot.getDate("timestamp");
                    postHolder.tvPostingDate.setText(sdf.format(Objects.requireNonNull(date)));
                }

                postHolder.tvPostingOwner.setText(snapshot.getString("user_name"));
                postHolder.tvCountViews.setText(String.valueOf(snapshot.getLong("cnt_view")));
                postHolder.tvCountComment.setText(String.valueOf(snapshot.getLong("cnt_comment")));

                // Set the user image
                if(!TextUtils.isEmpty(snapshot.getString("user_pic"))) {
                    bindUserImage(Uri.parse(snapshot.getString("user_pic")));
                } else {
                    bindUserImage(Uri.parse(Constants.imgPath + "ic_user_blank_white"));
                }

                // Set the thumbnail. When Glide applies, async issue occurs so that Glide.clear() should be
                // invoked and the imageview is made null to prevent images from having wrong positions.
                if(snapshot.get("post_images") != null) {
                    BoardActivity.PostImages objImages = snapshot.toObject(BoardActivity.PostImages.class);
                    List<String> postImages = Objects.requireNonNull(objImages).getPostImages();
                    String thumbnail = postImages.get(0);
                    if(!TextUtils.isEmpty(thumbnail)) bindAttachedImage(Uri.parse(thumbnail));

                } else {
                    Glide.with(context).clear(postHolder.imgAttached);
                    postHolder.imgAttached.setImageDrawable(null);
                }

                // Set the listener for clicking the item with position
                holder.itemView.setOnClickListener(view -> {
                    if(mListener != null) mListener.onPostItemClicked(snapshot, position);
                });

                break;

            case AD_VIEW_TYPE:
                break;
        }
    }

    // Do a partial binding for updating either the view count or the comment count, which is passed
    // with payloads. No payload performs the full binding.
    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads){
        //holder.setIsRecyclable(false);
        if(payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
        else {
            for(Object payload : payloads) {
                if(payload instanceof Long) {
                    postBinding.tvCountViews.setText(String.valueOf(payload));
                } else if(payload instanceof SparseLongArray) {
                    SparseLongArray sparseArray = (SparseLongArray)payload;
                    postBinding.tvCountComment.setText(String.valueOf(sparseArray.valueAt(0)));
                }
            }
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
        //return snapshotList.get(position).hashCode();
    }

    // Guess this will be useful to apply plug-in ads.
    @Override
    public int getItemViewType(int position) {
        /* Seems not working. return value should be position, stragne, though.
        switch(multiTypeItemList.get(position).getViewType()){
            case 0: return CONTENT_VIEW_TYPE;
            case 1: return AD_VIEW_TYPE;
            default: return -1;

        }
        */
        //category = multiTypeItemList.get(position).getViewType();
        return position;
    }

    @Override
    public int getItemCount() {
        return snapshotList.size();
        //return multiTypeItemList.size();
    }

    void bindUserImage(Uri uri) {
        int size = Constants.ICON_SIZE_POSTING_LIST;
        imgUtil.applyGlideToImageView(uri, postBinding.imgUser, size, size, true);
    }

    void bindAttachedImage(Uri uri) {
        int x = postBinding.imgAttached.getWidth();
        int y = postBinding.imgAttached.getHeight();
        imgUtil.applyGlideToImageView(uri, postBinding.imgAttached, x, y, false);
    }
}
