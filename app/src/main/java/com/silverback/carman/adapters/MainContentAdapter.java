package com.silverback.carman.adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman.R;
import com.silverback.carman.databinding.MainContentAdsBinding;
import com.silverback.carman.databinding.MainContentExpenseGasBinding;
import com.silverback.carman.databinding.MainContentExpenseSvcBinding;
import com.silverback.carman.databinding.MainContentFooterBinding;
import com.silverback.carman.databinding.MainContentNotificationBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

public class MainContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(MainContentAdapter.class);

    // Define View Types
    private final int NOTIFICATION = 0;
    private final int BANNER_AD_1 = 1;
    private final int EXPENSE_GAS = 2;
    private final int EXPENSE_SVC = 3;
    private final int BANNER_AD_2 = 4;
    private final int COMPANY_INFO = 5;

    // Objects
    private MainContentNotificationBinding newsBinding;
    private MainContentAdsBinding adsBinding;


    // Constructor
    public MainContentAdapter() {
        super();
    }

    //public MainContentNotificationBinding binding; //DataBiding in JetPack
    public static class ContentViewHolder extends RecyclerView.ViewHolder {
        public ContentViewHolder(View itemView) {
            super(itemView);
            // Define click listener for the ViewHolder's View.
        }
    }

    /*
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
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                ViewGroup.LayoutParams params = parent.getLayoutParams();
                log.i("onDrawOver: %s, %s:", child, params);
            }
        }
    }

     */

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        switch(viewType) {
            case NOTIFICATION:
                newsBinding = MainContentNotificationBinding.inflate(inflater, viewGroup, false);
                return new ContentViewHolder(newsBinding.getRoot());

            case EXPENSE_GAS:
                MainContentExpenseGasBinding gasBinding = MainContentExpenseGasBinding.inflate(inflater);
                return new ContentViewHolder(gasBinding.getRoot());

            case EXPENSE_SVC:
                MainContentExpenseSvcBinding svcBinding = MainContentExpenseSvcBinding.inflate(inflater);
                return new ContentViewHolder(svcBinding.getRoot());

            case BANNER_AD_1: case BANNER_AD_2:
                adsBinding = MainContentAdsBinding.inflate(inflater, viewGroup, false);
                return new ContentViewHolder(adsBinding.getRoot());

            case COMPANY_INFO:
                MainContentFooterBinding footerBinding = MainContentFooterBinding.inflate(inflater, viewGroup, false);
                return new ContentViewHolder(footerBinding.getRoot());

            default: return new ContentViewHolder(null);
        }
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch(position) {
            case NOTIFICATION:
                FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                firestore.collection("admin_post").orderBy("timestamp", Query.Direction.DESCENDING).limit(5)
                        .get()
                        .addOnSuccessListener(querySnapshots -> {
                            RecentPostAdapter recentPostAdapter = new RecentPostAdapter(querySnapshots);
                            newsBinding.recyclerview.setAdapter(recentPostAdapter);
                        });

                break;

            case BANNER_AD_1:
                adsBinding.imgviewAd.setImageResource(R.drawable.ad_ioniq5);
                break;

            case BANNER_AD_2:
                adsBinding.imgviewAd.setImageResource(R.drawable.ad_insurance);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return 6;
    }

    @Override
    public int getItemViewType(int position) {
        switch(position) {
            case 0: return NOTIFICATION;
            case 1: return BANNER_AD_1;
            case 2: return EXPENSE_GAS;
            case 3: return EXPENSE_SVC;
            case 4: return BANNER_AD_2;
            case 5: return COMPANY_INFO;
            default: return -1;
        }
    }
}