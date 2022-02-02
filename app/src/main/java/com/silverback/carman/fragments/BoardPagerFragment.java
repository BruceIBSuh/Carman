package com.silverback.carman.fragments;


import static com.silverback.carman.BoardActivity.AUTOCLUB;
import static com.silverback.carman.BoardActivity.PAGINATION;

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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
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
import com.silverback.carman.utils.RecyclerDividerUtil;
import com.silverback.carman.viewmodels.FragmentSharedModel;

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

/*
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
 * posts notified by the viewmodel(FragmentSharedModel). Comments in BoardReadFragment, however,
 * applies SnapshotListener.
 *
 * Refactoring based on MVVM to improve the query performance should be made. At the moment, related
 * codes are commented.
 */
public class BoardPagerFragment extends Fragment implements
        BoardActivity.OnAutoFilterCheckBoxListener,
        QueryPostPaginationUtil.OnQueryPaginationCallback,
        //BoardReadFragment.OnDialogDismissListener,
        //QueryClubPostingUtil.OnPaginationListener,
        BoardPostingAdapter.OnRecyclerItemClickListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPagerFragment.class);

    // Objects
    //private List<MultiTypeItem> multiTypeItemList;
    private List<DocumentSnapshot> snapshotList;

    private FirebaseFirestore firestore;
    //private PostingBoardViewModel postingModel;
    //private PostingBoardRepository postRepo;
    //private QueryClubPostingUtil clubRepo;
    //private ListenerRegistration listenerRegistration;
    private QueryPostPaginationUtil queryPagingUtil;

    private FragmentSharedModel fragmentModel;
    private BoardPagerAdapter pagerAdapter;
    private BoardPostingAdapter postingAdapter;

    private ArrayList<String> autoFilter;
    private SimpleDateFormat sdf;
    private ApplyImageResourceUtil imgutil;

    // UIs
    private FragmentBoardPagerBinding binding;
    private ProgressBar progbar;
    //private PostingRecyclerView recyclerPostView;
    private FloatingActionButton fabWrite;
    //private TextView tvEmptyView;
    //private TextView tvSorting;


    // Fields
    private String automaker;
    private int currentPage;
    private boolean isViewOrder;
    private boolean isLoading; // to block the RecyclerView from scrolling while loading posts.
    private int index;
    //private boolean isLastPage;
    //private boolean isViewUpdated;
    //private boolean isScrolling;

    // Constructor
    public BoardPagerFragment() {
        // Required empty public constructor
    }

    // Singleton for the autoclub page which has the checkbox values and display title names
    // in the tab menu.
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
        //multiTypeItemList = new ArrayList<>();
        snapshotList = new ArrayList<>();

        firestore = FirebaseFirestore.getInstance();
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        imgutil = new ApplyImageResourceUtil(getContext());
        sdf = new SimpleDateFormat("MM.dd HH:mm", Locale.getDefault());

        progbar = ((BoardActivity)requireActivity()).getLoadingProgressBar();
        // Instantiate the query and pagination util class and create the RecyclerView adapter to
        // show the posting list.

        postingAdapter = new BoardPostingAdapter(snapshotList, this);
        //postingAdapter = new BoardPostingAdapter(multiTypeItemList, this);
        queryPagingUtil = new QueryPostPaginationUtil(firestore, this);

        /*
        if(currentPage == AUTOCLUB) {
            //clubRepo = new QueryClubPostingUtil(firestore);
            //clubRepo.setOnPaginationListener(this);
            postingAdapter = new BoardPostingAdapter(clubshotList, this);
        } else {
            //postRepo = new PostingBoardRepository();
            //postingModel = new ViewModelProvider(this, new PostingBoardModelFactory(postRepo)).get(PostingBoardViewModel.class);
            postingAdapter = new BoardPostingAdapter(postshotList, this);
        }
         */

        // Implement OnFilterCheckBoxListener to receive values of the chkbox each time any chekcbox
        // values changes.
        ((BoardActivity)requireActivity()).setAutoFilterListener(this);
        pagerAdapter = ((BoardActivity)requireActivity()).getPagerAdapter();

    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        log.i("BoardPagerFragment onCreateView()");
        binding = FragmentBoardPagerBinding.inflate(inflater);
        // Wrapping class to trhow IndexOutOfBound exception which is occasionally casued by RecyclerView.
        //WrapContentLinearLayoutManager layoutManager = new WrapContentLinearLayoutManager(requireActivity());
        LinearLayoutManager layout = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        RecyclerDividerUtil divider = new RecyclerDividerUtil(Constants.DIVIDER_HEIGHT_POSTINGBOARD,
                0, ContextCompat.getColor(requireContext(), R.color.recyclerDivider));

        binding.recyclerBoardPostings.setHasFixedSize(false); // for banner plugin
        binding.recyclerBoardPostings.setLayoutManager(layout);
        binding.recyclerBoardPostings.addItemDecoration(divider);
        binding.recyclerBoardPostings.setItemAnimator(new DefaultItemAnimator());
        //SimpleItemAnimator itemAnimator = (SimpleItemAnimator)binding.recyclerBoardPostings.getItemAnimator();
        //itemAnimator.setSupportsChangeAnimations(false);
        binding.recyclerBoardPostings.setAdapter(postingAdapter);
        binding.recyclerBoardPostings.addOnScrollListener(scrollListener);

        // Show/hide Floating Action Button as the recyclerview scrolls.
        fabWrite = ((BoardActivity)Objects.requireNonNull(requireActivity())).getFAB();

        /* Based on MVVM
        if(currentPage == BoardActivity.AUTOCLUB) {
            // Initialize the club board if any filter is set.
            if(!TextUtils.isEmpty(automaker)) {
                isLastPage = false;
                //postshotList.clear();
                clubshotList.clear();
                clubRepo.setPostingQuery(isViewOrder);
            }
        } else queryPostSnapshot(currentPage);
        */
        isLoading = true;
        queryPagingUtil.setPostQuery(currentPage, isViewOrder);

        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        log.i("onViewCreated in BoardPagerFragment invoked by ViewModel");
        // On completing UploadPostTask, update BoardPostingAdapter to show a new post, which depends
        // upon which currentPage the viewpager contains.
        // not invoked by the viewmodel. Instead, call notifyItemChanged() in addViewPager().
        fragmentModel.getNewPosting().observe(getViewLifecycleOwner(), docId -> {
            log.i("new posting: %s", docId);
            postingAdapter.notifyItemInserted(0);
            ((BoardActivity)requireActivity()).addViewPager();
            /*
            if(!TextUtils.isEmpty(docId)) {
                queryPagingUtil.setPostQuery(currentPage, isViewOrder);
            }

             */
        });

        // The post has been deleted in BoardReadFragment which sequentially popped up AlertDialog
        // for confirm and the result is sent back, then deletes the posting item from Firestore.
        // With All done, receive another LiveData containing the position of the deleted posting item
        // and update the adapter.
        /*
        fragmentModel.getRemovedPosting().observe(getViewLifecycleOwner(), position -> {
            log.i("Post removed: %s", position);
            //postingAdapter.notifyItemRemoved(position);
            //((BoardActivity)requireActivity()).addViewPager();
            //if(!TextUtils.isEmpty(docId)) queryPagingUtil.setPostQuery(currentPage, isViewOrder);
        });

        fragmentModel.getEditedPosting().observe(requireActivity(), docId -> {
            log.i("edited posting viewmodel");
            if(!TextUtils.isEmpty(docId)) queryPagingUtil.setPostQuery(currentPage, isViewOrder);
        });

        // Observe the viewmodel for partial binding to BoardPostingAdapter to update the comment count,
        // the livedata of which is created when a comment has finished uploadingb in BoardReadFragment.
        fragmentModel.getNewComment().observe(requireActivity(), sparseArray -> {
            log.i("new comment");
            postingAdapter.notifyItemChanged(sparseArray.keyAt(0), sparseArray);
        });
         */

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    // Create the toolbar menu of the auto club page in the fragment, not in the activity, which
    // should be customized to have an imageview and textview underneath instead of setting icon
    // by setting actionLayout(app:actionLayout in xml).
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Do something different from in the parent activity
        //this.menu = menu;
        //menu.getItem(0).setVisible(false);
        //menu.getItem(1).setVisible(false);
        if(currentPage == AUTOCLUB) {
            View actionView = menu.getItem(0).getActionView();
            ImageView imgEmblem = actionView.findViewById(R.id.img_action_emblem);
            ProgressBar pbEmblem = actionView.findViewById(R.id.pb_emblem);
            if(TextUtils.isEmpty(automaker)) {
                menu.getItem(0).setVisible(false);
                //actionView.setVisibility(View.INVISIBLE);
            } else {
                menu.getItem(0).setVisible(true);
                actionView.setVisibility(View.VISIBLE);
                setAutoMakerEmblem(pbEmblem, imgEmblem);
                actionView.setOnClickListener(view -> onOptionsItemSelected(menu.getItem(0)));
            }

        } else super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_automaker_emblem) {
            // Initialize fields when clicking the menu for switching timestamp and cnt_view
            //isLastPage = false;
            //clubshotList.clear();
            //clubRepo.setPostingQuery(isViewOrder);

            // Requery the autoclub post with the field switched.
            //isLoading = true;
            //pbPaging.setVisibility(View.GONE);
            //binding.progbarBoardPaging.setVisibility(View.GONE);
            //queryPagingUtil.setPostQuery(currentPage, isViewOrder);

            // Rotate the automaker emblem
            ObjectAnimator rotation = ObjectAnimator.ofFloat(item.getActionView(), "rotationY", 0.0f, 360f);
            rotation.setDuration(500);
            rotation.setInterpolator(new AccelerateDecelerateInterpolator());
            // Use AnimatorListenerAdapter to take callback methods seletively.
            rotation.addListener(new AnimatorListenerAdapter(){
                @Override
                public void onAnimationEnd(Animator animation, boolean isReverse) {
                    rotation.cancel();
                    String sorting = (isViewOrder)? getString(R.string.board_autoclub_sort_time) :
                            getString(R.string.board_autoclub_sort_view);
                    TextView tvSorting = item.getActionView().findViewById(R.id.tv_sorting_order);
                    tvSorting.setText(sorting);
                    isViewOrder = !isViewOrder;
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
        //pagerAdapter.notifyDataSetChanged();
        pagerAdapter.notifyItemChanged(AUTOCLUB, autofilter);

        //pageHelper.setPostingQuery(BoardActivity.AUTOCLUB, isViewOrder);
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
        BoardReadFragment readPostFragment = new BoardReadFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("tabPage", currentPage);
        log.i("tabPage: %s", currentPage);
        // TEST CODING FOR UPDATING THE COMMENT NUMBER
        bundle.putInt("position", position);

        bundle.putString("documentId", snapshot.getId());
        bundle.putString("postTitle", snapshot.getString("post_title"));

        if(currentPage == BoardActivity.NOTIFICATION) {
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

        if(snapshot.get("post_images") != null) {
            BoardActivity.PostImages objImages = snapshot.toObject(BoardActivity.PostImages.class);
            bundle.putStringArrayList("urlImgList", Objects.requireNonNull(objImages).getPostImages());
        }

        readPostFragment.setArguments(bundle);
        //readPostFragment.setDialogDismissListener(this);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, readPostFragment)
                .addToBackStack(null)
                .commit();

        // Update the field of "cnt_view" increasing the number.
        DocumentReference docref = snapshot.getReference();
        addViewCount(docref, position);
    }

    /*
    @Override
    public void notifyDialogDismissed(int position) {
        log.i("Read Dialog dismissed:%s", position);
        // This problem is caused by RecyclerView Data modified in different thread.
        // The best way is checking all data access. And a workaround is wrapping LinearLayoutManager.
        //postingAdapter.notifyItemRemoved(position);
        log.i("Item size: %s", multiTypeItemList.size());
        pagerAdapter.notifyItemChanged(currentPage);
        //((BoardActivity)requireActivity()).addViewPager();
    }

     */

    /*
     * Implement QueryPaginationUtil.OnQueryPaginationCallback overriding the following 4 methods that
     * performs to query posts with orderBy() and limit() conditioned up to the pagination number
     * which is defined in BoardActivity.PAGINATION.
     *
     * getFirestQueryResult(): setPostQuery() has completed with queried posts up to the pagination number.
     * getNextQueryResult(): setNextQuery() has completed with queried posts up to the pagination number.
     * getLastQueryResult(); setNextQuery() has completed with queried posts under the pagination number.
     * getQueryErrorResult(); callback invoked if any error has occurred while querying.
     */
    @Override
    public void getFirstQueryResult(QuerySnapshot querySnapshot) {
        if(querySnapshot.size() == 0) {
            progbar.setVisibility(View.GONE);
            binding.recyclerBoardPostings.setVisibility(View.GONE);
            binding.tvEmptyView.setVisibility(View.VISIBLE);
            return;
        } else {
            binding.recyclerBoardPostings.setVisibility(View.VISIBLE);
            binding.tvEmptyView.setVisibility(View.GONE);
        }

        //index = 0;
        //multiTypeItemList.clear();
        snapshotList.clear();


        // In case that no post exists or the automaker filter is emepty in the autoclub page,
        // display the empty view in the custom RecyclerView.
        /*
        if(querySnapshot == null || querySnapshot.size() == 0) {
            progbar.setVisibility(View.GONE);
            binding.recyclerBoardPostings.setEmptyView(binding.tvEmptyView);
            return;
        }

        if(currentPage == BoardActivity.AUTOCLUB && TextUtils.isEmpty(automaker)) {
            progbar.setVisibility(View.GONE);
            binding.recyclerBoardPostings.setEmptyView(binding.tvEmptyView);
            return;
        }

         */

        // Add DocumentSnapshot to List<DocumentSnapshot> which is paassed to RecyclerView.Adapter.
        // The autoclub page should separately handle query and pagination to sorts out the document
        // snapshot with given filters.
        for(DocumentSnapshot document : querySnapshot) {
            if (currentPage == AUTOCLUB) sortClubPost(document);
            else {
                snapshotList.add(document);
                //multiTypeItemList.add(new MultiTypeItem(0, index, document));
            }
            //index++;
        }

        // Test Code: for pre-occupying the banner slot.
        //addDummySlotForAds();
        //multiTypeItemList.add(new MultiTypeItem(1));
        postingAdapter.notifyItemRangeChanged(0, querySnapshot.size());
        progbar.setVisibility(View.GONE);
        isLoading = false;

        // If the sorted posts are less than the pagination number, keep querying until it's up to
        // the number. Manually update the adapter each time posts amount to the pagination number.
        if(currentPage == AUTOCLUB) {
            //if(multiTypeItemList.size() < PAGINATION) {
            if(snapshotList.size() < PAGINATION) {
                isLoading = true;
                queryPagingUtil.setNextQuery();
            }
        }
    }

    @Override
    public void getNextQueryResult(QuerySnapshot nextShots) {
        //final int start = multiTypeItemList.size();
        final int start = snapshotList.size();
        for(DocumentSnapshot document : nextShots) {
            if (currentPage == AUTOCLUB) sortClubPost(document);
            else {
                snapshotList.add(document);
                //multiTypeItemList.add(new MultiTypeItem(0, index, document));
            }
            //index++;
        }

        // ADD THE AD BANNER: refactor required for the convenience's sake.
        //multiTypeItemList.add(new MultiTypeItem(1));
        postingAdapter.notifyItemRangeChanged(start, nextShots.size());
        //binding.progbarBoardPaging.setVisibility(View.INVISIBLE);

        // Keep querying if sorted posts are less than the pagination number. When it reaches the
        // number, update the apdater.
        if(currentPage == AUTOCLUB) {
            //if(multiTypeItemList.size() < PAGINATION) {
            if(snapshotList.size() < PAGINATION) {
                isLoading = true;
                queryPagingUtil.setNextQuery();
            }//else postingAdapter.notifyDataSetChanged();
        }

        isLoading = false;
    }

    @Override
    public void getLastQueryResult(QuerySnapshot lastShots) {
        final int start = snapshotList.size();
        //final int start = multiTypeItemList.size();
        //int index = snapshotList.size();
        for(DocumentSnapshot document : lastShots) {
            if(currentPage == AUTOCLUB) sortClubPost(document);
            else {
                snapshotList.add(document);
                //multiTypeItemList.add(new MultiTypeItem(0, index, document));
            }
            index++;
        }
        postingAdapter.notifyItemRangeChanged(start, lastShots.size());
        //binding.progbarBoardPaging.setVisibility(View.GONE);
        //isLoading = true; // Block the scroll listener from keeping querying.
        binding.recyclerBoardPostings.removeOnScrollListener(scrollListener);

        // On clicking the filter item on the autoclub page, if nothing has been posted, display
        // the empty view.
        if(currentPage == AUTOCLUB && snapshotList.size() == 0) {
        //if(currentPage == AUTOCLUB && multiTypeItemList.size() == 0) {
            //recyclerPostView.setEmptyView(binding.tvEmptyView);
            //binding.recyclerBoardPostings.setEmptyView(binding.tvEmptyView);
            binding.recyclerBoardPostings.setVisibility(View.GONE);
            binding.tvEmptyView.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerBoardPostings.setVisibility(View.VISIBLE);
            binding.tvEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void getQueryErrorResult(Exception e) {
        //progbar.setVisibility(View.GONE);
        e.printStackTrace();
        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        //isLoading = true;
        binding.recyclerBoardPostings.removeOnScrollListener(scrollListener);
    }


    // This method sorts out the autoclub posts based on the autofilter by removing a document out of
    // the list if it has no autofilter field or its nested filter which can be accessed w/ the dot
    // notation
    private void sortClubPost(DocumentSnapshot snapshot) {
        snapshotList.add(snapshot);
        //multiTypeItemList.add(new MultiTypeItem(0, index, snapshot));
        /*
        if(snapshot.get("auto_filter") == null) snapshotList.remove(snapshot);
        else {
            for(String filter : autoFilter) {
                if ((snapshot.get("auto_filter." + filter) == null)) {
                    snapshotList.remove(snapshot);
                    break;
                }
            }
        }

         */
    }

    // RecyclerView.OnScrollListener is an abstract class to receive messages when a scrolling event
    // has occurred on that RecyclerView, which has 2 abstract methods of onScrollStateChanged() and
    // onScrolled(); the former is to be invoked when RecyclerView's scroll state changes and the
    // latter invoked when the RecyclerView has been scrolled.
    //private RecyclerView.OnScrollListener setRecyclerViewScrollListener() {
    private final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener(){
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
            fabWrite.setAlpha(0.8f);

            //WrapContentLinearLayoutManager layout = (WrapContentLinearLayoutManager)recyclerView.getLayoutManager();
            LinearLayoutManager layout = (LinearLayoutManager)recyclerView.getLayoutManager();
            if (layout != null) {
                int firstVisibleProductPosition = layout.findFirstVisibleItemPosition();
                int visiblePostCount = layout.getChildCount();
                int totalPostCount = layout.getItemCount();
                //log.i("totalPostCount: %s, %s, %s", firstVisibleProductPosition, visiblePostCount, totalPostCount);

                if (!isLoading && (firstVisibleProductPosition + visiblePostCount == totalPostCount)) {
                    //isScrolling = false;
                    isLoading = true;
                    //if(currentPage != AUTOCLUB) pbPaging.setVisibility(View.VISIBLE);
                    // If the totalPostCount is less than PAGINATION, setNextQuery will
                    // return null value, which results in an error as in Notification board. Accrodingly,
                    // a condition has to be added to prevent setNextQuery().
                    if(currentPage != AUTOCLUB && totalPostCount >= PAGINATION) {
                        //binding.progbarBoardPaging.setVisibility(View.VISIBLE);
                        queryPagingUtil.setNextQuery();
                    }

                    //if(currentPage != AUTOCLUB) queryPostSnapshot(currentPage);
                    //else if(!isLastPage) clubRepo.setNextQuery();
                }
            }
        }

    };



    /*
     * Check if a user is the post's owner or has read the post before in order to increate the view
     * count. In order to do so, get the user id from the internal storage and from the post as well.
     * Get the user id and query the "viewers" sub-collection to check if the user id exists in the
     * documents, which means whether the user has read the post before. If so, do not increase
     * the view count. Otherwise, add the user id to the "viewers" collection and increase the
     * view count;
     */
    private void addViewCount(DocumentReference docref, int position) {
        try(FileInputStream fis = Objects.requireNonNull(requireActivity()).openFileInput("userId");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            final String viewerId = br.readLine();

            CollectionReference subCollection = docref.collection("viewers");
            subCollection.document(viewerId).get().addOnSuccessListener(snapshot -> {
                // In case the user has not read the post before and adoes not exists in the "viewers"
                // collection
                if(!snapshot.exists()) {
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
                          if(data.exists())
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
        firestore.collection("autodata").document(automaker).get().addOnSuccessListener(doc -> {
            String emblem = doc.getString("auto_emblem");
            if(TextUtils.isEmpty(emblem)) return;
            else {
                Uri uri = Uri.parse(emblem);
                final int x = imgview.getMeasuredWidth();
                final int y = imgview.getMeasuredHeight();
                imgutil.applyGlideToEmblem(uri, x, y, imgview);
            }

            pb.setVisibility(View.GONE);

        });

        /*
        //firestore.collection("autodata").whereEqualTo("auto_maker", automaker).get()
        firestore.collection("autodata").whereEqualTo(FieldPath.documentId(), automaker).get()
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

         */
    }

    public static class MultiTypeItem {
        DocumentSnapshot snapshot;
        int index;
        int viewType;

        public MultiTypeItem(int viewType, int index, DocumentSnapshot snapshot) {
            this.snapshot = snapshot;
            this.viewType = viewType;
            this.index = index;
        }

        public MultiTypeItem(int viewType){
            this.viewType = viewType;
        }

        public DocumentSnapshot getItemSnapshot() {
            return snapshot;
        }
        public int getViewType() {
            return viewType;
        }
        public int getItemIndex() {
            return index;
        }
    }

    // Wrapper class to throw java.lang.IndexOutOfBoundsException: Inconsistency detected.
    // Invalid view holder adapter positionPostViewHolder
    /*
    private static class WrapContentLinearLayoutManager extends LinearLayoutManager {
        // Constructor
        public WrapContentLinearLayoutManager(Context context) {
            super(context, LinearLayoutManager.VERTICAL, false);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            try {
                super.onLayoutChildren(recycler, state);
            } catch(IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }

     */

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



}


