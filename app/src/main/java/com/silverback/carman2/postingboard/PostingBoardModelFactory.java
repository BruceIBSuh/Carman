package com.silverback.carman2.postingboard;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class PostingBoardModelFactory implements ViewModelProvider.Factory {

    private static final LoggingHelper log = LoggingHelperFactory.create(PostingBoardModelFactory.class);

    private int page;
    private boolean isViewOrder;

    public PostingBoardModelFactory(int page, boolean isViewOrder) {
        this.page = page;
        this.isViewOrder = isViewOrder;
        log.i("params: %s, %s", page, isViewOrder);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if(modelClass.equals(PostingBoardViewModel.class)) {
            return (T) new PostingBoardViewModel(page, isViewOrder);

        } else throw new IllegalArgumentException("unexpected model class: " + modelClass);
    }
}
