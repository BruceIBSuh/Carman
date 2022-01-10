package com.silverback.carman.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.silverback.carman.BoardActivity;
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

public class MainContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final LoggingHelper log = LoggingHelperFactory.create(MainContentAdapter.class);

    // Objects
    private final MainContentAdapterListener mListener;
    private final FirebaseFirestore firestore;
    private MainContentNotificationBinding notiBinding;
    private MainContentExpenseBinding expBinding;
    private MainContentAdsBinding adsBinding;
    private MainContentCarlifeBinding carlifeBinding;
    private final MainExpPagerAdapter expensePagerAdapter;

    //private final FragmentSharedModel fragmentModel;

    // Interface to notify the parent activity of any event
    public interface MainContentAdapterListener {
        void onClickBoard(int category);
    }

    // Constructor
    public MainContentAdapter(Context context, MainContentAdapterListener listener) {
        super();
        this.mListener = listener;
        firestore= FirebaseFirestore.getInstance();
        expensePagerAdapter = new MainExpPagerAdapter((FragmentActivity)context);
        //fragmentModel = new ViewModelProvider((FragmentActivity)context).get(FragmentSharedModel.class);
    }

    //public MainContentNotificationBinding binding; //DataBiding in JetPack
    private static class ContentViewHolder extends RecyclerView.ViewHolder {
        public ContentViewHolder(View itemView) {
            super(itemView);
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(itemView.getLayoutParams());
            params.setMargins(0, 0, 0, Constants.DIVIDER_HEIGHT_MAIN);
            itemView.setLayoutParams(params);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch(viewType) {
            case Constants.NOTIFICATION:
                notiBinding = MainContentNotificationBinding.inflate(inflater, parent, false);
                notiBinding.imgbtnNotification.setOnClickListener(view -> {
                    mListener.onClickBoard(Constants.BOARD_NOTIFICATION);
                });
                return new ContentViewHolder(notiBinding.getRoot());

            case Constants.VIEWPAGER_EXPENSE:
                expBinding = MainContentExpenseBinding.inflate(inflater, parent, false);
                return new ContentViewHolder(expBinding.getRoot());

            case Constants.CARLIFE:
                carlifeBinding = MainContentCarlifeBinding.inflate(inflater, parent, false);
                carlifeBinding.imgbtnCarlife.setOnClickListener(view -> {
                    mListener.onClickBoard(Constants.BOARD_RECENT);
                });
                return new ContentViewHolder(carlifeBinding.getRoot());

            case Constants.BANNER_AD_1: case Constants.BANNER_AD_2:
                adsBinding = MainContentAdsBinding.inflate(inflater, parent, false);
                return new ContentViewHolder(adsBinding.getRoot());

            case Constants.COMPANY_INFO:
                MainContentFooterBinding footerBinding = MainContentFooterBinding.inflate(inflater, parent, false);
                return new ContentViewHolder(footerBinding.getRoot());

            //default: return new ContentViewHolder(null);
            default: return new ContentViewHolder(notiBinding.getRoot());
        }
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch(position) {
            case Constants.NOTIFICATION:
                firestore.collection("admin_post").orderBy("timestamp", Query.Direction.DESCENDING).limit(3)
                        .addSnapshotListener((querySanpshots, e) -> {
                            if(e != null || querySanpshots == null) return;
                            RecentPostAdapter adapter = new RecentPostAdapter(querySanpshots);
                            notiBinding.recyclerview.setAdapter(adapter);
                        });
                /*
                firestore.collection("admin_post").orderBy("timestamp", Query.Direction.DESCENDING).limit(3)
                        .get()
                        .addOnSuccessListener(querySnapshots -> {
                            RecentPostAdapter recentPostAdapter = new RecentPostAdapter(querySnapshots);
                            notiBinding.recyclerview.setAdapter(recentPostAdapter);
                        });
                 */
                break;

            case Constants.VIEWPAGER_EXPENSE:
                expBinding.mainPagerExpense.setAdapter(expensePagerAdapter);
                break;

            case Constants.CARLIFE:
                firestore.collection("board_general").orderBy("timestamp", Query.Direction.DESCENDING).limit(3)
                        .addSnapshotListener((querySnapshots, e) -> {
                            if(e != null || querySnapshots == null) return;
                            RecentPostAdapter adapter = new RecentPostAdapter(querySnapshots);
                            carlifeBinding.recyclerCarlife.setAdapter(adapter);

                        });
                /*
                firestore.collection("board_general").orderBy("timestamp", Query.Direction.DESCENDING).limit(3)
                        .get()
                        .addOnSuccessListener(querySnapshots -> {
                            RecentPostAdapter carlifeAdapter = new RecentPostAdapter(querySnapshots);
                            carlifeBinding.recyclerCarlife.setAdapter(carlifeAdapter);
                        });
                 */
                break;

            case Constants.BANNER_AD_1:
                adsBinding.imgviewAd.setImageResource(R.drawable.ad_ioniq5);
                break;

            case Constants.BANNER_AD_2:
                adsBinding.imgviewAd.setImageResource(R.drawable.ad_insurance);
                break;
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {

        /*
        switch(position){
            case Constants.NOTIFICATION:
                break;
            case Constants.VIEWPAGER_EXPENSE:
                log.i("new data come in:%s", payloads);
                //expBinding.mainPagerExpense.setAdapter(expensePagerAdapter);
                //expensePagerAdapter.notifyDataSetChanged();
                expensePagerAdapter.notifyItemChanged(0, payloads);
                break;
            default: super.onBindViewHolder(holder, position, payloads);
        }

         */


        if(payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            log.i("payloads:%s", payloads.get(0));
            if(position == Constants.VIEWPAGER_EXPENSE) {
                if(payloads.get(0).equals(0)) {
                    log.i("return to the default");
                    //expBinding.mainPagerExpense.setCurrentItem(0, true);
                    super.onBindViewHolder(holder, position, payloads);
                } else {
                    log.i("new data come in:%s, %s", expensePagerAdapter.getItemCount(), payloads);
                    expBinding.mainPagerExpense.setAdapter(expensePagerAdapter);
                    //expensePagerAdapter.notifyDataSetChanged();
                    //expensePagerAdapter.notifyItemRangeChanged(0, expensePagerAdapter.getItemCount(), payloads);

                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return 6;
    }

    @Override
    public int getItemViewType(int position) {
        switch(position) {
            case 0: return Constants.NOTIFICATION;
            case 1: return Constants.BANNER_AD_1;
            case 2: return Constants.VIEWPAGER_EXPENSE;
            case 3: return Constants.CARLIFE;
            case 4: return Constants.BANNER_AD_2;
            case 5: return Constants.COMPANY_INFO;
            default: return -1;
        }
    }
}