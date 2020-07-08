package com.silverback.carman2.postingboard;

import androidx.lifecycle.ViewModel;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

//@SuppressWarnings("WeakerAccess")
public class PostingBoardViewModel extends ViewModel {

    private static final LoggingHelper log = LoggingHelperFactory.create(PostingBoardViewModel.class);

    // Not only instantiate PostingBoardRepository but also implement PostingBoardLiveDataCallback
    // interface at the same time.
    private PostingBoardLiveDataCallback mLiveDataCallback = new PostingBoardRepository();

    public interface PostingBoardLiveDataCallback {
        PostingBoardLiveData getPostingBoardLiveData();
    }
    // Referenced in getBoardPost()
    public PostingBoardLiveData getPostingBoardLiveData() {
        return mLiveDataCallback.getPostingBoardLiveData();
    }


}
