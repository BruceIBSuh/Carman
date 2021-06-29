package com.silverback.carman.utils;

import androidx.databinding.Observable;
import androidx.databinding.ObservableArrayList;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.List;

public class DispRecentPost {

    private static final LoggingHelper log = LoggingHelperFactory.create(DispRecentPost.class);

    public List<String> titleList;
    public ObservableArrayList<String> observableArrayList;

    public DispRecentPost(QuerySnapshot querySnapshot) {
        titleList = new ArrayList<>();
        observableArrayList = new ObservableArrayList<>();
        for(QueryDocumentSnapshot doc : querySnapshot) {
            log.i("post title: %s", doc.getString("post_title"));
            titleList.add(doc.getString("post_title"));
        }
    }
}
