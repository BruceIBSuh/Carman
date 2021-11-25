package com.silverback.carman.fragments;


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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import com.silverback.carman.BoardActivity;
import com.silverback.carman.R;
import com.silverback.carman.adapters.BoardPagerAdapter;
import com.silverback.carman.adapters.BoardPostingAdapter;
import com.silverback.carman.databinding.FragmentBoardPagerBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.QueryPostPaginationUtil;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.views.PostingRecyclerView;

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
import java.util.Objects;

/**
 * The viewpager statically creates this fragment using BoardPagerAdapter, which has the custom
 * recyclerview to show the posting board by category.
 *
 * QueryPostPaginationUtil is a util class that performs query and pagination based on orderby() and
 * limit(). This class is made of the initial, next, and last query for pagination, listening to
 * scrolling of the recyclerview. MVVM-based architecture is provided in the board package just for
 * referernce.
 *
 * Instead of using SnapshotListener for realtime update, which seems difficult to handle cache data
 * in the viewpager fragments, the util simply reads posts using get() and updates are made by requerying
 * posts notified by the viewmodel(FragmentSharedModel). Comments in BoardReadDlgFragment, however,
 * applies SnapshotListener.
 *
 */
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

    private FragmentSharedModel fragmentModel;
    private BoardPagerAdapter pagerAdapter;
    private BoardPostingAdapter postingAdapter;
    private List<DocumentSnapshot> postList;
    private ArrayList<String> autoFilter;
    private SimpleDateFormat sdf;
    private ApplyImageResourceUtil imgutil;

    // UIs
    private FragmentBoardPagerBinding binding;
    private ProgressBar pbLoading;
    private PostingRecyclerView recyclerPostView;
    private FloatingActionButton fabWrite;
    //private TextView tvEmptyView;
    //private TextView tvSorting;


    // Fields
    private String automaker;
    private int currentPage;
    private boolean isViewOrder;
    private boolean isLoading; // to block the RecyclerView from scrolling while loading posts.
    //private boolean isLastPage;
    //private boolean isViewUpdated;
    //private boolean isScrolling;

    // Constructor
    public BoardPagerFragment() {
        // Required empty public constructor
    }

    // Singleton for the autoclub page which has the checkbox values and title names to display
    // overlaying the tab menu.
    public static BoardPagerFragment newInstance(int page, ArrayList<String> values){
        BoardPagerFragment fragment = new BoardPagerFragment();
        Bundle args = new Bundle();
        args.putInt("currentPage", page);
        args.putStringArrayList("autoFilter", values);
        fragment.setArguments(args);
        return fragment;
    }


    //@SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // When it comes to the autoclub page, it will pass the arguments of page and filter values.
        // BUG!!: currentPage works only when it comes in first. Require to get an indicator to
        // tell the current page.
        if(getArguments() != null) {
            currentPage = getArguments().getInt("currentPage");
            autoFilter = getArguments().getStringArrayList("autoFilter");
            if(autoFilter != null && autoFilter.size() > 0) automaker = autoFilter.get(0);
        }

        // Make the toolbar menu available in the Fragment.
        setHasOptionsMenu(true);

        // Instantiate objects.
        firestore = FirebaseFirestore.getInstance();
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        imgutil = new ApplyImageResourceUtil(getContext());

        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());
        pbLoading = ((BoardActivity)requireActivity()).getLoadingProgressBar();


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
        // Instantiate the query and pagination util class and create the RecyclerView adapter to
        // show the posting list.
        queryPagingUtil = new QueryPostPaginationUtil(firestore, this);

        // Implement OnFilterCheckBoxListener to receive values of the chkbox each time any chekcbox
        // values changes.
        ((BoardActivity)requireActivity()).setAutoFilterListener(this);
        pagerAdapter = ((BoardActivity)requireActivity()).getPagerAdapter();

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentBoardPagerBinding.inflate(inflater);

        //View localView = inflater.inflate(R.layout.fragment_board_pager, container, false);
        //pbPaging = localView.findViewById(R.id.progbar_board_paging);
        //tvEmptyView = localView.findViewById(R.id.tv_empty_view);
        //recyclerPostView = localView.findViewById(R.id.recycler_board_postings);

        // In case of inserting the banner, the item size will change.
        binding.recyclerBoardPostings.setHasFixedSize(false);
        LinearLayoutManager layout = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);

        binding.recyclerBoardPostings.setLayoutManager(layout);
        //binding.recyclerBoardPostings.setItemAnimator(new DefaultItemAnimator());
        //SimpleItemAnimator itemAnimator = (SimpleItemAnimator)binding.recyclerBoardPostings.getItemAnimator();
        //itemAnimator.setSupportsChangeAnimations(false);

        postList = new ArrayList<>();
        postingAdapter = new BoardPostingAdapter(postList, this);
        binding.recyclerBoardPostings.setAdapter(postingAdapter);
        binding.recyclerBoardPostings.addOnScrollListener(setRecyclerViewScrollListener());

        // Show/hide Floating Action Button as the recyclerview scrolls.
        fabWrite = ((BoardActivity)getActivity()).getFAB();
        //setRecyclerViewScrollListener();

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
        log.i("current page: %s", currentPage);
        queryPagingUtil.setPostQuery(currentPage, isViewOrder);
        pbLoading.setVisibility(View.VISIBLE);

        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // On completing UploadPostTask, update BoardPostingAdapter to show a new post, which depends
        // upon which currentPage the viewpager contains.
        /*
        fragmentModel.getNewPosting().observe(getViewLifecycleOwner(), docId -> {
            if(!TextUtils.isEmpty(docId)) {
                //log.i("Upload Post: %s", docId);
                queryPagingUtil.setPostQuery(currentPage, isViewOrder);
            }
        });
        */
        // The post has been deleted in BoardReadDlgFragment which sequentially popped up AlertDialog
        // for confirm and the result is sent back, then deletes the posting item from Firestore.
        // With All done, receive another LiveData containing the position of the deleted posting item
        // and update the adapter.
        fragmentModel.getRemovedPosting().observe(getViewLifecycleOwner(), docId -> {
            //log.i("Posting removed: %s", docId);
            if(!TextUtils.isEmpty(docId)) {
                queryPagingUtil.setPostQuery(currentPage, isViewOrder);
            }
        });

        fragmentModel.getEditedPosting().observe(requireActivity(), docId -> {
            if(!TextUtils.isEmpty(docId)) {
                queryPagingUtil.setPostQuery(currentPage, isViewOrder);
            }
        });

        // Observe the viewmodel for partial binding to BoardPostingAdapter to update the comment count,
        // the livedata of which is created when a comment has finished uploadingb in BoardReadDlgFragment.
        fragmentModel.getNewComment().observe(requireActivity(), sparseArray ->
                postingAdapter.notifyItemChanged(sparseArray.keyAt(0), sparseArray)
        );
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
            //tvSorting = rootView.findViewById(R.id.tv_sorting_order);

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
            //binding..setText(sortLabel);

            // Initialize fields when clicking the menu for switching timestamp and cnt_view
            //isLastPage = false;
            //clubshotList.clear();
            //clubRepo.setPostingQuery(isViewOrder);

            // Requery the autoclub post with the field switched.
            isLoading = true;
            //pbPaging.setVisibility(View.GONE);
            binding.progbarBoardPaging.setVisibility(View.GONE);
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

    // Implement OnFilterCheckBoxListener which notifies any change of checkbox values, which
    // perform a new query with new autofilter values set.
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
    //@SuppressWarnings({"unchecked", "ConstantConditions"})
    @Override
    public void onPostItemClicked(DocumentSnapshot snapshot, int position) {
        // Initiate the task to query the board collection and the user collection.
        // Show the dialog with the full screen. The container is android.R.id.content.
        BoardReadDlgFragment readPostFragment = new BoardReadDlgFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("tabPage", currentPage);
        log.i("tabPage: %s", currentPage);
        // TEST CODING FOR UPDATING THE COMMENT NUMBER
        bundle.putInt("position", position);

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


        bundle.putInt("cntComment", Objects.requireNonNull(snapshot.getLong("cnt_comment")).intValue());
        bundle.putInt("cntCompathy", Objects.requireNonNull(snapshot.getLong("cnt_compathy")).intValue());
        bundle.putString("postContent", snapshot.getString("post_content"));
        bundle.putString("timestamp", sdf.format(Objects.requireNonNull(snapshot.getDate("timestamp"))));
        bundle.putStringArrayList("uriImgList", (ArrayList<String>)snapshot.get("post_images"));

        readPostFragment.setArguments(bundle);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, readPostFragment)
                .addToBackStack(null)
                .commit();

        // Update the field of "cnt_view" increasing the number.
        DocumentReference docref = snapshot.getReference();
        addViewCount(docref, position);
    }

    /**
     * Implement QueryPaginationUtil.OnQueryPaginationCallback overriding the following 4 methods that
     * performs to query posts with orderBy() and limit() conditioned up to the pagination number
     * which is defined in Constants.PAGINATION.
     *
     * getFirestQueryResult(): setPostQuery() has completed with queried posts up to the pagination number.
     * getNextQueryResult(): setNextQuery() has completed with queried posts up to the pagination number.
     * getLastQueryResult(); setNextQuery() has completed with queried posts under the pagination number.
     * getQueryErrorResult(); callback invoked if any error has occurred while querying.
     */
    @Override
    public void getFirstQueryResult(QuerySnapshot querySnapshot) {
        postList.clear();
        // In case that no post exists or the automaker filter is emepty in the autoclub page,
        // display the empty view in the custom RecyclerView.
        if(querySnapshot == null || querySnapshot.size() == 0) {
            binding.recyclerBoardPostings.setEmptyView(binding.tvEmptyView);
            return;
        }

        if(currentPage == Constants.BOARD_AUTOCLUB && TextUtils.isEmpty(automaker)) {
            binding.recyclerBoardPostings.setEmptyView(binding.tvEmptyView);
            return;
        }

        // Add DocumentSnapshot to List<DocumentSnapshot> which is paassed to RecyclerView.Adapter.
        // The autoclub page should separately handle query and pagination to sorts out the document
        // snapshot with given filters.
        int pos = 0;
        for(DocumentSnapshot document : querySnapshot) {
            if (currentPage == Constants.BOARD_AUTOCLUB) sortClubPost(document);
            else {
                // Consider if it is appropraite to call nofityDataSetChanged every time.
                postList.add(document);
                postingAdapter.notifyItemChanged(pos);
                //postingAdapter.notifyItemRangeChanged(0, querySnapshot.size(), querySnapshot);
                pos++;
            }
        }

        pbLoading.setVisibility(View.GONE);
        isLoading = false;

        // If the sorted posts are less than the pagination number, keep querying until it's up to
        // the number. Manually update the adapter each time posts amount to the pagination number.
        if(currentPage == Constants.BOARD_AUTOCLUB) {
            if(postList.size() < Constants.PAGINATION) {
                isLoading = true;
                queryPagingUtil.setNextQuery();
                //return;
            }
        }



        //postingAdapter.notifyItemRangeChanged(0, querySnapshot.size() - 1, "query result");
    }

    // Called by QueryPaginationUtil.setNextQuery()
    @Override
    public void getNextQueryResult(QuerySnapshot nextShots) {
        int pos = postList.size();
        for(DocumentSnapshot document : nextShots) {
            if (currentPage == Constants.BOARD_AUTOCLUB) sortClubPost(document);
            else {
                postList.add(document);
                postingAdapter.notifyItemChanged(pos);
                //postingAdapter.notifyDataSetChanged();
                pos++;
            }
        }

        binding.progbarBoardPaging.setVisibility(View.INVISIBLE);
        isLoading = false;

        // Keep querying if sorted posts are less than the pagination number. When it reaches the
        // number, update the apdater.
        if(currentPage == Constants.BOARD_AUTOCLUB) {
            if(postList.size() < Constants.PAGINATION) {
                isLoading = true;
                queryPagingUtil.setNextQuery();
            } //else postingAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void getLastQueryResult(QuerySnapshot lastShots) {
        int pos = postList.size();
        for(DocumentSnapshot document : lastShots) {
            if(currentPage == Constants.BOARD_AUTOCLUB) sortClubPost(document);
            else {
                postList.add(document);
                postingAdapter.notifyItemChanged(pos);
                pos++;
            }
        }

        binding.progbarBoardPaging.setVisibility(View.GONE);
        isLoading = true; // Block the scroll listener from keeping querying.

        // On clicking the filter item on the autoclub page, if nothing has been posted, display
        // the empty view.
        if(currentPage == Constants.BOARD_AUTOCLUB && postList.size() == 0) {
            recyclerPostView.setEmptyView(binding.tvEmptyView);
        }
    }

    @Override
    public void getQueryErrorResult(Exception e) {
        pbLoading.setVisibility(View.GONE);
        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        isLoading = true;
    }


    // This method sorts out the autoclub posts based on the autofilter by removing a document out of
    // the list if it has no autofilter field or its nested filter which can be accessed w/ the dot
    // notation
    private void sortClubPost(DocumentSnapshot snapshot) {
        postList.add(snapshot);
        if(snapshot.get("auto_filter") == null) postList.remove(snapshot);
        else {
            for(String filter : autoFilter) {
                if ((snapshot.get("auto_filter." + filter) == null)) {
                    postList.remove(snapshot);
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
            recyclerPostView.setEmptyView(binding.tvEmptyView);
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

    // RecyclerView.OnScrollListener is an abstract class to receive messages when a scrolling event
    // has occurred on that RecyclerView, which has 2 abstract methods of onScrollStateChanged() and
    // onScrolled(); the former is to be invoked when RecyclerView's scroll state changes and the
    // latter invoked when the RecyclerView has been scrolled.
    private RecyclerView.OnScrollListener setRecyclerViewScrollListener() {
        // RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener(){
        return new RecyclerView.OnScrollListener() {
            //boolean isScrolling;
            /*
             * Callback to be invoked when RecyclerView's scroll state changes.
             * @param recyclerView being scrolled.
             * @param newState: SCROLL_STATE_IDLE, SCROLL_STATE_DRAGGING, SCROLL_STATE_SETTLING
             */
            /*
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    log.i("scrolling idle state");
                    fabWrite.show();
                    fabWrite.setAlpha(0.5f);
                    //isScrolling = false;
                    // Exclude the fab from showing on the notificaiton page.
                    //if(currentPage != Constants.BOARD_NOTIFICATION) fabWrite.show();
                } else if(fabWrite.isShown()) {
                    fabWrite.hide();
                }
            }

             */

            /*
             * Callback to be invoked when the RecyclerView has been scrolled, which will be called
             * right after the scroll has completed. This callback will also be called if visible
             * item range changes after a layout calculation, in which dx and dy will be 0.
             * @param recyclerView being scrolled
             * @param dx The amount of horizontal scroll
             * @param dy The amount of vertical scroll
             */
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                log.i("oScrolled: %s, %s", dx, dy);
                fabWrite.setAlpha(0.3f);

                // FAB visibility control that hides the button while scrolling.
                //if(dy != 0 && fabWrite.isShown()) fabWrite.hide();
                LinearLayoutManager layout = (LinearLayoutManager)recyclerView.getLayoutManager();
                if (layout != null) {
                    int firstVisibleProductPosition = layout.findFirstVisibleItemPosition();
                    int visiblePostCount = layout.getChildCount();
                    int totalPostCount = layout.getItemCount();

                    if (!isLoading && (firstVisibleProductPosition + visiblePostCount == totalPostCount)) {
                        //isScrolling = false;
                        isLoading = true;
                        //if(currentPage != Constants.BOARD_AUTOCLUB) pbPaging.setVisibility(View.VISIBLE);
                        // If the totalPostCount is less than Constants.Pagination, setNextQuery will
                        // return null value, which results in an error as in Notification board. Accrodingly,
                        // a condition has to be added to prevent setNextQuery().
                        if(currentPage != Constants.BOARD_AUTOCLUB && totalPostCount >= Constants.PAGINATION) {
                            binding.progbarBoardPaging.setVisibility(View.VISIBLE);
                            queryPagingUtil.setNextQuery();
                        }

                        //if(currentPage != Constants.BOARD_AUTOCLUB) queryPostSnapshot(currentPage);
                        //else if(!isLastPage) clubRepo.setNextQuery();
                    }
                }
            }

        };
    }


    /*
     * Check if a user is the post's owner or has read the post before in order to increate the view
     * count. In order to do so, get the user id from the internal storage and from the post as well.
     * Get the user id and query the "viewers" sub-collection to check if the user id exists in the
     * documents, which means whether the user has read the post before. If so, do not increase
     * the view count. Otherwise, add the user id to the "viewers" collection and increase the
     * view count;
     */
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
                          if(data != null && data.exists())
                              // Partial binding to BoardPostingAdapter to update the view count in
                              // the post document.
                              postingAdapter.notifyItemChanged(position, data.getLong("cnt_view"));
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
                }).addOnFailureListener(e -> {
                    pb.setVisibility(View.GONE);
                    e.printStackTrace();
                });
    }

}


