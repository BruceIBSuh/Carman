package com.silverback.carman2.utils;

import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.adapters.BoardRecyclerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.List;

public class PaginateRecyclerView extends RecyclerView.OnScrollListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(PaginateRecyclerView.class);

    // Objects
    private BoardRecyclerAdapter recyclerAdapter;
    private CollectionReference colRef;
    private QuerySnapshot querySnapshot;
    private List<DocumentSnapshot> snapshotList;

    // Fields
    private boolean isScrolling = false;
    private boolean isLastItem = false;
    private int limit;

    // Constructor
    public PaginateRecyclerView(CollectionReference colref, List<DocumentSnapshot> list, int limit) {
        this.colRef = colref;
        this.limit = limit;
        this.snapshotList = list;
    }

    public void setQuerySnapshot(QuerySnapshot querySnapshot) {
        this.querySnapshot = querySnapshot;
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);

        if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            isScrolling = true;
            log.i("RecyclerView is scrolling");
        }
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        log.i("onScrolled");
        super.onScrolled(recyclerView, dx, dy);

        LinearLayoutManager layoutManager = (LinearLayoutManager)recyclerView.getLayoutManager();
        if(layoutManager == null) return;

        int firstItemPos = layoutManager.findFirstVisibleItemPosition();
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();
        log.i("Item Status by LayoutManager: %s, %s, %s", firstItemPos, visibleItemCount, totalItemCount);

        if(isScrolling && (firstItemPos + visibleItemCount == totalItemCount) && !isLastItem) {
            log.i("Pagination");
            isScrolling = false;

            // Get the last visible document in the first query, then make the next query using
            // startAfter().
            DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
            Query nextQuery = colRef.orderBy("timestamp", Query.Direction.DESCENDING)
                    .startAfter(lastDoc).limit(5);
            nextQuery.get().addOnSuccessListener(querySnapshot -> {
                if(querySnapshot.size() < limit) isLastItem = true;
                for(DocumentSnapshot snapshot : querySnapshot) {
                    log.i("document: %s", snapshot);
                    snapshotList.add(snapshot);
                }
            });

        }

    }



}
