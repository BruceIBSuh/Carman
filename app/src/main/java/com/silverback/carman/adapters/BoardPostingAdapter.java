package com.silverback.carman.adapters;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.SparseLongArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
    private final int CONTENT_VIEW_TYPE = 1;
    private final int AD_VIEW_TYPE = 2;

    // Objects
    private Context context;
    private final List<DocumentSnapshot> snapshotList;
    private final OnRecyclerItemClickListener mListener;
    private final SimpleDateFormat sdf;
    private ApplyImageResourceUtil imgUtil;

    private BoardRecyclerviewPostBinding postBinding;

    // Interface to notify BoardPagerFragment of pressing a recyclerview item.
    public interface OnRecyclerItemClickListener {
        void onPostItemClicked(DocumentSnapshot snapshot, int position);
    }

    //public MainContentNotificationBinding binding; //DataBiding in JetPack
    private static class PostViewHolder extends RecyclerView.ViewHolder {
        PostViewHolder(View itemView) {
            super(itemView);
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(itemView.getLayoutParams());
            params.setMargins(0, 0, 0, Constants.DIVIDER_HEIGHT_POSTINGBOARD);
            itemView.setLayoutParams(params);
        }
    }

    private static class AdViewHolder extends RecyclerView.ViewHolder {
        AdViewHolder(View view) {
            super(view);
        }
    }

    // Constructor
    public BoardPostingAdapter(List<DocumentSnapshot> snapshots, OnRecyclerItemClickListener listener) {
        super();
        mListener = listener;
        snapshotList = snapshots;
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
        //setHasStableIds(true);
    }

    // Create 2 difference viewholders, one of which is to display the general post content and
    // the other is to display the plug-in content which will be used for
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        this.context = viewGroup.getContext();
        imgUtil = new ApplyImageResourceUtil(context);
        LayoutInflater inflater = LayoutInflater.from(context);

        switch(viewType) {
            case CONTENT_VIEW_TYPE:
                postBinding = BoardRecyclerviewPostBinding.inflate(inflater, viewGroup, false);
                return new PostViewHolder(postBinding.getRoot());

            case AD_VIEW_TYPE:
            default:
                CardView bannerView = (CardView)LayoutInflater.from(context)
                        .inflate(R.layout.cardview_board_banner, viewGroup, false);
                return new AdViewHolder(bannerView);

        }

    }

    //@SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        int AD_POSITION = 20; // Temp code
        log.i("position: %s", position);
        switch(viewType) {
            case CONTENT_VIEW_TYPE:
                final DocumentSnapshot snapshot = snapshotList.get(position);
                // Calculate the index number by taking the plugin at the end of the pagination
                // into account.

                int offset = (position / AD_POSITION) - 1;
                //int index = (AD_POSITION > position) ? position + 1 : position - offset;
                int index = position + 1;


                // Timestamp consists of seconds and nanoseconds. To format it as date, get the
                // seconds using Timestamp.getSeconds() and apply SimpleDateFormat.format() despite
                // a loss when it should be muliplied by 1000 for making it milliseconds.

                // Refactor considered: day based format as like today, yesterday format, 2 days ago.
                //Timestamp timeStamp = (Timestamp)snapshot.get("timestamp");
                //long postingTime = timeStamp.getSeconds() * 1000;
                //log.i("timestamp: %s", postingTime);
                postBinding.tvPostTitle.setText(snapshot.getString("post_title"));
                postBinding.tvNumber.setText(String.valueOf(index));

                // Some posts may have weird data type. This condition should be removed once the
                // board is cleared out.
                if(snapshot.getDate("timestamp") != null) {
                    Date date = snapshot.getDate("timestamp");
                    postBinding.tvPostingDate.setText(sdf.format(date));
                }

                postBinding.tvPostOwner.setText(snapshot.getString("user_name"));
                postBinding.tvCountViews.setText(String.valueOf(snapshot.getLong("cnt_view")));
                postBinding.tvCountComment.setText(String.valueOf(snapshot.getLong("cnt_comment")));

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
                    Glide.with(context).clear(postBinding.imgAttached);
                    postBinding.imgAttached.setImageDrawable(null);
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

        if(payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
        else  {
            log.i("payloads exists");
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
        DocumentSnapshot snapshot =  snapshotList.get(position);
        return snapshot.hashCode();
    }

    @Override
    public int getItemCount() {
        return snapshotList.size();
    }

    // Guess this will be useful to apply plug-in ads.
    @Override
    public int getItemViewType(int position) {
        //return (position > 0 && position % AD_POSITION == 0) ? AD_VIEW_TYPE : CONTENT_VIEW_TYPE;
        return 1;
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
