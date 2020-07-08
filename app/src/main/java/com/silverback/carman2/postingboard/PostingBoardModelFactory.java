package com.silverback.carman2.postingboard;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class PostingBoardModelFactory implements ViewModelProvider.Factory{

    private int page;
    private boolean isViewOrder;

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return PostingBoardViewModel;
    }

}
