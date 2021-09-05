package com.silverback.carman.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman.R;
import com.silverback.carman.databinding.MainContentAdsBinding;
import com.silverback.carman.databinding.MainContentCarlifeBinding;
import com.silverback.carman.databinding.MainContentExpenseBinding;
import com.silverback.carman.databinding.MainContentFooterBinding;
import com.silverback.carman.databinding.MainContentNotificationBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

public class MainContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(MainContentAdapter.class);

    // Define View Types
    private final int NOTIFICATION = 0;
    private final int BANNER_AD_1 = 1;
    private final int VIEWPAGER_EXPENSE = 2;
    private final int CARLIFE = 3;
    private final int BANNER_AD_2 = 4;
    private final int COMPANY_INFO = 5;

    // Objects
    private final FirebaseFirestore firestore;
    private MainContentNotificationBinding notiBinding;
    private MainContentExpenseBinding expBinding;
    private MainContentAdsBinding adsBinding;
    private MainContentCarlifeBinding carlifeBinding;
    private final MainExpPagerAdapter expensePagerAdapter;

    // Constructor
    public MainContentAdapter(Context context) {
        super();
        firestore= FirebaseFirestore.getInstance();
        expensePagerAdapter  = new MainExpPagerAdapter((FragmentActivity)context);
    }

    //public MainContentNotificationBinding binding; //DataBiding in JetPack
    public static class ContentViewHolder extends RecyclerView.ViewHolder {
        public ContentViewHolder(View itemView) {
            super(itemView);
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(itemView.getLayoutParams());
            params.setMargins(0, 0, 0, Constants.DIVIDER_HEIGHT_MAIN);
            itemView.setLayoutParams(params);
            // Define click listener for the ViewHolder's View.
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch(viewType) {
            case NOTIFICATION:
                notiBinding = MainContentNotificationBinding.inflate(inflater, parent, false);
                return new ContentViewHolder(notiBinding.getRoot());

            case VIEWPAGER_EXPENSE:
                expBinding = MainContentExpenseBinding.inflate(inflater, parent, false);
                return new ContentViewHolder(expBinding.getRoot());

            case CARLIFE:
                carlifeBinding = MainContentCarlifeBinding.inflate(inflater, parent, false);
                return new ContentViewHolder(carlifeBinding.getRoot());

            case BANNER_AD_1: case BANNER_AD_2:
                adsBinding = MainContentAdsBinding.inflate(inflater, parent, false);
                return new ContentViewHolder(adsBinding.getRoot());

            case COMPANY_INFO:
                MainContentFooterBinding footerBinding = MainContentFooterBinding.inflate(inflater, parent, false);
                return new ContentViewHolder(footerBinding.getRoot());

            //default: return new ContentViewHolder(null);
            default: return new ContentViewHolder(notiBinding.getRoot());
        }
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch(position) {
            case NOTIFICATION:
                firestore.collection("admin_post").orderBy("timestamp", Query.Direction.DESCENDING).limit(3)
                        .get()
                        .addOnSuccessListener(querySnapshots -> {
                            RecentPostAdapter recentPostAdapter = new RecentPostAdapter(querySnapshots);
                            notiBinding.recyclerview.setAdapter(recentPostAdapter);
                        });
                break;

            case VIEWPAGER_EXPENSE:
                expBinding.mainPagerExpense.setAdapter(expensePagerAdapter);
                break;

            case CARLIFE:
                firestore.collection("board_general").orderBy("timestamp", Query.Direction.DESCENDING).limit(3)
                        .get()
                        .addOnSuccessListener(querySnapshots -> {
                            RecentPostAdapter carlifeAdapter = new RecentPostAdapter(querySnapshots);
                            carlifeBinding.recyclerCarlife.setAdapter(carlifeAdapter);
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
            case 2: return VIEWPAGER_EXPENSE;
            case 3: return CARLIFE;
            case 4: return BANNER_AD_2;
            case 5: return COMPANY_INFO;
            default: return -1;
        }
    }


}