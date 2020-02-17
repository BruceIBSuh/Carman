package com.silverback.carman2.fragments;


import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardPostingAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.utils.PaginationHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class BoardPagerFragment extends Fragment implements
        PaginationHelper.OnPaginationListener,
        BoardPostingAdapter.OnRecyclerItemClickListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPagerFragment.class);

    // Objects
    private Source source;
    private FragmentSharedModel sharedModel;
    private BoardPostingAdapter postingAdapter;
    private PaginationHelper pageHelper;
    private List<DocumentSnapshot> snapshotList;
    private SimpleDateFormat sdf;

    // UIs
    private ProgressBar pagingProgbar;
    private FloatingActionButton fabWrite;

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

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null) page = getArguments().getInt("fragment");

        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
        sharedModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);
        snapshotList = new ArrayList<>();
        postingAdapter = new BoardPostingAdapter(snapshotList, this);
        pageHelper = new PaginationHelper();
        pageHelper.setOnPaginationListener(this);


        // When initially connecting to Firestore, the snapshot listener checks if there is any
        // changes in the borad and upadte the posting board. On completing the inital update,
        // the lisitener should be detached for purpose of preventing excessive connection to the
        // server.
        CollectionReference postRef = FirebaseFirestore.getInstance().collection("board_general");
        ListenerRegistration postListener = postRef.addSnapshotListener((querySnapshot, e) -> {
            if(e != null) return;
            source = querySnapshot != null && querySnapshot.getMetadata().hasPendingWrites()?
                   Source.CACHE  : Source.SERVER ;
            log.i("Source: %s", source);
        });
        postListener.remove();

        CollectionReference userRef = FirebaseFirestore.getInstance().collection("users");
        ListenerRegistration userListener = userRef.addSnapshotListener((querySnapshot, e) -> {
            if(e != null) return;
            String source = querySnapshot != null && querySnapshot.getMetadata().hasPendingWrites()?
                    "Local" : "Server";
            log.i("Source: %s", source);
        });
        userListener.remove();

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_board_pager, container, false);
        pagingProgbar = localView.findViewById(R.id.progbar_paging);
        fabWrite = localView.findViewById(R.id.fab_board_write);
        RecyclerView recyclerPostView = localView.findViewById(R.id.recycler_board);

        recyclerPostView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerPostView.setAdapter(postingAdapter);
        // Show/hide Floating Action Button as the recyclerview scrolls.
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
        // Paginate the recyclerview with the preset limit. PaginationHelper subclasses RecyclerView.
        // OnScrollListner.
        recyclerPostView.addOnScrollListener(pageHelper);

        // Floating Action Button to show BoardReadDlgFragment which reads a post when clicking it.
        // Also, as the reyclcerview scrolls, the button hides itself and the button appears again
        // when the scroll stops.
        fabWrite.setSize(FloatingActionButton.SIZE_AUTO);
        fabWrite.setOnClickListener(view -> {
            // MUST initialize the model to prevent getImageObserver() in BoardWriteDlgFragment from
            // automatically invoking startActivityForResult() when the fragment pops up.
            sharedModel.getImageChooser().setValue(-1);

            BoardWriteDlgFragment writePostFragment = new BoardWriteDlgFragment();
            getActivity().getSupportFragmentManager().beginTransaction()
                    .add(android.R.id.content, writePostFragment)
                    .commit();
        });

        // Get the field name of each fragment in the viewpager and query the posting items using
        // PaginationHelper which sends the dataset back to the callbacks such as setFirstQuery(),
        // setNextQueryStart(), and setNextQueryComplete().
        //if(snapshotList != null && snapshotList.size() > 0) snapshotList.clear();
        String field = getQueryFieldToViewPager(page);
        pageHelper.setPostingQuery(source, field);

        return localView;
    }

    // This lifecycle is invoked at the time not only the viewpager sets the adapter first time,
    // but also each time the viewpager chages the page. Thus, the viewmodels should prevent
    // listeners from running automatically with params given as conditions.
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        log.i("onActivityCreated");
        // Notified that uploading the post has completed by UploadPostTask.
        // It seems not working. What if Srouce.CACHE options are applied?
        sharedModel.getNewPosting().observe(getActivity(), documentId -> {
            log.i("New posting: %s", page);
            if(!TextUtils.isEmpty(documentId)) {
                snapshotList.clear();
                String field = getQueryFieldToViewPager(page);
                pageHelper.setPostingQuery(Source.CACHE, field);
            }
        });

        // The post has been deleted in BoardReadDlgFragment which sequentially popped up AlertDialog
        // for confirmation and the result sent back, then deleted the posting item from Firestore.
        // With All done, receive another LiveData containing the postion of the deleted posting item
        // and update the adapter.
        sharedModel.getRemovedPosting().observe(getActivity(), docId -> {
            log.i("Posting removed: %s", docId);
            if(!TextUtils.isEmpty(docId)) {
                snapshotList.clear();
                String field = getQueryFieldToViewPager(page);
                pageHelper.setPostingQuery(Source.CACHE, field);
            }
        });


    }


    // Implement the callbacks of PaginationHelper.OnPaginationListener which notifies the adapter
    // of the first and the next query result.
    @Override
    public void setFirstQuery(QuerySnapshot snapshot) {
        for(DocumentSnapshot document : snapshot) snapshotList.add(document);
        postingAdapter.notifyDataSetChanged();

    }
    @Override
    public void setNextQueryStart(boolean b) {
        pagingProgbar.setVisibility(View.VISIBLE);
    }

    @Override
    public void setNextQueryComplete(QuerySnapshot querySnapshot) {
        for(DocumentSnapshot document : querySnapshot) snapshotList.add(document);
        pagingProgbar.setVisibility(View.INVISIBLE);
        postingAdapter.notifyDataSetChanged();
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
        bundle.putInt("position", position);
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
                postingAdapter.notifyItemChanged(position, data.getLong("cnt_view"));
                postingAdapter.notifyItemChanged(position, data.getLong("cnt_comment"));
            }
        });
    }

    private String getQueryFieldToViewPager(int page) {
        switch(page) {
            case 0: // Recent
                return "timestamp";
            case 1: // Popular
                return "cnt_view";
            case 2: // Auto Club
                return "autoclub";
            case 3: // Notification
                return "notificaiton";
            default: return null;
        }
    }

}
