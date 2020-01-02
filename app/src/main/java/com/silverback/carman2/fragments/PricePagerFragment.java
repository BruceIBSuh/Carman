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
import com.silverback.carman2.models.OpinetViewModel;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.views.OpinetSidoPriceView;
import com.silverback.carman2.views.OpinetSigunPriceView;
import com.silverback.carman2.views.OpinetStationPriceView;

public class PricePagerFragment extends Fragment {

    // Constants
    private static final LoggingHelper log = LoggingHelperFactory.create(PricePagerFragment.class);

    // Objects
    private CarmanDatabase mDB;
    //private OpinetViewModel opinetModel;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDB = CarmanDatabase.getDatabaseInstance(getContext());
        //opinetModel = ViewModelProviders.of(this).get(OpinetViewModel.class);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedStateInstance) {

        if(getArguments() != null) {
            page = getArguments().getInt("page");
            fuelCode = getArguments().getString("fuelCode");
        }

        final int DISTRICT_PRICE = 0;
        final int STATION_PRICE = 1;

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

                // Check if any favorite gas station has registered. The first registered station,
                // if any, stores its name and price in the internal cache directory.
                mDB.favoriteModel().firstFavRegLiveData(Constants.GAS)
                        .observe(getViewLifecycleOwner(), count -> {
                            if( count == 0) {
                                log.i("First set favorite void");
                                stnPriceView.removePriceView();
                            } else {
                                stnPriceView.addPriceView(fuelCode);
                            }

                        });

                return secondPage;
        }


        return null;
    }

}
