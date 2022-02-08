package com.silverback.carman.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman.R;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.databinding.MainPagerDistrictPriceBinding;
import com.silverback.carman.databinding.MainPagerStationPriceBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.FavoritePriceTask;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.OpinetViewModel;
import com.silverback.carman.views.OpinetStationPriceView;

/**
 * This fragment is to display the gas prices of the district and the favorite station.
 */
public class MainPricePagerFragment extends Fragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(MainPricePagerFragment.class);

    // Constants
    private static final int DISTRICT_PRICE = 0;
    private static final int STATION_PRICE = 1;

    private CarmanDatabase mDB;
    private MainPagerDistrictPriceBinding distBinding;
    private MainPagerStationPriceBinding stnBinding;
    private FavoritePriceTask favPriceTask;
    private OpinetViewModel opinetModel;
    private FragmentSharedModel fragmentModel;
    private int page;
    private String fuelCode;


    // Constructor
    private MainPricePagerFragment() {
        // Default private construcotr leaving empty.
    }

    // ViewPager fragment should instantiate multiple MainPricePagerFragment which depends on
    // how many page the viewpager contains.
    public static MainPricePagerFragment getInstance(String fuelCode, int page) {
        MainPricePagerFragment fragment = new MainPricePagerFragment();
        Bundle args = new Bundle();
        args.putInt("page", page);
        args.putString("fuelCode", fuelCode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDB = CarmanDatabase.getDatabaseInstance(getContext());
        fragmentModel = new ViewModelProvider(requireActivity()).get(FragmentSharedModel.class);
        opinetModel = new ViewModelProvider(requireActivity()).get(OpinetViewModel.class);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedStateInstance) {
        log.i("onCreateView of MainPricePagerFragment");
        if(getArguments() != null) {
            page = getArguments().getInt("page");
            fuelCode = getArguments().getString("fuelCode");
        }

        switch(page) {
            case DISTRICT_PRICE:
                log.i("DISTRICT_PRICE");
                distBinding = MainPagerDistrictPriceBinding.inflate(inflater);
                distBinding.sidoPriceView.addPriceView(fuelCode);
                distBinding.sigunPriceView.addPriceView(fuelCode);
                return distBinding.getRoot();

            case STATION_PRICE:
                log.i("STATION_PRICE");
                stnBinding = MainPagerStationPriceBinding.inflate(inflater);
                //stnBinding.stnPriceView.addPriceView(fuelCode);

                mDB.favoriteModel().getFirstFavorite(Constants.GAS).observe(getViewLifecycleOwner(), id -> {
                    if(TextUtils.isEmpty(id)) {
                        stnBinding.stnPriceView.removePriceView("No Favorite Station exists");
                    } else {
                        log.i("FavPriceTask");
                        favPriceTask = ThreadManager2.getFavoriteStationTask(getContext(), opinetModel, id, true);
                        opinetModel.favoritePriceComplete().observe(getViewLifecycleOwner(), isDone -> {
                            log.i("new station price data saved in the file");
                            stnBinding.stnPriceView.addPriceView(fuelCode);
                        });
                    }
                });

                return stnBinding.getRoot();
        }

        return null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        opinetModel.getFavoritePriceData().observe(getViewLifecycleOwner(), isDone -> {
            log.i("new station price data saved in the file");
            stnBinding.stnPriceView.addPriceView(fuelCode);
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if(favPriceTask != null) favPriceTask = null;
    }

    public void reload(int position, String gasCode) {
        if(position == 0 && distBinding != null) {
            log.i("update district view: %s", gasCode);
            distBinding.sidoPriceView.addPriceView(gasCode);
            distBinding.sigunPriceView.addPriceView(gasCode);
        } else if(position == 1 && stnBinding != null) {
            log.i("favorite station update required");
        }
    }

}
