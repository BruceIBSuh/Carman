package com.silverback.carman.utils;

import static com.silverback.carman.BoardActivity.AUTOCLUB;
import static com.silverback.carman.BoardActivity.NOTIFICATION;
import static com.silverback.carman.BoardActivity.PAGINATION;
import static com.silverback.carman.BoardActivity.PAGING_COMMENT;
import static com.silverback.carman.BoardActivity.POPULAR;
import static com.silverback.carman.BoardActivity.RECENT;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class QueryPostPaginationUtil {

    private static final LoggingHelper log = LoggingHelperFactory.create(QueryPostPaginationUtil.class);

    // Objects
    private FirebaseFirestore mDB;
    private final OnQueryPaginationCallback mCallback;
    private final FirebaseFirestore firestore;
    private CollectionReference colRef;
    private Query query;
    private QuerySnapshot querySnapshot;

    // Fields
    private int category;
    private boolean isViewOrder;
    private String field;

    // Interface
    public interface OnQueryPaginationCallback {
        void getFirstQueryResult(QuerySnapshot postShots);
        void getNextQueryResult(QuerySnapshot nextShots);
        void getLastQueryResult(QuerySnapshot lastShots);
        void getRemovedQueryResult(DocumentSnapshot doc);
        void getModifiedQueryResult(DocumentSnapshot doc);
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
    public ListenerRegistration setPostQuery(int category) {//, boolean isViewOrder) {
        this.category = category;
        //colRef = firestore.collection("board_general");
        colRef = firestore.collection("user_post");
        switch(category){
            case RECENT:
                this.field = "timestamp";
                query = colRef.orderBy(field, Query.Direction.DESCENDING);
                break;

            case POPULAR:
                this.field = "cnt_view";
                query = colRef.orderBy(field, Query.Direction.DESCENDING);
                break;

            case AUTOCLUB:
                this.field = (isViewOrder)? "cnt_view" : "timestamp";
                query = colRef.orderBy(field, Query.Direction.DESCENDING);
                break;

            case NOTIFICATION:
                query = firestore.collection("admin_post").orderBy("timestamp", Query.Direction.DESCENDING);
                break;
        }

        return query.limit(PAGINATION).addSnapshotListener(MetadataChanges.INCLUDE, (querySnapshot, e) -> {
            if(e != null || querySnapshot == null) return;
            for(DocumentChange dc : querySnapshot.getDocumentChanges()) {
                switch(dc.getType()) {
                    case ADDED:
                        log.i("ADDED: %s", dc.getDocument().getData());
                        this.querySnapshot = querySnapshot;
                        mCallback.getFirstQueryResult(querySnapshot);
                        break;
                    case MODIFIED:
                        log.i("MODIFIED: %s", dc.getDocument().getData());
                        mCallback.getModifiedQueryResult(dc.getDocument());
                        break;
                    case REMOVED:
                        log.i("REMOVED: %s", dc.getDocument().getData());
                        mCallback.getRemovedQueryResult(dc.getDocument());
                        break;
                }
            }
        });

        /*
        query.limit(PAGINATION).get(source).addOnSuccessListener(querySnapshot -> {
            this.querySnapshot = querySnapshot;
            mCallback.getFirstQueryResult(querySnapshot);
        }).addOnFailureListener(mCallback::getQueryErrorResult);
        */
        /*
        query.limit(PAGINATION).addSnapshotListener((querySnapshot, e) -> {
            if(e != null) return;
            for(DocumentSnapshot doc : querySnapshot) {
                String source = doc != null && doc.getMetadata().hasPendingWrites()?"LOCAL":"SERVER";
                if(source.matches("LOCAL")) log.i("cached data");
                else log.i("server data");
            }
            this.querySnapshot = querySnapshot;
            mCallback.getFirstQueryResult(querySnapshot);
        });
         */

    }

    // Make an initial query of comments in BoardReadFragment.
    public void setCommentQuery(DocumentReference docRef, String field){
        querySnapshot = null;
        this.field = field;
        //colRef = docRef.collection("comments");
        //colRef.orderBy(field, Query.Direction.DESCENDING).limit(PAGINATION).get()
        query = docRef.collection("comments").orderBy(field, Query.Direction.DESCENDING).limit(PAGING_COMMENT);
        /*
        query.addSnapshotListener((commentshot, e) -> {
            if(e != null) return;
            this.querySnapshot = commentshot;
            mCallback.getFirstQueryResult(commentshot);
        });
        */
        query.get().addOnSuccessListener(queryCommentShot -> {
            // What if the first query comes to the last page? "isLoading" field in BoardPagerFragment
            // is set to true, which disables the recyclerview scroll listener to call setNextQuery().
            this.querySnapshot = queryCommentShot;
            mCallback.getFirstQueryResult(queryCommentShot);
        }).addOnFailureListener(mCallback::getQueryErrorResult);


    }

    // The recyclerview scorll listener notifies that the view scrolls down to the last item and needs
    // to make an next query, which will be repeated until query comes to the last page.
    public void setNextPostQuery() {
        DocumentSnapshot lastVisible = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        //if(category == Constants.BOARD_POPULAR) query = colRef.whereEqualTo("post_general", true);
        switch(category) {
            case RECENT: case POPULAR:
                query = colRef/*.whereEqualTo("post_general", true)*/.orderBy(field, Query.Direction.DESCENDING);
                break;
            case AUTOCLUB:
                query = colRef.orderBy(field, Query.Direction.DESCENDING);
                break;
            case NOTIFICATION:
                query = firestore.collection("admin_post").orderBy(field, Query.Direction.DESCENDING);
                break;
        }

        query.startAfter(lastVisible).limit(PAGINATION).get().addOnSuccessListener(
                nextSnapshot -> {
                    this.querySnapshot = nextSnapshot;
                    if(nextSnapshot.size() >= PAGINATION) mCallback.getNextQueryResult(nextSnapshot);
                    else mCallback.getLastQueryResult(nextSnapshot);
                }).addOnFailureListener(mCallback::getQueryErrorResult);
    }

    public void setNextCommentQuery() {
        DocumentSnapshot lastVisible = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        query.startAfter(lastVisible).limit(PAGING_COMMENT).get().addOnSuccessListener(comments ->{
            this.querySnapshot = comments;
            if(comments.size() >= PAGING_COMMENT) mCallback.getNextQueryResult(querySnapshot);
            else mCallback.getLastQueryResult(querySnapshot);
        }).addOnFailureListener(mCallback::getQueryErrorResult);
    }

    public void setAutoclubOrder(boolean b) {
        this.isViewOrder = b;
    }


}
