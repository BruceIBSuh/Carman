package com.silverback.carman2.fragments;


import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import com.silverback.carman2.BoardActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardPagerAdapter;
import com.silverback.carman2.adapters.BoardPostingAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.viewmodels.BoardViewModel;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.PaginationHelper;
import com.silverback.carman2.views.PostingRecyclerView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

/**
 * A simple {@link Fragment} subclass.
 *
 */
public class BoardPagerFragment extends Fragment implements
        BoardActivity.OnFilterCheckBoxListener,
        PaginationHelper.OnPaginationListener, //CheckBox.OnCheckedChangeListener,
        BoardPostingAdapter.OnRecyclerItemClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPagerFragment.class);

    // Objects
    private FirebaseFirestore firestore;
    private Source source;
    private ListenerRegistration postListener;
    private FragmentSharedModel fragmentModel;
    private BoardViewModel boardModel;
    private BoardPagerAdapter pagerAdapter;
    private BoardPostingAdapter postingAdapter;
    private PaginationHelper pageHelper;
    private List<DocumentSnapshot> snapshotList;
    private SimpleDateFormat sdf;
    // prevent the progressbar from leaking in the static fragment, use the weak reference.
    //private WeakReference<ProgressBar> weakProgbar;

    // UIs
    private ProgressBar pbPaging, pbLoading;
    private PostingRecyclerView recyclerPostView;
    private TextView tvEmptyView;

    // Fields
    private ArrayList<CharSequence> autoFilter;
    private int currentPage;
    private boolean isGeneralPost;

    // Constructor
    private BoardPagerFragment() {
        // Required empty public constructor
    }

    // Singleton for not AutoClub pages.
    public static BoardPagerFragment newInstance(int page) {
        BoardPagerFragment fragment = new BoardPagerFragment();
        Bundle arg = new Bundle();
        arg.putInt("currentPage", page);
        fragment.setArguments(arg);

        return fragment;

    }

    // Singleton for AutoClub currentPage which has the checkbox values and title names.
    //public static BoardPagerFragment newInstance(int currentPage, String cbName, boolean[] cbValue) {
    public static BoardPagerFragment newInstance(int page, ArrayList<CharSequence> values) {
        BoardPagerFragment fragment = new BoardPagerFragment();
        Bundle args = new Bundle();
        args.putInt("currentPage", page);
        args.putCharSequenceArrayList("autoFilter", values);
        fragment.setArguments(args);

        return fragment;
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null) {
            currentPage = getArguments().getInt("currentPage");
            if(currentPage == Constants.BOARD_AUTOCLUB) {
                autoFilter = getArguments().getCharSequenceArrayList("autoFilter");
            }
        }

        firestore = FirebaseFirestore.getInstance();
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
        fragmentModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);
        boardModel = new ViewModelProvider(requireActivity()).get(BoardViewModel.class);

        pagerAdapter = ((BoardActivity)getActivity()).getPagerAdapter();
        pbLoading = ((BoardActivity)getActivity()).getLoadingProgressBar();
        snapshotList = new ArrayList<>();
        postingAdapter = new BoardPostingAdapter(snapshotList, this);


        pageHelper = new PaginationHelper();
        pageHelper.setOnPaginationListener(this);

        // Implement OnFilterCheckBoxListener to receive values of the chkbox each time any chekcbox
        // values changes.
        if(currentPage == Constants.BOARD_AUTOCLUB)
            ((BoardActivity)getActivity()).setAutoFilterListener(this);

        /*
        ((BoardActivity)getActivity()).setAutoFilterListener(values -> {
            for(CharSequence filter : values) log.i("chkbox values changed: %s", filter);
            pageHelper.setPostingQuery(source, Constants.BOARD_AUTOCLUB, values);
            // BoardPostingAdapter mab be updated by postingAdapter.notifyDataSetChanged() in
            // setFirstQuery() but it is requried to make BoardPagerAdapter updated in order to
            // invalidate PostingRecyclerView, a custom recyclerview that contains the empty view
            // when no dataset exists.
            pagerAdapter.notifyDataSetChanged();
        });

         */

        /*
         * Realtime update SnapshotListener: server vs cache policy.
         * When initially connecting to Firestore, the snapshot listener checks if there is any
         * changes in the borad and upadte the posting board. On completing the inital update,
         * the lisitener should be detached for purpose of preventing excessive connection to the
         * server.
         */
        CollectionReference postRef = firestore.collection("board_general");
        postListener = postRef.addSnapshotListener((querySnapshot, e) -> {
            if(e != null) return;
            //source = querySnapshot != null && querySnapshot.getMetadata().hasPendingWrites()?
            source = querySnapshot != null ?
                   Source.CACHE  : Source.SERVER ;
            log.i("Source: %s", source);
        });

        CollectionReference userRef = firestore.collection("users");
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
        pbPaging = localView.findViewById(R.id.progbar_board_paging);
        tvEmptyView = localView.findViewById(R.id.tv_empty_view);
        recyclerPostView = localView.findViewById(R.id.recycler_board_postings);

        recyclerPostView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(
                getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerPostView.setLayoutManager(layoutManager);
        recyclerPostView.setItemAnimator(new DefaultItemAnimator());
        recyclerPostView.setAdapter(postingAdapter);

        // Show/hide Floating Action Button as the recyclerview scrolls.
        FloatingActionButton fabWrite = ((BoardActivity)getActivity()).getFAB();
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
        // Paginate the recyclerview with the preset limit attaching OnScrollListener because
        // PaginationHelper subclasses RecyclerView.OnScrollListner.
        recyclerPostView.addOnScrollListener(pageHelper);

        pageHelper.setPostingQuery(source, currentPage, autoFilter);
        return localView;
    }

    public void onPause() {
        super.onPause();
        postListener.remove();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        log.i("onActivityCreated");
        // On completing UploadPostTask, update BoardPostingAdapter to show a new post, which depends
        // upon which currentPage the viewpager contains.
        fragmentModel.getNewPosting().observe(getActivity(), docId -> {
            if(!TextUtils.isEmpty(docId)) {
                // Instead of using notifyItemInserted(), query should be done due to the post
                // sequential number to be updated..
                pageHelper.setPostingQuery(source, currentPage, autoFilter);
            }
        });

        // The post has been deleted in BoardReadDlgFragment which sequentially popped up AlertDialog
        // for confirm and the result is sent back, then deletes the posting item from Firestore.
        // With All done, receive another LiveData containing the postion of the deleted posting item
        // and update the adapter.
        fragmentModel.getRemovedPosting().observe(getActivity(), docId -> {
            log.i("Posting removed: %s", docId);
            if(!TextUtils.isEmpty(docId)) {
                //snapshotList.clear();
                pageHelper.setPostingQuery(source, currentPage, autoFilter);
            }
        });

    }


    // Implement PaginationHelper.OnPaginationListener which notifies the adapter of the first and
    // the next query results.
    @Override
    public void setFirstQuery(QuerySnapshot snapshots) {
        snapshotList.clear();
        for(QueryDocumentSnapshot snapshot : snapshots) snapshotList.add(snapshot);
        postingAdapter.notifyDataSetChanged();

        // If posts exist, dismiss the progressbar. No posts exist, set the textview to the empty
        // view in the custom recyclerview.
        if(snapshotList.size() > 0) pbLoading.setVisibility(View.GONE);
        else recyclerPostView.setEmptyView(tvEmptyView);
    }

    @Override
    public void setNextQueryStart(boolean b) {
        //pagingProgbar.setVisibility(View.VISIBLE);
        //weakProgbar.get().setVisibility(View.VISIBLE);
        pbPaging.setVisibility(View.VISIBLE);
    }

    @Override
    public void setNextQueryComplete(QuerySnapshot querySnapshot) {
        for(DocumentSnapshot document : querySnapshot) snapshotList.add(document);
        //pagingProgbar.setVisibility(View.INVISIBLE);
        postingAdapter = new BoardPostingAdapter(snapshotList, this);
        //weakProgbar.get().setVisibility(View.GONE);
        pbPaging.setVisibility(View.GONE);
        postingAdapter.notifyDataSetChanged();
    }


    // Implement the callback of BoardPostingAdapter.OnRecyclerItemClickListener when an item is clicked.
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public void onPostItemClicked(DocumentSnapshot snapshot, int position) {
        // Initiate the task to query the board collection and the user collection.
        // Show the dialog with the full screen. The container is android.R.id.content.
        BoardReadDlgFragment readPostFragment = new BoardReadDlgFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("tabPage", currentPage);
        bundle.putInt("position", position);
        bundle.putString("documentId", snapshot.getId());
        bundle.putString("postTitle", snapshot.getString("post_title"));
        bundle.putString("userId", snapshot.getString("user_id"));
        bundle.putString("userName", snapshot.getString("user_name"));
        bundle.putString("userPic", snapshot.getString("user_pic"));
        bundle.putInt("cntComment", snapshot.getLong("cnt_comment").intValue());
        bundle.putInt("cntCompathy", snapshot.getLong("cnt_compathy").intValue());
        bundle.putString("postContent", snapshot.getString("post_content"));
        bundle.putStringArrayList("uriImgList", (ArrayList<String>)snapshot.get("post_images"));
        bundle.putString("timestamp", sdf.format(snapshot.getDate("timestamp")));

        // With the user id given as an argument, query the user(posting writer) to fetch the auto data
        // which contains auto_maker, auto_type, auto_model and auto_year in JSON string. On completion,
        // set it to the dialog fragment and pop it up.
        firestore.collection("users").document(snapshot.getString("user_id")).get()
                .addOnSuccessListener(document -> {
                    if(document.exists()) {
                        String auto = document.getString("auto_data");
                        if(!TextUtils.isEmpty(auto)) bundle.putString("autoData", auto);

                        readPostFragment.setArguments(bundle);
                        // What if Fragment calls another fragment? What is getChildFragmentManager() for?
                        // android.R.id.content makes DialogFragment fit to the full screen.
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .add(android.R.id.content, readPostFragment)
                                .addToBackStack(null)
                                .commit();
                    }
                });

        /*
        postDialogFragment.setArguments(bundle);
        // What if Fragment calls another fragment? What is getChildFragmentManager() for?
        // android.R.id.content makes DialogFragment fit to the full screen.
        getActivity().getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, postDialogFragment)
                .addToBackStack(null)
                .commit();
        */

        // Update the field of "cnt_view" increasing the number.
        DocumentReference docref = snapshot.getReference();
        addViewCount(docref, position);
        //docref.update("cnt_view", FieldValue.increment(1));

    }

    // Get the user id and query the "viewers" sub-collection to check if the user id exists in the
    // documents, which means whether the user has read the post before. If so, do not increase
    // the view count. Otherwise, add the user id to the "viewers" collection and increase the
    // view count;
    @SuppressWarnings("ConstantConditions")
    private void addViewCount(DocumentReference docref, int position) {
        try(FileInputStream fis = getActivity().openFileInput("userId");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            final String viewerId = br.readLine();

            CollectionReference viewerCollection = docref.collection("viewers");
            viewerCollection.document(viewerId).get().addOnSuccessListener(snapshot -> {
                // In case the user does not exists in the "viewers" collection
                if(snapshot == null || !snapshot.exists()) {
                  log.i("vierer not exists");
                  // Increase the view count
                  docref.update("cnt_view", FieldValue.increment(1));

                  // Set timestamp and the user ip with the user id used as the document id.
                  Map<String, Object> viewerData = new HashMap<>();
                  Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
                  Date date = calendar.getTime();

                  viewerData.put("timestamp", new Timestamp(date));
                  viewerData.put("viewer_ip", "");

                  viewerCollection.document(viewerId).set(viewerData).addOnSuccessListener(aVoid -> {
                      log.i("Successfully set the data");

                      // Listener to events for local changes, which is notified with the new data
                      // before the data is sent to the backend.
                      docref.get(Source.CACHE).addOnSuccessListener(data -> {
                          if(data != null && data.exists()) {
                              //log.i("source: %s", source + "data: %s" + data.getData());
                              postingAdapter.notifyItemChanged(position, data.getLong("cnt_view"));
                              postingAdapter.notifyItemChanged(position, data.getLong("cnt_comment"));
                          }
                      });

                      /*
                      docref.addSnapshotListener(MetadataChanges.INCLUDE, (data, e) ->{
                          if(e != null) return;
                          //String source = data != null && data.getMetadata().hasPendingWrites()?"Local":"Servier";
                          if(data != null && data.exists()) {
                              //log.i("source: %s", source + "data: %s" + data.getData());
                              postingAdapter.notifyItemChanged(position, data.getLong("cnt_view"));
                              postingAdapter.notifyItemChanged(position, data.getLong("cnt_comment"));
                          }
                      });
                       */
                  });
                }
            });

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCheckBoxValueChange(ArrayList<CharSequence> autofilter) {
        for(CharSequence filter : autofilter) log.i("chkbox values changed: %s", filter);
        pageHelper.setPostingQuery(source, Constants.BOARD_AUTOCLUB, autofilter);
        // BoardPostingAdapter mab be updated by postingAdapter.notifyDataSetChanged() in
        // setFirstQuery() but it is requried to make BoardPagerAdapter updated in order to
        // invalidate PostingRecyclerView, a custom recyclerview that contains the empty view
        // when no dataset exists.
        pagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onGeneralPost(boolean b) {
        log.i("isGenera;Post: %s", b);
        isGeneralPost = b;
    }
}


