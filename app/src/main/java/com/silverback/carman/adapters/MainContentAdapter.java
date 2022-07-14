package com.silverback.carman.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman.MainActivity;
import com.silverback.carman.R;
import com.silverback.carman.databinding.MainContentAdsBinding;
import com.silverback.carman.databinding.MainContentCarlifeBinding;
import com.silverback.carman.databinding.MainContentExpenseBinding;
import com.silverback.carman.databinding.MainContentFooterBinding;
import com.silverback.carman.databinding.MainContentNotificationBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.Constants;

import java.util.List;

public class MainContentAdapter extends RecyclerView.Adapter<MainContentAdapter.ViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(MainContentAdapter.class);

    // Main content components which is displayed in RecyclerView based on Enum, which makes add or
    // remove a component enabled in MainActivity.
    public enum ContentType {
        NOTIFICATION,
        BANNER_AD_1,
        VIEWPAGER_EXPENSE,
        CARLIFE,
        BANNDER_AD_2,
        COMPANY_INFO
    }

    // Objects
    private final MainContentAdapterListener mListener;
    private final FirebaseFirestore firestore;
    private MainContentNotificationBinding notiBinding;
    private MainContentExpenseBinding expBinding;
    private MainContentAdsBinding adsBinding;
    private MainContentCarlifeBinding carlifeBinding;
    private final MainExpensePagerAdapter mainExpPagerAdapter;

    // Interface to notify the parent activity of any event
    public interface MainContentAdapterListener {
        void onClickPostingIcon(int category);
    }

    // Constructor
    public MainContentAdapter(FragmentActivity fa, MainContentAdapterListener listener) {
        super();
        this.mListener = listener;
        firestore = FirebaseFirestore.getInstance();
        mainExpPagerAdapter = new MainExpensePagerAdapter(fa.getSupportFragmentManager(), fa.getLifecycle());
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(itemView.getLayoutParams());
            params.setMargins(0, 0, 0, Constants.DIVIDER_HEIGHT_MAIN);
            itemView.setLayoutParams(params);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ContentType contentType = ContentType.values()[viewType];

        //switch(viewType) {
        switch(contentType) {
            case NOTIFICATION:
                notiBinding = MainContentNotificationBinding.inflate(inflater, parent, false);
                notiBinding.imgbtnNotification.setOnClickListener(view ->
                    mListener.onClickPostingIcon(contentType.ordinal())
                );
                return new ViewHolder(notiBinding.getRoot());

            case VIEWPAGER_EXPENSE:
                expBinding = MainContentExpenseBinding.inflate(inflater, parent, false);
                return new ViewHolder(expBinding.getRoot());

            case CARLIFE:
                carlifeBinding = MainContentCarlifeBinding.inflate(inflater, parent, false);
                carlifeBinding.imgbtnCarlife.setOnClickListener(view ->
                    mListener.onClickPostingIcon(contentType.ordinal())
                );
                return new ViewHolder(carlifeBinding.getRoot());

            case BANNER_AD_1: case BANNDER_AD_2:
                adsBinding = MainContentAdsBinding.inflate(inflater, parent, false);
                return new ViewHolder(adsBinding.getRoot());

            case COMPANY_INFO:
                MainContentFooterBinding footerBinding = MainContentFooterBinding.inflate(inflater, parent, false);
                return new ViewHolder(footerBinding.getRoot());

            default: return new ViewHolder(notiBinding.getRoot());
        }
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContentType contentType = ContentType.values()[position];
        switch(contentType) {
            case NOTIFICATION:
                firestore.collection("admin_post").orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(3)
                        .addSnapshotListener((querySanpshots, e) -> {
                            if(e != null || querySanpshots == null) return;
                            RecentPostAdapter adapter = new RecentPostAdapter(querySanpshots);
                            notiBinding.recyclerview.setAdapter(adapter);
                        });
                break;

            case VIEWPAGER_EXPENSE:
                expBinding.mainPagerExpense.setAdapter(mainExpPagerAdapter);
                break;

            case CARLIFE:
                firestore.collection("board_general").orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(3)
                        .addSnapshotListener((querySnapshots, e) -> {
                            if(e != null || querySnapshots == null) return;
                            RecentPostAdapter adapter = new RecentPostAdapter(querySnapshots);
                            carlifeBinding.recyclerCarlife.setAdapter(adapter);
                        });
                break;

            case BANNER_AD_1:
                adsBinding.imgviewAd.setImageResource(R.drawable.ad_ioniq5);
                break;

            case BANNDER_AD_2:
                adsBinding.imgviewAd.setImageResource(R.drawable.ad_insurance);
                break;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            if(position ==ContentType.values()[position].ordinal()) {
                if((Integer)payloads.get(0) > 0) {
                    expBinding.mainPagerExpense.setAdapter(mainExpPagerAdapter);
                    //final int total = (Integer)obj;
                    //mainExpPagerAdapter.notifyItemRangeChanged(0, mainExpPagerAdapter.getItemCount(), total);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return ContentType.values().length;
    }

    @Override
    public int getItemViewType(int position) {
        return ContentType.values()[position].ordinal();
        /*
        switch(position) {
            case 0: return NOTIFICATION;
            case 1: return BANNER_AD_1;
            case 2: return VIEWPAGER_EXPENSE;
            case 3: return CARLIFE;
            case 4: return BANNER_AD_2;
            case 5: return COMPANY_INFO;
            default: return -1;
        }

         */
    }
}