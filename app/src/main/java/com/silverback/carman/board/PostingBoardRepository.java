package com.silverback.carman.board;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

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
    private int page;

    public PostingBoardRepository() {
        firestore = FirebaseFirestore.getInstance();
        colRef = firestore.collection("board_general");
    }

    public void setPostingQuery(int page) {
        query = colRef;
        this.page = page;

        switch(page) {
            case Constants.BOARD_RECENT:
                query = query.whereEqualTo("post_general", true)
                        .orderBy("timestamp", Query.Direction.DESCENDING).limit(Constants.PAGINATION);
                break;

            case Constants.BOARD_POPULAR:
                query = query.whereEqualTo("post_general", true)
                        .orderBy("cnt_view", Query.Direction.DESCENDING).limit(Constants.PAGINATION);
                break;

            case Constants.BOARD_NOTIFICATION:
                query = firestore.collection("board_admin")
                        .orderBy("timestamp", Query.Direction.DESCENDING).limit(Constants.PAGINATION);
                break;
        }

    }

    public void setCommentQuery(DocumentReference docRef){
        query = docRef.collection("comments").orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(Constants.PAGINATION);
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
