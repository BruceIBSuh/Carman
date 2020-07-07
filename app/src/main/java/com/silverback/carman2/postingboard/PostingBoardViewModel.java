package com.silverback.carman2.postingboard;

import androidx.lifecycle.ViewModel;

@SuppressWarnings("WeakerAccess")
public class PostingBoardViewModel extends ViewModel {

    final private PostingBoardRepo postingBoardRepo = new PostingBoardRepository();


    public interface PostingBoardRepo {
        PostingBoardLiveData getPostingBoardLiveData();
    }

    public PostingBoardLiveData getPostingBoardLiveData() {
        return postingBoardRepo.getPostingBoardLiveData();
    }


}
