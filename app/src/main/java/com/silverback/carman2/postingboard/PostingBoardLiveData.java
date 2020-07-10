package com.silverback.carman2.postingboard;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;

import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Query;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

/**
 * MVVM Pattenr based query and pagination class for the posting board.
 */
public class PostingBoardLiveData extends LiveData<PostingBoardOperation> implements EventListener<QuerySnapshot> {

    private final static LoggingHelper log = LoggingHelperFactory.create(PostingBoardLiveData.class);

    // Objects
    private Query query;
    private ListenerRegistration listenerRegit;
    private OnLastVisibleListener lastVisibleCallback;
    private OnLastPageListener lastPostCallback;

    private int page;

    // Interface
    public interface OnLastVisibleListener {
        void setLastVisible(DocumentSnapshot lastVisible);
    }

    public interface OnLastPageListener {
        void setLastPage(boolean isLastPage);
    }


    // Constructor
    public PostingBoardLiveData(Query query, int page,
            OnLastVisibleListener lastVisbleCallback, OnLastPageListener lastPostCallback) {

        this.query = query;
        this.lastVisibleCallback = lastVisbleCallback;
        this.lastPostCallback = lastPostCallback;
        this.page = page;

    }

    // LiveData has the following methods to get notified when number of active Observers changes
    // b/w 0(inactive) and 1(active), which allows LiveData to release any heavy resources when it
    // does not have any Observers not actively observing.
    @Override
    protected void onActive() {
        log.i("onActive() state");
        listenerRegit = query.addSnapshotListener(MetadataChanges.INCLUDE, this);
    }

    @Override
    protected void onInactive() {
        listenerRegit.remove();
    }

    @Override
    public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException e) {
        if(e != null || querySnapshot == null) return;

        for(DocumentChange documentChange : querySnapshot.getDocumentChanges()) {
            log.i("LiveData page: %s", page);
            switch(documentChange.getType()) {
                case ADDED:
                    DocumentSnapshot addShot = documentChange.getDocument();
                    PostingBoardOperation addPost = new PostingBoardOperation(addShot, 0);
                    setValue(addPost);
                    break;

                case MODIFIED:
                    DocumentSnapshot modifyShot = documentChange.getDocument();
                    PostingBoardOperation modifyPost = new PostingBoardOperation(modifyShot, 1);
                    setValue(modifyPost);
                    break;

                case REMOVED:
                    DocumentSnapshot removeShot = documentChange.getDocument();
                    PostingBoardOperation removePost = new PostingBoardOperation(removeShot, 2);
                    setValue(removePost);
                    break;
            }
        }

        // Listeners are notified of the last visible post and the last post.
        final int shotSize = querySnapshot.size();
        if(shotSize < Constants.PAGINATION) {
            lastPostCallback.setLastPage(true);
        } else {
            DocumentSnapshot lastVisibleShot = querySnapshot.getDocuments().get(shotSize - 1);
            lastVisibleCallback.setLastVisible(lastVisibleShot);
        }
    }

}
