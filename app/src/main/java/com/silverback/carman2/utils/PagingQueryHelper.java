package com.silverback.carman2.utils;

import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class is to paginate the posting items which is handled in BoardPagerFragment which implements
 * OnPaginationListener to have document snaoshots from FireStore.
 */
public class PagingQueryHelper extends RecyclerView.OnScrollListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(PagingQueryHelper.class);

    // Objects
    private FirebaseFirestore firestore;
    private CollectionReference colRef;
    private QuerySnapshot querySnapshot;
    private OnPaginationListener mListener;

    // Fields
    private boolean isScrolling;
    private boolean isLastItem;
    private String field;

    // Interface w/ BoardPagerFragment to notify the state of querying process and pagination.
    public interface OnPaginationListener {
        void setFirstQuery(QuerySnapshot snapshot);
        void setNextQueryStart(boolean b);
        void setNextQueryComplete(QuerySnapshot querySnapshot);
    }

    // private constructor
    public PagingQueryHelper() {
        firestore = FirebaseFirestore.getInstance();
        colRef = firestore.collection("board_general");
        querySnapshot = null;
    }

    // Method for implementing the inteface in BoardPagerFragment, which notifies the caller of
    // having QuerySnapshot retrieved.
    public void setOnPaginationListener(OnPaginationListener listener) {
        mListener = listener;
    }

    // Create queries for each page.
    public void setPostingQuery(Source source, int page, ArrayList<String> autofilter) {
        log.i("source in setPostingQuery param: %s", source);
        switch(page) {
            case Constants.BOARD_RECENT:
                this.field = "timestamp";
                colRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(Constants.PAGINATION)
                        .get(source)
                        .addOnSuccessListener(querySnapshot -> {
                            this.querySnapshot = querySnapshot;
                            mListener.setFirstQuery(querySnapshot);

                        }).addOnFailureListener(Throwable::printStackTrace);
                break;

            case Constants.BOARD_POPULAR:
                this.field = "cnt_view";
                colRef.orderBy("cnt_view", Query.Direction.DESCENDING).limit(Constants.PAGINATION)
                        .get(source)
                        .addOnSuccessListener(querySnapshot -> {
                            this.querySnapshot = querySnapshot;
                            mListener.setFirstQuery(querySnapshot);

                        }).addOnFailureListener(Throwable::printStackTrace);
                break;

            case Constants.BOARD_AUTOCLUB:
                this.field = "auto_club";
                if(autofilter == null || autofilter.size() == 0) return;
                Query query = colRef;
                // Query depends on whether the autofilter contains the automaker only or more filter
                // values because the automaker works as a sufficient condition and other filters
                // works as necessary conditions.
                for(int i = 0; i < autofilter.size(); i++) {
                    final String field = "auto_filter." + autofilter.get(i);
                    query = query.whereEqualTo(field, true);
                }

                query.get(source).addOnSuccessListener(autoclubShot -> {
                    this.querySnapshot = autoclubShot;
                    mListener.setFirstQuery(autoclubShot);
                }).addOnFailureListener(Exception::printStackTrace);

                break;

            // Should create a new collection managed by Admin.(e.g. board_admin)
            case Constants.BOARD_NOTIFICATION: // notification
                break;
        }
    }


    public void setCommentQuery(Source source, final String field, final String docId) {
        this.field = field;
        colRef = firestore.collection("board_general").document(docId).collection("comments");
        colRef.orderBy(field, Query.Direction.DESCENDING).limit(Constants.PAGINATION)
                .get(source)
                .addOnSuccessListener(querySnapshot -> {
                    this.querySnapshot = querySnapshot;
                    mListener.setFirstQuery(querySnapshot);
                });
    }


    // Callback method to be invoked when RecyclerView's scroll state changes.
    //
    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        log.i("newState: %s", newState);
        if(newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            isScrolling = true;
        }
    }

    // Callback method to be invoked when the RecyclerView has been scrolled. This will be called
    // after the scroll has completed. This callback will also be called if visible item range changes
    // after a layout calculation. In that case, dx and dy will be 0.
    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        log.i("onScrolled");
        LinearLayoutManager layoutManager = (LinearLayoutManager)recyclerView.getLayoutManager();
        if(layoutManager == null || dy == 0) return;

        int firstItemPos = layoutManager.findFirstVisibleItemPosition();
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        log.i("Item: %s, %s, %s", firstItemPos, visibleItemCount, totalItemCount);

        if((firstItemPos + visibleItemCount == totalItemCount) && !isLastItem) {
        //if(isScrolling && (firstItemPos + visibleItemCount == totalItemCount) && !isLastItem) {
            log.i("Query next items");
            mListener.setNextQueryStart(true);
            isScrolling = false;

            // Get the last visible document in the first query, then make the next query following
            // the document using startAfter(). QuerySnapshot must be invalidated with the value by
            // nextQuery.
            DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
            log.i("last doc: %s", lastDoc.getString("post_title"));

            // Error!!!!!
            // Invalid query. You are trying to start or end a query using a document for which the
            // field 'auto_club' (used as the orderBy) does not exist.
            Query nextQuery = colRef.orderBy(field, Query.Direction.DESCENDING)
                    .startAfter(lastDoc).limit(Constants.PAGINATION);

            nextQuery.get().addOnSuccessListener(nextSnapshot -> {
                log.i("isLastItem: %s, %s", nextSnapshot.size(), Constants.PAGINATION);

                // Check if the next query reaches the last document.
                if((nextSnapshot.size()) < Constants.PAGINATION) isLastItem = true;

                mListener.setNextQueryComplete(nextSnapshot);
                querySnapshot = nextSnapshot;
            });

        }

    }
}
