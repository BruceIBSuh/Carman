package com.silverback.carman2.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;
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
        View.OnClickListener,
        PaginationHelper.OnPaginationListener,
        BoardPostingAdapter.OnRecyclerItemClickListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPagerFragment.class);

    // Objects
    //private FirebaseFirestore firestore;
    private BoardPostingAdapter recyclerAdapter;
    private List<DocumentSnapshot> snapshotList;
    private SimpleDateFormat sdf;

    // UIs
    private ProgressBar pagingPB;

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

        //if(getActivity() == null) return;
        if(getArguments() != null) page = getArguments().getInt("fragment");

        //firestore = FirebaseFirestore.getInstance();
        snapshotList = new ArrayList<>();
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_board_pager, container, false);
        pagingPB = localView.findViewById(R.id.progbar_paging);
        FloatingActionButton fabWrite = localView.findViewById(R.id.fab_board_write);
        RecyclerView recyclerPostView = localView.findViewById(R.id.recycler_board);
        recyclerPostView.setLayoutManager(new LinearLayoutManager(getContext()));

        recyclerAdapter = new BoardPostingAdapter(snapshotList, this);
        recyclerPostView.setAdapter(recyclerAdapter);

        // Floating Action Button to show BoardReadDlgFragment which reads a post when clicking it.
        // Also, as the reyclcerview scrolls, the button hides itself and the button appears again
        // when the scroll stops.
        fabWrite.setOnClickListener(this);
        fabWrite.setSize(FloatingActionButton.SIZE_AUTO);
        recyclerPostView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 || dy < 0 && fabWrite.isShown()) fabWrite.hide();
            }
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) fabWrite.show();
                super.onScrollStateChanged(recyclerView, newState);
            }
        });


        // Paginate the recyclerview with the preset limit.
        //final CollectionReference colRef = firestore.collection("board_general");
        PaginationHelper paginationHelper = new PaginationHelper();
        paginationHelper.setOnPaginationListener(this);
        recyclerPostView.addOnScrollListener(paginationHelper);

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
        pagingPB.setVisibility(View.VISIBLE);
    }

    @Override
    public void setNextQueryComplete(QuerySnapshot querySnapshot) {
        for(DocumentSnapshot document : querySnapshot) snapshotList.add(document);
        pagingPB.setVisibility(View.INVISIBLE);
        recyclerAdapter.notifyDataSetChanged();
    }


    // Implement the callback of BoardPostingAdapter.OnRecyclerItemClickListener when an item is clicked.
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public void onPostItemClicked(DocumentSnapshot snapshot, int position) {
        // Initiate the task to query the board collection and the user collection.
        // Show the dialog with the full screen. The container is android.R.id.content.
        BoardReadDlgFragment postDialogFragment = new BoardReadDlgFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("tabPage", page);
        bundle.putString("documentId", snapshot.getId());
        bundle.putString("userId", snapshot.getString("user_id"));
        bundle.putString("postTitle", snapshot.getString("post_title"));
        bundle.putString("userName", snapshot.getString("user_name"));
        bundle.putString("userPic", snapshot.getString("user_pic"));
        bundle.putInt("cntComment", snapshot.getLong("cnt_comment").intValue());
        bundle.putInt("cntCompathy", snapshot.getLong("cnt_compathy").intValue());
        bundle.putString("postContent", snapshot.getString("post_content"));
        bundle.putStringArrayList("uriImgList", (ArrayList<String>)snapshot.get("post_images"));
        bundle.putString("timestamp", sdf.format(snapshot.getDate("timestamp")));

        postDialogFragment.setArguments(bundle);

        // What if Fragment calls another fragment? What is getChildFragmentManager() for?
        // android.R.id.content makes DialogFragment fit to the full screen.
        getActivity().getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, postDialogFragment)
                .addToBackStack(null)
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

    @Override
    public void onClick(View v) {

    }
}
