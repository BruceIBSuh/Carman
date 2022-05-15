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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.silverback.carman.BoardActivity;
import com.silverback.carman.R;
import com.silverback.carman.adapters.BoardPostingAdapter;
import com.silverback.carman.databinding.FragmentBoardPagerBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.CustomPostingObject;
import com.silverback.carman.utils.QueryPostPaginationUtil;
import com.silverback.carman.utils.RecyclerDividerUtil;
import com.silverback.carman.viewmodels.FragmentSharedModel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BoardPagerFragment extends Fragment implements
        QueryPostPaginationUtil.OnQueryPaginationCallback,
        BoardPostingAdapter.OnPostingAdapterListener {

    private static final LoggingHelper log = LoggingHelperFactory.create(BoardPagerFragment.class);

    private FirebaseFirestore mDB;
    private ListenerRegistration regListener;
    private CollectionReference colRef;
    private QueryPostPaginationUtil queryPagingUtil;
    private BoardPostingAdapter postingAdapter;
    private ApplyImageResourceUtil imgutil;
    private FragmentSharedModel fragmentModel;

    private FragmentBoardPagerBinding binding;
    private ProgressBar progbar;
    private FloatingActionButton fabWrite;
    private Menu menu;

    //private List<MultiTypeItem> multiTypeItemList;
    private List<DocumentSnapshot> postingList;
    private ArrayList<String> autofilter;
    private String automaker;
    private String userId;
    private int currentPage;
    private boolean isViewOrder;
    private boolean isScrollable; // to block recyclerview from scrolling while loading posts.

    private ImageView imgEmblem;
    private ProgressBar pbEmblem;

    // Constructor
    private BoardPagerFragment() {
        // Required empty public constructor
    }

    public static BoardPagerFragment newInstance(int page, String userId, ArrayList<String> values){
        BoardPagerFragment fragment = new BoardPagerFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
        args.putInt("currentPage", page);
        args.putStringArrayList("autofilter", values);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if(getArguments() != null) {
            userId = getArguments().getString("userId");
            currentPage = getArguments().getInt("currentPage");
            autofilter = getArguments().getStringArrayList("autofilter");
            if(autofilter != null && autofilter.size() > 0) automaker = autofilter.get(0);
        }

        progbar = ((BoardActivity)requireActivity()).getLoadingProgressBar();
        imgutil = new ApplyImageResourceUtil(getContext());

        // Instantiate objects.
        mDB = FirebaseFirestore.getInstance();
        postingList = new ArrayList<>();
        //multiTypeItemList = new ArrayList<>();
        postingAdapter = new BoardPostingAdapter(postingList, this);
        //postingAdapter = new BoardPostingAdapter(multiTypeItemList, this);
        //postingAdapter.setHasStableIds(true);

        queryPagingUtil = new QueryPostPaginationUtil(mDB, this);
        colRef = mDB.collection("user_post");
        if(currentPage == AUTOCLUB) queryPagingUtil.setAutoClubOrder(isViewOrder);
        //regListener = queryPagingUtil.setPostQuery(colRef, currentPage);
        queryPagingUtil.setPostQuery(colRef, currentPage);
        isScrollable = false;
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentBoardPagerBinding.inflate(inflater);

        LinearLayoutManager layout = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        RecyclerDividerUtil divider = new RecyclerDividerUtil(Constants.DIVIDER_HEIGHT_POSTINGBOARD,
                0, ContextCompat.getColor(requireContext(), R.color.recyclerDivider));
        binding.recyclerBoardPostings.setHasFixedSize(false);
        binding.recyclerBoardPostings.setLayoutManager(layout);
        binding.recyclerBoardPostings.addItemDecoration(divider);
        binding.recyclerBoardPostings.setItemAnimator(new DefaultItemAnimator());
        //SimpleItemAnimator itemAnimator = (SimpleItemAnimator)binding.recyclerBoardPostings.getItemAnimator();
        //if(itemAnimator != null) itemAnimator.setSupportsChangeAnimations(false);
        binding.recyclerBoardPostings.setAdapter(postingAdapter);
        binding.recyclerBoardPostings.addOnScrollListener(scrollListener);
        fabWrite = ((BoardActivity)Objects.requireNonNull(requireActivity())).getFAB();

        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);

        fragmentModel.getNewPosting().observe(getViewLifecycleOwner(), postRef -> {
            log.i("new posting: %s", currentPage);
            postRef.get().addOnCompleteListener(task -> {
                if(task.isSuccessful()) {
                    DocumentSnapshot snapshot = task.getResult();
                    log.i("post images: %s", snapshot.get("post_images"));
                    queryPagingUtil.setPostQuery(colRef, currentPage);
                }
            });
        });

        fragmentModel.getRemovedPosting().observe(getViewLifecycleOwner(), post -> {
            log.i("posting removed: %s", currentPage);
            postingList.remove(post);
            postingAdapter.submitPostList(postingList);
            //postingAdapter.updatePostList(postingList);
            queryPagingUtil.setPostQuery(colRef, currentPage);
        });

        fragmentModel.getEditedPosting().observe(getViewLifecycleOwner(), sparseArray -> {
            final int position = sparseArray.keyAt(0);
            final DocumentReference docRef = (DocumentReference)sparseArray.valueAt(0);
            docRef.get().addOnSuccessListener(doc -> {
                postingList.set(position, doc);
                queryPagingUtil.setPostQuery(colRef, currentPage);
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

    }

    @Override
    public void onDestroyView() {
        if(regListener != null) regListener.remove();
        binding.recyclerBoardPostings.removeOnScrollListener(scrollListener);
        super.onDestroyView();
    }

    // Create the toolbar menu of the autoclub page in the fragment, not in the activity, which
    // should be customized to have an imageview and textview underneath instead of setting icon
    // by setting actionLayout(app:actionLayout in xml).
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        this.menu = menu;
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        if(currentPage == AUTOCLUB) {
            View actionView = menu.getItem(0).getActionView();
            imgEmblem = actionView.findViewById(R.id.img_action_emblem);
            pbEmblem = actionView.findViewById(R.id.pb_emblem);

            if(TextUtils.isEmpty(automaker)) {
                menu.getItem(0).setVisible(false);
                actionView.setVisibility(View.GONE);
            } else {
                menu.getItem(0).setVisible(true);
                actionView.setVisibility(View.VISIBLE);
                setAutoMakerEmblem(pbEmblem, imgEmblem);
                actionView.setOnClickListener(view -> onOptionsItemSelected(menu.getItem(0)));
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_automaker_emblem) {
            rotateAutoEmblem(item).start();
            return true;
        } return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPostItemClicked(DocumentSnapshot snapshot, int position) {
        BoardReadFragment readPostFragment = new BoardReadFragment();
        CustomPostingObject toObject = snapshot.toObject(CustomPostingObject.class);
        assert toObject != null;
        Bundle bundle = new Bundle();
        bundle.putInt("tabPage", currentPage);
        bundle.putInt("position", position);
        bundle.putString("viewerId", userId);
        bundle.putString("documentId", snapshot.getId());
        bundle.putParcelable("postingObj", toObject);

        readPostFragment.setArguments(bundle);
        requireActivity().getSupportFragmentManager().beginTransaction()
                .add(android.R.id.content, readPostFragment)
                .addToBackStack(null)
                .commit();

        // Update the field of "cnt_view" increasing the number.
        //DocumentReference docref = snapshot.getReference();
        addViewCount(snapshot.getReference(), position);
    }

    @Override
    public void getFirstQueryResult(QuerySnapshot querySnapshot) {
        postingList.clear();
        addPostByCategory(querySnapshot, false);
        isScrollable = true;
    }

    @Override
    public void getNextQueryResult(QuerySnapshot nextShots) {
        addPostByCategory(nextShots, false);
        isScrollable = true;
    }

    @Override
    public void getLastQueryResult(QuerySnapshot lastShots) {
        addPostByCategory(lastShots, true);
        isScrollable = false;
    }


    @Override
    public void getQueryErrorResult(Exception e) {
        progbar.setVisibility(View.GONE);
        e.printStackTrace();
        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
        isScrollable = false;
        binding.recyclerBoardPostings.removeOnScrollListener(scrollListener);
    }

    private void addPostByCategory(QuerySnapshot querySnapshot, boolean isLast) {
        for(DocumentSnapshot doc : querySnapshot) {
            if(currentPage == AUTOCLUB) {
                if(autofilter == null || autofilter.size() == 0) break;
                CustomPostingObject toObject = doc.toObject(CustomPostingObject.class);
                if(toObject == null) return;
                ArrayList<String> filters = new ArrayList<>(toObject.getAutofilter());
                if(filters.containsAll(autofilter)) postingList.add(doc);
            } else postingList.add(doc);
        }

        if(currentPage == AUTOCLUB) {
            if (!isLast && postingList.size() < PAGINATION) {
                queryPagingUtil.setNextPostQuery();
                isScrollable = false;
                return;
            } else {
                progbar.setVisibility(View.GONE);
                postingAdapter.submitPostList(postingList);
                //postingAdapter.updatePostList(postingList);
            }
        } else {
            progbar.setVisibility(View.GONE);
            postingAdapter.submitPostList(postingList);
            //postingAdapter.updatePostList(postingList);
        }


        // Visibility control relying on whether the posting list exists. Refactor required.
        if(isLast && postingList.size() == 0) {
            progbar.setVisibility(View.GONE);
            binding.recyclerBoardPostings.setVisibility(View.GONE);
            binding.tvEmptyView.setVisibility(View.VISIBLE);
        } else {
            binding.recyclerBoardPostings.setVisibility(View.VISIBLE);
            binding.tvEmptyView.setVisibility(View.GONE);
            //binding.recyclerBoardPostings.smoothScrollToPosition(0);
        }

    }

    @Override
    public void onSubmitListDone() {
        log.i("submit done:%s", postingList.size());
        //binding.recyclerBoardPostings.smoothScrollToPosition(0);
        postingAdapter.notifyItemRangeChanged(0, postingList.size(), "indexing");
        int index = 0;
        for(DocumentSnapshot snapshot : postingList) {
            if(snapshot.get("post_images") != null) {
                postingAdapter.notifyItemChanged(index, "images");
            }
            index++;
        }

    }

    public void resetAutoFilter(ArrayList<String> autofilter) {
        if(!menu.getItem(0).isVisible()) requireActivity().invalidateOptionsMenu();
        this.autofilter = autofilter;
        isScrollable = false;
        String field = (isViewOrder) ? "cnt_view" : "timestamp";
        queryPagingUtil.setAutofilterQuery(field);
    }

    private ObjectAnimator rotateAutoEmblem(MenuItem item) {
        isViewOrder = !isViewOrder;
        String label = (isViewOrder)? getString(R.string.board_autoclub_sort_view)
                : getString(R.string.board_autoclub_sort_time);
        TextView tvSorting = item.getActionView().findViewById(R.id.tv_sorting_order);
        tvSorting.setText(label);

        ObjectAnimator rotation = ObjectAnimator.ofFloat(item.getActionView(), "rotationY", 0.0f, 360f);
        rotation.setDuration(500);
        rotation.setInterpolator(new AccelerateDecelerateInterpolator());
        // Use AnimatorListenerAdapter to take callback methods seletively.
        // Seems not work with Android API 24(Android6.0
        rotation.addListener(new AnimatorListenerAdapter(){
            @Override
            public void onAnimationEnd(Animator animation, boolean isReverse) {
                //currentPage = AUTOCLUB;
                resetAutoFilter(autofilter);
                rotation.cancel();
            }
        });

        return rotation;
    }

    // RecyclerView.OnScrollListener is an abstract class to receive messages when a scrolling event
    // has occurred on that RecyclerView, which has 2 abstract methods of onScrollStateChanged() and
    // onScrolled(); the former is to be invoked when RecyclerView's scroll state changes and the
    // latter invoked when the RecyclerView has been scrolled.
    //private RecyclerView.OnScrollListener setRecyclerViewScrollListener() {
    private final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener(){
        /* Callback to be invoked when the RecyclerView has been scrolled, which will be called
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
            LinearLayoutManager layout = (LinearLayoutManager)recyclerView.getLayoutManager();
            if (layout != null) {
                int firstVisibleProductPosition = layout.findFirstVisibleItemPosition();
                int visiblePostCount = layout.getChildCount();
                int totalPostCount = layout.getItemCount();
                if (isScrollable && (firstVisibleProductPosition + visiblePostCount == totalPostCount)) {
                    isScrollable = false;
                    if(currentPage != AUTOCLUB && totalPostCount >= PAGINATION) {
                        queryPagingUtil.setNextPostQuery();
                    }
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
    private void addViewCount(DocumentReference docref, int pos) {
        try(FileInputStream fis = Objects.requireNonNull(requireActivity()).openFileInput("userId");
            BufferedReader br = new BufferedReader(new InputStreamReader(fis))) {
            final String viewerId = br.readLine();

            //CollectionReference subCollection = docref.collection("viewers");
            DocumentReference viewerRef = docref.collection("viewers").document(viewerId);
            mDB.runTransaction(transaction -> {
                DocumentSnapshot viewershot = transaction.get(viewerRef);
                if(!viewershot.exists()) {
                    docref.update("cnt_view", FieldValue.increment(1));
                    Map<String, Object> data = new HashMap<>();
                    data.put("timestamp", FieldValue.serverTimestamp());
                    data.put("viewer_ip", "192.0.0.255"); // code for getting the viewer id required.
                    docref.collection("viewers").document(viewerId).set(data, SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                docref.get().addOnSuccessListener(doc ->
                                    postingAdapter.notifyItemChanged(pos, doc.getLong("cnt_view"))
                                );
                            }).addOnFailureListener(Throwable::printStackTrace);
                }
                return null;
            });
        } catch(IOException e) { e.printStackTrace();}
    }

    // Attemp to retrieve the emblem uri from Firestore only when an auto maker is provided. For this
    // reason, the method should be placed at the end of createAutoFilterCheckBox() which receives
    // auto data as json type.
    private void setAutoMakerEmblem(ProgressBar pb, ImageView imgview) {
        // Make the progressbar visible until getting the emblem from Firetore
        pb.setVisibility(View.VISIBLE);
        mDB.collection("autodata").document(automaker).get().addOnSuccessListener(doc -> {
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
    }


    /*
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
     */
}


