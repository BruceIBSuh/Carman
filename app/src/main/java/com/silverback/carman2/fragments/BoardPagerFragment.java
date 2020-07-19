package com.silverback.carman2.fragments;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.BoardActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardPagerAdapter;
import com.silverback.carman2.adapters.BoardPostingAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.ApplyImageResourceUtil;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.QueryPostPaginationUtil;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
import com.silverback.carman2.views.PostingRecyclerView;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class BoardPagerFragment extends Fragment implements
        BoardActivity.OnAutoFilterCheckBoxListener,
        QueryPostPaginationUtil.OnQueryPaginationCallback,
        //QueryClubPostingUtil.OnPaginationListener,
        BoardPostingAdapter.OnRecyclerItemClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPagerFragment.class);

    // Objects
    private FirebaseFirestore firestore;
    //private PostingBoardViewModel postingModel;
    //private PostingBoardRepository postRepo;
    //private QueryClubPostingUtil clubRepo;
    //private ListenerRegistration listenerRegistration;
    private QueryPostPaginationUtil queryPagingUtil;


    private BoardPagerAdapter pagerAdapter;
    private FragmentSharedModel fragmentModel;
    private BoardPostingAdapter postingAdapter;
    private List<DocumentSnapshot> postshotList;
    private ArrayList<String> autoFilter;
    private SimpleDateFormat sdf;
    private ApplyImageResourceUtil imgutil;

    // UIs
    private ProgressBar pbLoading, pbPaging;
    private PostingRecyclerView recyclerPostView;
    private TextView tvEmptyView;
    private TextView tvSorting;
    private FloatingActionButton fabWrite;

    // Fields
    private String automaker;
    private int currentPage;
    private boolean isViewOrder;
    private boolean isLoading;
    //private boolean isLastPage;
    //private boolean isViewUpdated;
    //private boolean isScrolling;

    // Constructor
    public BoardPagerFragment() {
        // Required empty public constructor
    }

    // Singleton for AutoClub currentPage which has the checkbox values and title names.
    public static BoardPagerFragment newInstance(int page, ArrayList<String> values) {
        BoardPagerFragment fragment = new BoardPagerFragment();
        Bundle args = new Bundle();
        args.putInt("currentPage", page);
        args.putStringArrayList("autoFilter", values);
        fragment.setArguments(args);

        return fragment;
    }


    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null) {
            currentPage = getArguments().getInt("currentPage");
            autoFilter = getArguments().getStringArrayList("autoFilter");
            if(autoFilter.size() > 0) automaker = autoFilter.get(0);
        }

        // Make the toolbar menu available in the Fragment.
        setHasOptionsMenu(true);

        firestore = FirebaseFirestore.getInstance();
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
        imgutil = new ApplyImageResourceUtil(getContext());
        fragmentModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);

        pbLoading = ((BoardActivity)getActivity()).getLoadingProgressBar();
        postshotList = new ArrayList<>();
        postingAdapter = new BoardPostingAdapter(postshotList, this);

        /*
        if(currentPage == Constants.BOARD_AUTOCLUB) {
            //clubRepo = new QueryClubPostingUtil(firestore);
            //clubRepo.setOnPaginationListener(this);
            postingAdapter = new BoardPostingAdapter(clubshotList, this);
        } else {
            //postRepo = new PostingBoardRepository();
            //postingModel = new ViewModelProvider(this, new PostingBoardModelFactory(postRepo)).get(PostingBoardViewModel.class);
            postingAdapter = new BoardPostingAdapter(postshotList, this);
        }
         */
        queryPagingUtil = new QueryPostPaginationUtil(firestore, this);
        postingAdapter = new BoardPostingAdapter(postshotList, this);

        // Implement OnFilterCheckBoxListener to receive values of the chkbox each time any chekcbox
        // values changes.
        ((BoardActivity)getActivity()).setAutoFilterListener(this);
        pagerAdapter = ((BoardActivity)getActivity()).getPagerAdapter();

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View localView = inflater.inflate(R.layout.fragment_board_pager, container, false);
        pbPaging = localView.findViewById(R.id.progbar_board_paging);
        tvEmptyView = localView.findViewById(R.id.tv_empty_view);
        recyclerPostView = localView.findViewById(R.id.recycler_board_postings);

        // In case of inserting the banner, the item size will change.
        recyclerPostView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);

        recyclerPostView.setLayoutManager(layoutManager);
        //recyclerPostView.setItemAnimator(new DefaultItemAnimator());
        //SimpleItemAnimator itemAnimator = (SimpleItemAnimator)recyclerPostView.getItemAnimator();
        //itemAnimator.setSupportsChangeAnimations(false);
        recyclerPostView.setAdapter(postingAdapter);


        // Show/hide Floating Action Button as the recyclerview scrolls.
        fabWrite = ((BoardActivity)getActivity()).getFAB();
        setRecyclerViewScrollListener();

        // Based on MVVM
        /*
        if(currentPage == Constants.BOARD_AUTOCLUB) {
            // Initialize the club board if any filter is set.
            if(!TextUtils.isEmpty(automaker)) {
                isLastPage = false;
                //postshotList.clear();
                clubshotList.clear();
                clubRepo.setPostingQuery(isViewOrder);
            }

        } else queryPostSnapshot(currentPage);
        */

        queryPagingUtil.setPostQuery(currentPage, isViewOrder);
        return localView;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        // On completing UploadPostTask, update BoardPostingAdapter to show a new post, which depends
        // upon which currentPage the viewpager contains.
        fragmentModel.getNewPosting().observe(requireActivity(), docId -> {
            if(!TextUtils.isEmpty(docId)) {
                log.i("Upload Post: %s", docId);
                queryPagingUtil.setPostQuery(currentPage, isViewOrder);
            }
        });

        // The post has been deleted in BoardReadDlgFragment which sequentially popped up AlertDialog
        // for confirm and the result is sent back, then deletes the posting item from Firestore.
        // With All done, receive another LiveData containing the postion of the deleted posting item
        // and update the adapter.
        fragmentModel.getRemovedPosting().observe(requireActivity(), docId -> {
            //log.i("Posting removed: %s", docId);
            if(!TextUtils.isEmpty(docId)) {
                queryPagingUtil.setPostQuery(currentPage, isViewOrder);
            }
        });

        fragmentModel.getEditPosting().observe(requireActivity(), docId -> {
            if(!TextUtils.isEmpty(docId)) {
                queryPagingUtil.setPostQuery(currentPage, isViewOrder);
            }
        });

    }

    // Create the toolbar menu of the auto club page in the fragment, not in the actity,  which
    // should be customized to have an imageview and textview underneath instead of setting icon
    // by setting actionLayout(app:actionLayout in xml).
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Do something different from in the parent activity
        //this.menu = menu;
        menu.getItem(0).setVisible(false);
        menu.getItem(1).setVisible(false);

        if(currentPage == Constants.BOARD_AUTOCLUB) {
            View rootView = menu.getItem(0).getActionView();
            ImageView imgEmblem = rootView.findViewById(R.id.img_action_emblem);
            ProgressBar pbEmblem = rootView.findViewById(R.id.pb_emblem);
            tvSorting = rootView.findViewById(R.id.tv_sorting_order);

            if(TextUtils.isEmpty(automaker)) {
                menu.getItem(0).setVisible(false);
                rootView.setVisibility(View.INVISIBLE);
            } else {
                menu.getItem(0).setVisible(true);
                rootView.setVisibility(View.VISIBLE);
                setAutoMakerEmblem(pbEmblem, imgEmblem);
                rootView.setOnClickListener(view -> onOptionsItemSelected(menu.getItem(0)));
            }
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_automaker_emblem) {
            isViewOrder = !isViewOrder;
            // Set the spannable string indicating what's the basis of sorting. The reason why the
            // span is set.
            String sortLabel = (isViewOrder)? getString(R.string.board_autoclub_sort_view) : getString(R.string.board_autoclub_sort_time);
            tvSorting.setText(sortLabel);

            // Initialize fields when clicking the menu for switching timestamp and cnt_view
            //isLastPage = false;
            //clubshotList.clear();
            //clubRepo.setPostingQuery(isViewOrder);

            // Requery the autoclub post with the field switched.
            isLoading = true;
            pbPaging.setVisibility(View.GONE);
            queryPagingUtil.setPostQuery(currentPage, isViewOrder);

            // Rotate the imageview holding emblem
            ObjectAnimator rotation = ObjectAnimator.ofFloat(item.getActionView(), "rotationY", 0.0f, 360f);
            rotation.setDuration(500);
            rotation.setInterpolator(new AccelerateDecelerateInterpolator());
            // Use AnimatorListenerAdapter to take callback methods seletively.
            rotation.addListener(new AnimatorListenerAdapter(){
                @Override
                public void onAnimationEnd(Animator animation, boolean isReverse) {
                    rotation.cancel();
                }
            });

            rotation.start();
            return true;
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        //postshotList.clear();
    }

    // Implement OnFilterCheckBoxListener which notifies any change of checkbox values, which
    // performa a new query with new autofilter values
    @Override
    public void onCheckBoxValueChange(ArrayList<String> autofilter) {
        this.autoFilter = autofilter;
        pagerAdapter.notifyDataSetChanged();
        //pageHelper.setPostingQuery(Constants.BOARD_AUTOCLUB, isViewOrder);
        // BoardPostingAdapter may be updated by postingAdapter.notifyDataSetChanged() in
        // setFirstQuery() but it is requried to make BoardPagerAdapter updated in order to
        // invalidate PostingRecyclerView, a custom recyclerview that contains the empty view
        // when no dataset exists.
    }

    // Implement BoardPostingAdapter.OnRecyclerItemClickListener when an item is clicked.
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public void onPostItemClicked(DocumentSnapshot snapshot, int position) {
        // Initiate the task to query the board collection and the user collection.
        // Show the dialog with the full screen. The container is android.R.id.content.
        BoardReadDlgFragment readPostFragment = new BoardReadDlgFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("tabPage", currentPage);
        bundle.putString("documentId", snapshot.getId());
        bundle.putString("postTitle", snapshot.getString("post_title"));

        if(currentPage == Constants.BOARD_NOTIFICATION) {
            bundle.putString("userId", "0000");
            bundle.putString("userName", "Admin");
            bundle.putString("userPic", null);
        } else {
            bundle.putString("userId", snapshot.getString("user_id"));
            bundle.putString("userName", snapshot.getString("user_name"));
            bundle.putString("userPic", snapshot.getString("user_pic"));
        }

        bundle.putInt("cntComment", snapshot.getLong("cnt_comment").intValue());
        bundle.putInt("cntCompathy", snapshot.getLong("cnt_compathy").intValue());
        bundle.putString("postContent", snapshot.getString("post_content"));
        bundle.putStringArrayList("uriImgList", (ArrayList<String>)snapshot.get("post_images"));
        bundle.putString("timestamp", sdf.format(snapshot.getDate("timestamp")));

        readPostFragment.setArguments(bundle);
        getActivity().getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, readPostFragment)
                .addToBackStack(null)
                .commit();

        // Update the field of "cnt_view" increasing the number.
        DocumentReference docref = snapshot.getReference();
        addViewCount(docref, position);


    }

    // Implement QueryPaginationUtil.OnQueryPaginationCallback called by QueryPaginationUtil.
    // setPostQuery()
    @Override
    public void getFirstQueryResult(QuerySnapshot querySnapshot) {
        postshotList.clear();

        // In case that no post exists or the automaker filter is emepty in the autoclub page,
        // display the empty view.
        if(querySnapshot == null || querySnapshot.size() == 0) {
            recyclerPostView.setEmptyView(tvEmptyView);
            return;
        } else if(currentPage == Constants.BOARD_AUTOCLUB && TextUtils.isEmpty(automaker)) {
            recyclerPostView.setEmptyView(tvEmptyView);
            return;
        }
        // Add DocumentSnapshot to List<DocumentSnapshot> which is paassed to RecyclerView.Adapter.
        // Autoclub sorts out the document snapshot with given filters.
        for(DocumentSnapshot document : querySnapshot) {
            if (currentPage == Constants.BOARD_AUTOCLUB) sortClubPost(document);
            else {
                postshotList.add(document);
                // Consider if it is appropraite to call nofityDataSetChanged every time.
                postingAdapter.notifyDataSetChanged();
            }

        }

        // If the sorted posts are less than the pagination number, keep querying until it's up to
        // the number. Manually update the adapter each time posts amounts to the pagination number.
        if(currentPage == Constants.BOARD_AUTOCLUB) {
            if(postshotList.size() < Constants.PAGINATION) {
                isLoading = true;
                queryPagingUtil.setNextQuery();
                return;
            } else postingAdapter.notifyDataSetChanged();
        }

        pbLoading.setVisibility(View.GONE);
        isLoading = false;
    }

    // Called by QueryPaginationUtil.setNextQuery()
    @Override
    public void getNextQueryResult(QuerySnapshot nextShots) {
        log.i("next query: %s", nextShots.size());
        for(DocumentSnapshot document : nextShots) {
            if (currentPage == Constants.BOARD_AUTOCLUB) sortClubPost(document);
            else {
                postshotList.add(document);
                postingAdapter.notifyDataSetChanged();
            }
        }

        // Keep querying if sorted posts are less than the pagination number. When it reaches the
        // number, update the apdater.
        if(currentPage == Constants.BOARD_AUTOCLUB) {
            if(postshotList.size() < Constants.PAGINATION) {
                isLoading = true;
                pbPaging.setVisibility(View.VISIBLE);
                queryPagingUtil.setNextQuery();
                return;
            } else postingAdapter.notifyDataSetChanged();
        }

        //postingAdapter.notifyDataSetChanged();
        pbPaging.setVisibility(View.INVISIBLE);
        isLoading = false;
    }

    @Override
    public void getLastQueryResult(QuerySnapshot lastShots) {
        for(DocumentSnapshot document : lastShots) {
            if(currentPage == Constants.BOARD_AUTOCLUB) sortClubPost(document);
            else postshotList.add(document);
            postingAdapter.notifyDataSetChanged();
        }

        // On clicking the filter item on the AutoClub page, if noting has been posted, display
        // the empty view.
        if(currentPage == Constants.BOARD_AUTOCLUB) {
            if(postshotList.size() == 0) {
                recyclerPostView.setEmptyView(tvEmptyView);
            }
        }

        pbPaging.setVisibility(View.GONE);
        isLoading = true; // Block the scroll listener from keeping querying.
    }


    // This method sorts out the autoclub posts based on the autofilter by removing a document out of
    // the list if it has no autofilter field or its nested filter which can be accessed w/ the dot
    // notation
    private void sortClubPost(DocumentSnapshot snapshot) {
        postshotList.add(snapshot);
        if(snapshot.get("auto_filter") == null) postshotList.remove(snapshot);
        else {
            for(String filter : autoFilter) {
                if ((snapshot.get("auto_filter." + filter) == null)) {
                    postshotList.remove(snapshot);
                    break;
                }
            }
        }
    }

    /*
    // Callback implemented by QueryClubPostingUtil.setPostingQuery() when initiating query for
    // the autoclub post. Receiving a result querysnapshot, categorize each snapshot by type, then
    // update the postshotList. To get the club list, filter the postshot list with the autofilter
    // using sortAutoClubPost(), adding a filtered snapshot to the club list. If the club list is
    // less than the pagination number, keep querying posts to add more club posts, calling
    // QueryClubPostingUtil.setNextQuery().
    @Override
    public void setClubQuerySnapshot(QuerySnapshot snapshots) {
        clubshotList.clear();
        if(snapshots.size() == 0) {
            recyclerPostView.setEmptyView(tvEmptyView);
            return;
        }

        for(DocumentChange documentChange : snapshots.getDocumentChanges()) {
            switch(documentChange.getType()) {
                case ADDED:
                    DocumentSnapshot addSnapshot = documentChange.getDocument();
                    postshotList.add(addSnapshot);
                    sortAutoClubPost(addSnapshot);
                    break;

                case MODIFIED:
                    DocumentSnapshot modifySnapshot = documentChange.getDocument();
                    for(int i = 0; i < postshotList.size(); i++) {
                        DocumentSnapshot snapshot = postshotList.get(i);
                        if(snapshot.getId().equals(modifySnapshot.getId())) {
                            postshotList.remove(snapshot);
                            postshotList.add(i, modifySnapshot);
                            break;
                        }
                    }

                    sortAutoClubPost(modifySnapshot);
                    break;

                case REMOVED:
                    DocumentSnapshot removeSnapshot = documentChange.getDocument();
                    for(int i = 0; i < postshotList.size(); i++) {
                        DocumentSnapshot snapshot = postshotList.get(i);
                        if(snapshot.getId().equals(removeSnapshot.getId())) {
                            postshotList.remove(snapshot);
                            break;
                        }
                    }

                    sortAutoClubPost(removeSnapshot);
                    break;
            }
        }

        if(snapshots.size() < Constants.PAGINATION) {
            isLastPage = true;
            postingAdapter.notifyDataSetChanged();
        } else {
            isLastPage = false;
            if(clubshotList.size() < Constants.PAGINATION) clubRepo.setNextQuery();
            else postingAdapter.notifyDataSetChanged();
        }

        pbLoading.setVisibility(View.GONE);
    }

    // This method sorts out the autoclub posts based on the autofilter by removing a document out of
    // the list if it has no autofilter field or its nested filter which can be accessed w/ the dot
    // notation
    private void sortAutoClubPost(DocumentSnapshot snapshot) {
        clubshotList.add(snapshot);
        if(snapshot.get("auto_filter") == null) clubshotList.remove(snapshot);
        else {
            for(String filter : autoFilter) {
                if ((snapshot.get("auto_filter." + filter) == null)) {
                    clubshotList.remove(snapshot);
                    break;
                }
            }
        }
    }

    // Query the general board except for the club board.
    private void queryPostSnapshot(int page) {
        postRepo.setPostingQuery(page);

        // Notified of documentsnapshot from the livedata
        PostingBoardLiveData postLiveData = postingModel.getPostingBoardLiveData();
        if(postLiveData != null) {
            postLiveData.observe(getViewLifecycleOwner(), operation -> {
                log.i("operation thread: %s", Thread.currentThread());

                int type = operation.getType();
                DocumentSnapshot postshot = operation.getDocumentSnapshot();
                // Add a post only if the post_general field is set to true at the general board.
                switch(type) {
                    case 0: // ADDED
                        log.i("Added: %s, %s", currentPage, postshot.getString("post_title"));
                        postshotList.add(postshot);
                        break;

                    case 1: // MODIFIED
                        log.i("Modified: %s, %s", currentPage, postshot.getString("post_title"));
                        for(int i = 0; i < postshotList.size(); i++) {
                            DocumentSnapshot snapshot = postshotList.get(i);
                            if(snapshot.getId().equals(postshot.getId())) {
                                postshotList.remove(snapshot);
                                postshotList.add(i, postshot);
                                break;
                            }
                        }

                        break;

                    case 2: // REMOVED
                        log.i("Removed: %s, %s", currentPage, postshot.getString("post_title"));
                        for(int i = 0; i < postshotList.size(); i++) {
                            DocumentSnapshot snapshot = postshotList.get(i);
                            if(snapshot.getId().equals(postshot.getId())) {
                                log.i("snapshot removed: %s", snapshot.getString("post_title"));
                                postshotList.remove(snapshot);
                                break;
                            }

                        }

                        break;
                }

                postingAdapter.notifyDataSetChanged();
            });

            pbLoading.setVisibility(View.GONE);
        }
    }
    */
    // Subclass of RecyclerView.ScrollViewListner
    private void setRecyclerViewScrollListener() {
        RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener(){
            boolean isScrolling;
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                //if (newState == RecyclerView.SCROLL_STATE_IDLE) fabWrite.show();
                if(newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL)
                    isScrolling = true;
                else fabWrite.show();
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (dy > 0 || dy < 0 && fabWrite.isShown()) fabWrite.hide();

                LinearLayoutManager layoutManager = ((LinearLayoutManager) recyclerView.getLayoutManager());
                if (layoutManager != null) {
                    int firstVisibleProductPosition = layoutManager.findFirstVisibleItemPosition();
                    int visiblePostCount = layoutManager.getChildCount();
                    int totalPostCount = layoutManager.getItemCount();

                    if (!isLoading && isScrolling && (firstVisibleProductPosition + visiblePostCount == totalPostCount)) {
                        log.i("scroll with next query");
                        isScrolling = false;
                        isLoading = true;

                        pbPaging.setVisibility(View.VISIBLE);
                        queryPagingUtil.setNextQuery();
                        //if(currentPage != Constants.BOARD_AUTOCLUB) queryPostSnapshot(currentPage);
                        //else if(!isLastPage) clubRepo.setNextQuery();
                    }
                }
            }

        };

        recyclerPostView.addOnScrollListener(scrollListener);
    }

    // Check if a user is the post's owner or has read the post before in order to increate the view
    // count. In order to do so, get the user id from the internal storage and from the post as well.
    // Get the user id and query the "viewers" sub-collection to check if the user id exists in the
    // documents, which means whether the user has read the post before. If so, do not increase
    // the view count. Otherwise, add the user id to the "viewers" collection and increase the
    // view count;
    @SuppressWarnings("ConstantConditions")
    private void addViewCount(DocumentReference docref, int position) {
        try(FileInputStream fis = getActivity().openFileInput("userId");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            final String viewerId = br.readLine();

            CollectionReference subCollection = docref.collection("viewers");
            subCollection.document(viewerId).get().addOnSuccessListener(snapshot -> {
                // In case the user has not read the post before and adoes not exists in the "viewers"
                // collection
                if(snapshot == null || !snapshot.exists()) {
                  docref.update("cnt_view", FieldValue.increment(1));
                  // Set timestamp and the user ip with the user id used as the document id.
                  Map<String, Object> viewerData = new HashMap<>();
                  /*
                  Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
                  Date date = calendar.getTime();
                   */
                  viewerData.put("timestamp", FieldValue.serverTimestamp());
                  viewerData.put("viewer_ip", "");

                  subCollection.document(viewerId).set(viewerData).addOnSuccessListener(aVoid -> {
                      // Listener to events for local changes, which is notified with the new data
                      // before the data is sent to the backend.
                      docref.get().addOnSuccessListener(data -> {
                          if(data != null && data.exists()) {
                              postingAdapter.notifyItemChanged(position, data.getLong("cnt_view"));
                              postingAdapter.notifyItemChanged(position, data.getLong("cnt_comment"));
                          }
                      }).addOnFailureListener(Exception::printStackTrace);
                  });
                }
            });

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    // Attemp to retrieve the emblem uri from Firestore only when an auto maker is provided. For this
    // reason, the method should be placed at the end of createAutoFilterCheckBox() which receives
    // auto data as json type.
    private void setAutoMakerEmblem(ProgressBar pb, ImageView imgview) {
        // Make the progressbar visible until getting the emblem from Firetore
        pb.setVisibility(View.VISIBLE);
        firestore.collection("autodata").whereEqualTo("auto_maker", automaker).get()
                .addOnSuccessListener(queires -> {
                    for(QueryDocumentSnapshot autoshot : queires) {
                        if(autoshot.exists()) {
                            String emblem = autoshot.getString("auto_emblem");
                            // Empty Check. Refactor should be taken to show an empty icon, instead.
                            if(TextUtils.isEmpty(emblem)) return;
                            else {
                                Uri uri = Uri.parse(emblem);
                                final int x = imgview.getMeasuredWidth();
                                final int y = imgview.getMeasuredHeight();
                                imgutil.applyGlideToEmblem(uri, x, y, imgview);
                            }

                            pb.setVisibility(View.GONE);
                            break;
                        }
                    }
                }).addOnFailureListener(e -> {
                    pb.setVisibility(View.GONE);
                    e.printStackTrace();
                });
    }


}


