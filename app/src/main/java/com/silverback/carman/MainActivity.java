package com.silverback.carman;

import static com.silverback.carman.SettingActivity.PREF_USER_IMAGE;
import static com.silverback.carman.adapters.StationEvAdapter.VIEW_COLLAPSED;
import static com.silverback.carman.adapters.StationEvAdapter.VIEW_EXPANDED;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.snackbar.Snackbar;
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
import com.silverback.carman.threads.StationEvRunnable;
import com.silverback.carman.threads.StationEvTask;
import com.silverback.carman.threads.StationGasRunnable;
import com.silverback.carman.threads.StationGasTask;
import com.silverback.carman.threads.StationHydroRunnable;
import com.silverback.carman.threads.StationHydroTask;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.RecyclerDividerUtil;
import com.silverback.carman.viewmodels.ImageViewModel;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.OpinetViewModel;
import com.silverback.carman.viewmodels.StationViewModel;
import com.silverback.carman.views.ProgressButton;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MainActivity extends BaseActivity implements
        StationGasAdapter.OnItemClickCallback,
        FinishAppDialogFragment.NoticeDialogListener,
        MainContentAdapter.MainContentAdapterListener,
        StationEvAdapter.OnExpandItemClicked,
        AdapterView.OnItemSelectedListener {

    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity.class);
    public static final String regexEvName = "\\d*\\([\\w\\s]*\\)";

    // Objects
    //private ActivityMainBinding binding;
    private ActivityMainBinding binding;

    private LocationViewModel locationModel;
    private StationViewModel stationModel;
    private ImageViewModel imgModel;
    private OpinetViewModel opinetModel;
    //private DataBindingViewModel bindingModel;

    private LocationTask locationTask;
    private StationGasTask gasTask;
    private StationEvTask evTask;
    private StationHydroTask hydroTask;

    private MainContentAdapter mainContentAdapter;
    private StationGasAdapter gasListAdapter;
    private StationEvAdapter evListAdapter;
    private StationHydroAdapter hydroAdapter;
    private MainPricePagerAdapter mainPricePagerAdapter;

    private Location mPrevLocation;
    //private List<Opinet.GasStnParcelable> gasStationList;
    private List<StationGasRunnable.Item> gasStationList;

    private ApplyImageResourceUtil imgResUtil;

    // Define Observers
    private Observer<Location> locationObserver;
    private Observer<List<StationGasRunnable.Item>> gasObserver;
    private Observer<List<StationEvRunnable.Item>> evObserver;
    private Observer<List<StationHydroRunnable.HydroStationObj>> hydroObserver;

    // Fields
    private List<ProgressButton> progbtnList;
    private List<StationEvRunnable.Item> evFullList;
    private List<MultiTypeEvItem> evSimpleList;
    private String[] arrGasCode, arrSidoCode, defaultParams;
    private String gasCode;
    private boolean isRadiusChanged, isGasTypeChanged, isStnViewOn;
    //private boolean hasStationInfo = false;
    private boolean bStnOrder = false; // false: distance true:price

    private int statusbar;
    private String sidoName;
    private int prevBtnId = -1;
    private int prevEvPos = -1;
    private int prevEvCount = -1;

    // The manual says that registerForActivityResult() is safe to call before a fragment or activity
    // is created
    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::getActivityResult);

    // EV station list displays in the multi-type RecyclerView.
    public static class MultiTypeEvItem {
        StationEvRunnable.Item item;
        int viewType;
        public MultiTypeEvItem(StationEvRunnable.Item item, int viewType) {
            this.item = item;
            this.viewType = viewType;
        }

        public int getViewType() { return viewType;}
        public StationEvRunnable.Item getItem() { return item;}
        public String getItemId() { return item.getChgerId();}
        public String getStatId() { return item.getStdId();}
    }

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

        binding.viewFlipper.setDisplayedChild(0);

        // AdapterView(Spinner) to select a gas type.
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this, R.array.spinner_fuel_name, R.layout.main_spinner_gas);
        spinnerAdapter.setDropDownViewResource(R.layout.main_spinner_dropdown);
        binding.mainTopFrame.spinnerGas.setAdapter(spinnerAdapter);
        setGasSpinnerSelection(defaultParams[0]);

        // Create MainPricePagerAdapter which displays the graphs for the last 3 month total expense
        // and the expense configuration of this month. More pages should be added to analyze the
        // user expense.
        binding.mainTopFrame.viewpagerPrice.setOffscreenPageLimit(1);
        binding.mainTopFrame.viewpagerPrice.setPageTransformer((page, position) -> {
            log.i("page transformer: %s, %s", page, position);
            page.setTranslationX(position * -15);
        });

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
        statusbar = 0;
        binding.appbar.addOnOffsetChangedListener((appbar, offset) ->
                showMultiStatusBar(offset, statusbar));
        binding.mainTopFrame.spinnerGas.setOnItemSelectedListener(this);
        binding.recyclerStations.addOnScrollListener(scrollListener);

        imgResUtil = new ApplyImageResourceUtil(this);
        // ViewModels
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
        stationModel = new ViewModelProvider(this).get(StationViewModel.class);
        imgModel = new ViewModelProvider(this).get(ImageViewModel.class);
        opinetModel = new ViewModelProvider(this).get(OpinetViewModel.class);

        // Create gasstation list and the full, imple Ev list
        gasStationList = new ArrayList<>();
        evFullList = new ArrayList<>();
        evSimpleList = new ArrayList<>();

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
        // Nullify thread tasks
        if(locationTask != null) locationTask = null;
        if(gasTask != null) gasTask = null;
        if(evTask != null) evTask = null;
        if(hydroTask != null) hydroTask = null;

        binding.mainTopFrame.viewpagerPrice.unregisterOnPageChangeCallback(pagerCallback);
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
            expenseIntent.putExtra("userId", userId);
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
        /*
        if(!hasStationInfo) {
            log.i("wait a sec"); // Check if necessary!!!
            return;
        }

         */

        Intent intent = new Intent(this, StationMapActivity.class);
        intent.putExtra("stationDetail", (Parcelable)gasStationList.get(pos));
        //intent.putExtra("gasStationId", gasStationList.get(pos).getStnId());
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
            gasTask = ThreadManager2.startGasStationTask(stationModel, mPrevLocation, defaultParams);
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

    // Implement StationEvAdapter.OnExpandItemClicked for expanding all chargers in the station.
    // Any sub items already expanded should be clcosed at first, then expand new sub items.
    // SubItems has a different view type
    @Override
    public void onEvExpandIconClicked(String name, int position, int count) {
        // Collapse an already expanded station item and reset the position if it follows the old
        // position
        if(prevEvPos != -1) {
            try {
                evSimpleList.subList(prevEvPos + 1, prevEvPos + 1 + prevEvCount).clear();
                evListAdapter.submitEvList(evSimpleList);
            } catch(IndexOutOfBoundsException e) { e.printStackTrace();}

            if(prevEvPos == position) {
                prevEvPos = -1;
                return;
            } else if(prevEvPos < position) position -= prevEvCount;
        }

        // Then, expand a new item by adding items with the same name from the full list, the number
        // of which
        int index = 1;
        for(int i = 0; i < evFullList.size(); i++) {
            String name2 = evFullList.get(i).getStdNm().replaceAll(regexEvName, "");
            if(name.matches(name2)) {
                evSimpleList.add(position + index, new MultiTypeEvItem(evFullList.get(i), VIEW_EXPANDED));
                if(index < count) index++;
                else break;
            }
        }

        prevEvPos = position;
        prevEvCount = count;
        evListAdapter.submitEvList(evSimpleList);
    }

    // Implement the abstract callback method of ActivityResultLauncher to check the location
    // permissiion, which is invoked in checkRuntimePermission(). Both methods are defined in
    // BaseActivity.
    @Override
    public void getPermissionResult(Boolean isLocationPermitted) {
        log.i("permission result");
        progbtnList.get(prevBtnId).resetProgress();;
        if(isLocationPermitted) locateStations(prevBtnId);
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
    /*
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

     */

    private void showMultiStatusBar(int offset, int childView) {
        binding.viewSwitcher.setDisplayedChild(childView);
        if(Math.abs(offset) == binding.appbar.getTotalScrollRange()) {
            binding.viewSwitcher.setVisibility(View.VISIBLE);
            ObjectAnimator anim = ObjectAnimator.ofFloat(binding.viewSwitcher, "alpha", 0, 1);
            anim.setDuration(1000);
            anim.start();
        } else binding.viewSwitcher.setVisibility(View.GONE);
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

    public void locateStations(int currentBtnId){
        log.i("multi button status: %s, %s", prevBtnId, currentBtnId);
        if(prevBtnId != -1) {
            progbtnList.get(prevBtnId).resetProgress();
            if(prevBtnId == currentBtnId) {
                binding.viewFlipper.setDisplayedChild(0);
                binding.fab.setVisibility(View.GONE);
                binding.appbar.setExpanded(true, true);
                statusbar = 0;
                prevBtnId = -1;
                return;

            } else prevBtnId = currentBtnId;

       // Click a multi-button initially.
        } else {
            if(binding.viewFlipper.getDisplayedChild() != 0) binding.viewFlipper.setDisplayedChild(0);
            prevBtnId = currentBtnId;
        }

        final String perm = Manifest.permission.ACCESS_FINE_LOCATION;
        final String rationale = "permission required to use Fine Location";
        checkRuntimePermission(binding.getRoot(), perm, rationale,  () -> {
            locationTask = ThreadManager2.fetchLocationTask(this, locationModel);
            progbtnList.get(currentBtnId).setProgress();

            locationObserver = new Observer<Location>() {
                @Override
                public void onChanged(Location location) {
                    log.i("observe location: %s", currentBtnId);
                    switch(currentBtnId) {
                        case 0: locateGasStations(location); break;
                        case 1: locateSvcStations(location); break;
                        case 2: locateEvStations(location); break;
                        case 3: locateHydroStations(location); break;
                    }
                    locationModel.getLocation().removeObserver(this);
                }
            };

            locationModel.getLocation().observe(this, locationObserver);
            locationModel.getLocationException().observe(this, exception -> {
                log.i("Exception occurred while fetching location");
                String msg = getString(R.string.general_no_location);
                SpannableString spannableString = new SpannableString(msg);
                //binding.stationRecyclerView.setVisibility(View.VISIBLE);
                //binding.stationRecyclerView.showSpannableTextView(spannableString);
            });

            /*
            stationModel.getExceptionMessage().observe(this, exception -> {
                log.i("Exception occurred while getting station list");
                locationTask = null;
                progbtnList.get(currentBtnId).stopProgress();
                progbtnList.get(currentBtnId).resetProgress();
            });
            */
        });

    }

    private void locateGasStations(Location location) {
        log.i("Locate Gas: %s", location);
        locationModel.getLocation().removeObserver(locationObserver);
        //if(mPrevLocation == null || (mPrevLocation.distanceTo(location) > Constants.UPDATE_DISTANCE)) {
        mPrevLocation = location;
        gasListAdapter = new StationGasAdapter(this);

        defaultParams[0] = gasCode;
        if(gasTask == null) gasTask = ThreadManager2.startGasStationTask(stationModel, location, defaultParams);

        stationModel.getNearStationList().observe(this, stnList -> {
            //log.i("gas station: %s", stnList);
            if (stnList != null && stnList.size() > 0) {
                gasStationList = stnList;
                gasListAdapter.setStationList(stnList);

                binding.viewFlipper.setDisplayedChild(1);
                progbtnList.get(0).stopProgress();
                binding.appbar.setExpanded(true, true);
                binding.fab.setVisibility(View.VISIBLE);
                statusbar = 0;

                binding.recyclerStations.setAdapter(gasListAdapter);

                //gasListAdapter.submitGasList(stnList);
                //stationModel.getNearStationList().removeObserver(this);

            } else {
                binding.viewFlipper.setDisplayedChild(2);
                progbtnList.get(0).resetProgress();
            }

            isRadiusChanged = false;
            isGasTypeChanged = false;

        });

        stationModel.getStationInfoArray().observe(this, sparseInfoArray -> {
            log.i("infoList: %s", sparseInfoArray.size());
            gasListAdapter.notifyItemRangeChanged(0, sparseInfoArray.size(), sparseInfoArray);
        });

        stationModel.getGasExceptionMessage().observe(this, msg -> {
            log.e("exception in Gas");
            String message = "Failed to fetch stations";
            binding.tvEmptyview.setText(message);
            binding.viewFlipper.setDisplayedChild(2);
            //Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
            progbtnList.get(0).resetProgress();
            //prevBtnId = -1;
            gasTask = null;
        });

        /*
        gasObserver = new Observer<List<StationGasRunnable.Item>>() {
            @Override
            public void onChanged(List<StationGasRunnable.Item> stnList) {
                if (stnList != null && stnList.size() > 0) {
                    gasStationList = stnList;
                    gasListAdapter.setStationList(stnList);

                    binding.viewFlipper.setDisplayedChild(1);
                    progbtnList.get(0).stopProgress();
                    binding.appbar.setExpanded(true, true);
                    binding.fab.setVisibility(View.VISIBLE);
                    statusbar = 0;

                    //gasListAdapter.submitGasList(stnList);
                    stationModel.getNearStationList().removeObserver(this);

                } else {
                    binding.viewFlipper.setDisplayedChild(2);
                    progbtnList.get(0).resetProgress();
                }

                isRadiusChanged = false;
                isGasTypeChanged = false;

                //binding.pbNearStns.setVisibility(View.GONE);
                //binding.progbtnGas.setProgress(true);
                //binding.recyclerContents.setVisibility(View.GONE);
                //binding.recyclerStations.setVisibility(View.VISIBLE);

            }
        };
        stationModel.getNearStationList().observe(this, gasObserver);
         */
    }


    private void locateEvStations(Location location) {
        log.i("Locate EV Station: %s", location);
        mPrevLocation = location;
        evSimpleList.clear();
        evFullList.clear();

        //locationModel.getLocation().removeObserver(locationObserver);
        if(binding.fab.getVisibility() == View.VISIBLE) binding.fab.setVisibility(View.GONE);
        evListAdapter = new StationEvAdapter(this);
        //binding.recyclerStations.setAdapter(evListAdapter);


        /*if(evTask == null)*/ evTask = ThreadManager2.startEVStationTask(this, stationModel, location);
        stationModel.getEvStationList().observe(this, evList -> {
            log.i("ev station List: %s", evList.size());
            evFullList.addAll(evList);

            List<StationEvRunnable.Item> tempList = new ArrayList<>(evList);
            if(evList.size() > 0) {
                for(int m = 0; m < tempList.size(); m++) {
                    String name = tempList.get(m).getStdNm().replaceAll(regexEvName, "");
                    int cntSame = 1;
                    int cntOpen = 0;
                    int cntCharging = 0;

                    if(tempList.get(m).getStat() == 2) cntOpen++;
                    else if(tempList.get(m).getStat() == 3) cntCharging++;

                    for(int n = tempList.size() - 1; n > m; n-- ) {
                        String name2 = tempList.get(n).getStdNm().replaceAll(regexEvName, "");
                        if(name.matches(name2)) {
                            cntSame++;

                            if(tempList.get(n).getStat() == 2) cntOpen++;
                            else if(tempList.get(n).getStat() == 3) cntCharging++;

                            tempList.remove(n);
                        }
                    }

                    tempList.get(m).setCntCharger(cntSame);
                    tempList.get(m).setCntOpen(cntOpen);
                    tempList.get(m).setCntCharging(cntCharging);
                    evSimpleList.add(new MultiTypeEvItem(tempList.get(m), VIEW_COLLAPSED));
                }

                binding.recyclerStations.setAdapter(evListAdapter);
                evListAdapter.submitEvList(evSimpleList);
                progbtnList.get(2).stopProgress();
                binding.viewFlipper.setDisplayedChild(1);

            } else {
                binding.viewFlipper.setDisplayedChild(2);
                progbtnList.get(2).stopProgress();
            }

            binding.appbar.setExpanded(true, true);
            statusbar = 1;
            tempList.clear();
            stationModel.getEvStationList().removeObservers(this);
        });

        /*
        evObserver = new Observer<List<StationEvRunnable.Item>>() {
            @Override
            public void onChanged(List<StationEvRunnable.Item> evList) {
                log.i("evList: %s", evList.size());
                if(evList.size() > 0) {
                    //stationModel.getEvStationList().removeObserver(this);
                    evFullList.addAll(evList);
                    for(int i = 0; i < evList.size(); i++) {
                        String name = evList.get(i).getStdNm().replaceAll(regexEvName, "");
                        int cntSame = 1;
                        int cntOpen = 0;
                        if(evList.get(i).getStat() == 2) cntOpen++;

                        for(int j = evList.size() - 1; j > i; j-- ) {
                            String name2 = evList.get(j).getStdNm().replaceAll(regexEvName, "");
                            if(name.matches(name2)) {
                                if(evList.get(j).getStat() == 2) cntOpen++;
                                evList.remove(j);
                                cntSame++;
                            }
                        }

                        evList.get(i).setCntCharger(cntSame);
                        evList.get(i).setCntOpen(cntOpen);
                        evSimpleList.add(new MultiTypeEvItem(evList.get(i), VIEW_COLLAPSED));
                    }


                    binding.recyclerStations.setAdapter(evListAdapter);
                    evListAdapter.submitEvList(evSimpleList);
                    progbtnList.get(2).stopProgress();
                    binding.viewFlipper.setDisplayedChild(1);

                } else {
                    binding.viewFlipper.setDisplayedChild(2);
                    progbtnList.get(2).stopProgress();
                    progbtnList.get(2).resetProgress();
                }

                //binding.fab.setVisibility(View.GONE);
                binding.appbar.setExpanded(true, true);
                statusbar = 1;
            }
        };
        stationModel.getEvStationList().observe(this, evObserver);
        */
        stationModel.getEvExceptionMessage().observe(this, err -> {
            log.e("exception in EV");
            binding.tvEmptyview.setText(err);
            binding.viewFlipper.setDisplayedChild(2);

            //prevBtnId = -1;
            progbtnList.get(2).resetProgress();

            evTask = null;
            locationModel.getLocation().removeObserver(locationObserver);
        });

    }

    private void locateHydroStations(Location location) {
        locationModel.getLocation().removeObserver(locationObserver);
        mPrevLocation = location;
        if(binding.fab.getVisibility() == View.VISIBLE) binding.fab.setVisibility(View.GONE);

        if(hydroTask == null) hydroTask = ThreadManager2.startHydroStationTask(this, stationModel, location);
        stationModel.getHydroStationList().observe(this, hydroList -> {
            if(hydroList != null && hydroList.size() > 0) {
                hydroAdapter = new StationHydroAdapter(hydroList);
                binding.recyclerStations.setAdapter(hydroAdapter);

                binding.viewFlipper.setDisplayedChild(1);
                //binding.recyclerStations.setVisibility(View.VISIBLE);
                //binding.recyclerContents.setVisibility(View.GONE);
                progbtnList.get(3).stopProgress();
            }

            //binding.fab.setVisibility(View.GONE);
            binding.appbar.setExpanded(true, true);
            //binding.fab.setVisibility(View.VISIBLE);
            //showCollapsedStatusBar(1);
            statusbar = 0;
        });

        stationModel.getHydroExceptionMessage().observe(this, err -> {
            log.e("exception in Hydro");
            binding.tvEmptyview.setText(err);
            binding.viewFlipper.setDisplayedChild(2);

            //prevBtnId = -1;
            progbtnList.get(3).resetProgress();

            hydroTask = null;
        });

        /*
        hydroObserver = new Observer<List<StationHydroRunnable.HydroStationObj>>() {
            @Override
            public void onChanged(List<StationHydroRunnable.HydroStationObj> hydroList) {
                log.i("Hydro invokded by Observer");
                if(hydroList != null && hydroList.size() > 0) {
                    hydroAdapter = new StationHydroAdapter(hydroList);
                    binding.recyclerStations.setAdapter(hydroAdapter);

                    binding.viewFlipper.setDisplayedChild(1);
                    //binding.recyclerStations.setVisibility(View.VISIBLE);
                    //binding.recyclerContents.setVisibility(View.GONE);
                    progbtnList.get(3).stopProgress();
                    stationModel.getHydroStationList().removeObserver(this);
                }
                //binding.fab.setVisibility(View.GONE);
                binding.appbar.setExpanded(true, true);
                //binding.fab.setVisibility(View.VISIBLE);
                //showCollapsedStatusBar(1);
                statusbar = 0;
            }
        };
        stationModel.getHydroStationList().observe(this, hydroObserver);

         */

    }

    private void locateSvcStations(Location location) {
        log.i("Service Stations: %s", location);
    }

    // Reorder near station list according to the distance/price, which is called from the layout
    // file as well.
    public void onFabClicked(View view) {
        bStnOrder = !bStnOrder;
        // Switch the FAB background.
        int res = (bStnOrder) ? R.drawable.bg_location : R.drawable.bg_currency_won;
        binding.fab.setImageDrawable(ContextCompat.getDrawable(this, res));

        gasStationList = gasListAdapter.sortStationList(bStnOrder);
        gasListAdapter.setStationList(gasStationList);
        gasListAdapter.notifyItemRangeChanged(0, gasStationList.size());
        //gasListAdapter.submitGasList(gasStationList);

        binding.appbar.setExpanded(true, true);
        binding.recyclerStations.smoothScrollToPosition(0);
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
        /*
        isStnViewOn = binding.recyclerStations.getVisibility() == View.VISIBLE;
        if(isStnViewOn) {
            //binding.stationRecyclerView.setVisibility(View.GONE);
            binding.viewFlipper.setDisplayedChild(0);
            //binding.recyclerStations.setVisibility(View.GONE);
            //binding.recyclerContents.setVisibility(View.VISIBLE);
            binding.fab.setVisibility(View.GONE);
            //binding.progbtnGas.setProgress(isStnViewOn);
            progbtnList.get(0).stopProgress();
        }

         */

        Intent resultIntent = result.getData();
        if(resultIntent == null) return;
        switch(result.getResultCode()) {
            case Constants.REQUEST_MAIN_EXPENSE_TOTAL: // ExpenseActivity result
                int total = resultIntent.getIntExtra("expense", 0);
                int content = MainContentAdapter.ContentType.VIEWPAGER_EXPENSE.ordinal();
                mainContentAdapter.notifyItemChanged(content, total);
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

