package com.silverback.carman2.fragments;


import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.silverback.carman2.BoardActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardPostingAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.PaginationHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 */
public class BoardPagerFragment extends Fragment implements
        PaginationHelper.OnPaginationListener,
        BoardPostingAdapter.OnRecyclerItemClickListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPagerFragment.class);

    // Objects
    //private FirebaseFirestore firestore;
    private BoardPostingAdapter recyclerAdapter;
    private List<DocumentSnapshot> snapshotList;
    private SimpleDateFormat sdf;

    // UIs
    private ProgressBar pagingProgressBar;

    // Fields
    private int page;

    // Constructor
    private BoardPagerFragment() {
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

        //firestore = FirebaseFirestore.getInstance();
        snapshotList = new ArrayList<>();
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_board_list, container, false);
        pagingProgressBar = localView.findViewById(R.id.progressBar);
        RecyclerView recyclerView = localView.findViewById(R.id.recycler_billboard);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        recyclerAdapter = new BoardPostingAdapter(snapshotList, this);
        recyclerView.setAdapter(recyclerAdapter);

        // Paginate the recyclerview with the preset limit.
        //final CollectionReference colRef = firestore.collection("board_general");
        PaginationHelper paginationHelper = new PaginationHelper();
        paginationHelper.setOnPaginationListener(this);
        recyclerView.addOnScrollListener(paginationHelper);

        if(snapshotList != null && snapshotList.size() > 0) snapshotList.clear();
        //if(getActivity() != null) ((BoardActivity)getActivity()).handleFabVisibility();

        switch(page) {
            case 0: // Recent post
                paginationHelper.setPostingQuery("timestamp", Constants.PAGINATION);
                break;

            case 1: // Popular post
                paginationHelper.setPostingQuery("cnt_view", Constants.PAGINATION);
                break;

            case 2: // Info n Tips
                //if(getActivity() != null) ((BoardActivity)getActivity()).handleFabVisibility();
                break;

            case 3: // Auto Club

                break;

            default:
                break;
        }

        return localView;
    }


    // Implement the callbacks of PaginationHelper.OnPaginationListener which notifies the adapter
    // of the first and the next query result.
    @Override
    public void setFirstQuery(QuerySnapshot snapshot) {
        for(DocumentSnapshot document : snapshot) snapshotList.add(document);
        recyclerAdapter.notifyDataSetChanged();

    }
    @Override
    public void setNextQueryStart(boolean b) {
        pagingProgressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void setNextQueryComplete(QuerySnapshot querySnapshot) {
        for(DocumentSnapshot document : querySnapshot) snapshotList.add(document);
        pagingProgressBar.setVisibility(View.INVISIBLE);
        recyclerAdapter.notifyDataSetChanged();
    }


    // Callback invoked by BoardPostingAdapter.OnRecyclerItemClickListener when an item is clicked.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onPostItemClicked(DocumentSnapshot snapshot, int position) {
        // Initiate the task to query the board collection and the user collection.
        // Show the dialog with the full screen. The container is android.R.id.content.
        BoardReadDlgFragment postDialogFragment = new BoardReadDlgFragment();
        Bundle bundle = new Bundle();
        bundle.putString("documentId", snapshot.getId());
        bundle.putString("userId", snapshot.getString("user_id"));
        bundle.putString("postTitle", snapshot.getString("post_title"));
        bundle.putString("userName", snapshot.getString("user_name"));
        bundle.putString("userPic", snapshot.getString("user_pic"));
        bundle.putInt("cntComment", snapshot.getLong("cnt_comment").intValue());
        bundle.putInt("cntCompathy", snapshot.getLong("cnt_compathy").intValue());
        bundle.putString("postContent", snapshot.getString("post_content"));
        bundle.putStringArrayList("imageUriList", (ArrayList<String>)snapshot.get("post_images"));
        bundle.putString("timestamp", sdf.format(snapshot.getDate("timestamp")));

        postDialogFragment.setArguments(bundle);

        // What if Fragment calls another fragment? What is getChildFragmentManager() for?
        getActivity().getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, postDialogFragment)
                //.addToBackStack(null)
                .commit();


        // Update the field of "cnt_view" increasing the number.
        DocumentReference docref = snapshot.getReference();
        docref.update("cnt_view", FieldValue.increment(1));

        // Listener to events for local changes, which will be notified with the new data before
        // the data is sent to the backend.
        docref.addSnapshotListener(MetadataChanges.INCLUDE, (data, e) ->{
            if(e != null) return;
            //String source = data != null && data.getMetadata().hasPendingWrites()?"Local":"Servier";
            if(data != null && data.exists()) {
                //log.i("source: %s", source + "data: %s" + data.getData());
                recyclerAdapter.notifyItemChanged(position, data.getLong("cnt_view"));
                recyclerAdapter.notifyItemChanged(position, data.getLong("cnt_comment"));
            }
        });
    }

}
