package com.silverback.carman.board;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

/*
 * This helper class is to paginate posting items downloaded from Firestore by its category passed
 * from the viewpager fragment(BoardPagerFragment), which is bound by FragmentStatePagerAdapter.
 * Query results will be sent back to the fragment by OnPaginationListener that is attached by
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
public class QueryClubPostingUtil implements EventListener<QuerySnapshot> {

    private static final LoggingHelper log = LoggingHelperFactory.create(QueryClubPostingUtil.class);

    // Objects
    private ListenerRegistration listenerRegit;
    private final CollectionReference colRef;
    private QuerySnapshot querySnapshot;
    private OnPaginationListener mCallback;

    // Fields
    private String field;

    // Interface w/ BoardPagerFragment to notify the state of querying process and pagination.
    public interface OnPaginationListener {
        void setClubQuerySnapshot(QuerySnapshot snapshot);
        //void setClubNextQuerySnapshot(QuerySnapshot snapshot);
    }
    // Method for implementing the inteface in BoardPagerFragment, which notifies the caller of
    // having QuerySnapshot retrieved.
    public void setOnPaginationListener(OnPaginationListener listener) {
        mCallback = listener;
    }

    // private constructor
    public QueryClubPostingUtil(FirebaseFirestore firestore) {
        colRef = firestore.collection("board_general");
    }

    public void setPostingQuery(boolean isViewOrder) {
        log.i("view order: %s", isViewOrder);
        field = (isViewOrder)? "cnt_view" : "timestamp";
        Query firstQuery = colRef.orderBy(field, Query.Direction.DESCENDING).limit(Constants.PAGINATION);
        listenerRegit = firstQuery.addSnapshotListener(MetadataChanges.INCLUDE, this);
    }

    // Make the next query manually particularily for the autoclub which performs a regular query
    // based on either cnt_view or timestamp, the results of which will be passed to the list
    // and sorted out with the autofilter values.
    public void setNextQuery() {
        log.i("querySnapshot: %s", querySnapshot.size());
        DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        Query nextQuery = colRef.orderBy(field, Query.Direction.DESCENDING).startAfter(lastDoc)
                .limit(Constants.PAGINATION);

        listenerRegit = nextQuery.addSnapshotListener(MetadataChanges.INCLUDE, this);
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException e) {
        if (e != null || querySnapshot == null) return;

        this.querySnapshot = querySnapshot;
        mCallback.setClubQuerySnapshot(querySnapshot);

        // In case of querying the last page, remove the listener.
        // However, if the listener is removed, adding or removing a post will not be updated!!
        //if(querySnapshot.size() < Constants.PAGINATION) listenerRegit.remove();
    }

}
