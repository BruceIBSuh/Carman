package com.silverback.carman.utils;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class QueryPostPaginationUtil {

    private static final LoggingHelper log = LoggingHelperFactory.create(QueryPostPaginationUtil.class);

    // Objects
    private final OnQueryPaginationCallback mCallback;
    private final FirebaseFirestore firestore;
    private CollectionReference colRef;
    private Query query;
    private QuerySnapshot querySnapshot;

    // Fields
    private int category;
    private String field;

    // Interface
    public interface OnQueryPaginationCallback {
        void getFirstQueryResult(QuerySnapshot postShots);
        void getNextQueryResult(QuerySnapshot nextShots);
        void getLastQueryResult(QuerySnapshot lastShots);
        void getQueryErrorResult(Exception e);
    }

    // Constructor
    public QueryPostPaginationUtil(FirebaseFirestore firestore, OnQueryPaginationCallback callback) {
        this.firestore = firestore;
        mCallback = callback;
    }

    // Make an initial query for the posting board by category. Recent and popular board are made of
    // composite index in Firestore. Autoclub board once queries posts, then filters them with given
    // keyword in the client side.
    public void setPostQuery(int category, boolean isViewOrder) {
        this.category = category;
        colRef = firestore.collection("board_general");
        switch(category){
            case Constants.BOARD_RECENT:
                log.i("BOARD_RECENT");
                this.field = "timestamp";
                query = colRef.whereEqualTo("post_general", true).orderBy(field, Query.Direction.DESCENDING);
                break;

            case Constants.BOARD_POPULAR:
                log.i("BOARD_POPULAR");
                this.field = "cnt_view";
                query = colRef.whereEqualTo("post_general", true).orderBy(field, Query.Direction.DESCENDING);
                break;

            case Constants.BOARD_AUTOCLUB:
                log.i("BOARD_AUTOCLUB");
                this.field = (isViewOrder)? "cnt_view" : "timestamp";

                query = colRef.orderBy(field, Query.Direction.DESCENDING);
                break;

            case Constants.BOARD_NOTIFICATION:
                log.i("BOARD_NOTIFICATION");
                query = firestore.collection("admin_post").orderBy("timestamp", Query.Direction.DESCENDING);
                break;
        }


        query.limit(Constants.PAGINATION).get().addOnSuccessListener(querySnapshot -> {
            this.querySnapshot = querySnapshot;
            mCallback.getFirstQueryResult(querySnapshot);
        }).addOnFailureListener(mCallback::getQueryErrorResult);

        /*
        query.limit(Constants.PAGINATION).addSnapshotListener((querySnapshot, e) -> {
            if(e != null) return;
            this.querySnapshot = querySnapshot;
            mCallback.getFirstQueryResult(querySnapshot);
        });

         */

    }

    // The recyclerview scorll listener notifies that the view scrolls down to the last item and needs
    // to make an next query, which will be repeated until query comes to the last page.
    public void setNextQuery() {
        DocumentSnapshot lastVisible = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        //if(category == Constants.BOARD_POPULAR) query = colRef.whereEqualTo("post_general", true);
        switch(category) {
            case Constants.BOARD_RECENT: case Constants.BOARD_POPULAR:
                query = colRef.whereEqualTo("post_general", true).orderBy(field, Query.Direction.DESCENDING);
                break;
            case Constants.BOARD_AUTOCLUB:
                query = colRef.orderBy(field, Query.Direction.DESCENDING);
                break;
            case Constants.BOARD_NOTIFICATION:
                query = firestore.collection("admin_post").orderBy(field, Query.Direction.DESCENDING);
                break;
        }

        query.startAfter(lastVisible).limit(Constants.PAGINATION).get().addOnSuccessListener(
                nextSnapshot -> {
                    this.querySnapshot = nextSnapshot;
                    if(nextSnapshot.size() >= Constants.PAGINATION) {
                        mCallback.getNextQueryResult(nextSnapshot);
                    } else {
                        mCallback.getLastQueryResult(nextSnapshot);
                    }
                }).addOnFailureListener(mCallback::getQueryErrorResult);
    }

    // Make an initial query of comments in BoardReadDlgFragment.
    public void setCommentQuery(DocumentReference docRef){
        querySnapshot = null;
        this.field = "timestamp";
        colRef = docRef.collection("comments");
        colRef.orderBy(field, Query.Direction.DESCENDING).limit(Constants.PAGINATION).get()
                .addOnSuccessListener(queryCommentShot -> {
                    // What if the first query comes to the last page? "isLoading" field in BoardPagerFragment
                    // is set to true, which disables the recyclerview scroll listener to call setNextQuery().
                    this.querySnapshot = queryCommentShot;
                    mCallback.getFirstQueryResult(queryCommentShot);
                }).addOnFailureListener(mCallback::getQueryErrorResult);
    }
}
