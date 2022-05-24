package com.silverback.carman.adapters;

import static com.silverback.carman.BoardActivity.PAGING_COMMENT;
import static com.silverback.carman.fragments.BoardReadFragment.COMMENT_HEADER;
import static com.silverback.carman.fragments.BoardReadFragment.COMMENT_LIST;
import static com.silverback.carman.fragments.BoardReadFragment.EMPTY_VIEW;
import static com.silverback.carman.fragments.BoardReadFragment.POST_CONTENT;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.silverback.carman.R;
import com.silverback.carman.databinding.BoardReadCommentBinding;
import com.silverback.carman.databinding.BoardReadHeaderBinding;
import com.silverback.carman.databinding.BoardReadPostBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.CustomPostingObject;
import com.silverback.carman.utils.RecyclerDividerUtil;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BoardReadFeedAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  implements
        CompoundButton.OnCheckedChangeListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardReadFeedAdapter.class);
    private static int NUM_FEED = 4;

    private Context context;
    private final ReadFeedAdapterCallback callback;
    private final CustomPostingObject postingObj;
    private BoardReadPostBinding postBinding;
    private BoardReadHeaderBinding headerBinding;
    private BoardReadCommentBinding commentBinding;

    private final BoardCommentAdapter commentAdapter;

    public interface ReadFeedAdapterCallback {
        void showCommentLoadButton(int isVisible);
        void onCommentSwitchChanged(boolean isChecked);
    }

    public BoardReadFeedAdapter(CustomPostingObject postingObj, BoardCommentAdapter commentAdapter,
                                ReadFeedAdapterCallback callback) {
        this.postingObj = postingObj;
        this.callback = callback;
        this.commentAdapter = commentAdapter;
    }

    public static class PostContentViewHolder extends RecyclerView.ViewHolder {
        public PostContentViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class CommentHeaderViewHolder extends RecyclerView.ViewHolder {
        BoardReadHeaderBinding binding;
        public CommentHeaderViewHolder(View itemView) {
            super(itemView);
            this.binding = BoardReadHeaderBinding.bind(itemView);
        }

        TextView getCommentCountView() {
            return binding.headerCommentCnt;
        }
        SwitchCompat getCommentSwitch() { return binding.switchComment; }
    }

    public static class CommentListViewHolder extends RecyclerView.ViewHolder {
        BoardReadCommentBinding binding;
        public CommentListViewHolder(View itemView) {
            super(itemView);
            RecyclerView.LayoutParams layout = (RecyclerView.LayoutParams)itemView.getLayoutParams();
            layout.setMargins(0, 15, 0, 15);
            itemView.setLayoutParams(layout);
            this.binding = BoardReadCommentBinding.bind(itemView);
        }

        RecyclerView getCommentRecyclerView() {
            return binding.recyclerComments;
        }
    }

    public static class EmptyViewHolder extends RecyclerView.ViewHolder{
        public EmptyViewHolder(View itemView) { super(itemView);}
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        switch(viewType) {
            case POST_CONTENT:
                postBinding = BoardReadPostBinding.inflate(inflater, parent, false);
                return new PostContentViewHolder(postBinding.getRoot());

            case COMMENT_HEADER:
                headerBinding = BoardReadHeaderBinding.inflate(inflater, parent, false);
                headerBinding.switchComment.setOnCheckedChangeListener(this);
                return new CommentHeaderViewHolder(headerBinding.getRoot());

            case COMMENT_LIST:
                commentBinding = BoardReadCommentBinding.inflate(inflater, parent,false);
                LinearLayoutManager layout = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
                RecyclerDividerUtil divider = new RecyclerDividerUtil(Constants.DIVIDER_HEIGHT_POSTINGBOARD,
                        0, ContextCompat.getColor(context, R.color.recyclerDivider));
                commentBinding.recyclerComments.setHasFixedSize(false); //due to banner plugin
                commentBinding.recyclerComments.setLayoutManager(layout);
                commentBinding.recyclerComments.addItemDecoration(divider);
                commentBinding.recyclerComments.setItemAnimator(new DefaultItemAnimator());
                commentBinding.recyclerComments.setAdapter(commentAdapter);
                return new CommentListViewHolder(commentBinding.getRoot());

            case EMPTY_VIEW:
            default:
                int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40,
                        context.getResources().getDisplayMetrics());
                View emptyView = new View(context);
                emptyView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px));
                return new EmptyViewHolder(emptyView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch(position) {
            case POST_CONTENT:
                readPostContent(postingObj);
                break;
            case COMMENT_HEADER:
                final CommentHeaderViewHolder headerHolder = (CommentHeaderViewHolder)holder;
                headerHolder.getCommentCountView().setText(String.valueOf(postingObj.getCntComment()));
                break;
            case COMMENT_LIST:
                final CommentListViewHolder commentHolder = (CommentListViewHolder)holder;
                commentHolder.getCommentRecyclerView().setAdapter(commentAdapter);
                break;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, List<Object> payloads) {
        if(payloads.isEmpty()) super.onBindViewHolder(holder, position, payloads);
        else {
            // Update the number of comments
            if(payloads.get(0) instanceof Integer) {
                CommentHeaderViewHolder commentHolder = (CommentHeaderViewHolder)holder;
                final String cntComment = String.valueOf(payloads.get(0));
                commentHolder.getCommentCountView().setText(cntComment);
                // Turn the comment switch on to show the comment list.
                if(!commentHolder.getCommentSwitch().isChecked()) {
                    commentHolder.getCommentSwitch().setChecked(true);
                }

            }
        }

    }

    @Override
    public int getItemViewType(int position) {
        switch(position) {
            case 0: return POST_CONTENT;
            case 1: return COMMENT_HEADER;
            case 2: return COMMENT_LIST;
            case 3: return EMPTY_VIEW;
            default: return -1;
        }
    }

    @Override
    public int getItemCount() {
        return NUM_FEED;
    }

    // Implement OnCheckedChangeListener of the comment switch button in the header
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if(isChecked) {
            commentBinding.recyclerComments.setVisibility(View.VISIBLE);
            // Handle the visibility of the comment loading button in the bottom
            final String count = headerBinding.headerCommentCnt.getText().toString();
            int visible = (Integer.parseInt(count) > PAGING_COMMENT) ? View.VISIBLE : View.INVISIBLE;
            callback.showCommentLoadButton(visible);
        } else {
            commentBinding.recyclerComments.setVisibility(View.GONE);
            callback.onCommentSwitchChanged(false);
            commentAdapter.switchCommentReplyOff();
        }
    }


    private void readPostContent(CustomPostingObject obj) {
        // When an image is attached as the post writes, the line separator is supposed to put in at
        // before and after the image. That's why the regex contains the line separator in order to
        // get the right end position
        final String content = obj.getPostContent();
        final String REGEX_MARKUP = "\\[image_\\d]";
        final Matcher m = Pattern.compile(REGEX_MARKUP).matcher(content);
        final ConstraintLayout parent = postBinding.constraintPosting;

        int index = 0;
        int start = 0;
        int target;
        int prevImageId = 0;

        // Create LayoutParams using LinearLayout(RelativeLayout).LayoutParams, not using Constraint
        // Layout.LayoutParams. WHY?
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // If the content contains images, which means any markup(s) exists in the content, the content
        // is split into parts of texts and images and connected to ConstraintSet separately.
        while(m.find()) {
            // Check whether the content starts w/ text or image, which depends on the value of start.
            String paragraph = content.substring(start, m.start());
            TextView tv = new TextView(context);
            tv.setId(View.generateViewId());
            tv.setText(paragraph);
            parent.addView(tv, params);
            target = (start == 0) ? postBinding.guideline.getId() : prevImageId;

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(parent);
            tvSet.connect(tv.getId(), ConstraintSet.START, parent.getId(), ConstraintSet.START, 16);
            tvSet.connect(tv.getId(), ConstraintSet.END, parent.getId(), ConstraintSet.END, 16);
            tvSet.connect(tv.getId(), ConstraintSet.TOP, target, ConstraintSet.BOTTOM, 16);
            tvSet.applyTo(parent);

            ImageView imgView = new ImageView(context);
            imgView.setId(View.generateViewId());
            prevImageId = imgView.getId();
            parent.addView(imgView, params);

            ConstraintSet imgSet = new ConstraintSet();
            imgSet.clone(parent);
            imgSet.connect(imgView.getId(), ConstraintSet.START, parent.getId(), ConstraintSet.START, 16);
            imgSet.connect(imgView.getId(), ConstraintSet.END, parent.getId(), ConstraintSet.END, 16);
            imgSet.connect(imgView.getId(), ConstraintSet.TOP, tv.getId(), ConstraintSet.BOTTOM, 0);
            imgSet.applyTo(parent);

            // Consider to apply Glide thumbnail() method.
            Glide.with(context).asBitmap().load(obj.getPostImages().get(index))
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).fitCenter().into(imgView);

            start = m.end();
            index++;
        }

        // Coordinate the position b/w the last part, no matter what is imageview or textview in the content,
        // and the following recyclerview which shows any comment
        // Simple text w/o any image
        if(start == 0) {
            TextView simpleText = new TextView(context);
            simpleText.setId(View.generateViewId());
            simpleText.setText(content);
            parent.addView(simpleText, params);

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(parent);
            tvSet.connect(simpleText.getId(), ConstraintSet.START, parent.getId(), ConstraintSet.START, 16);
            tvSet.connect(simpleText.getId(), ConstraintSet.END, parent.getId(), ConstraintSet.END, 16);
            tvSet.connect(simpleText.getId(), ConstraintSet.TOP, postBinding.guideline.getId(), ConstraintSet.BOTTOM, 16);
            tvSet.applyTo(parent);

        // Text after an image
        } else if(start < content.length()) {
            String lastParagraph = content.substring(start);
            log.i("text after an image: %s", lastParagraph.length());
            TextView lastView = new TextView(context);
            lastView.setId(View.generateViewId());
            lastView.setText(lastParagraph);
            parent.addView(lastView, params);

            ConstraintSet tvSet = new ConstraintSet();
            tvSet.clone(parent);
            tvSet.connect(lastView.getId(), ConstraintSet.START, parent.getId(), ConstraintSet.START, 16);
            tvSet.connect(lastView.getId(), ConstraintSet.END, parent.getId(), ConstraintSet.END, 16);
            tvSet.connect(lastView.getId(), ConstraintSet.TOP, prevImageId, ConstraintSet.BOTTOM, 0);
            tvSet.applyTo(parent);

        // No text after the last image
        } else if(start == content.length()) {
            log.i("image positioned at the last");
            ConstraintSet imageSet = new ConstraintSet();
            imageSet.clone(parent);
            imageSet.connect(parent.getId(), ConstraintSet.TOP, prevImageId, ConstraintSet.BOTTOM, 0);
            imageSet.applyTo(parent);
        }
    }


}
