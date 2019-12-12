package com.silverback.carman2.utils;

import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class PaginationUtil extends RecyclerView.OnScrollListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(PaginationUtil.class);

    // Objects
    private CollectionReference colRef;
    private QuerySnapshot querySnapshot;
    private OnPaginationListener mListener;

    // Fields
    private boolean isScrolling;
    private boolean isLastItem;
    private int pagingLimit;
    private String field;

    // Interface w/ BoardPagerFragment
    public interface OnPaginationListener {
        void setQueryStart(boolean b);
        void setNextQueryComplete(QuerySnapshot querySnapshot);
    }

    // Constructor

    public PaginationUtil(CollectionReference colref, int limit) {
        this.colRef = colref;
        pagingLimit = limit;
    }

    // Method for implementing the inteface in BoardPagerFragment, which notifies the caller of
    // having QuerySnapshot retrieved.
    public void setOnPaginationListener(OnPaginationListener listener) {
        mListener = listener;
    }

    public void setQuerySnapshot(QuerySnapshot querySnapshot, final String field) {
        this.querySnapshot = querySnapshot;
        this.field = field;
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            isScrolling = true;
        }
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        LinearLayoutManager layoutManager = (LinearLayoutManager)recyclerView.getLayoutManager();
        if(layoutManager == null) return;

        int firstItemPos = layoutManager.findFirstVisibleItemPosition();
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        log.i("Item Status by LayoutManager: %s, %s, %s", firstItemPos, visibleItemCount, totalItemCount);

        if(isScrolling && (firstItemPos + visibleItemCount == totalItemCount) && !isLastItem) {
            mListener.setQueryStart(true);
            isScrolling = false;

            // Get the last visible document in the first query, then make the next query following
            // the document using startAfter().
            DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
            Query nextQuery = colRef.orderBy(field, Query.Direction.DESCENDING)
                    .startAfter(lastDoc).limit(pagingLimit);

            nextQuery.get().addOnSuccessListener(nextQuerySnapshot -> {
                if(nextQuerySnapshot.size() <= pagingLimit) isLastItem = true;
                log.i("isLastItem: %s, %s, %s", nextQuerySnapshot.size(), pagingLimit, isLastItem);
                mListener.setNextQueryComplete(nextQuerySnapshot);
            });

        }

    }



}
