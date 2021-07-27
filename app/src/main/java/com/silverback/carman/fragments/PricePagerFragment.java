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

        if(getArguments() != null) {
            page = getArguments().getInt("page");
            fuelCode = getArguments().getString("fuelCode");
        }

        switch(page) {
            case DISTRICT_PRICE:
                log.i("DISTRICT_PRICE");
                View firstPage = inflater.inflate(R.layout.pager_district_price, container,false);
                OpinetSidoPriceView sidoView = firstPage.findViewById(R.id.sidoPriceView);
                OpinetSigunPriceView sigunView = firstPage.findViewById(R.id.sigunPriceView);

                sidoView.addPriceView(fuelCode);
                sigunView.addPriceView(fuelCode);

                return firstPage;

            case STATION_PRICE:
                View secondPage = inflater.inflate(R.layout.pager_station_price, container, false);
                OpinetStationPriceView stnPriceView = secondPage.findViewById(R.id.stationPriceView);
                stnPriceView.addPriceView(fuelCode);

                fragmentModel.getFirstPlaceholderId().observe(getViewLifecycleOwner(), stnId -> {
                    if(stnId != null) {
                        favPriceTask = ThreadManager.startFavoritePriceTask(getContext(), opinetModel, stnId, true);
                    } else {
                        stnPriceView.removePriceView(getString(R.string.general_opinet_stn_reset));
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
                    stnPriceView.addPriceView(fuelCode);
                });

                return secondPage;
        }


        return null;
    }

}
