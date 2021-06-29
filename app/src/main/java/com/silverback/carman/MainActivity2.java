package com.silverback.carman;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman.adapters.MainContentAdapter;
import com.silverback.carman.adapters.PricePagerAdapter;
import com.silverback.carman.adapters.StationListAdapter;
import com.silverback.carman.fragments.MainScreenFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.LocationTask;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.StationListViewModel;
import com.silverback.carman.views.OpinetAvgPriceView;
import com.silverback.carman.views.StationRecyclerView;

import java.util.List;
import java.util.Objects;

public class MainActivity2 extends BaseActivity implements
        StationListAdapter.OnRecyclerItemClickListener,
        AdapterView.OnItemSelectedListener {

    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity2.class);

    // Objects
    private RecyclerView contentRecyclerView;
    private StationRecyclerView stationRecyclerView;

    private OpinetAvgPriceView opinetAvgPriceView;

    private LocationViewModel locationModel;
    private StationListViewModel stnModel;

    private LocationTask locationTask;
    private Location mPrevLocation;

    private List<Opinet.GasStnParcelable> mStationList;
    private StationListAdapter mAdapter;

    private String[] defaults;
    private String defaultFuel;

    private boolean isStationOn = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        AppBarLayout appbar = findViewById(R.id.appbar);
        View priceView = findViewById(R.id.view_price_info);

        View mainLayout = findViewById(R.id.main_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        stationRecyclerView = findViewById(R.id.stationRecyclerView);
        contentRecyclerView = findViewById(R.id.recycler_contents);
        OpinetAvgPriceView avgPrice = findViewById(R.id.avgPriceView);
        ImageButton btnStation = findViewById(R.id.imgbtn_station);
        ImageButton btnSvc = findViewById(R.id.imgbtn_svc);
        Spinner fuelSpinner = findViewById(R.id.spinner_gas);
        ViewPager2 pricePager = findViewById(R.id.viewpager_price);

        // Set the toolbar
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(false);

        appbar.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                log.i("toolbar size:%s", getActionbarHeight());
                log.i("appbar offset:%s, %s", verticalOffset, appbar.getTotalScrollRange());
                if(Math.abs(verticalOffset) == appbar.getTotalScrollRange()) {
                    priceView.setVisibility(View.VISIBLE);
                } else priceView.setVisibility(View.GONE);

            }
        });

        // Price info
        avgPrice.addPriceView("B027");

        // Set the spinner w/ fuel lists
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this, R.array.spinner_fuel_name, R.layout.spinner_main_fuel);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_main_dropdown);
        fuelSpinner.setAdapter(spinnerAdapter);
        fuelSpinner.setOnItemSelectedListener(this);

        // ViewPager2
        PricePagerAdapter pricePagerAdapter = new PricePagerAdapter(this);
        pricePagerAdapter.setFuelCode("B027");
        pricePager.setAdapter(pricePagerAdapter);

        // MainContent RecyclerView
        MainContentAdapter adapter = new MainContentAdapter();
        MainContentAdapter.MainItemDecoration itemDeco = new MainContentAdapter.MainItemDecoration(50, 0);
        contentRecyclerView.setAdapter(adapter);
        contentRecyclerView.addItemDecoration(itemDeco);

        // ViewModels
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
        stnModel = new ViewModelProvider(this).get(StationListViewModel.class);

        btnStation.setOnClickListener(view -> {
            if(!isStationOn) locationTask = ThreadManager2.fetchLocationTask(this, locationModel);
            else {
                stationRecyclerView.setVisibility(View.GONE);
                contentRecyclerView.setVisibility(View.VISIBLE);
                isStationOn = !isStationOn;
            }
        });


        locationModel.getLocation().observe(this, location -> {
            if(location == null) return;
            if(mPrevLocation == null || (mPrevLocation.distanceTo(location) > Constants.UPDATE_DISTANCE)) {
                log.i("location fetched: %s", location);
                mPrevLocation = location;
                ThreadManager2.startStationListTask(stnModel, location, getDefaultParams());
            } else {
                contentRecyclerView.setVisibility(View.GONE);
                stationRecyclerView.setVisibility(View.VISIBLE);
                isStationOn = !isStationOn;
                Snackbar.make(mainLayout, getString(R.string.general_snackkbar_inbounds), Snackbar.LENGTH_SHORT).show();
            }
        });

        // Location has failed to fetch.
        locationModel.getLocationException().observe(this, exception -> {
            log.i("Exception occurred while fetching location");
            SpannableString spannableString = new SpannableString(getString(R.string.general_no_location));
            stationRecyclerView.showTextView(spannableString);

        });

        // Receive station(s) within the radius. If no stations exist, post the message that
        // indicate why it failed to fetch stations. It would be caused by any network problem or
        // no stations actually exist within the radius.
        stnModel.getNearStationList().observe(this, stnList -> {
            if (stnList != null && stnList.size() > 0) {
                mStationList = stnList;
                mAdapter = new StationListAdapter(mStationList, this);
                //stationRecyclerView.showStationListRecyclerView();
                stationRecyclerView.setAdapter(mAdapter);

                contentRecyclerView.setVisibility(View.GONE);
                stationRecyclerView.setVisibility(View.VISIBLE);

                isStationOn = !isStationOn;


            } else {
                log.i("no station");
                // No near stations post an message that contains the clickable span to link to the
                // SettingPreferenceActivity for resetting the searching radius.
                //SpannableString spannableString = handleStationListException();
                //stationRecyclerView.showTextView(spannableString);
            }
        });

        // Update the carwash info to StationList and notify the data change to Adapter.
        // Adapter should not assume that the payload will always be passed to onBindViewHolder()
        // e.g. when the view is not attached.
        stnModel.getStationCarWashInfo().observe(this, sparseArray -> {
            for(int i = 0; i < sparseArray.size(); i++) {
                mStationList.get(i).setIsWash(sparseArray.valueAt(i));
                mAdapter.notifyItemChanged(sparseArray.keyAt(i), sparseArray.valueAt(i));
            }
        });

    }

    @Override
    public void onStop() {
        super.onStop();
        if(locationTask != null) locationTask = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_options_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressWarnings("all")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.action_garage:
                startActivity(new Intent(this, ExpenseActivity.class));
                return true;

            case R.id.action_board:
                startActivity(new Intent(this, BoardActivity.class));
                return true;

            case R.id.action_login:
                return true;

            case R.id.action_setting:
                // Apply startActivityForresult() to take the price data and the username back from
                // SettingPreferenceActivity to onActivityResult() if the values have changed.
                Intent settingIntent = new Intent(this, SettingPrefActivity.class);
                int requestCode = Constants.REQUEST_MAIN_SETTING_GENERAL;
                settingIntent.putExtra("requestCode", requestCode);
                startActivityForResult(settingIntent, requestCode);
                return true;
            default: return false;
        }
    }

    @Override
    public void onItemClicked(int pos) {

    }

    @Override
    public void onBackPressed() {

    }

    // The following 2 methods are the fuel spinner event handler.
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    // Ref: expand the station recyclerview up to wrap_content

}

