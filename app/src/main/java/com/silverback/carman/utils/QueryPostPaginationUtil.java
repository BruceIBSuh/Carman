package com.silverback.carman.utils;

import static com.silverback.carman.BoardActivity.AUTOCLUB;
import static com.silverback.carman.BoardActivity.NOTIFICATION;
import static com.silverback.carman.BoardActivity.PAGINATION;
import static com.silverback.carman.BoardActivity.PAGING_COMMENT;
import static com.silverback.carman.BoardActivity.PAGING_REPLY;
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
import com.google.firebase.firestore.Source;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;

public class QueryPostPaginationUtil {

    private static final LoggingHelper log = LoggingHelperFactory.create(QueryPostPaginationUtil.class);

    // Objects
    private final OnQueryPaginationCallback mCallback;
    private final FirebaseFirestore firestore;
    private CollectionReference colRef;
    private Query query;
    private Source source;
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
        void getQueryErrorResult(Exception e);
    }

    // Constructor
    public QueryPostPaginationUtil(FirebaseFirestore firestore, OnQueryPaginationCallback callback) {
        this.firestore = firestore;
        mCallback = callback;
    }

    // Initial param to set the autoclub order.
    public void setAutoClubOrder(boolean b) {
        this.isViewOrder = b;
    }

    // Make an initial query for the posting board by category. Recent and popular board are made of
    // composite index in Firestore. Autoclub board once queries posts, then filters them with given
    // keyword in the client side.
    //public void setPostQuery(CollectionReference colRef, int category) {
    public ListenerRegistration setPostQuery(CollectionReference colRef, int category) {
        this.querySnapshot = null;
        this.category = category;
        this.colRef = colRef;

        switch(category){
            case RECENT:
                this.field = "timestamp";
                query = colRef.whereEqualTo("isGeneral", true).orderBy(field, Query.Direction.DESCENDING);
                break;
            case POPULAR:
                this.field = "cnt_view";
                query = colRef.whereEqualTo("isGeneral", true).orderBy(field, Query.Direction.DESCENDING);
                break;
            case AUTOCLUB:
                this.field = (isViewOrder)? "cnt_view" : "timestamp";
                query = colRef.whereEqualTo("isAutoClub", true).orderBy(field, Query.Direction.DESCENDING);
                break;
            case NOTIFICATION:
                this.field = "timestamp";
                query = firestore.collection("admin_post").orderBy(field, Query.Direction.DESCENDING);
                break;
        }

        return query.limit(PAGINATION).addSnapshotListener((querySnapshot, e) -> {
            if(e != null || querySnapshot == null) return;
            this.querySnapshot = querySnapshot;
            // firebase.firestore.FieldValue.serverTimestamp() gives a document a timestamp, then
            // onSnaphot will fire twice. This seem to be because when you add a new document to
            // your database onSnapshot will fire, but the serverTimestamp has not run yet. After
            // a few milliseconds serverTimestamp will run and update you document => onSnapshot
            // will fire again. For avoiding the repeated call, hasPendingWrite() of the meta data
            // should be alternatively set.
            if(!querySnapshot.getMetadata().hasPendingWrites()) {
                mCallback.getFirstQueryResult(querySnapshot);
            }
        });
        /*
        query.limit(PAGINATION).get(source).addOnSuccessListener(querySnapshot -> {
            this.querySnapshot = querySnapshot;
            log.i("query: %s", querySnapshot.getMetadata().isFromCache());
            mCallback.getFirstQueryResult(querySnapshot);
        }).addOnFailureListener(Throwable::printStackTrace);
        */
    }

    public void setAutofilterQuery(String field) {
        this.querySnapshot = null;
        this.field = field;
        colRef.whereEqualTo("isAutoClub", true).orderBy(field, Query.Direction.DESCENDING)
                .limit(PAGINATION).get()
                .addOnSuccessListener(querySnapshot -> {
                    this.querySnapshot = querySnapshot;
                    if(!querySnapshot.getMetadata().hasPendingWrites()) {
                        mCallback.getFirstQueryResult(querySnapshot);
                    }
                }).addOnFailureListener(Throwable::printStackTrace);
    }

    // The recyclerview scorll listener notifies that the view scrolls down to the last item and needs
    // to make an next query, which will be repeated until query comes to the last page.
    public void setNextPostQuery() {
        DocumentSnapshot lastVisible = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        switch(category) {
            case RECENT: case POPULAR:
                query = colRef.whereEqualTo("isGeneral", true).orderBy(field, Query.Direction.DESCENDING);
                break;
            case AUTOCLUB:
                query = colRef.whereEqualTo("isAutoClub", true).orderBy(field, Query.Direction.DESCENDING);
                break;
            case NOTIFICATION:
                query = firestore.collection("admin_post").orderBy(field, Query.Direction.DESCENDING);
                break;
        }

        query.startAfter(lastVisible).limit(PAGINATION).get(source).addOnSuccessListener(nextshots -> {
            this.querySnapshot = nextshots;
            if(nextshots.size() >= PAGINATION) mCallback.getNextQueryResult(nextshots);
            else mCallback.getLastQueryResult(nextshots);
        }).addOnFailureListener(mCallback::getQueryErrorResult);
    }


    // Make an initial query of comments in BoardReadFragment.
    public void setCommentQuery(DocumentReference docRef, String field){
        querySnapshot = null;
        this.field = field;
        query = docRef.collection("comments").orderBy(field, Query.Direction.DESCENDING).limit(PAGING_COMMENT);
        query.get().addOnSuccessListener(queryCommentShot -> {
            this.querySnapshot = queryCommentShot;
            mCallback.getFirstQueryResult(queryCommentShot);
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

    public void setCommentReplyQuery(DocumentReference commentRef, String field) {
        querySnapshot = null;
        this.field = field;
        query = commentRef.collection("replies").orderBy(field, Query.Direction.DESCENDING).limit(PAGING_REPLY);
        query.get().addOnSuccessListener(queryReplyShot -> {
            this.querySnapshot = queryReplyShot;
            mCallback.getFirstQueryResult(queryReplyShot);
        }).addOnFailureListener(mCallback::getQueryErrorResult);
    }

    public void setNextCommentReplyQuery() {
        DocumentSnapshot lastVisible = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        query.startAfter(lastVisible).limit(PAGING_REPLY).get().addOnSuccessListener(replies ->{
            this.querySnapshot = replies;
            if(replies.size() >= PAGING_REPLY) mCallback.getNextQueryResult(replies);
            else mCallback.getLastQueryResult(querySnapshot);
        }).addOnFailureListener(mCallback::getQueryErrorResult);
    }


}
