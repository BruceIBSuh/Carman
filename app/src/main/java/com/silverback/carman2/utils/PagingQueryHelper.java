package com.silverback.carman2.utils;

import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is to paginate the posting items which is handled in BoardPagerFragment which implements
 * OnPaginationListener to have document snaoshots from FireStore.
 *
 * Pagination is also enabled to use Paged Library or FirestoreRecylerAdapter.
 */
public class PagingQueryHelper extends RecyclerView.OnScrollListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(PagingQueryHelper.class);

    // Objects
    private FirebaseFirestore firestore;
    private CollectionReference colRef;
    private QuerySnapshot querySnapshot;
    private OnPaginationListener mListener;
    private List<String> autofilter;

    // Fields
    private boolean isLoading;
    private boolean isLastItem;
    private String field;

    // Interface w/ BoardPagerFragment to notify the state of querying process and pagination.
    public interface OnPaginationListener {
        void setFirstQuery(QuerySnapshot snapshot);
        void setNextQueryStart(boolean b);
        void setNextQueryComplete(QuerySnapshot snapshot);
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

    /*
     * Create a query sentence with conditions passed as params. As queries completes, the result
     * will be notified to BoardPagerFragment via OnPaginationListener interface.
     * As far as the autoclub query is concerned, it makes a simple query with the autofilter values
     * conditioned and orderBy() is not conditioned here. In order to apply orderBy(time or view),
     * a compound query has to make an index. Thus, once retrieving a result, the autoclub sorts
     * using Collection.sort(List, Comparator).
     *
     * @param source Firestore source - Source.SERVER or Source.CACHE
     * @param page current page to query
     * @param autofilter the autoclub page query conditions.
     */
    public void setPostingQuery(int page, ArrayList<String> autofilter, boolean isViewCount) {
        Query query = colRef;
        switch(page) {
            case Constants.BOARD_RECENT:
                this.field = "timestamp";
                query = query.orderBy("timestamp", Query.Direction.DESCENDING);
                break;

            case Constants.BOARD_POPULAR:
                this.field = "cnt_view";
                query = query.orderBy("cnt_view", Query.Direction.DESCENDING);
                break;

            case Constants.BOARD_AUTOCLUB:
                this.field = "auto_club";
                this.autofilter = autofilter;

                if(autofilter == null || autofilter.size() == 0) return;
                // Query depends on whether the autofilter contains the automaker only or more filter
                // values because the automaker works as a sufficient condition and other filters
                // works as necessary conditions.
                for(int i = 0; i < autofilter.size(); i++) {
                    final String field = "auto_filter." + autofilter.get(i);
                    query = query.whereEqualTo(field, true);
                }
                // Compound query that has multiple where() methods to create more specific queries
                // (Logial AND) but it does not require a composite index which is necessary when
                // where() methods are combined with a range or array-contains clause. Here, it
                // is simply combined with orderBy() and limt(), thus, no compount query is required.
                String clubOrder = (isViewCount)? "cnt_view" : "timestamp";
                query = query.orderBy(clubOrder, Query.Direction.DESCENDING);
                break;

            // Should create a new collection managed by Admin.(e.g. board_admin)
            case Constants.BOARD_NOTIFICATION: // notification
                query = firestore.collection("board_admin").orderBy("timestamp", Query.Direction.DESCENDING);
                break;
        }

        // Add SnapshotListener to the query built up to the board with its own conditions.
        // Refactor should be considered to apply Source.CACHE or Source.SERVER depending on whehter
        // querysnapshot has existed or hasPendingWrite is true.
        query.limit(Constants.PAGINATION).addSnapshotListener((querySnapshot, e) -> {
            if(e != null) return;
            this.querySnapshot = querySnapshot;
            mListener.setFirstQuery(querySnapshot);
        });
    }

    public void setCommentQuery(Source source, final String field, final String docId) {
        this.field = field;
        colRef = firestore.collection("board_general").document(docId).collection("comments");
        colRef.orderBy(field, Query.Direction.DESCENDING).limit(Constants.PAGINATION)
                .addSnapshotListener((querySnapshot, e) -> {
                    if(e != null) return;
                    this.querySnapshot = querySnapshot;
                    mListener.setFirstQuery(querySnapshot);
                });
    }


    // Callback method to be invoked when RecyclerView's scroll state changes.
    //
    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        /*
        if(newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            isScrolling = true;
        }
         */
    }

    // Callback method to be invoked when the RecyclerView has been scrolled. This will be called
    // after the scroll has completed. This callback will also be called if visible item range changes
    // after a layout calculation. In that case, dx and dy will be 0.
    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        LinearLayoutManager layoutManager = (LinearLayoutManager)recyclerView.getLayoutManager();
        if(layoutManager == null || dy == 0) return;

        int firstItemPos = layoutManager.findFirstVisibleItemPosition();
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();

        if(!isLoading && !isLastItem && firstItemPos + visibleItemCount >= totalItemCount) {
            mListener.setNextQueryStart(true);

            // Get the last visible document in the first query, then make the next query following
            // the document using startAfter(). QuerySnapshot must be invalidated with the value by
            // nextQuery.
            isLoading = true;
            DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
            // Making the next query, the autoclub page has to be handled in a diffent way than
            // the other pages because it queries with different conditions.
            Query nextQuery = colRef;
            if (field.equals("auto_club")) {
                for (int i = 0; i < autofilter.size(); i++) {
                    final String field = "auto_filter." + autofilter.get(i);
                    nextQuery = nextQuery.whereEqualTo(field, true);
                }
                nextQuery = nextQuery.startAfter(lastDoc).limit(Constants.PAGINATION);

            } else {
                nextQuery = colRef.orderBy(field, Query.Direction.DESCENDING)
                        .startAfter(lastDoc).limit(Constants.PAGINATION);
            }

            nextQuery.limit(Constants.PAGINATION).addSnapshotListener((nextSnapshot, e) -> {
                // Check if the next query reaches the last document.
                if(e != null || nextSnapshot == null) return;

                isLastItem = (nextSnapshot.size()) < Constants.PAGINATION;
                isLoading = false; // ready to make a next query

                mListener.setNextQueryStart(false); //hide the loading progressbar
                mListener.setNextQueryComplete(nextSnapshot); // add the query result to the list.

                querySnapshot = nextSnapshot;
            });
            /*
            nextQuery.get().addOnSuccessListener(nextSnapshot -> {
                // Check if the next query reaches the last document.
                isLastItem = (nextSnapshot.size()) < Constants.PAGINATION;
                isLoading = false; // ready to make a next query

                mListener.setNextQueryStart(false); //hide the loading progressbar
                mListener.setNextQueryComplete(nextSnapshot); // add the query result to the list.

                querySnapshot = nextSnapshot;
            });

             */

        }
    }
}
