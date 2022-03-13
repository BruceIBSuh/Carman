package com.silverback.carman.board;

import static com.silverback.carman.BoardActivity.NOTIFICATION;
import static com.silverback.carman.BoardActivity.PAGING_POST;
import static com.silverback.carman.BoardActivity.POPULAR;
import static com.silverback.carman.BoardActivity.RECENT;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class PostingBoardRepository implements
        PostingBoardViewModel.PostingBoardLiveDataCallback,
        PostingBoardLiveData.OnLastVisibleListener,
        PostingBoardLiveData.OnLastPageListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(PostingBoardRepository.class);

    private final FirebaseFirestore firestore;
    private final CollectionReference colRef;
    private Query query;
    private DocumentSnapshot lastVisibleshot;
    private boolean isLastPage;
    //private int page;

    public PostingBoardRepository() {
        firestore = FirebaseFirestore.getInstance();
        colRef = firestore.collection("board_general");
    }

    public void setPostingQuery(int page) {
        query = colRef;
        //this.page = page;

        switch(page) {
            case RECENT:
                query = query.whereEqualTo("post_general", true)
                        .orderBy("timestamp", Query.Direction.DESCENDING).limit(PAGING_POST);
                break;

            case POPULAR:
                query = query.whereEqualTo("post_general", true)
                        .orderBy("cnt_view", Query.Direction.DESCENDING).limit(PAGING_POST);
                break;

            case NOTIFICATION:
                query = firestore.collection("board_admin")
                        .orderBy("timestamp", Query.Direction.DESCENDING).limit(PAGING_POST);
                break;
        }

    }

    public void setCommentQuery(DocumentReference docRef){
        query = docRef.collection("comments").orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(PAGING_POST);
    }

    // Implement PostingBoardViewModel.PostingBoardLiveDataCallback to instantiate PostingBoardLiveData.class
    // with params, the result of which should be notified to the view(BoardPagerFragment).
    @Override
    public PostingBoardLiveData getPostingBoardLiveData() {
        if(isLastPage) return null;
        if(lastVisibleshot != null) query = query.startAfter(lastVisibleshot);
        return new PostingBoardLiveData(query, this, this);
    }

    @Override
    public void setLastVisible(DocumentSnapshot lastVisibleshot) {
        this.lastVisibleshot = lastVisibleshot;
    }

    @Override
    public void setLastPage(boolean isLastPage) {
        this.isLastPage = isLastPage;
    }

}
