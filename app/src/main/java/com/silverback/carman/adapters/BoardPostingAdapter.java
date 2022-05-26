package com.silverback.carman.adapters;

import static com.silverback.carman.BoardActivity.AD_POSITION;
import static com.silverback.carman.BoardActivity.AD_VIEW_TYPE;
import static com.silverback.carman.BoardActivity.CONTENT_VIEW_TYPE;

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
import androidx.recyclerview.widget.AsyncListDiffer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.common.collect.Lists;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman.R;
import com.silverback.carman.databinding.BoardItemviewPostBinding;
import com.silverback.carman.fragments.BoardPagerFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.MultiTypePostingItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Callable;


public class BoardPostingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPostingAdapter.class);

    // Objects
    private Context context;
    private final FirebaseFirestore mDB;
    private final OnPostingAdapterCallback postingAdapterCallback;
    //private final AsyncListDiffer<DocumentSnapshot> mDiffer;
    private final AsyncListDiffer<MultiTypePostingItem> mDiffer;
    //private final AsyncListDiffer<BoardPagerFragment.MultiTypeItemList> mDiffer;
    private final SimpleDateFormat sdf;
    //private final List<DocumentSnapshot> snapshotList;
    private int category;//Must be refactored when the multi-type list applies.
    private int adNum;

    public interface OnPostingAdapterCallback {
        void onPostItemClicked(DocumentSnapshot snapshot, int position);
        //void onSubmitPostingListDone();
    }

    // Constructor
    public BoardPostingAdapter(OnPostingAdapterCallback callback) {
        super();
        this.postingAdapterCallback = callback;
        //snapshotList = snapshots;
        mDB = FirebaseFirestore.getInstance();
        mDiffer = new AsyncListDiffer<>(this, DIFF_CALLBACK_POST);
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
        adNum = 0;
    }

    // Update the adapter using AsyncListDiffer.ItemCallback<T> which uses the background thread.
    /*
    public void submitPostList(List<DocumentSnapshot> snapshotList) {
        mDiffer.submitList(Lists.newArrayList(snapshotList), postingAdapterCallback::onSubmitPostingListDone);
    }
     */
    public void submitPostList(List<MultiTypePostingItem> multiTypeList) {
        adNum = 0;//subtract the number
        mDiffer.submitList(Lists.newArrayList(multiTypeList)/*, postingAdapterCallback::onSubmitPostingListDone*/);
    }

    // Upadte the adapter using DiffUtil.Callback which uses the main thread.
    /*
    public void updatePostList(List<DocumentSnapshot> newPostList) {
        final BoardDiffUtilCallback diffUtilCallback = new BoardDiffUtilCallback(snapshotList, newPostList);
        final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(diffUtilCallback);
        if(snapshotList.size() > 0) snapshotList.clear();
        snapshotList.addAll(newPostList);

        diffResult.dispatchUpdatesTo(this);
    }
     */

    // Multi-type viewholder:PostViewHolder and AdViewHolder
    private static class PostViewHolder extends RecyclerView.ViewHolder {
        BoardItemviewPostBinding postBinding;
        PostViewHolder(View itemView) {
            super(itemView);
            postBinding = BoardItemviewPostBinding.bind(itemView);
        }

        TextView getPostTitleView() {return postBinding.tvPostTitle; }
        TextView getPostNumView() { return postBinding.tvNumber; }
        TextView getPostDateView() { return postBinding.tvPostingDate; }
        TextView getCommentCountView() { return postBinding.tvCountComment; }
        TextView getViewerCountView() { return postBinding.tvCountViews; }
        TextView getPostOwner() { return postBinding.tvPostOwner; }
        ImageView getAttachedImageView() { return postBinding.imgAttached; }
        ImageView getUserImageView() { return postBinding.imgUser; }
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
        log.i("onCreateViewHolder");
        this.context = viewGroup.getContext();

        switch(viewType) {
            case CONTENT_VIEW_TYPE:
                View view = LayoutInflater.from(context).inflate(R.layout.board_itemview_post, viewGroup, false);
                ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(view.getLayoutParams());
                params.setMargins(0, 0, 0, Constants.DIVIDER_HEIGHT_POSTINGBOARD);
                view.setLayoutParams(params);

                return new PostViewHolder(view);

            case AD_VIEW_TYPE:
            default:
                CardView bannerView = (CardView)LayoutInflater.from(context)
                        .inflate(R.layout.cardview_board_banner, viewGroup, false);
                return new AdViewHolder(bannerView);
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = mDiffer.getCurrentList().get(position).getViewType();
        switch(viewType) {
            case CONTENT_VIEW_TYPE:
                final PostViewHolder postHolder = (PostViewHolder)holder;
                MultiTypePostingItem multiTypeItem = mDiffer.getCurrentList().get(position);
                DocumentSnapshot snapshot = multiTypeItem.getSnapshot();

                //((PostViewHolder)holder).bindPostingItem(snapshot, position);
                // Calculate the index number by taking the plugin at the end of the pagination
                // into account.
                //int index = multiTypeItemList.get(position).getItemIndex();
                postHolder.getPostTitleView().setText(snapshot.getString("post_title"));
                postHolder.getPostNumView().setText(String.valueOf(position + 1 - adNum));

                // Refactor considered: day based format as like today, yesterday format, 2 days ago.
                if(snapshot.getDate("timestamp") != null) {
                    Date date = snapshot.getDate("timestamp");
                    postHolder.getPostDateView().setText(sdf.format(Objects.requireNonNull(date)));
                }

                postHolder.getViewerCountView().setText(String.valueOf(snapshot.getLong("cnt_view")));
                postHolder.getCommentCountView().setText(String.valueOf(snapshot.getLong("cnt_comment")));

                // Set the user name and image which should be fetched from the user collection to
                // be updated. The notification page has no user id.
                final String userId = snapshot.getString("user_id");
                if(TextUtils.isEmpty(userId)) {
                    postHolder.getPostOwner().setText(snapshot.getString("user_name"));
                    Glide.with(context).load(R.drawable.ic_user_blank_white).into(postHolder.getUserImageView());
                } else setPostUserProfile(userId, postHolder.getPostOwner(), postHolder.getUserImageView());

                // Set the attached image in the post, if any.
                if(snapshot.get("post_images") != null) {
                    List<?> postImageList = (List<?>)snapshot.get("post_images");
                    assert postImageList != null;
                    String thumbnail = ((String)postImageList.get(0));
                    final Uri uri = Uri.parse(thumbnail);
                    Glide.with(context).load(uri).fitCenter().into(postHolder.getAttachedImageView());
                } else Glide.with(context).clear(postHolder.getAttachedImageView());

                // Set the listener for clicking the item with position
                holder.itemView.setOnClickListener(view -> postingAdapterCallback.onPostItemClicked(
                        snapshot, holder.getBindingAdapterPosition()));
                break;

            case AD_VIEW_TYPE:
                log.i("Ad view type");
                adNum += 1;
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
            log.i("partial binding: %s", payloads);
            PostViewHolder postHolder = (PostViewHolder)holder;
            //for(Object payload : payloads) {
               if(payloads.get(0) instanceof Long) {
                    postHolder.getViewerCountView().setText(String.valueOf(payloads.get(0)));
               } else if(payloads.get(0) instanceof Integer) {
                   log.i("comment count update: %s", payloads.get(0));
                   postHolder.getCommentCountView().setText(String.valueOf(payloads.get(0)));
               }

               /*
                } else if(payloads.get(0) instanceof String) {
                   log.i("update the posting index number");
                   String bindingPos = String.valueOf(holder.getBindingAdapterPosition() + 1);
                   postHolder.getPostNumView().setText(bindingPos);

                }
                */
            //}
        }
    }

    @Override
    public long getItemId(int position) {
        //return position;
        //return snapshotList.get(position).hashCode();
        //return mDiffer.getCurrentList().get(position).hashCode();
        MultiTypePostingItem multiItem = mDiffer.getCurrentList().get(position);
        return (long)multiItem.getViewId();
    }


    @Override
    public int getItemViewType(int position) {
        return mDiffer.getCurrentList().get(position).getViewType();
    }

    @Override
    public int getItemCount() {
        return mDiffer.getCurrentList().size();
    }

    private void setPostUserProfile(String userId, TextView textView, ImageView imageView) {
        if(TextUtils.isEmpty(userId)) return;
        mDB.collection("users").document(userId).get().addOnSuccessListener(user -> {
            List<?> names = (List<?>)user.get("user_names");
            assert names != null;
            textView.setText((String)names.get(names.size() - 1));

            String image = user.getString("user_pic");
            Uri uri = (!TextUtils.isEmpty(image))? Uri.parse(image) : null;
            Glide.with(context).load(uri).placeholder(R.drawable.ic_user_blank_white).fitCenter()
                    .circleCrop().into(imageView);
        });
    }


    // AsyncListDiffer which helps compute the dfference b/w two lists via DiffUtil on a background
    // thread.
    /*
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

     */

    private static final DiffUtil.ItemCallback<MultiTypePostingItem> DIFF_CALLBACK_POST =
            new DiffUtil.ItemCallback<MultiTypePostingItem>() {
                @Override
                public boolean areItemsTheSame(@NonNull MultiTypePostingItem oldItem,
                                               @NonNull MultiTypePostingItem newItem) {
                    return oldItem.getViewId() == newItem.getViewId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull MultiTypePostingItem oldItem,
                                                  @NonNull MultiTypePostingItem newItem) {
                    if(oldItem.getViewType() == newItem.getViewType()) {
                        return oldItem.getSnapshot().getId().equals(newItem.getSnapshot().getId());
                    } else return false;
                }

                public Object getChangePayload(@NonNull MultiTypePostingItem oldItem,
                                               @NonNull MultiTypePostingItem newItem) {
                    return super.getChangePayload(oldItem, newItem);
                }
            };

    /*
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

     */
}
