package com.silverback.carman2.utils;

import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class PaginationHelper extends RecyclerView.OnScrollListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(PaginationHelper.class);

    // Objects
    private CollectionReference colRef;
    private QuerySnapshot querySnapshot;
    private OnPaginationListener mListener;

    // Fields
    private boolean isScrolling;
    private boolean isLastItem;
    private int pagingLimit;
    private String field;

    // Interface w/ BoardPagerFragment to notify the state of querying process and pagination.
    public interface OnPaginationListener {
        void setFirstQuery(QuerySnapshot snapshot);
        void setNextQueryStart(boolean b);
        void setNextQueryComplete(QuerySnapshot querySnapshot);
    }

    // Constructor
    public PaginationHelper() {
        // default constructor left empty.
    }

    // Method for implementing the inteface in BoardPagerFragment, which notifies the caller of
    // having QuerySnapshot retrieved.
    public void setOnPaginationListener(OnPaginationListener listener) {
        mListener = listener;
    }


    public void setPostingQuery(final String field, final int limit) {
        this.field = field;
        pagingLimit = limit;

        // Initate the first query
        colRef = FirebaseFirestore.getInstance().collection("board_general");
        colRef.orderBy(field, Query.Direction.DESCENDING).limit(limit).get()
                .addOnSuccessListener(querySnapshot -> {
                    this.querySnapshot = querySnapshot;
                    mListener.setFirstQuery(querySnapshot);
                });
    }

    public void setCommentQuery(String id, final String field, final int limit) {
        this.field = field;
        pagingLimit = limit;

        FirebaseFirestore.getInstance().collection("board_general").document(id).get()
                .addOnSuccessListener(document -> {
                    colRef = document.getReference().collection("comments");
                    colRef.orderBy(field, Query.Direction.DESCENDING).limit(limit).get()
                            .addOnSuccessListener(querySnapshot -> {
                                this.querySnapshot = querySnapshot;
                                mListener.setFirstQuery(querySnapshot);
                            });
                });


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
        if(layoutManager == null || dy == 0) return;

        int firstItemPos = layoutManager.findFirstVisibleItemPosition();
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();

        if(isScrolling && (firstItemPos + visibleItemCount == totalItemCount) && !isLastItem) {
            log.i("Query next items");
            mListener.setNextQueryStart(true);
            isScrolling = false;

            // Get the last visible document in the first query, then make the next query following
            // the document using startAfter(). QuerySnapshot must be invalidated with the value by
            // nextQuery.
            DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
            log.i("last doc: %s", lastDoc.getString("post_title"));
            Query nextQuery = colRef.orderBy(field, Query.Direction.DESCENDING)
                    .startAfter(lastDoc).limit(pagingLimit);

            nextQuery.get().addOnSuccessListener(nextSnapshot -> {
                log.i("isLastItem: %s, %s", nextSnapshot.size(), pagingLimit);

                // Check if the next query reaches the last document.
                if((nextSnapshot.size()) < pagingLimit) isLastItem = true;

                mListener.setNextQueryComplete(nextSnapshot);
                querySnapshot = nextSnapshot;
            });

        }

    }



}
