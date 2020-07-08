package com.silverback.carman2.postingboard;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;

public class PostingBoardRepository implements
        PostingBoardViewModel.PostingBoardLiveDataCallback,
        PostingBoardLiveData.OnLastVisibleListener,
        PostingBoardLiveData.OnLastPostListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(PostingBoardRepository.class);

    private FirebaseFirestore firestore;
    private CollectionReference colRef;
    private Query query;
    private DocumentSnapshot lastVisibleshot;
    private boolean isLastPage;

    public PostingBoardRepository(int page, boolean isViewOrder) {
        firestore = FirebaseFirestore.getInstance();
        colRef = firestore.collection("board_general");
        setPostingQuery(page, isViewOrder);
        log.i("page and sort: %s, %s", page, isViewOrder);
    }

    public void setPostingQuery(int page, boolean isViewOrder) {
        query = colRef;
        switch(page) {
            case Constants.BOARD_RECENT:
                //this.field = "timestamp";
                query = query.orderBy("timestamp", Query.Direction.DESCENDING).limit(Constants.PAGINATION);
                break;

            case Constants.BOARD_POPULAR:
                //this.field = "cnt_view";
                query = query.orderBy("cnt_view", Query.Direction.DESCENDING).limit(Constants.PAGINATION);
                break;

            case Constants.BOARD_AUTOCLUB:
                String field = (isViewOrder)? "cnt_view" : "timestamp";
                query = query.orderBy(field, Query.Direction.DESCENDING).limit(Constants.PAGINATION);
                break;

            // Should create a new collection managed by Admin.(e.g. board_admin)
            case Constants.BOARD_NOTIFICATION:
                query = firestore.collection("board_admin").orderBy("timestamp", Query.Direction.DESCENDING);
                break;
        }

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
