package com.silverback.carman;

import static com.silverback.carman.SettingActivity.PREF_USER_IMAGE;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.silverback.carman.adapters.MainContentAdapter;
import com.silverback.carman.adapters.MainPricePagerAdapter;
import com.silverback.carman.adapters.StationEvAdapter;
import com.silverback.carman.adapters.StationGasAdapter;
import com.silverback.carman.adapters.StationHydroAdapter;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.databinding.ActivityMainBinding;
import com.silverback.carman.fragments.FinishAppDialogFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.GasPriceTask;
import com.silverback.carman.threads.LocationTask;
import com.silverback.carman.threads.StationEvTask;
import com.silverback.carman.threads.StationGasTask;
import com.silverback.carman.threads.StationHydroTask;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.RecyclerDividerUtil;
import com.silverback.carman.viewmodels.ImageViewModel;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.OpinetViewModel;
import com.silverback.carman.viewmodels.StationListViewModel;
import com.silverback.carman.views.ProgressButton;

import org.json.JSONArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;



public class MainActivity extends BaseActivity implements
        StationGasAdapter.OnRecyclerItemClickListener,
        FinishAppDialogFragment.NoticeDialogListener,
        MainContentAdapter.MainContentAdapterListener,
        AdapterView.OnItemSelectedListener {

    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity.class);

    public static final int NOTIFICATION = 0;
    public static final int BANNER_AD_1 = 1;
    public static final int VIEWPAGER_EXPENSE = 2;
    public static final int CARLIFE = 3;
    public static final int BANNER_AD_2 = 4;
    public static final int COMPANY_INFO = 5;


    // Objects
    //private ActivityMainBinding binding;
    private ActivityMainBinding binding;

    private LocationViewModel locationModel;
    private StationListViewModel stationModel;
    private ImageViewModel imgModel;
    private OpinetViewModel opinetModel;
    //private DataBindingViewModel bindingModel;

    private LocationTask locationTask;
    private StationGasTask stationGasTask;
    private StationEvTask evTask;
    private StationHydroTask hydroTask;

    private MainContentAdapter mainContentAdapter;
    private StationGasAdapter stnListAdapter;
    private StationEvAdapter evListAdapter;
    private StationHydroAdapter hydroAdapter;
    private MainPricePagerAdapter mainPricePagerAdapter;

    private Location mPrevLocation;
    private List<Opinet.GasStnParcelable> mStationList;
    private ApplyImageResourceUtil imgResUtil;


    // Fields
    private List<ProgressButton> progbtnList;
    private String[] arrGasCode, arrSidoCode, defaultParams;
    private String gasCode;
    private boolean isRadiusChanged, isGasTypeChanged, isStnViewOn;
    private boolean hasStationInfo = false;
    private boolean bStnOrder = false; // false: distance true:price
    private int prevStation = -1;
    private int activeStation;

    // The manual says that registerForActivityResult() is safe to call before a fragment or activity
    // is created
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::getActivityResult);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set the toolbar with icon, titile. The OptionsMenu are defined below to override
        // methods.
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(false);
        String title = mSettings.getString(Constants.USER_NAME, "Carman");
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);

        // Set initial values to fields
        defaultParams = getNearStationParams();// 0:gas type 1:radius 2:order(distance or price)
        arrGasCode = getResources().getStringArray(R.array.spinner_fuel_code);
        arrSidoCode = getResources().getStringArray(R.array.sido_name);
        mPrevLocation = null;

        // MainContent RecyclerView to display main content feeds in the activity
        mainContentAdapter = new MainContentAdapter(MainActivity.this, this);
        RecyclerDividerUtil divider = new RecyclerDividerUtil(Constants.DIVIDER_HEIGHT_MAIN, 0, getColor(R.color.recyclerDivider));
        RecyclerDividerUtil divider2 = new RecyclerDividerUtil(Constants.DIVIDER_HEIGHT_STN, 0, getColor(R.color.recyclerDivider));
        binding.recyclerContents.setAdapter(mainContentAdapter);
        binding.recyclerContents.addItemDecoration(divider);

        // AdapterView(Spinner) to select a gas type.
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this, R.array.spinner_fuel_name, R.layout.main_spinner_gas);
        spinnerAdapter.setDropDownViewResource(R.layout.main_spinner_dropdown);
        binding.mainTopFrame.spinnerGas.setAdapter(spinnerAdapter);
        setGasSpinnerSelection(defaultParams[0]);

        ArrayAdapter<CharSequence> spinnerAdapter2 = ArrayAdapter.createFromResource(
                this, R.array.sido_name, R.layout.main_spinner_ev);
        spinnerAdapter.setDropDownViewResource(R.layout.main_spinner_dropdown);
        binding.evStatus.spinnerEv.setAdapter(spinnerAdapter2);

        //JSONArray jsonArray = getDistrictJSONArray();


        // Create MainPricePagerAdapter which displays the graphs for the last 3 month total expense
        // and the expense configuration of this month. More pages should be added to analyze the
        // user expense.
        mainPricePagerAdapter = new MainPricePagerAdapter(this);
        mainPricePagerAdapter.setFuelCode(defaultParams[0]);
        binding.mainTopFrame.viewpagerPrice.setAdapter(mainPricePagerAdapter);
        binding.mainTopFrame.avgPriceView.addPriceView(defaultParams[0]);

        // Set the station recyclerview
        binding.recyclerStations.setHasFixedSize(true);
        binding.recyclerStations.addItemDecoration(divider2);
        binding.recyclerStations.setItemAnimator(new DefaultItemAnimator());

        // Multi-button tab in the bottom
        progbtnList = new ArrayList<>();
        progbtnList.add(binding.progbtnGas);
        progbtnList.add(binding.progbtnSvc);
        progbtnList.add(binding.progbtnElec);
        progbtnList.add(binding.progbtnHydro);


        // Event Handlers
        //binding.appbar.addOnOffsetChangedListener((appbar, offset) -> showCollapsedStatusBar(offset));
        binding.mainTopFrame.spinnerGas.setOnItemSelectedListener(this);
        binding.recyclerStations.addOnScrollListener(scrollListener);

        imgResUtil = new ApplyImageResourceUtil(this);
        // ViewModels
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
        stationModel = new ViewModelProvider(this).get(StationListViewModel.class);
        imgModel = new ViewModelProvider(this).get(ImageViewModel.class);
        opinetModel = new ViewModelProvider(this).get(OpinetViewModel.class);
        //bindingModel = new ViewModelProvider(this).get(DataBindingViewModel.class);



    }

    @Override
    public void onResume() {
        super.onResume();
        //binding.mainTopFrame.viewpagerPrice.registerOnPageChangeCallback(pagerCallback);
        String userImg = mSettings.getString(PREF_USER_IMAGE, null);
        String imgUri = (TextUtils.isEmpty(userImg))?Constants.imgPath + "ic_user_blank_gray":userImg;
        imgResUtil.applyGlideToDrawable(imgUri, Constants.ICON_SIZE_TOOLBAR_USERPIC, imgModel);
        imgModel.getGlideDrawableTarget().observe(this, userImage -> {
            if(getSupportActionBar() != null) getSupportActionBar().setIcon(userImage);
        });
        // Return the fuel price pager to the first page.
        binding.mainTopFrame.viewpagerPrice.setCurrentItem(0, true);

    }

    @Override
    public void onPause() {
        super.onPause();
        if(locationTask != null) locationTask = null;
        if(stationGasTask != null) stationGasTask = null;
        if(evTask != null) evTask = null;
        if(hydroTask != null) hydroTask = null;

        binding.mainTopFrame.viewpagerPrice.unregisterOnPageChangeCallback(pagerCallback);

    }

    @Override
    public void getPermissionResult(Boolean isPermitted) {

    }

    @Override
    public void onStop() {
        super.onStop();
        //activityResultLauncher.unregister();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_options_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Turn the near station recyclerview off.
        if(item.getItemId() == R.id.action_garage) {
            // Result back to MainExpensePagerFragment to update the expense in the feed.
            Intent expenseIntent = new Intent(this, ExpenseActivity.class);
            expenseIntent.putExtra("caller", Constants.REQUEST_MAIN_EXPENSE_TOTAL);
            activityResultLauncher.launch(expenseIntent);
        } else if(item.getItemId() == R.id.action_board) {
            startActivity(new Intent(this, BoardActivity.class));
        } else if(item.getItemId() == R.id.action_login) {
            log.i("login process required");
        } else if(item.getItemId() == R.id.action_setting) {
            // Results back to MainActivity to update the user settings
            Intent settingIntent = new Intent(this, SettingActivity.class);
            settingIntent.putExtra("caller", Constants.REQUEST_MAIN_SETTING_GENERAL);
            activityResultLauncher.launch(settingIntent);
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        FinishAppDialogFragment endDialog = new FinishAppDialogFragment();
        endDialog.show(getSupportFragmentManager(), "endDialog");
    }

    // Implement StationGasAdapter.OnRecyclerItemClickListener
    @Override
    public void onItemClicked(final int pos) {
        // Disable click until any data has been downloaded from Firestore.
        if(!hasStationInfo) {
            log.i("wait a sec"); // Check if necessary!!!
            return;
        }

        Intent intent = new Intent(this, StationMapActivity.class);
        intent.putExtra("gasStationId", mStationList.get(pos).getStnId());
        activityResultLauncher.launch(intent);
    }


    // Implement AdapterView(Spinner).OnItemSelectedListener
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        //gasCode = (defaultParams[0].matches(arrGasCode[pos]))?defaultParams[0]:arrGasCode[pos];
        if(defaultParams[0].matches(arrGasCode[pos])) gasCode = defaultParams[0];
        else {
            gasCode = arrGasCode[pos];
            binding.mainTopFrame.avgPriceView.addPriceView(gasCode);
        }

        mainPricePagerAdapter.setFuelCode(gasCode);
        mainPricePagerAdapter.notifyItemRangeChanged(0, mainPricePagerAdapter.getItemCount() - 1, gasCode);

        // Update the average gas price and the hidden price bar.
        createGasStatusBar();



        // As far as the near-station recyclerview is in the foreground, update the price info with
        // a new gas selected. refactor required: any station with a selected gas type does not
        // exist, indicate it in the view.
        isStnViewOn = binding.recyclerStations.getVisibility() == View.VISIBLE;
        if(isStnViewOn) {
            defaultParams[0] = gasCode;
            stationGasTask = ThreadManager2.startGasStationListTask(stationModel, mPrevLocation, defaultParams);
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    // Implement FinishAppDialogFragment.NoticeDialogListener interface ;
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        File cacheDir = getCacheDir();
        if(cacheDir.exists() && checkPriceUpdate()) {
            for(File file : Objects.requireNonNull(cacheDir.listFiles())) {
                if(file.delete()) log.i("file deleted: %s", file.getPath());
            }
        }

        if(CarmanDatabase.getDatabaseInstance(this) != null) CarmanDatabase.destroyInstance();
        finishAffinity();
    }
    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {}

    // Implement MainContentAdapter.MainContentAdapterListener for the buttons defined in the
    // notification and the postings feed.
    @Override
    public void onClickPostingIcon(int category) {
        Intent boardIntent = new Intent(this, BoardActivity.class);
        boardIntent.putExtra("category", category);
        startActivity(boardIntent);
    }

    // Reset the default fuel code
    private void setGasSpinnerSelection(String gasCode) {
        for(int i = 0; i < arrGasCode.length; i++) {
            if(arrGasCode[i].matches(gasCode)) {
                binding.mainTopFrame.spinnerGas.setSelection(i);
                this.gasCode = arrGasCode[i];
                break;
            }
        }
    }

    private void setAltSpinnerSelection(String sidoName) {

    }


    // Implement the abstract class of ViewPager2.OnPageChangeCallback to listen to the viewpager
    // changing a page.
    private final ViewPager2.OnPageChangeCallback pagerCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            log.i("page: %s", position);
        }
    };


    // Ref: expand the station recyclerview up to wrap_content
    // Select a status bar using ViewSwitcher b/w gas and ev, hydro.
    private void showCollapsedStatusBar(int sort) {
        binding.viewSwitcher.setDisplayedChild(sort);
        binding.appbar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            if(Math.abs(verticalOffset) == binding.appbar.getTotalScrollRange()) {
                binding.viewSwitcher.setVisibility(View.VISIBLE);
                ObjectAnimator anim = ObjectAnimator.ofFloat(binding.viewSwitcher, "alpha", 0, 1);
                anim.setDuration(500);
                anim.start();

            } else binding.viewSwitcher.setVisibility(View.GONE);
        });

    }


    // Scale the size of the fab as the station recyclerview scrolls up and down.
    private final RecyclerView.OnScrollListener scrollListener = new RecyclerView.OnScrollListener(){
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            //if (dy > 0 || dy < 0 && binding.fab.isShown()) binding.fab.hide();
            super.onScrolled(recyclerView, dx, dy);
        }
        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            /*
            if (newState == RecyclerView.SCROLL_STATE_IDLE){
                //binding.fab.show();
            }
             */
            super.onScrollStateChanged(recyclerView, newState);
        }
    };

    public void locateStations(int activeStation, boolean isActive){
        // Turn the previous button off if another button clicks
        this.activeStation = activeStation;

        if(prevStation != -1 && prevStation != activeStation) {
            progbtnList.get(prevStation).resetProgress();
            //binding.recyclerStations.setVisibility(View.GONE);

            // Remove the observer to prevent invoking the previous progbtn as the current button
            // fetches a location.
            locationModel.getLocation().removeObservers(this);
        }

        if(!isActive) {
            final String perm = Manifest.permission.ACCESS_FINE_LOCATION;
            final String rationale = "permission required to use Fine Location";
            checkRuntimePermission(binding.getRoot(), perm, rationale,  () -> {
                locationTask = ThreadManager2.fetchLocationTask(this, locationModel);
                progbtnList.get(activeStation).setProgress();
                locationModel.getLocation().observe(this, location -> {
                    log.i("observe location:%s", activeStation);
                    switch(activeStation) {
                        case 0: locateGasStations(location); break;
                        case 1: locateSvcStations(location); break;
                        case 2: locateEvStations(location); break;
                        case 3: locateHydroStations(location); break;
                    }
                });

                locationModel.getLocationException().observe(this, exception -> {
                    log.i("Exception occurred while fetching location");
                    String msg = getString(R.string.general_no_location);
                    SpannableString spannableString = new SpannableString(msg);
                    //binding.stationRecyclerView.setVisibility(View.VISIBLE);
                    //binding.stationRecyclerView.showSpannableTextView(spannableString);
                });

                stationModel.getExceptionMessage().observe(this, exception -> {
                    log.i("Exception occurred while getting station list");
                    locationTask = null;
                    progbtnList.get(activeStation).stopProgress();

                    //progbtnList.get(activeButton).stopProgress();
                });
            });

            prevStation = activeStation;

        } else {
            binding.recyclerStations.setVisibility(View.GONE);
            binding.recyclerContents.setVisibility(View.VISIBLE);
            binding.fab.setVisibility(View.GONE);
            progbtnList.get(activeStation).resetProgress();

            // temp: test required
            if(locationModel.getLocation() != null) locationModel.getLocation().removeObservers(this);
        }
    }

    private void locateGasStations(Location location) {
        mPrevLocation = location;
        defaultParams[0] = gasCode;
        stationGasTask = ThreadManager2.startGasStationListTask(stationModel, location, defaultParams);

        stationModel.getNearStationList().observe(this, stnList -> {
            if (stnList != null && stnList.size() > 0) {
                mStationList = stnList;
                stnListAdapter = new StationGasAdapter(mStationList, this);
                //binding.stationRecyclerView.getRecyclerView().setAdapter(stnListAdapter);
                //binding.stationRecyclerView.showStationRecyclerView();``
                binding.recyclerStations.setAdapter(stnListAdapter);

                progbtnList.get(0).stopProgress();
                binding.appbar.setExpanded(true, true);
                binding.fab.setVisibility(View.VISIBLE);
                // Set the listener to handle the visibility of the price bar by scrolling.
                //createGasStatusBar();
                showCollapsedStatusBar(0);

            } else {
                // No near stations post an message that contains the clickable span to link to the
                // SettingPreferenceActivity for resetting the searching radius.
                //SpannableString spannableString = handleStationListException();
                //binding.stationRecyclerView.showSpannableTextView(spannableString);
            }

            isRadiusChanged = false;
            isGasTypeChanged = false;
            //binding.pbNearStns.setVisibility(View.GONE);
            //binding.progbtnGas.setProgress(true);
            binding.recyclerContents.setVisibility(View.GONE);
            //binding.stationRecyclerView.setVisibility(View.VISIBLE);
            binding.recyclerStations.setVisibility(View.VISIBLE);


            progbtnList.get(0).stopProgress();
        });

        // Update the carwash info to StationList and notify the data change to Adapter.
        // Adapter should not assume that the payload will always be passed to onBindViewHolder()
        // e.g. when the view is not attached.
        stationModel.getStationCarWashInfo().observe(this, sparseArray -> {
            for(int i = 0; i < sparseArray.size(); i++) {
                mStationList.get(i).setIsWash(sparseArray.valueAt(i));
                stnListAdapter.notifyItemChanged(sparseArray.keyAt(i), sparseArray.valueAt(i));
            }
            // To notify that fetching station list has completed.
            hasStationInfo = true;
        });
    }


    private void locateEvStations(Location location) {
        mPrevLocation = location;
        evTask = ThreadManager2.startEVStatoinListTask(this, stationModel, location);

        stationModel.getEvStationList().observe(this, evList -> {
            if(evList != null && evList.size() > 0) {
                evListAdapter = new StationEvAdapter(evList);
                binding.recyclerStations.setAdapter(evListAdapter);

                binding.recyclerStations.setVisibility(View.VISIBLE);
                binding.recyclerContents.setVisibility(View.GONE);
                progbtnList.get(2).stopProgress();

                //binding.fab.setVisibility(View.GONE);
                stationModel.getEvStationList().removeObservers(this);

                binding.appbar.setExpanded(true, true);
                showCollapsedStatusBar(1);
                if(binding.fab.getVisibility() == View.GONE) binding.fab.setVisibility(View.VISIBLE);

            }


        });

        stationModel.getExceptionMessage().observe(this, err -> {
            Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
            progbtnList.get(2).resetProgress();
            binding.fab.setVisibility(View.GONE);
            evTask = null;
        });


        //binding.fab.setVisibility(View.GONE);

    }

    private void locateHydroStations(Location location) {
        mPrevLocation = location;
        hydroTask = ThreadManager2.startHydroStationListTask(this, stationModel, location);

        stationModel.getHydroStationList().observe(this, hydroList -> {
            if(hydroList != null && hydroList.size() > 0) {
                log.i("hydrolist: %s, %s", hydroList.size(), hydroList.get(0).getName());
                hydroAdapter = new StationHydroAdapter(hydroList);
                binding.recyclerStations.setAdapter(hydroAdapter);

                binding.recyclerStations.setVisibility(View.VISIBLE);
                binding.recyclerContents.setVisibility(View.GONE);
                progbtnList.get(3).stopProgress();

                //binding.fab.setVisibility(View.GONE);
                stationModel.getHydroStationList().removeObservers(this);
                binding.appbar.setExpanded(true, true);
                binding.fab.setVisibility(View.VISIBLE);
                showCollapsedStatusBar(1);
            }

        });
    }

    private void locateSvcStations(Location location) {
        log.i("Service Stations: %s", location);
    }

    // Reorder near station list according to the distance/price, which is called from the layout
    // file as well.
    public void onFabClicked(View view) {
        if(activeStation != 0) {
            log.i("alt station required to be handled");
            return;
        }

        bStnOrder = !bStnOrder;
        Uri uri = saveNearStationList(mStationList);
        if(uri == null) return;
        // Switch the FAB background.
        if(bStnOrder) binding.fab.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.bg_location));
        else binding.fab.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.bg_currency_won));

        mStationList = stnListAdapter.sortStationList(bStnOrder);

    }

    private Uri saveNearStationList(List<Opinet.GasStnParcelable> list) {
        File file = new File(getCacheDir(), Constants.FILE_CACHED_NEAR_STATIONS);
        // Delete the file before saving a new list.
        if(file.exists()) {
            boolean delete = file.delete();
            if(delete) log.i("cache cleared");
        }

        try(FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(list);
            return Uri.fromFile(file);
        } catch (IOException e) { e.printStackTrace();}

        return null;
    }

    private SpannableString handleStationListException(){
        SpannableString spannableString;
        if(isNetworkConnected) {
            String msg = getString(R.string.main_no_station_fetched);
            String radius = defaultParams[1];
            // In case the radius is already set to the maximum value(5000m), no need to change the value.
            if(radius != null && radius.matches("5000")) {
                msg = msg.substring(0, msg.indexOf("\n"));
                return new SpannableString(defaultParams[1] + msg);
            }

            String format = String.format("%s%s", radius, msg);

            spannableString = new SpannableString(format);
            spannableString.setSpan(
                    new ForegroundColorSpan(Color.RED), 0,
                    Objects.requireNonNull(radius).length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Set the ClickableSpan range
            String spanned = getString(R.string.main_index_reset);
            int start = format.indexOf(spanned);
            int end = start + spanned.length();

            spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Intent settingIntent = new Intent(MainActivity.this, SettingActivity.class);
                    activityResultLauncher.launch(settingIntent);
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);


        } else {
            String message = getString(R.string.errror_no_network);
            spannableString = new SpannableString(message);

            // Set the ClickableSpan range.
            String spanned = getString(R.string.error_index_retry);
            int start = message.indexOf(spanned);
            int end = start + spanned.length();

            // Refactor required: move to network setting to check if it turns on.
            spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Intent networkIntent = new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS);
                    activityResultLauncher.launch(networkIntent);
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannableString;
    }

    // Implement ActivityResultCallback<Intent> defined as a param in registerForActivityResult.
    private void getActivityResult(ActivityResult result) {
        // If the station reyelcerview is in the visible state, it should be gone
        //isStnViewOn = binding.stationRecyclerView.getVisibility() == View.VISIBLE;
        isStnViewOn = binding.recyclerStations.getVisibility() == View.VISIBLE;
        if(isStnViewOn) {
            //binding.stationRecyclerView.setVisibility(View.GONE);
            binding.recyclerStations.setVisibility(View.GONE);
            binding.fab.setVisibility(View.GONE);
            binding.recyclerContents.setVisibility(View.VISIBLE);
            //binding.progbtnGas.setProgress(isStnViewOn);
            progbtnList.get(0).stopProgress();
        }

        Intent resultIntent = result.getData();
        if(resultIntent == null) return;
        switch(result.getResultCode()) {
            case Constants.REQUEST_MAIN_EXPENSE_TOTAL: // ExpenseActivity result
                int total = resultIntent.getIntExtra("expense", 0);
                mainContentAdapter.notifyItemChanged(VIEWPAGER_EXPENSE, total);
                break;

            case Constants.REQUEST_MAIN_SETTING_GENERAL:
                updateSettingResult(result);
                break;
        }
    }

    // Get the price info saved in the cache and show them in the price bar.
    private void createGasStatusBar() {
        final String[] arrFile = {Constants.FILE_CACHED_SIDO_PRICE, Constants.FILE_CACHED_SIGUN_PRICE };
        String avgPrice = String.valueOf(binding.mainTopFrame.avgPriceView.getAvgGasPrice());
        binding.gasStatus.tvCollapsedAvgPrice.setText(avgPrice);
        //bindingModel.getAvgPrice().setValue(avgPrice);

        // Set the sido and sigun price which is stored in the cache at an interval.
        for(String fName : arrFile) {
            File file = new File(getCacheDir(), fName);
            Uri uri = Uri.fromFile(file);
            try (InputStream is = getContentResolver().openInputStream(uri);
                 ObjectInputStream ois = new ObjectInputStream(is)) {
                Object obj = ois.readObject();
                Iterable<?> itr = (Iterable<?>) obj;
                for (Object x : itr) {
                    switch (fName) {
                        case Constants.FILE_CACHED_SIDO_PRICE:
                            Opinet.SidoPrice sido = (Opinet.SidoPrice) x;
                            if (sido.getProductCd().matches(gasCode)) {
                                //bindingModel.getSidoName().setValue(sido.getSidoName());
                                //bindingModel.getSidoPrice().setValue(String.valueOf(sido.getPrice()));
                                binding.gasStatus.tvCollapsedSido.setText(sido.getSidoName());
                                binding.gasStatus.tvCollapsedSidoPrice.setText(String.valueOf(sido.getPrice()));
                            }
                            break;

                        case Constants.FILE_CACHED_SIGUN_PRICE:
                            Opinet.SigunPrice sigun = (Opinet.SigunPrice) x;
                            if (sigun.getProductCd().matches(gasCode)) {
                                //bindingModel.getSigunName().setValue(sigun.getSigunName());
                                //bindingModel.getSigunPrice().setValue(String.valueOf(sigun.getPrice()));
                                binding.gasStatus.tvCollapsedSigun.setText(sigun.getSigunName());
                                binding.gasStatus.tvCollapsedSigunPrice.setText(String.valueOf(sigun.getPrice()));
                            }
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    // Implement ActivityResult callback, the result of which is sent from SettingPrefActivity.
    private void updateSettingResult(ActivityResult result) {
        final Intent resultIntent = result.getData();
        if(resultIntent == null) return;

        String userName = resultIntent.getStringExtra("userName");
        String district = resultIntent.getStringExtra("distCode");
        String gasType = resultIntent.getStringExtra("gasCode");
        String searchRadius = resultIntent.getStringExtra("searchRadius");
        log.i("getting result: %s, %s, %s, %s", userName, district, gasType, searchRadius);

        // Update the user name
        if(!TextUtils.isEmpty(userName)) {
            Objects.requireNonNull(getSupportActionBar()).setTitle(userName);
        }

        if(!TextUtils.isEmpty(district)) {
            GasPriceTask gasPriceTask = ThreadManager2.startGasPriceTask(this, opinetModel, district);
            opinetModel.distPriceComplete().observe(this, isDone -> {
                log.i("viewmodel");
                if(isDone) {
                    if(!TextUtils.isEmpty(gasType)) {
                        isGasTypeChanged = true;
                        setGasSpinnerSelection(gasType);
                        defaultParams[0] = gasType;
                        mainPricePagerAdapter.notifyItemChanged(0, gasType);
                    }

                    createGasStatusBar();
                    mainPricePagerAdapter.notifyItemRangeChanged(0, mainPricePagerAdapter.getItemCount(), gasCode);
                }
            });
        }

        if(!TextUtils.isEmpty(gasType)) {
            isGasTypeChanged = true;
            setGasSpinnerSelection(gasType);
            defaultParams[0] = gasType;
            mainPricePagerAdapter.notifyItemChanged(0, gasType);
        }

        // Update the price in the viewpager, which depends upon whether either district or gas type
        // or both changes.
        /*
        if(district != null && gasType != null) {
            gasPriceTask = ThreadManager2.startGasPriceTask(this, opinetModel, district);
            opinetModel.distPriceComplete().observe(this, isDone -> {
                if(isDone) {
                    mainPricePagerAdapter.notifyItemChanged(0, gasType);
                    setGasSpinnerSelection(gasType);
                    setCollapsedPriceBar();
                }
            });

        } else if (district != null) {
            gasPriceTask = ThreadManager2.startGasPriceTask(this, opinetModel, district);
            opinetModel.distPriceComplete().observe(this, isDone -> {
                if(isDone) {
                    log.i("GasPriceTask successfully done: %s", gasCode);
                    mainPricePagerAdapter.notifyItemChanged(0, gasCode);
                    setCollapsedPriceBar();
                }
            });

        } else if(gasType != null) {
            isGasTypeChanged = true;
            setGasSpinnerSelection(gasType);
            defaultParams[0] = gasType;
            mainPricePagerAdapter.notifyItemChanged(0, gasType);
        }
         */

        // Reset the searching radius for near gas stations.
        if(!TextUtils.isEmpty(searchRadius)) {
            isRadiusChanged = true;
            defaultParams[1] = searchRadius;
        }
    }


}

