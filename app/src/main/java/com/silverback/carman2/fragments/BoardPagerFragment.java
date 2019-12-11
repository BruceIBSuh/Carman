package com.silverback.carman2.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.Query;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardRecyclerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FirestoreViewModel;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.PaginateRecyclerView;

import org.w3c.dom.Document;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardPagerFragment extends Fragment implements
        BoardRecyclerAdapter.OnRecyclerItemClickListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPagerFragment.class);

    // Objects
    private FirebaseFirestore firestore;
    private FragmentSharedModel fragmentModel;
    private BoardRecyclerAdapter recyclerAdapter;
    private PaginateRecyclerView paginateRecyclerView;
    private List<DocumentSnapshot> snapshotList;
    private SimpleDateFormat sdf;

    // Fields
    private int page;
    private boolean isScrolling;

    public BoardPagerFragment() {
        // Required empty public constructor
    }

    public static BoardPagerFragment newInstance(int page) {
        BoardPagerFragment fragment = new BoardPagerFragment();
        Bundle args = new Bundle();
        args.putInt("fragment", page);
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getActivity() == null) return;
        if(getArguments() != null) page = getArguments().getInt("fragment");

        firestore = FirebaseFirestore.getInstance();
        fragmentModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        snapshotList = new ArrayList<>();

        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        final int limit = 7;

        View localView = inflater.inflate(R.layout.fragment_billboard, container, false);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_billboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        CollectionReference colRef = firestore.collection("board_general");
        // Paginate the recyclerview with the preset limit.
        paginateRecyclerView = new PaginateRecyclerView(colRef, snapshotList, limit);

        switch(page) {
            case 0: // Recent post
                // Pagination should be programmed.
                Query firstQuery = colRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(limit);
                firstQuery.get().addOnSuccessListener(querySnapshot -> {
                    for(DocumentSnapshot snapshot : querySnapshot) snapshotList.add(snapshot);
                    paginateRecyclerView.setQuerySnapshot(querySnapshot);

                    // Get the last visible document in the first query.
                    /*
                    DocumentSnapshot lastDoc = querySnapshot.getDocuments().get(querySnapshot.size() - 1);
                    Query nextQuery = colRef.orderBy("timestamp", Query.Direction.DESCENDING)
                            .startAfter(lastDoc).limit(5);
                    */

                    recyclerAdapter = new BoardRecyclerAdapter(snapshotList, this);
                    recyclerView.setAdapter(recyclerAdapter);
                    recyclerView.addOnScrollListener(paginateRecyclerView);

                });


                break;

            case 1: // Popular post
                snapshotList.clear();
                colRef.orderBy("cnt_view", Query.Direction.DESCENDING).limit(25)
                        .get().addOnSuccessListener(querySnapshot -> {
                            recyclerAdapter = new BoardRecyclerAdapter(snapshotList, this);
                            recyclerView.setAdapter(recyclerAdapter);
                });
                break;

            case 2:
                break;

            case 3:
                break;

            default:
                break;
        }

        return localView;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
    }

    // Callback invoked by BoardRecyclerAdapter.OnRecyclerItemClickListener when an item is clicked.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onPostItemClicked(DocumentSnapshot snapshot, int position) {
        log.i("Post item clicked");
        // Initiate the task to query the board collection and the user collection.
        // Show the dialog with the full screen. The container is android.R.id.content.
        BoardReadDlgFragment postDialogFragment = new BoardReadDlgFragment();

        Bundle bundle = new Bundle();
        bundle.putString("postTitle", snapshot.getString("post_title"));
        bundle.putString("userName", snapshot.getString("user_name"));
        bundle.putString("userPic", snapshot.getString("user_pic"));
        bundle.putString("postContent", snapshot.getString("post_content"));
        bundle.putStringArrayList("imageUriList", (ArrayList<String>)snapshot.get("post_images"));
        bundle.putString("timestamp", sdf.format(snapshot.getDate("timestamp")));
        bundle.putString("userId", snapshot.getString("user_id"));

        postDialogFragment.setArguments(bundle);

        getFragmentManager().beginTransaction()
                .add(android.R.id.content, postDialogFragment)
                .addToBackStack(null)
                .commit();


        // Update the field of "cnt_view" increasing the number.
        DocumentReference docref = snapshot.getReference();
        docref.update("cnt_view", FieldValue.increment(1));

        // Listener to events for local changes, which will be notified with the new data before
        // the data is sent to the backend.
        docref.addSnapshotListener(MetadataChanges.INCLUDE, (data, e) ->{
            if(e != null) {
                log.e("SnapshotListener erred: %s", e.getMessage());
                return;
            }

            String source = data != null && data.getMetadata().hasPendingWrites()?"Local":"Servier";
            if(data != null && data.exists()) {
                log.i("source: %s", source + "data: %s" + data.getData());
                recyclerAdapter.notifyItemChanged(position, data.getLong("cnt_view"));
            }
        });

    }



}
