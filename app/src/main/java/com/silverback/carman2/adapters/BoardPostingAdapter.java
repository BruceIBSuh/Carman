package com.silverback.carman2.adapters;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
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
public class BoardPostingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPostingAdapter.class);

    // Constants
    private final int CONTENT_VIEW_TYPE = 1;
    private final int AD_VIEW_TYPE = 2;
    private final int AD_POSITION = 20;

    // Objects
    private Context context;
    private List<DocumentSnapshot> snapshotList;
    private OnRecyclerItemClickListener mListener;
    private SimpleDateFormat sdf;
    private ApplyImageResourceUtil imgUtil;
    private ImageViewModel imgModel;

    // Fields
    private int index;

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
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        this.context = viewGroup.getContext();
        imgUtil = new ApplyImageResourceUtil(context);

        switch(viewType) {
            case CONTENT_VIEW_TYPE:
                CardView postView = (CardView) LayoutInflater.from(context)
                        .inflate(R.layout.cardview_board_post, viewGroup, false);
                return new PostViewHolder(postView);

            case AD_VIEW_TYPE:

            default:
                CardView bannerView = (CardView)LayoutInflater.from(context)
                        .inflate(R.layout.cardview_board_banner, viewGroup, false);
                return new AdViewHolder(bannerView);


        }

    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        index = position + 1;

        switch(viewType) {
            case CONTENT_VIEW_TYPE:
                final DocumentSnapshot snapshot = snapshotList.get(position);

                // Calculate the index number by taking the plugin at the end of the pagination
                // into account.
                int offset = (position / AD_POSITION) - 1;
                int index = (AD_POSITION > position) ? position + 1 : position - offset;

                PostViewHolder postHolder = (PostViewHolder)holder;
                postHolder.tvPostTitle.setText(snapshot.getString("post_title"));
                //postHolder.tvNumber.setText(String.valueOf(position + 1));
                postHolder.tvNumber.setText(String.valueOf(index));
                postHolder.tvPostingDate.setText(sdf.format(snapshot.getDate("timestamp")));
                postHolder.tvUserName.setText(snapshot.getString("user_name"));
                postHolder.tvViewCount.setText(String.valueOf(snapshot.getLong("cnt_view")));
                postHolder.tvCommentCount.setText(String.valueOf(snapshot.getLong("cnt_comment")));

                // Set the user image
                if(!TextUtils.isEmpty(snapshot.getString("user_pic"))) {
                    postHolder.bindUserImage(Uri.parse(snapshot.getString("user_pic")));
                } else {
                    postHolder.bindUserImage(Uri.parse(Constants.imgPath + "ic_user_blank_white"));
                }

                // Set the thumbnail. When Glide applies, async issue occurs so that Glide.clear() should be
                // invoked and the imageview is made null to prevent images from having wrong positions.
                if(snapshot.get("post_images") != null) {
                    String thumb = ((ArrayList<String>)snapshot.get("post_images")).get(0);
                    postHolder.bindAttachedImage(Uri.parse(thumb));
                } else {
                    Glide.with(context).clear(postHolder.imgAttached);
                    postHolder.imgAttached.setImageDrawable(null);
                }

                // Set the listener for clicking the item with position
                holder.itemView.setOnClickListener(view -> {
                    log.i("position: %s, %s", position, holder.getAdapterPosition());
                    if(mListener != null) mListener.onPostItemClicked(snapshot, position);
                });

                break;

            case AD_VIEW_TYPE:
                break;
        }
    }

    // Partial binding when the count is increased in terms of the view count and comment count.
    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {

        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            ((PostViewHolder)holder).tvViewCount.setText(String.valueOf(payloads.get(0)));
            ((PostViewHolder)holder).tvCommentCount.setText(String.valueOf(payloads.get(1)));
        }
    }


    @Override
    public int getItemCount() {
        return snapshotList.size();
    }

    // Guess this will be useful to apply plug-in ads.
    @Override
    public int getItemViewType(int position) {
        return (position > 0 && position % AD_POSITION == 0) ? AD_VIEW_TYPE : CONTENT_VIEW_TYPE;
        //return -1;
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

    class AdViewHolder extends RecyclerView.ViewHolder {

        AdViewHolder(View view) {
            super(view);
        }
    }




}
