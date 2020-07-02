package com.silverback.carman2.utils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

/**
 * This helper class is to paginate posting items downloaded from Firestore by its category passed
 * from the viewpager fragment(BoardPagerFragment) which is bound by FragmentStatePagerAdapter.
 * Query results will be sent back to the fragment by OnPaginationListener which is attached by
 * calling setOnPaginationListener() and has the following callbacks.
 *
 * setFirstQuery(): pass the first query result
 * setNextQueryStart(boolean): if true, the progressbar starts and vice versa.
 * setNextQueryComplete(); pass the next query reuslt.
 *
 * The autoclub board should be handled with special care that it manages queries in a different way
 * to avoid compound query in Firestore with composite index. The autofilter should performs multiple
 * whereEqualTo() queries(Logical AND) with orderBy() and limit() based on a different field. For doing
 * so, it requires to create a compoite index which is very expensive to perform. Thus, query is made
 * in a simple way, the result is passed to the list and sorts out the list elements on the client
 * side.
 */
public class PagingQueryHelper extends RecyclerView.OnScrollListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(PagingQueryHelper.class);
    // Objects
    private FirebaseFirestore firestore;
    private CollectionReference colRef;
    private QuerySnapshot queryPostShot;
    private OnPaginationListener mListener;

    // Fields
    private boolean isLoading;
    private boolean isLastPage;
    private String field;
    private int currentPage;

    // Interface w/ BoardPagerFragment to notify the state of querying process and pagination.
    public interface OnPaginationListener {
        void setFirstQuery(int page, QuerySnapshot snapshot);
        void setNextQueryStart(boolean b);
        void setNextQueryComplete(int page, QuerySnapshot snapshot);
    }

    // private constructor
    public PagingQueryHelper() {
        firestore = FirebaseFirestore.getInstance();
        colRef = firestore.collection("board_general");

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
    //public void setPostingQuery(int page, ArrayList<String> autofilter) {
    public void setPostingQuery(int page, boolean isViewOrder) {
        Query query = colRef;
        queryPostShot = null;

        currentPage = page;
        isLastPage = false;
        isLoading = true;

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
                this.field = (isViewOrder)? "cnt_view" : "timestamp";
                query = query.orderBy(field, Query.Direction.DESCENDING);
                break;


            // Should create a new collection managed by Admin.(e.g. board_admin)
            case Constants.BOARD_NOTIFICATION:
                query = firestore.collection("board_admin").orderBy("timestamp", Query.Direction.DESCENDING);
                break;
        }

        // Add SnapshotListener to the query built up to the board with its own conditions.
        // Refactor should be considered to apply Source.CACHE or Source.SERVER depending on whehter
        // querysnapshot has existed or hasPendingWrite is true.
        //query.limit(Constants.PAGINATION).get(source).addOnSuccessListener((querySnapshot) -> {
        query.limit(Constants.PAGINATION).addSnapshotListener((querySnapshot, e) -> {
            isLoading = false;
            if(e != null || querySnapshot == null || querySnapshot.size() == 0) return;
            this.queryPostShot = querySnapshot;
            isLastPage = (querySnapshot.size()) < Constants.PAGINATION;
            mListener.setFirstQuery(page, queryPostShot);

        });
    }

    // Query comments in BoardRedDlgFragment
    public void setCommentQuery(int page, final String field, final String docId) {
        this.field = field;
        queryPostShot = null;
        isLastPage = false;
        isLoading = true;

        colRef = firestore.collection("board_general").document(docId).collection("comments");
        colRef.orderBy(field, Query.Direction.DESCENDING).limit(Constants.PAGINATION)
                .addSnapshotListener((queryCommentShot, e) -> {
                    isLoading = false;
                    if(e != null || queryCommentShot == null || queryCommentShot.size() == 0) return;
                    this.queryPostShot = queryCommentShot;
                    isLastPage = queryCommentShot.size() < Constants.PAGINATION;
                    mListener.setFirstQuery(page, queryCommentShot);
                });
    }

    // Make the next query manually particularily for the autoclub which performs a regular query
    // based on either cnt_view or timestamp, the results of which will be passed to the list
    // and sorted out with the autofilter values.
    public void setNextQuery(QuerySnapshot querySnapshot) {
        DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        mListener.setNextQueryStart(true);
        colRef.orderBy(field, Query.Direction.DESCENDING).startAfter(lastDoc)
                .limit(Constants.PAGINATION)
                .addSnapshotListener((nextSnapshot, e) -> {
                    if (e != null || nextSnapshot == null || nextSnapshot.size() == 0) return;
                    // Hide the loading progressbar and add the query results to the list
                    mListener.setNextQueryStart(false);
                    mListener.setNextQueryComplete(Constants.BOARD_AUTOCLUB, nextSnapshot);
                });
    }

    // Callback method to be invoked when RecyclerView's scroll state changes.
    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
    }

    // Callback method to be invoked when the RecyclerView has been scrolled. This will be called
    // after the scroll has completed. This callback will also be called if visible item range changes
    // after a layout calculation. In that case, dx and dy will be 0.
    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        // Exclude the auto club page b/c it is manually queried by calling setNextQuery() and
        if(currentPage == Constants.BOARD_AUTOCLUB) return;

        LinearLayoutManager layoutManager = (LinearLayoutManager)recyclerView.getLayoutManager();
        if(layoutManager == null || dy == 0) return;

        int firstItemPos = layoutManager.findFirstVisibleItemPosition();
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();

        if(!isLoading && !isLastPage && firstItemPos + visibleItemCount >= totalItemCount) {
            isLoading = true;
            mListener.setNextQueryStart(true);
            // Get the last visible document in the first query, then make the next query following
            // the document using startAfter(). QuerySnapshot must be invalidated with the value by
            // nextQuery.
            DocumentSnapshot lastDoc = queryPostShot.getDocuments().get(queryPostShot.size() - 1);
            // Making the next query, the autoclub page has to be handled in a diffent way than
            // the other pages because it queries with different conditions.
            Query nextQuery = colRef;
            nextQuery.orderBy(field, Query.Direction.DESCENDING).startAfter(lastDoc)
                    .limit(Constants.PAGINATION)
                    .addSnapshotListener(MetadataChanges.INCLUDE, (nextSnapshot, e) -> {
                        // Check if the next query reaches the last document.
                        if(e != null || nextSnapshot == null) return;

                        // Hide the loading progressbar and add the query results to the list
                        mListener.setNextQueryStart(false);

                        if(!isLastPage) {
                            mListener.setNextQueryComplete(currentPage, nextSnapshot);
                            queryPostShot = nextSnapshot;
                        }

                        isLastPage = (nextSnapshot.size()) < Constants.PAGINATION;
                        isLoading = false; // ready to make a next query
                    });
        }
    }

}
