package com.silverback.carman.adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman.R;
import com.silverback.carman.databinding.MainContentAdsBinding;
import com.silverback.carman.databinding.MainContentExpenseGasBinding;
import com.silverback.carman.databinding.MainContentExpenseSvcBinding;
import com.silverback.carman.databinding.MainContentNotificationBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class MainContentAdapter extends RecyclerView.Adapter<MainContentAdapter.ViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(MainContentAdapter.class);

    // Objects
    private MainContentNotificationBinding newsBinding;
    private MainContentExpenseGasBinding gasBinding;
    private MainContentExpenseSvcBinding svcBinding;
    private MainContentAdsBinding adsBinding;


    // Constructor
    public MainContentAdapter() {
        super();
    }

    //public MainContentNotificationBinding binding; //DataBiding in JetPack
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View.
        }
    }

    public static class MainItemDecoration extends RecyclerView.ItemDecoration {
        private final int margin;
        private final int columns;
        private final DividerItemDecoration divider;

        public MainItemDecoration(Context context, int margin, int columns) {
            this.margin = margin;
            this.columns = columns;
            divider = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
        }

        @Override
        public void getItemOffsets(
                @NonNull Rect outRect, @NonNull View view,
                @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            //super.getItemOffsets(outRect, view, parent, state);
            int position = parent.getChildLayoutPosition(view);
            outRect.bottom = margin;
            if(position < columns) outRect.top = margin;
        }

        @Override
        public void onDrawOver(
                @NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            for(int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                ViewGroup.LayoutParams params = parent.getLayoutParams();
                log.i("onDrawOver: %s, %s:", child, params);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        switch(viewType) {
            case 0:
                newsBinding = MainContentNotificationBinding.inflate(inflater, viewGroup, false);
                return new ViewHolder(newsBinding.getRoot());

            case 2:
                gasBinding = MainContentExpenseGasBinding.inflate(inflater);
                return new ViewHolder(gasBinding.getRoot());
            case 3:
                svcBinding = MainContentExpenseSvcBinding.inflate(inflater);
                return new ViewHolder(svcBinding.getRoot());

            case 1: case 4:
                adsBinding = MainContentAdsBinding.inflate(inflater, viewGroup, false);
                return new ViewHolder(adsBinding.getRoot());

            default: return new ViewHolder(null);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        switch(position) {
            case 0:
                FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                firestore.collection("admin_post").orderBy("timestamp", Query.Direction.DESCENDING).limit(5)
                        .get()
                        .addOnSuccessListener(querySnapshots -> {
                            RecentPostAdapter recentPostAdapter = new RecentPostAdapter(querySnapshots);
                            newsBinding.recyclerview.setAdapter(recentPostAdapter);
                        });

                break;

            case 1:
                adsBinding.imgviewAd.setImageResource(R.drawable.ad_ioniq5);
                break;
            case 4:
                adsBinding.imgviewAd.setImageResource(R.drawable.ad_insurance);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }
}