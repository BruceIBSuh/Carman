package com.silverback.carman2.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.silverback.carman2.R;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.models.OpinetViewModel;
import com.silverback.carman2.threads.FavoritePriceTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.views.OpinetSidoPriceView;
import com.silverback.carman2.views.OpinetSigunPriceView;
import com.silverback.carman2.views.OpinetStationPriceView;

public class PricePagerFragment extends Fragment {

    private static final LoggingHelper log = LoggingHelperFactory.create(PricePagerFragment.class);

    // Constants
    private static final int DISTRICT_PRICE = 0;
    private static final int STATION_PRICE = 1;

    // Objects
    private CarmanDatabase mDB;
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
        mDB = CarmanDatabase.getDatabaseInstance(getContext());
        fragmentModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);
        opinetModel = ViewModelProviders.of(getActivity()).get(OpinetViewModel.class);

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
                    log.i("First placeholder: %s", stnId);
                    if(stnId != null) {
                        favPriceTask = ThreadManager.startFavoritePriceTask(getContext(), null, stnId, true);
                    } else {
                        stnPriceView.removePriceView();
                    }

                });

                opinetModel.favoritePriceComplete().observe(getViewLifecycleOwner(), isDone -> {
                    stnPriceView.addPriceView(fuelCode);
                });

                return secondPage;
        }


        return null;
    }

}
