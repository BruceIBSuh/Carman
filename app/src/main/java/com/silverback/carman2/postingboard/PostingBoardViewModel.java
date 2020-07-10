package com.silverback.carman2.postingboard;

import androidx.lifecycle.ViewModel;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

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
    public PostingBoardViewModel(int page) {
        mCallback = new PostingBoardRepository(page);
    }


    public PostingBoardLiveData getPostingBoardLiveData() {
        return mCallback.getPostingBoardLiveData();
    }


}
