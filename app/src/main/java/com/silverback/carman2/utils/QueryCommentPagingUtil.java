package com.silverback.carman2.utils;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;


public class QueryCommentPagingUtil {

    private static final LoggingHelper log = LoggingHelperFactory.create(QueryCommentPagingUtil.class);

    private OnQueryPaginationCallback mCallback;
    private CollectionReference colRef;
    private QuerySnapshot querySnapshot;

    // Interface
    public interface OnQueryPaginationCallback {
        void getFirstQueryResult(QuerySnapshot postShots);
        void getNextQueryResult(QuerySnapshot nextShots);
        void getLastQueryResult(QuerySnapshot lastShots);
    }

    // Constructor
    public QueryCommentPagingUtil(FirebaseFirestore firestore, QueryCommentPagingUtil.OnQueryPaginationCallback callback) {
        colRef = firestore.collection("board_general");
        mCallback = callback;
    }

    public void setCommentQuery(DocumentReference docRef){
        querySnapshot = null;
        docRef.collection("comments").orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(Constants.PAGINATION)
                .addSnapshotListener((commentSnapshot, e) -> {
                    // exceepthion handling required
                    if(e != null || commentSnapshot == null) return;

                    // What if the first query comes to the last page? "isLoading" field in BoardPagerFragment
                    // is set to true, which disables the recyclerview scroll listener to call setNextQuery().
                    this.querySnapshot = commentSnapshot;
                    mCallback.getFirstQueryResult(commentSnapshot);
                });
    }

    public void setNextQuery() {
        DocumentSnapshot lastVisibleShot = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
        colRef.orderBy("timestamp", Query.Direction.DESCENDING).startAfter(lastVisibleShot)
                .limit(Constants.PAGINATION)
                .addSnapshotListener((nextSnapshot, e) -> {
                    if(e != null || nextSnapshot == null) return;

                    /*
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

                     */

                    if(nextSnapshot.size() < Constants.PAGINATION) {
                        log.i("query last: %s", nextSnapshot.size());
                        querySnapshot = null;
                        mCallback.getLastQueryResult(nextSnapshot);

                    } else {
                        log.i("query next: %s", nextSnapshot.size());
                        this.querySnapshot = nextSnapshot;
                        mCallback.getNextQueryResult(nextSnapshot);
                    }
                });

    }
}
