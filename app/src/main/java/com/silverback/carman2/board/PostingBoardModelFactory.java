package com.silverback.carman2.board;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class PostingBoardModelFactory implements ViewModelProvider.Factory {

    private static final LoggingHelper log = LoggingHelperFactory.create(PostingBoardModelFactory.class);

    private PostingBoardRepository repo;


    public PostingBoardModelFactory(PostingBoardRepository repo) {
        this.repo = repo;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if(modelClass.equals(PostingBoardViewModel.class)) {
            return (T) new PostingBoardViewModel(repo);

        } else throw new IllegalArgumentException("unexpected model class: " + modelClass);
    }
}
