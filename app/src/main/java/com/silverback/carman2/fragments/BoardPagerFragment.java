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
import android.widget.ImageView;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.silverback.carman2.BoardActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.adapters.BoardPagerAdapter;
import com.silverback.carman2.adapters.BoardPostingAdapter;
import com.silverback.carman2.utils.ApplyImageResourceUtil;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.utils.PagingQueryHelper;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
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
 *
 */
public class BoardPagerFragment extends Fragment implements
        BoardActivity.OnAutoFilterCheckBoxListener,
        PagingQueryHelper.OnPaginationListener,
        BoardPostingAdapter.OnRecyclerItemClickListener {

    // Logging
    //private static final LoggingHelper log = LoggingHelperFactory.create(BoardPagerFragment.class);

    // Objects
    private FirebaseFirestore firestore;
    private PagingQueryHelper pageHelper;
    private BoardPagerAdapter pagerAdapter;
    private FragmentSharedModel fragmentModel;
    private BoardPostingAdapter postingAdapter;
    private List<DocumentSnapshot> snapshotList;
    private ArrayList<String> autoFilter;
    private SimpleDateFormat sdf;
    private ApplyImageResourceUtil imgutil;

    // UIs
    private LinearLayoutManager layoutManager;
    private ProgressBar pbLoading, pbPaging;
    private PostingRecyclerView recyclerPostView;
    private TextView tvEmptyView;
    private TextView tvSorting;

    // Fields
    private String automaker;
    private int currentPage;
    private boolean isViewOrder;
    private boolean isLoading;
    private boolean isLastPage;

    // Constructor
    public BoardPagerFragment() {
        // Required empty public constructor
    }

    // Singleton for the fragments other than AutoClub
    /*
    public static BoardPagerFragment newInstance(int page, String automaker) {
        BoardPagerFragment fragment = new BoardPagerFragment();
        Bundle args = new Bundle();
        args.putInt("currentPage", page);
        args.putString("automaker", automaker);
        fragment.setArguments(args);

        return fragment;
    }

     */

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

        //pagerAdapter = ((BoardActivity)getActivity()).getPagerAdapter();
        pbLoading = ((BoardActivity)getActivity()).getLoadingProgressBar();
        snapshotList = new ArrayList<>();
        postingAdapter = new BoardPostingAdapter(snapshotList, this);

        pageHelper = new PagingQueryHelper();
        pageHelper.setOnPaginationListener(this);

        // Implement OnFilterCheckBoxListener to receive values of the chkbox each time any chekcbox
        // values changes.
        ((BoardActivity)getActivity()).setAutoFilterListener(this);
        pagerAdapter = ((BoardActivity)getActivity()).getPagerAdapter();
        /*
         * Realtime update SnapshotListener: server vs cache policy.
         * When initially connecting to Firestore, the snapshot listener checks if there is any
         * changes in the borad and upadte the posting board. On completing the inital update,
         * the lisitener should be detached for purpose of preventing excessive connection to the
         * server.
         */
        /*
        CollectionReference postRef = firestore.collection("board_general");
        postListener = postRef.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, e) -> {
            if(e != null) return;

            source = (snapshot != null && snapshot.getMetadata().hasPendingWrites())?
                   Source.CACHE  : Source.SERVER ;
            log.i("source: %s", source);
        });
         */
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
        layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
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
        // PagingQueryHelper subclasses RecyclerView.OnScrollListner.
        recyclerPostView.addOnScrollListener(pageHelper);

        // Unless any autofilter is checked and the automaker retrievedfrom the autofilter is null,
        // the autoclub post is not queried.
        if(currentPage == Constants.BOARD_AUTOCLUB) {
            if(!TextUtils.isEmpty(automaker)) {
                pageHelper.setPostingQuery(currentPage, isViewOrder);
                isLoading = false;
                isLastPage = false;
            }
        } else pageHelper.setPostingQuery(currentPage, isViewOrder);

        return localView;
    }



    @Override
    public void onPause() {
        super.onPause();
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        // On completing UploadPostTask, update BoardPostingAdapter to show a new post, which depends
        // upon which currentPage the viewpager contains.
        fragmentModel.getFirestorePostingDone().observe(getActivity(), docId -> {
            if(!TextUtils.isEmpty(docId)) {
                // Instead of using notifyItemInserted(), query should be done due to the post
                // sequential number to be updated..
                pageHelper.setPostingQuery(currentPage, isViewOrder);

            }
        });

        // The post has been deleted in BoardReadDlgFragment which sequentially popped up AlertDialog
        // for confirm and the result is sent back, then deletes the posting item from Firestore.
        // With All done, receive another LiveData containing the postion of the deleted posting item
        // and update the adapter.
        fragmentModel.getRemovedPosting().observe(getActivity(), docId -> {
            //log.i("Posting removed: %s", docId);
            if(!TextUtils.isEmpty(docId)) {
                pageHelper.setPostingQuery(currentPage, isViewOrder);
            }
        });

        fragmentModel.getEditPosting().observe(requireActivity(), docId -> {
            if(!TextUtils.isEmpty(docId)) {
                pageHelper.setPostingQuery(currentPage, isViewOrder);
            }
        });

    }

    // Create the toolbar menu of the auto club page in the fragment, not in the actity,  which
    // should be customized to have an imageview and textview underneath instead of setting icon
    // by setting actionLayout(app:actionLayout in xml).
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Do something different from in the parent activity
        if(currentPage == Constants.BOARD_AUTOCLUB) {
            //final MenuItem emblem = menu.findItem(R.id.action_automaker_emblem);
            menu.getItem(0).setVisible(true);
            //emblem.setVisible(true);
            View rootView = menu.getItem(0).getActionView();
            //ImageView imgEmblem = (ImageView)menu.getItem(0).getActionView();
            ImageView imgEmblem = rootView.findViewById(R.id.img_action_emblem);
            ProgressBar pbEmblem = rootView.findViewById(R.id.pb_emblem);
            tvSorting = rootView.findViewById(R.id.tv_sorting_order);

            // Set the automaker emblem in the toolbar imageview which is created as a custom view
            // replacing the toolbar menu icon.
            if(!TextUtils.isEmpty(automaker)) setAutoMakerEmblem(pbEmblem, imgEmblem);

            rootView.setOnClickListener(view -> onOptionsItemSelected(menu.getItem(0)));
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_automaker_emblem) {
            isViewOrder = !isViewOrder;
            // Make the post sorting by time-wise or viewer-wise basis.
            //sortDocumentByTimeOrView();

            // Set the spannable string indicating what's the basis of sorting. The reason why the
            // span is set.
            String sortLabel = (isViewOrder)? getString(R.string.board_autoclub_sort_view) : getString(R.string.board_autoclub_sort_time);
            tvSorting.setText(sortLabel);
            snapshotList.clear();
            pageHelper.setPostingQuery(Constants.BOARD_AUTOCLUB, isViewOrder);

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


    // Implement PagingQueryHelper.OnPaginationListener which notifies the adapter of the first and
    // the next query results.
    @Override
    public void setFirstQuery(int page, QuerySnapshot snapshots) {
        snapshotList.clear();
        if(snapshots.size() == 0) recyclerPostView.setEmptyView(tvEmptyView);
        //log.i("First Query: %s", snapshots.size());
        for(QueryDocumentSnapshot snapshot : snapshots) {
            // In the autoclub page, the query result is added to the list regardless of whether the
            // field value of 'post_general" is true or not. The other boards, however, the result
            // is added as far as the post_general is true.
            switch(page) {
                case Constants.BOARD_AUTOCLUB:
                    sortAutoClubPost(snapshot);
                    pageHelper.setNextQuery(snapshots);
                    break;

                case Constants.BOARD_NOTIFICATION:
                    snapshotList.add(snapshot);
                    break;
                default:
                    if((boolean)snapshot.get("post_general")) snapshotList.add(snapshot);
                    break;
            }
        }

        /*
        if(page == Constants.BOARD_AUTOCLUB) {
            //pageHelper.setNextQuery(snapshots);
            if(snapshotList.size() < Constants.PAGINATION) postingAdapter.notifyDataSetChanged();

        } else postingAdapter.notifyDataSetChanged();
        */
        // If posts exist, dismiss the progressbar. No posts exist, set the textview to the empty
        // view of the custom recyclerview.
        pbLoading.setVisibility(View.GONE);
    }

    @Override
    public void setNextQueryStart(boolean isNextQuery) {
        if(isNextQuery) pbPaging.setVisibility(View.VISIBLE);
        else pbPaging.setVisibility(View.GONE);
    }

    @Override
    public void setNextQueryComplete(int page, QuerySnapshot snapshots) {
        if(snapshots.size() == 0) return;
        for(QueryDocumentSnapshot snapshot : snapshots) {
            switch(page) {
                case Constants.BOARD_AUTOCLUB:
                    sortAutoClubPost(snapshot);
                    isLoading = false;
                    isLastPage = snapshots.size() < Constants.PAGINATION;
                    postingAdapter.notifyDataSetChanged();
                    break;

                case Constants.BOARD_NOTIFICATION:
                    snapshotList.add(snapshot);
                    break;

                default:
                    if((boolean)snapshot.get("post_general")) snapshotList.add(snapshot);
                    break;
            }
        }

        if(page == Constants.BOARD_AUTOCLUB) {
            // Keep querying the autoclub posts until the sorted posts are equal to or more than
            // the pagination limit unless the next query is the last page.
            if(!isLastPage && snapshotList.size() < Constants.PAGINATION){
                pageHelper.setNextQuery(snapshots);
                return;
            }

            // The autoclub repeats the next query manually until it comes to the last query. The
            // other board makes the next query automatically by scrolling. The autoclub updates
            // the adapter only when the last query is done.
            // CONDITION SHOULD BE CREATED TO PREVENT EXCESSIVE QUERY RESULTS FROM ADDING TO THE
            // LIST!!!
            recyclerPostView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if(layoutManager == null || dy == 0) return;

                    int firstItemPos = layoutManager.findFirstVisibleItemPosition();
                    int visibleItemCount = layoutManager.getChildCount();

                    if(!isLoading && !isLastPage && firstItemPos + visibleItemCount >= snapshotList.size()) {
                        isLoading = true;
                        pageHelper.setNextQuery(snapshots);

                    } //else postingAdapter.notifyDataSetChanged();
                }
            });

        } //else postingAdapter.notifyDataSetChanged();

        postingAdapter.notifyDataSetChanged();
        pbPaging.setVisibility(View.GONE);

    }

    // Implement OnFilterCheckBoxListener which notifies any change of checkbox values, which
    // performa a new query with new autofilter values
    @Override
    public void onCheckBoxValueChange(ArrayList<String> autofilter) {
        this.autoFilter = autofilter;
        pagerAdapter.notifyDataSetChanged();
        pageHelper.setPostingQuery(Constants.BOARD_AUTOCLUB, isViewOrder);
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

        /*
        // Query the user(posting writer) with userId given to fetch the autoclub which contains
        // auto_maker, auto_model, eco_type and auto_year as JSON string. On completion, set it to
        // the fragment arguments and pass them to BoardReadDlgFragment
        firestore.collection("users").document(snapshot.getString("user_id")).get()
                .addOnSuccessListener(document -> {
                    if(document.exists()) {
                        String auto = document.getString("user_club");
                        if(!TextUtils.isEmpty(auto)) bundle.putString("autoClub", auto);

                        readPostFragment.setArguments(bundle);
                        // What if Fragment calls another fragment? What is getChildFragmentManager() for?
                        // android.R.id.content makes DialogFragment fit to the full screen.
                        getActivity().getSupportFragmentManager().beginTransaction()
                                .add(android.R.id.content, readPostFragment)
                                .addToBackStack(null)
                                .commit();
                    }
                });
        */

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

    // This method sorts out the autoclub posts based on the autofilter by removing a document out of
    // the list if it has no autofilter field or its nested filter which can be accessed w/ the dot
    // notation
    private void sortAutoClubPost(QueryDocumentSnapshot snapshot) {
        snapshotList.add(snapshot);
        if(snapshot.get("auto_filter") == null) snapshotList.remove(snapshot);
        else {
            for(String filter : autoFilter) {
                if ((snapshot.get("auto_filter." + filter) == null)) {
                    snapshotList.remove(snapshot);
                    break;
                }
            }
        }
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
                  Calendar calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault());
                  Date date = calendar.getTime();

                  viewerData.put("timestamp", new Timestamp(date));
                  // coding required.
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


