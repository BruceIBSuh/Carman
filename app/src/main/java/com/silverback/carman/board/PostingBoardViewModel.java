package com.silverback.carman.board;

import androidx.lifecycle.ViewModel;

import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

@SuppressWarnings("WeakerAccess")
public class PostingBoardViewModel extends ViewModel {

    private static final LoggingHelper log = LoggingHelperFactory.create(PostingBoardViewModel.class);

    // Objects
    private PostingBoardLiveDataCallback mCallback;

    // Implement to get the livedata in the repo.
    public interface PostingBoardLiveDataCallback {
        PostingBoardLiveData getPostingBoardLiveData();
    }

    // Constructor
    public PostingBoardViewModel(PostingBoardRepository repo) {
        mCallback = (PostingBoardLiveDataCallback) repo;
    }


    public PostingBoardLiveData getPostingBoardLiveData() {
        return mCallback.getPostingBoardLiveData();
    }
}