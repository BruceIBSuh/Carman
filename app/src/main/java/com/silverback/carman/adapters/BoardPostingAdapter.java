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
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.common.collect.Lists;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;
import com.silverback.carman.BoardActivity;
import com.silverback.carman.R;
import com.silverback.carman.databinding.BoardRecyclerviewPostBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    private Context context;
    private final FirebaseFirestore mDB;
    private final OnPostingAdapterListener postingAdapterListener;
    private final AsyncListDiffer<DocumentSnapshot> mDiffer;
    private final SimpleDateFormat sdf;
    private BoardRecyclerviewPostBinding postBinding;
    private final List<DocumentSnapshot> snapshotList;
    private int category = 0;

    public interface OnPostingAdapterListener {
        void onPostItemClicked(DocumentSnapshot snapshot, int position);
        void onSubmitListDone();
    }

    // Constructor
    public BoardPostingAdapter(List<DocumentSnapshot> snapshots, OnPostingAdapterListener listener) {
        super();
        this.postingAdapterListener = listener;
        snapshotList = snapshots;
        //snapshotList = new ArrayList<>();
        mDB = FirebaseFirestore.getInstance();
        mDiffer = new AsyncListDiffer<>(this, DIFF_CALLBACK_POST);
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
    }

    // Update the adapter using AsyncListDiffer.ItemCallback<T> which uses the background thread.
    public void submitPostList(List<DocumentSnapshot> snapshotList) {
        mDiffer.submitList(Lists.newArrayList(snapshotList), postingAdapterListener::onSubmitListDone);
    }

    // Upadte the adapter using DiffUtil.Callback which uses the main thread.
    public void updatePostList(List<DocumentSnapshot> newPostList) {
        final BoardDiffUtilCallback diffUtilCallback = new BoardDiffUtilCallback(snapshotList, newPostList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffUtilCallback);
        if(snapshotList.size() > 0) snapshotList.clear();
        snapshotList.addAll(newPostList);
        log.i("snapshotList: %s", this.snapshotList.size());

        diffResult.dispatchUpdatesTo(this);
    }

    // Multi-type viewholder:PostViewHolder and AdViewHolder
    private static class PostViewHolder extends RecyclerView.ViewHolder {
        final BoardRecyclerviewPostBinding postBinding;
        final TextView tvTitle;
        final TextView tvNumber;
        final TextView tvPostingDate;
        final TextView tvPostingOwner;
        final TextView tvCountViews;
        final TextView tvCountComment;
        final ImageView userImage;
        final ImageView imgAttached;

        PostViewHolder(BoardRecyclerviewPostBinding binding) {
            super(binding.getRoot());
            this.postBinding = binding;

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
            userImage = binding.imgUser;
        }
    }

    private static class AdViewHolder extends RecyclerView.ViewHolder {
        public AdViewHolder(View view) {
            super(view);
        }
    }



    // Create 2 difference viewholders, one of which is to display the general post content and
    // the other is to display the plug-in content which will be used for
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        this.context = viewGroup.getContext();
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
                //DocumentSnapshot snapshot = snapshotList.get(position);
                //DocumentSnapshot snapshot = multiTypeItemList.get(position).getItemSnapshot();
                DocumentSnapshot snapshot = mDiffer.getCurrentList().get(position);
                // Calculate the index number by taking the plugin at the end of the pagination
                // into account.
                //int index = multiTypeItemList.get(position).getItemIndex();
                postHolder.tvTitle.setText(snapshot.getString("post_title"));
                postHolder.tvNumber.setText(String.valueOf(position + 1));

                // Refactor considered: day based format as like today, yesterday format, 2 days ago.
                if(snapshot.getDate("timestamp") != null) {
                    Date date = snapshot.getDate("timestamp");
                    postHolder.tvPostingDate.setText(sdf.format(Objects.requireNonNull(date)));
                }

                //postHolder.tvPostingOwner.setText(snapshot.getString("user_name"));
                postHolder.tvCountViews.setText(String.valueOf(snapshot.getLong("cnt_view")));
                postHolder.tvCountComment.setText(String.valueOf(snapshot.getLong("cnt_comment")));

                // Set the user name and image which should be fetched from the user collection to enable
                // it to be updated.
                final String userId = snapshot.getString("user_id");
                assert userId != null;
                ((BoardActivity)context).setUserProfile(userId, postHolder.tvPostingOwner, postHolder.userImage);

                // Set the attached image in the post, if any.
                if(snapshot.get("post_images") != null) {
                    List<?> postImageList = (List<?>)snapshot.get("post_images");
                    assert postImageList != null;
                    String thumbnail = ((String)postImageList.get(0));
                    final Uri uri = Uri.parse(thumbnail);
                    Glide.with(context).load(uri).fitCenter().into(postHolder.imgAttached);

                } else Glide.with(context).clear(postHolder.imgAttached);


                // Set the listener for clicking the item with position
                holder.itemView.setOnClickListener(view -> postingAdapterListener.onPostItemClicked (
                        snapshot, holder.getBindingAdapterPosition()));

                break;

            case AD_VIEW_TYPE:
                break;
        }
    }

    // Do a partial binding for updating either the view count or the comment count, which is passed
    // with payloads. No payload performs the full binding.
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads){
        //holder.setIsRecyclable(false);
        if(payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
        else {
            for(Object payload : payloads) {
                if(payload instanceof Long) {
                    postBinding.tvCountViews.setText(String.valueOf(payload));

                } else if(payload instanceof SparseLongArray) {
                    SparseLongArray sparseArray = (SparseLongArray)payload;
                    postBinding.tvCountComment.setText(String.valueOf(sparseArray.valueAt(0)));

                } else if(payload instanceof String) {
                    String bindingPos = String.valueOf(holder.getBindingAdapterPosition() + 1);
                    ((PostViewHolder) holder).tvNumber.setText(bindingPos);
                }

            }
        }
    }

    @Override
    public long getItemId(int position) {
        //return position;
        //return snapshotList.get(position).hashCode();
        return mDiffer.getCurrentList().get(position).hashCode();
    }

    /*
    @Override
    public int getItemViewType(int position) {
        switch(snapshotList.get(position).getViewType()){
            case 0: return CONTENT_VIEW_TYPE;
            case 1: return AD_VIEW_TYPE;
            default: return -1;
        }
        //category = multiTypeItemList.get(position).getViewType();
        return position;
    }

     */

    @Override
    public int getItemCount() {
        //return snapshotList.size();
        return mDiffer.getCurrentList().size();
        //return multiTypeItemList.size();

    }


    // AsyncListDiffer which helps compute the dfference b/w two lists via DiffUtil on a background
    // thread.
    private static final DiffUtil.ItemCallback<DocumentSnapshot> DIFF_CALLBACK_POST =
        new DiffUtil.ItemCallback<DocumentSnapshot>() {
            @Override
            public boolean areItemsTheSame(@NonNull DocumentSnapshot oldItem, @NonNull DocumentSnapshot newItem) {
                return oldItem.getId().equals(newItem.getId());
            }
            @Override
            public boolean areContentsTheSame(@NonNull DocumentSnapshot oldItem, @NonNull DocumentSnapshot newItem) {
                return oldItem.equals(newItem);
            }
            public Object getChangePayload(@NonNull DocumentSnapshot oldItem, @NonNull DocumentSnapshot newItem) {
                return super.getChangePayload(oldItem, newItem);
            }
        };


    private static class BoardDiffUtilCallback extends DiffUtil.Callback {
        List<DocumentSnapshot> oldList, newList;
        public BoardDiffUtilCallback(List<DocumentSnapshot> oldList, List<DocumentSnapshot> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }
        @Override
        public int getOldListSize() {
            return oldList.size();
        }
        @Override
        public int getNewListSize() {
            return newList.size();
        }
        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
        }
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition) == newList.get(newItemPosition);
        }
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            return super.getChangePayload(oldItemPosition, newItemPosition);
        }
    }
}
