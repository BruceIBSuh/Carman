package com.silverback.carman.adapters;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
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
    private final int VIEWPAGER_EXPENSE = 2;
    private final int EXPENSE_SVC = 3;
    private final int BANNER_AD_2 = 4;
    private final int COMPANY_INFO = 5;

    // Objects
    private MainContentNotificationBinding contentBinding;
    private MainContentExpenseGasBinding gasBinding;
    private MainContentAdsBinding adsBinding;
    private final MainExpPagerAdapter expensePagerAdapter;

    // Constructor
    public MainContentAdapter(Context context) {
        super();
        expensePagerAdapter = new MainExpPagerAdapter((FragmentActivity)context);
    }

    //public MainContentNotificationBinding binding; //DataBiding in JetPack
    public static class ContentViewHolder extends RecyclerView.ViewHolder {
        public ContentViewHolder(View itemView) {
            super(itemView);
            // Define click listener for the ViewHolder's View.

        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        switch(viewType) {
            case NOTIFICATION:
                contentBinding = MainContentNotificationBinding.inflate(inflater, viewGroup, false);
                return new ContentViewHolder(contentBinding.getRoot());

            case VIEWPAGER_EXPENSE:
                gasBinding = MainContentExpenseGasBinding.inflate(inflater, viewGroup, false);
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

            //default: return new ContentViewHolder(null);
            default: return new ContentViewHolder(contentBinding.getRoot());
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
                            contentBinding.recyclerview.setAdapter(recentPostAdapter);
                        });
                break;

            case VIEWPAGER_EXPENSE:
                gasBinding.pagerExpense.setAdapter(expensePagerAdapter);
                break;

            case EXPENSE_SVC:
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
            case 2: return VIEWPAGER_EXPENSE;
            case 3: return EXPENSE_SVC;
            case 4: return BANNER_AD_2;
            case 5: return COMPANY_INFO;
            default: return -1;
        }
    }


}