package com.silverback.carman;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman.adapters.MainContentAdapter;
import com.silverback.carman.adapters.PricePagerAdapter;
import com.silverback.carman.adapters.StationListAdapter;
import com.silverback.carman.databinding.ActivityMainBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.LocationTask;
import com.silverback.carman.threads.StationListTask;
import com.silverback.carman.threads.ThreadManager;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.ImageViewModel;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.StationListViewModel;
import com.silverback.carman.views.OpinetAvgPriceView;
import com.silverback.carman.views.OpinetSidoPriceView;
import com.silverback.carman.views.OpinetSigunPriceView;
import com.silverback.carman.views.StationRecyclerView;

import java.util.List;
import java.util.Objects;

public class MainActivity extends BaseActivity implements
        StationListAdapter.OnRecyclerItemClickListener,
        AdapterView.OnItemSelectedListener {

    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity.class);

    // Objects
    private ActivityMainBinding binding;

    private LocationViewModel locationModel;
    private StationListViewModel stnModel;
    private ImageViewModel imgModel;

    private LocationTask locationTask;
    private Location mPrevLocation;

    private List<Opinet.GasStnParcelable> mStationList;
    private PricePagerAdapter pricePagerAdapter;
    private StationListAdapter mAdapter;
    private StationListTask stationListTask;

    private ApplyImageResourceUtil imgResUtil;

    // Fields
    private String[] defaults;
    private String defaultFuel;
    private boolean isStationOn = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View rootView = binding.getRoot();
        setContentView(rootView);

        // Set the toolbar
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(false);
        binding.appbar.addOnOffsetChangedListener((appbar, offset) -> showCollapsedPrice(offset));

        // Price info
        setSpinnerToDefaultFuel();
        binding.mainHeader.avgPriceView.addPriceView(defaultFuel);

        // Set the fuel-selecting spinner
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this, R.array.spinner_fuel_name, R.layout.spinner_main_fuel);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_main_dropdown);
        binding.mainHeader.spinnerGas.setAdapter(spinnerAdapter);

        binding.mainHeader.spinnerGas.setOnItemSelectedListener(this);

        // ViewPager2
        pricePagerAdapter = new PricePagerAdapter(this);
        pricePagerAdapter.setFuelCode(defaultFuel);
        binding.mainHeader.viewpagerPrice.setAdapter(pricePagerAdapter);

        // MainContent RecyclerView
        MainContentAdapter adapter = new MainContentAdapter();
        MainContentAdapter.MainItemDecoration itemDeco = new MainContentAdapter.MainItemDecoration(50, 0);
        binding.recyclerContents.setAdapter(adapter);
        binding.recyclerContents.addItemDecoration(itemDeco);

        // ViewModels
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
        stnModel = new ViewModelProvider(this).get(StationListViewModel.class);
        imgModel = new ViewModelProvider(this).get(ImageViewModel.class);
        // Instantiate utils
        imgResUtil = new ApplyImageResourceUtil(this);

        // Event Handlers
        binding.imgbtnStation.setOnClickListener(view -> {
            if(!isStationOn) locationTask = ThreadManager2.fetchLocationTask(this, locationModel);
            else {
                binding.stationRecyclerView.setVisibility(View.GONE);
                binding.recyclerContents.setVisibility(View.VISIBLE);
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
                binding.recyclerContents.setVisibility(View.GONE);
                binding.stationRecyclerView.setVisibility(View.VISIBLE);
                isStationOn = !isStationOn;
                Snackbar.make(rootView, getString(R.string.general_snackkbar_inbounds), Snackbar.LENGTH_SHORT).show();
            }
        });

        // Location has failed to fetch.
        locationModel.getLocationException().observe(this, exception -> {
            log.i("Exception occurred while fetching location");
            SpannableString spannableString = new SpannableString(getString(R.string.general_no_location));
            binding.stationRecyclerView.showTextView(spannableString);

        });

        // Receive station(s) within the radius. If no stations exist, post the message that
        // indicate why it failed to fetch stations. It would be caused by any network problem or
        // no stations actually exist within the radius.
        stnModel.getNearStationList().observe(this, stnList -> {
            if (stnList != null && stnList.size() > 0) {
                mStationList = stnList;
                mAdapter = new StationListAdapter(mStationList, this);
                binding.stationRecyclerView.setAdapter(mAdapter);
                binding.stationRecyclerView.showStationListRecyclerView();

                binding.recyclerContents.setVisibility(View.GONE);
                binding.stationRecyclerView.setVisibility(View.VISIBLE);

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
    public void onResume() {
        super.onResume();
        // Set the user name and customizable icon in the toolbar.
        String title = mSettings.getString(Constants.USER_NAME, null);
        if(title != null) Objects.requireNonNull(getSupportActionBar()).setTitle(title);

        String userImg = mSettings.getString(Constants.USER_IMAGE, null);
        String imgUri = (TextUtils.isEmpty(userImg))?Constants.imgPath + "ic_user_blank_gray" : userImg;
        imgResUtil.applyGlideToDrawable(imgUri, Constants.ICON_SIZE_TOOLBAR_USERPIC, imgModel);
        imgModel.getGlideDrawableTarget().observe(this, resource -> {
            if(getSupportActionBar() != null) getSupportActionBar().setIcon(resource);
        });

        String avgPrice = String.valueOf(binding.mainHeader.avgPriceView.getAvgGasPrice());
        log.i("avgPrice:%s", avgPrice);
        binding.tvCollapsedAvgPrice.setText(avgPrice);

    }

    @Override
    public void onPause() {
        super.onPause();
        if(locationTask != null) locationTask = null;
        if(stationListTask != null) stationListTask = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_options_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //@SuppressWarnings("all")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_garage) {
            startActivity(new Intent(this, ExpenseActivity.class));
        } else if(item.getItemId() == R.id.action_board) {
            startActivity(new Intent(this, BoardActivity.class));
        } else if(item.getItemId() == R.id.action_login) {
            log.i("login process required");
        } else if(item.getItemId() == R.id.action_setting) {
            Intent settingIntent = new Intent(this, SettingPrefActivity.class);
            int requestCode = Constants.REQUEST_MAIN_SETTING_GENERAL;
            settingIntent.putExtra("requestCode", requestCode);
            startActivityForResult(settingIntent, requestCode);
        }

        return true;
        /*
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
         */
    }

    @Override
    public void onItemClicked(final int pos) {
        Intent intent = new Intent(this, StationMapActivity.class);
        intent.putExtra("gasStationId", mStationList.get(pos).getStnId());
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {

    }

    // The following 2 methods are the fuel spinner event handler.
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        switch(pos){
            case 0: defaults[0] = "B027"; break; // gasoline
            case 1: defaults[0] = "D047"; break; // diesel
            case 2: defaults[0] = "K015"; break; // LPG
            case 3: defaults[0] = "B034"; break; // premium gasoline
            default: break;
        }

        if(!defaultFuel.matches(defaults[0])) {
            defaultFuel = defaults[0];
            log.i("onItemSelected:%s", defaultFuel);
            // Retrives the price data respectively saved in the cache directory with a fuel selected
            // by the spinner.
            binding.mainHeader.avgPriceView.addPriceView(defaultFuel);

            // Attach the viewpager adatepr with a fuel code selected by the spinner.
            pricePagerAdapter.setFuelCode(defaultFuel);
            binding.mainHeader.viewpagerPrice.setAdapter(pricePagerAdapter);

            // Retrieve near stations based on a newly selected fuel code if the spinner selection
            // has changed. Temporarily make this not working for preventing excessive access to the
            // server.
            stationListTask = ThreadManager.startStationListTask(stnModel, mPrevLocation, defaults);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    // Ref: expand the station recyclerview up to wrap_content
    private void showCollapsedPrice(int offset) {
        //log.i("offset:%s, %s", offset, appbar.getTotalScrollRange());
        //float value = (float)Math.abs(offset) / appbar.getTotalScrollRange();
        //priceView.setAlpha(value);
        if(Math.abs(offset) == binding.appbar.getTotalScrollRange()) {
            binding.viewCollapsedPrice.setVisibility(View.VISIBLE);
            ObjectAnimator objAnim = ObjectAnimator.ofFloat(binding.viewCollapsedPrice, "alpha", 0f, 1f);
            objAnim.setDuration(500);
            objAnim.start();
        } else binding.viewCollapsedPrice.setVisibility(View.GONE);
    }

    private void setSpinnerToDefaultFuel() {
        String[] code = getResources().getStringArray(R.array.spinner_fuel_code);
        defaults = getDefaultParams();
        for(int i = 0; i < code.length; i++) {
            if(code[i].matches(defaults[0])) {
                binding.mainHeader.spinnerGas.setSelection(i);
                defaultFuel = defaults[0];
            }
        }
    }

}

