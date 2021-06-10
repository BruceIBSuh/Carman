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
    private OnQueryPaginationCallback mCallback;
    private FirebaseFirestore firestore;
    private CollectionReference colRef;
    private Query query;
    private QuerySnapshot querySnapshot;

    // Fields
    private boolean isUpdated;
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
    public void setPostQuery(int page, boolean isViewOrder) {
        colRef = firestore.collection("board_general");
        querySnapshot = null;

        switch(page){
            case Constants.BOARD_RECENT:
                this.field = "timestamp";
                query = colRef.whereEqualTo("post_general", true).orderBy(field, Query.Direction.DESCENDING);
                break;

            case Constants.BOARD_POPULAR:
                this.field = "cnt_view";
                query = colRef.whereEqualTo("post_general", true).orderBy(field, Query.Direction.DESCENDING);
                break;

            case Constants.BOARD_AUTOCLUB:
                this.field = (isViewOrder)? "cnt_view" : "timestamp";
                query = colRef.orderBy(field, Query.Direction.DESCENDING);
                break;

            case Constants.BOARD_NOTIFICATION:
                query = firestore.collection("board_admin").orderBy("timestamp", Query.Direction.DESCENDING);
                break;
        }


        query.limit(Constants.PAGINATION).get().addOnSuccessListener(querySnapshot -> {
            this.querySnapshot = querySnapshot;
            mCallback.getFirstQueryResult(querySnapshot);
        }).addOnFailureListener(e -> mCallback.getQueryErrorResult(e));

        /*
        query.limit(Constants.PAGINATION).addSnapshotListener((querySnapshot, e) -> {
            if(e != null) return;
            this.querySnapshot = querySnapshot;
            mCallback.getFirstQueryResult(querySnapshot);
        });

         */
    }

    // Make an initial query of comments in BoardReadDlgFragment.
    public void setCommentQuery(DocumentReference docRef){
        querySnapshot = null;
        this.field = "timestamp";
        colRef = docRef.collection("comments");
        colRef.orderBy(field, Query.Direction.DESCENDING).limit(Constants.PAGINATION)
                .get()
                .addOnSuccessListener(queryCommentShot -> {
                    // What if the first query comes to the last page? "isLoading" field in BoardPagerFragment
                    // is set to true, which disables the recyclerview scroll listener to call setNextQuery().
                    this.querySnapshot = queryCommentShot;
                    mCallback.getFirstQueryResult(queryCommentShot);
                }).addOnFailureListener(e -> mCallback.getQueryErrorResult(e));
    }

    // The recyclerview scorll listener notifies that the view scrolls down to the last item and needs
    // to make an next query, which will be repeated until query comes to the last page.
    public void setNextQuery() {
        DocumentSnapshot lastVisibleShot = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        colRef.orderBy(field, Query.Direction.DESCENDING).startAfter(lastVisibleShot).limit(Constants.PAGINATION)
                .get()
                .addOnSuccessListener(nextSnapshot -> {
                    if(nextSnapshot.size() >= Constants.PAGINATION) {
                        this.querySnapshot = nextSnapshot;
                        mCallback.getNextQueryResult(nextSnapshot);
                    } else {
                        log.i("last page");
                        querySnapshot = null;
                        mCallback.getLastQueryResult(nextSnapshot);
                    }
                }).addOnFailureListener(e -> {
                    log.e("error in querying");
                    mCallback.getQueryErrorResult(e);
                });

        /*
        colRef.orderBy(field, Query.Direction.DESCENDING).startAfter(lastVisibleShot)
                .limit(Constants.PAGINATION)
                .addSnapshotListener((nextSnapshot, e) -> {
                    if(e != null || nextSnapshot == null) return;
                    log.i("meta data: %s", nextSnapshot.getMetadata().isFromCache());
                    for(DocumentChange dc : nextSnapshot.getDocumentChanges()) {
                        switch(dc.getType()){
                            case ADDED:
                                log.i("ADDED: %s", dc.getDocument().getString("post_title"));
                                break;

                            case MODIFIED:
                                log.i("MODIFIED: %s", dc.getDocument().getString("post_title"));
                                break;

                            case REMOVED:
                                log.i("REMOVED: %s", dc.getDocument().getString("post_title"));
                                break;
                        }
                    }

                    if(nextSnapshot.size() >= Constants.PAGINATION) {
                        log.i("query next: %s", nextSnapshot.size());
                        this.querySnapshot = nextSnapshot;
                        mCallback.getNextQueryResult(nextSnapshot);
                    } else {
                        log.i("query last: %s", nextSnapshot.size());
                        querySnapshot = null;
                        mCallback.getLastQueryResult(nextSnapshot);
                    }
                });

         */
    }
}
