package com.silverback.carman.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.silverback.carman.R;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.databinding.PagerDistrictPriceBinding;
import com.silverback.carman.databinding.PagerStationPriceBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.viewmodels.FragmentSharedModel;
import com.silverback.carman.viewmodels.OpinetViewModel;
import com.silverback.carman.threads.FavoritePriceTask;
import com.silverback.carman.threads.ThreadManager;
import com.silverback.carman.views.OpinetSidoPriceView;
import com.silverback.carman.views.OpinetSigunPriceView;
import com.silverback.carman.views.OpinetStationPriceView;

/**
 * This fragment is to display the gas prices of the favorite district and the favorite station.
 */
public class PricePagerFragment extends Fragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(PricePagerFragment.class);

    // Constants
    private static final int DISTRICT_PRICE = 0;
    private static final int STATION_PRICE = 1;

    private PagerDistrictPriceBinding distBinding;
    private PagerStationPriceBinding stnBinding;
    private FavoritePriceTask favPriceTask;
    private OpinetViewModel opinetModel;
    private FragmentSharedModel fragmentModel;
    private int page;
    private String fuelCode;


    // Constructor
    private PricePagerFragment() {
        // Default private construcotr leaving empty.
    }

    public static PricePagerFragment getInstance(String fuelCode, int position) {
        PricePagerFragment pagerFragment = new PricePagerFragment();
        Bundle args = new Bundle();
        args.putInt("page", position);
        args.putString("fuelCode", fuelCode);
        pagerFragment.setArguments(args);

        return pagerFragment;
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Objects
        //CarmanDatabase mDB = CarmanDatabase.getDatabaseInstance(getContext());
        fragmentModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);
        opinetModel = new ViewModelProvider(getActivity()).get(OpinetViewModel.class);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedStateInstance) {

        log.i("onCreateView of PricePagerFragment");
        if(getArguments() != null) {
            page = getArguments().getInt("page");
            fuelCode = getArguments().getString("fuelCode");
        }

        switch(page) {
            case DISTRICT_PRICE:
                log.i("DISTRICT_PRICE");
                distBinding = PagerDistrictPriceBinding.inflate(inflater);
                distBinding.sidoPriceView.addPriceView(fuelCode);
                distBinding.sigunPriceView.addPriceView(fuelCode);

                return distBinding.getRoot();

            case STATION_PRICE:
                log.i("STATION_PRICE");
                stnBinding = PagerStationPriceBinding.inflate(inflater);
                stnBinding.stnPriceView.addPriceView(fuelCode);

                fragmentModel.getFirstPlaceholderId().observe(getViewLifecycleOwner(), stnId -> {
                    if(stnId != null) {
                        favPriceTask = ThreadManager.startFavoritePriceTask(getContext(), opinetModel, stnId, true);
                    } else {
                        stnBinding.stnPriceView.removePriceView(getString(R.string.general_opinet_stn_reset));
                        /*
                        mDB.favoriteModel().getFirstFavorite(Constants.GAS).observe(getViewLifecycleOwner(), id -> {
                            if(id == null) stnPriceView.removePriceView();
                            else {
                                log.i("The second placeholder should be the first one: %s", id);
                                favPriceTask = ThreadManager.startFavoritePriceTask(getContext(), opinetModel, id, true);
                            }
                        });
                        */
                    }

                });

                opinetModel.favoritePriceComplete().observe(getViewLifecycleOwner(), isDone -> {
                    log.i("favoritePriceComplete() done");
                    stnBinding.stnPriceView.addPriceView(fuelCode);
                });

                return stnBinding.getRoot();
        }


        return null;
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
