package com.silverback.carman2.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

/**
 * Custom recyclerview
 */
public class PostingRecyclerView extends RecyclerView {

    private static final LoggingHelper log = LoggingHelperFactory.create(PostingRecyclerView.class);

    // Objects
    private View mEmptyView;

    public PostingRecyclerView(Context context) {
        super(context);
    }
    public PostingRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public PostingRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    private void initEmptyView() {
        if(mEmptyView != null) {
            int emptyView = (getAdapter() == null || getAdapter().getItemCount() == 0)? VISIBLE : GONE;
            int thisView = (getAdapter() == null || getAdapter().getItemCount() == 0)? GONE : VISIBLE;
            log.i("Visibility control: %s, %s", emptyView, thisView);

            mEmptyView.setVisibility(emptyView);
            PostingRecyclerView.this.setVisibility(thisView);
        }
    }

    AdapterDataObserver dataObserver = new AdapterDataObserver(){
        @Override
        public void onChanged(){
            log.i("onChanged");
            super.onChanged();
            initEmptyView();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            log.i("onItemRangeInserted");
            super.onItemRangeInserted(positionStart, itemCount);
            initEmptyView();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            log.i("onItemRangeRemoved");
            super.onItemRangeRemoved(positionStart, itemCount);
            initEmptyView();
        }
    };

    @Override
    public void setAdapter(Adapter adapter) {
        // The adapter fetched by getAdapter() should be one right before a new adapter is created.
        Adapter oldAdapter = getAdapter();
        super.setAdapter(adapter);

        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(dataObserver);
        }

        if (adapter != null) {
            adapter.registerAdapterDataObserver(dataObserver);
        }
    }

    public void setEmptyView(View view) {
        this.mEmptyView = view;
        initEmptyView();
    }
}
