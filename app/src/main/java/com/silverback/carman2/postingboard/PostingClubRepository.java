package com.silverback.carman2.postingboard;

import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

/*
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
 *
 * @boolean isLastPage
 * @booelan isLoading
 */
public class PostingClubRepository {

    private static final LoggingHelper log = LoggingHelperFactory.create(PostingClubRepository.class);

    // Objects
    private CollectionReference colRef;
    private QuerySnapshot querySnapshot;
    private OnPaginationListener mListener;

    // Fields
    private String field;

    // Interface w/ BoardPagerFragment to notify the state of querying process and pagination.
    public interface OnPaginationListener {
        void setClubInitQuerySnapshot(QuerySnapshot snapshot);
        void setClubNextQuerySnapshot(QuerySnapshot snapshot);
    }

    // private constructor
    public PostingClubRepository(FirebaseFirestore firestore) {
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
    public void setPostingQuery(boolean isViewOrder) {
        log.i("view order: %s", isViewOrder);
        field = (isViewOrder)? "cnt_view" : "timestamp";
        Query firstQuery = colRef.orderBy(field, Query.Direction.DESCENDING).limit(Constants.PAGINATION);
        firstQuery.addSnapshotListener(MetadataChanges.INCLUDE, (querySnapshot, e) -> {
                    if (e != null || querySnapshot == null) return;
                    this.querySnapshot = querySnapshot;
                    mListener.setClubInitQuerySnapshot(querySnapshot);
                });
    }

    // Make the next query manually particularily for the autoclub which performs a regular query
    // based on either cnt_view or timestamp, the results of which will be passed to the list
    // and sorted out with the autofilter values.
    public void setNextQuery() {
        log.i("querySnapshot: %s", querySnapshot.size());
        if(querySnapshot.size() < Constants.PAGINATION) return;
        DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        Query nextQuery = colRef.orderBy(field, Query.Direction.DESCENDING).startAfter(lastDoc)
                .limit(Constants.PAGINATION);
        nextQuery.addSnapshotListener((nextSnapshot, e) -> {
                    if (e != null || nextSnapshot == null) return;
                    this.querySnapshot = nextSnapshot;
                    mListener.setClubNextQuerySnapshot(nextSnapshot);
                });
    }

}
