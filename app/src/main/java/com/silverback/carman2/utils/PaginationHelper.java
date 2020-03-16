package com.silverback.carman2.utils;

import android.widget.AbsListView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * This class is to paginate the posting items which is handled in BoardPagerFragment which implements
 * OnPaginationListener to have document snaoshots from FireStore.
 */
public class PaginationHelper extends RecyclerView.OnScrollListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(PaginationHelper.class);

    // Objects
    private FirebaseFirestore firestore;
    private CollectionReference colRef;
    private QuerySnapshot querySnapshot;
    private OnPaginationListener mListener;

    // Fields
    private boolean isScrolling;
    private boolean isLastItem;
    private String field;

    // Interface w/ BoardPagerFragment to notify the state of querying process and pagination.
    public interface OnPaginationListener {
        void setFirstQuery(QuerySnapshot snapshot);
        void setNextQueryStart(boolean b);
        void setNextQueryComplete(QuerySnapshot querySnapshot);
    }

    // private constructor
    public PaginationHelper() {
        firestore = FirebaseFirestore.getInstance();
    }

    // Method for implementing the inteface in BoardPagerFragment, which notifies the caller of
    // having QuerySnapshot retrieved.
    public void setOnPaginationListener(OnPaginationListener listener) {
        mListener = listener;
    }

    // Create queries for each page.
    public void setPostingQuery(Source source, int page, List<String> filters) {
        colRef = firestore.collection("board_general");
        switch(page) {
            case Constants.BOARD_RECENT:
                this.field = "timestamp";
                colRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(Constants.PAGINATION)
                        .get(source)
                        .addOnSuccessListener(querySnapshot -> {
                            this.querySnapshot = querySnapshot;
                            mListener.setFirstQuery(querySnapshot);
                        })
                        .addOnFailureListener(Throwable::printStackTrace);
                break;

            case Constants.BOARD_POPULAR:
                this.field = "cnt_view";

                colRef.orderBy("cnt_view", Query.Direction.DESCENDING).limit(Constants.PAGINATION)
                        .get(source)
                        .addOnSuccessListener(querySnapshot -> {
                            this.querySnapshot = querySnapshot;
                            mListener.setFirstQuery(querySnapshot);
                        })
                        .addOnFailureListener(Throwable::printStackTrace);
                break;

            case Constants.BOARD_AUTOCLUB:
                this.field = "auto_club";

                /*
                List<String> autovalueList = new ArrayList<>();
                //for(boolean b : arrAutoValues) autovalueList.add(b);
                autovalueList.add("현대차");
                autovalueList.add("Palisade");
                */
                for(String filter : filters) log.i("Filter: %s", filter);

                colRef.whereEqualTo("auto_club", filters)
                //colRef.whereArrayContains("auto_club", true)
                        // orderBy field must be the same as the field used in where_. Otherwise,
                        // it does not work.
                        //.orderBy("timestamp", Query.Direction.DESCENDING).limit(Constants.PAGINATION)
                        .get()
                        .addOnSuccessListener(autoclubShot -> {
                            log.i("auto_club query: %s", autoclubShot.size());
                            this.querySnapshot = autoclubShot;
                            mListener.setFirstQuery(autoclubShot);
                        }).addOnFailureListener(Throwable::printStackTrace);
                break;

            case Constants.BOARD_NOTIFICATION: // notification
                break;
        }
    }


    public void setCommentQuery(Source source, final String field, final String docId) {
        this.field = field;
        colRef = firestore.collection("board_general").document(docId).collection("comments");
        colRef.orderBy(field, Query.Direction.DESCENDING).limit(Constants.PAGINATION)
                .get(source)
                .addOnSuccessListener(querySnapshot -> {
                    this.querySnapshot = querySnapshot;
                    mListener.setFirstQuery(querySnapshot);
                });
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        super.onScrollStateChanged(recyclerView, newState);
        if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
            isScrolling = true;
        }
    }


    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);

        LinearLayoutManager layoutManager = (LinearLayoutManager)recyclerView.getLayoutManager();
        if(layoutManager == null || dy == 0) return;

        int firstItemPos = layoutManager.findFirstVisibleItemPosition();
        int visibleItemCount = layoutManager.getChildCount();
        int totalItemCount = layoutManager.getItemCount();

        if(isScrolling && (firstItemPos + visibleItemCount == totalItemCount) && !isLastItem) {
            log.i("Query next items");
            mListener.setNextQueryStart(true);
            isScrolling = false;

            // Get the last visible document in the first query, then make the next query following
            // the document using startAfter(). QuerySnapshot must be invalidated with the value by
            // nextQuery.
            DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
            log.i("last doc: %s", lastDoc.getString("post_title"));
            Query nextQuery = colRef.orderBy(field, Query.Direction.DESCENDING)
                    .startAfter(lastDoc).limit(Constants.PAGINATION);

            nextQuery.get().addOnSuccessListener(nextSnapshot -> {
                log.i("isLastItem: %s, %s", nextSnapshot.size(), Constants.PAGINATION);

                // Check if the next query reaches the last document.
                if((nextSnapshot.size()) < Constants.PAGINATION) isLastItem = true;

                mListener.setNextQueryComplete(nextSnapshot);
                querySnapshot = nextSnapshot;
            });

        }

    }
}
