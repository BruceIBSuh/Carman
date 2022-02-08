package com.silverback.carman;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
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

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.silverback.carman.adapters.MainContentAdapter;
import com.silverback.carman.adapters.MainPricePagerAdapter;
import com.silverback.carman.adapters.StationListAdapter;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.databinding.ActivityMainBinding;
import com.silverback.carman.fragments.FinishAppDialogFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.GasPriceTask;
import com.silverback.carman.threads.LocationTask;
import com.silverback.carman.threads.StationListTask;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.utils.RecyclerDividerUtil;
import com.silverback.carman.viewmodels.ImageViewModel;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.OpinetViewModel;
import com.silverback.carman.viewmodels.StationListViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Objects;



public class MainActivity extends BaseActivity implements
        StationListAdapter.OnRecyclerItemClickListener,
        FinishAppDialogFragment.NoticeDialogListener,
        MainContentAdapter.MainContentAdapterListener,
        AdapterView.OnItemSelectedListener {

    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity.class);

    // Objects
    private ActivityMainBinding binding;

    private LocationViewModel locationModel;
    private StationListViewModel stnModel;
    private ImageViewModel imgModel;
    private OpinetViewModel opinetModel;

    private LocationTask locationTask;
    private StationListTask stationListTask;
    private GasPriceTask gasPriceTask;

    private MainContentAdapter mainContentAdapter;
    private StationListAdapter stnListAdapter;
    private MainPricePagerAdapter mainPricePagerAdapter;

    private Location mPrevLocation;
    private List<Opinet.GasStnParcelable> mStationList;
    private ApplyImageResourceUtil imgResUtil;


    // Fields
    private String[] arrGasCode;
    private String[] defaultParams;
    private String gasCode;
    private boolean isRadiusChanged, isGasTypeChanged, isStnViewOn;
    private boolean hasStationInfo = false;
    private boolean bStnOrder = false; // false: distance true:price


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
        mPrevLocation = null;

        // MainContent RecyclerView to display main content feeds in the activity
        mainContentAdapter = new MainContentAdapter(MainActivity.this, this);
        RecyclerDividerUtil divider = new RecyclerDividerUtil(
                Constants.DIVIDER_HEIGHT_MAIN, 0, getColor(R.color.recyclerDivider));
        binding.recyclerContents.setAdapter(mainContentAdapter);
        binding.recyclerContents.addItemDecoration(divider);

        // AdapterView(Spinner) to select a gas type.
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this, R.array.spinner_fuel_name, R.layout.spinner_main_fuel);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_main_dropdown);
        binding.mainTopFrame.spinnerGas.setAdapter(spinnerAdapter);
        setGasSpinnerSelection(defaultParams[0]);

        // Create MainPricePagerAdapter which displays the graphs for the last 3 month total expense
        // and the expense configuration of this month. More pages should be added to analyze the
        // user expense.
        mainPricePagerAdapter = new MainPricePagerAdapter(this);
        mainPricePagerAdapter.setFuelCode(defaultParams[0]);
        binding.mainTopFrame.viewpagerPrice.setAdapter(mainPricePagerAdapter);
        binding.mainTopFrame.avgPriceView.addPriceView(defaultParams[0]);

        // ViewModels
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
        stnModel = new ViewModelProvider(this).get(StationListViewModel.class);
        imgModel = new ViewModelProvider(this).get(ImageViewModel.class);
        opinetModel = new ViewModelProvider(this).get(OpinetViewModel.class);

        imgResUtil = new ApplyImageResourceUtil(this);

        // Event Handlers
        binding.appbar.addOnOffsetChangedListener((appbar, offset) -> showCollapsedPricebar(offset));
        binding.mainTopFrame.spinnerGas.setOnItemSelectedListener(this);
        binding.stationRecyclerView.getRecyclerView().addOnScrollListener(scrollListener);

        // Method for implementing ViewModel callbacks to fetch a location and near station list.
        observeViewModel(locationModel);
        observeViewModel(stnModel);
    }

    @Override
    public void onResume() {
        super.onResume();
        //binding.mainTopFrame.viewpagerPrice.registerOnPageChangeCallback(pagerCallback);

        String userImg = mSettings.getString(Constants.USER_IMAGE, null);
        String imgUri = (TextUtils.isEmpty(userImg))?Constants.imgPath + "ic_user_blank_gray":userImg;
        imgResUtil.applyGlideToDrawable(imgUri, Constants.ICON_SIZE_TOOLBAR_USERPIC, imgModel);
        imgModel.getGlideDrawableTarget().observe(this, resource -> {
            if(getSupportActionBar() != null) getSupportActionBar().setIcon(resource);
        });

        // Return the fuel price pager to the first page.
        binding.mainTopFrame.viewpagerPrice.setCurrentItem(0, true);

    }

    @Override
    public void onPause() {
        super.onPause();
        //binding.mainTopFrame.viewpagerPrice.unregisterOnPageChangeCallback(pagerCallback);
    }

    @Override
    public void onStop() {
        //activityResultLauncher.unregister();
        if(locationTask != null) locationTask = null;
        if(stationListTask != null) stationListTask = null;
        //if(gasPriceTask != null) gasPriceTask = null;
        super.onStop();
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
            Intent expenseIntent = new Intent(this, ExpenseActivity.class);
            expenseIntent.putExtra("caller", Constants.REQUEST_MAIN_EXPENSE_TOTAL);
            activityResultLauncher.launch(expenseIntent);
        } else if(item.getItemId() == R.id.action_board) {
            startActivity(new Intent(this, BoardActivity.class));
        } else if(item.getItemId() == R.id.action_login) {
            log.i("login process required");
        } else if(item.getItemId() == R.id.action_setting) {
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

    // Implement StationListAdapter.OnRecyclerItemClickListener
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
        gasCode = (defaultParams[0].matches(arrGasCode[pos]))?defaultParams[0]:arrGasCode[pos];
        mainPricePagerAdapter.setFuelCode(gasCode);
        binding.mainTopFrame.avgPriceView.addPriceView(gasCode);
        mainPricePagerAdapter.notifyItemRangeChanged(0, mainPricePagerAdapter.getItemCount(), gasCode);
        // Update the average gas price and the hidden price bar.
        setCollapsedPriceBar();
        // As far as the near-station recyclerview is in the foreground, update the price info with
        // a new gas selected. refactor required: any station with a selected gas type does not
        // exist, indicate it in the view.
        isStnViewOn = binding.stationRecyclerView.getVisibility() == View.VISIBLE;
        if(isStnViewOn) {
            defaultParams[0] = gasCode;
            stationListTask = sThreadManager.startStationListTask(stnModel, mPrevLocation, defaultParams);
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    // Implement FinishAppDialogFragment.NoticeDialogListener interface ;
    //@SuppressWarnings("ResultOfMethodCallIgnored")
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
    // Animate the visibility of the collapsed price bar.
    private void showCollapsedPricebar(int offset) {
        if(Math.abs(offset) == binding.appbar.getTotalScrollRange()) {
            binding.pricebar.getRoot().setVisibility(View.VISIBLE);
            ObjectAnimator anim = ObjectAnimator.ofFloat(binding.pricebar.getRoot(), "alpha", 0, 1);
            anim.setDuration(500);
            anim.start();
        } else binding.pricebar.getRoot().setVisibility(View.INVISIBLE);
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

    // Implement onClickListener of the toggle button which is defined in the xml file.
    public void locateNearStations(View view) {
        isStnViewOn = binding.stationRecyclerView.getVisibility() == View.VISIBLE;
        if(!isStnViewOn) {
            checkRuntimePermission(binding.getRoot(), Manifest.permission.ACCESS_FINE_LOCATION, ()->{
                binding.progbtnGas.setProgressColor(isStnViewOn);
                locationTask = sThreadManager.fetchLocationTask(this, locationModel);
            });

        } else {
            binding.stationRecyclerView.setVisibility(View.GONE);
            binding.fab.setVisibility(View.GONE);
            binding.recyclerContents.setVisibility(View.VISIBLE);
            binding.progbtnGas.setProgressColor(isStnViewOn);

            // Return the viewpagers to the initial page.
            binding.mainTopFrame.viewpagerPrice.setCurrentItem(0, true);
            mainContentAdapter.notifyItemChanged(Constants.VIEWPAGER_EXPENSE, 0);
        }
    }

    public void locateNearServices(View view) {
        log.i("method for locating near servce centers");
    }

    // Reorder near station list according to the distance/price, which is called from the layout
    // file as well.
    public void switchNearStationOrder(View view) {
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


    // Method for implementing ViewModel callbacks to fetch a location and station list around
    // the location.
    private void observeViewModel(ViewModel model) {
        if(model instanceof LocationViewModel) {
            locationModel.getLocation().observe(this, location -> {
                // Location fetched or changed at a preset distance.
                if(mPrevLocation == null || mPrevLocation.distanceTo(location) > Constants.UPDATE_DISTANCE ){
                    //binding.pbNearStns.setVisibility(View.VISIBLE);
                    mPrevLocation = location;
                    defaultParams[0] = gasCode;
                    stationListTask = sThreadManager.startStationListTask(stnModel, location, defaultParams);

                // Station default params changed from SettingPrefActivity.
                } else if(isRadiusChanged || isGasTypeChanged) {
                    log.i("params changed: %s, %s", defaultParams[0], defaultParams[1]);
                    stationListTask = sThreadManager.startStationListTask(stnModel, location, defaultParams);

                } else {
                    //binding.pbNearStns.setVisibility(View.GONE);
                    binding.recyclerContents.setVisibility(View.GONE);
                    binding.stationRecyclerView.setVisibility(View.VISIBLE);
                    binding.fab.setVisibility(View.VISIBLE);
                    binding.progbtnGas.setProgressColor(true);
                }
            });

            locationModel.getLocationException().observe(this, exception -> {
                log.i("Exception occurred while fetching location");
                SpannableString spannableString = new SpannableString(getString(R.string.general_no_location));
                //binding.pbNearStns.setVisibility(View.GONE);
                binding.stationRecyclerView.setVisibility(View.VISIBLE);
                binding.stationRecyclerView.showSpannableTextView(spannableString);

            });

        } else if(model instanceof StationListViewModel) {
            stnModel.getNearStationList().observe(this, stnList -> {
                binding.recyclerContents.setVisibility(View.GONE);
                binding.stationRecyclerView.setVisibility(View.VISIBLE);
                //binding.btnToggleStation.setChecked(true);

                if (stnList != null && stnList.size() > 0) {
                    log.i("near stations: %s", stnList.size());
                    mStationList = stnList;
                    stnListAdapter = new StationListAdapter(mStationList, this);
                    binding.stationRecyclerView.getRecyclerView().setAdapter(stnListAdapter);
                    binding.stationRecyclerView.showStationRecyclerView();
                    binding.fab.setVisibility(View.VISIBLE);

                } else {
                    // No near stations post an message that contains the clickable span to link to the
                    // SettingPreferenceActivity for resetting the searching radius.
                    SpannableString spannableString = handleStationListException();
                    binding.stationRecyclerView.showSpannableTextView(spannableString);
                }

                isRadiusChanged = false;
                isGasTypeChanged = false;
                //binding.pbNearStns.setVisibility(View.GONE);
                binding.progbtnGas.setProgressColor(true);
            });

            // Update the carwash info to StationList and notify the data change to Adapter.
            // Adapter should not assume that the payload will always be passed to onBindViewHolder()
            // e.g. when the view is not attached.
            stnModel.getStationCarWashInfo().observe(this, sparseArray -> {
                for(int i = 0; i < sparseArray.size(); i++) {
                    mStationList.get(i).setIsWash(sparseArray.valueAt(i));
                    stnListAdapter.notifyItemChanged(sparseArray.keyAt(i), sparseArray.valueAt(i));
                }
                // To notify that fetching station list has completed.
                hasStationInfo = true;
            });
        }
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
        log.i("activity result: %s", result);
        // If the station reyelcerview is in the visible state, it should be gone
        isStnViewOn = binding.stationRecyclerView.getVisibility() == View.VISIBLE;
        log.i("isStnView in callActivityResult:%s",isStnViewOn);
        if(isStnViewOn) {
            binding.stationRecyclerView.setVisibility(View.GONE);
            binding.fab.setVisibility(View.GONE);
            binding.recyclerContents.setVisibility(View.VISIBLE);
            binding.progbtnGas.setProgressColor(isStnViewOn);
        }

        Intent resultIntent = result.getData();
        if(resultIntent == null) return;

        switch(result.getResultCode()) {
            case Activity.RESULT_OK: // SettingActivity result
                log.i("activity result ok?");
                updateSettingResult(result);
                break;

            case Constants.REQUEST_MAIN_EXPENSE_TOTAL: // ExpenseActivity result
                int total = resultIntent.getIntExtra("expense", 0);
                mainContentAdapter.notifyItemChanged(Constants.VIEWPAGER_EXPENSE, total);
                break;

            case Constants.REQUEST_MAIN_SETTING_GENERAL:
                log.i("ActivityResult from Setting");
                updateSettingResult(result);
                break;
        }
    }

    // Get the price info saved in the cache and show them in the price bar.
    private void setCollapsedPriceBar() {
        final String[] arrFile = {Constants.FILE_CACHED_SIDO_PRICE, Constants.FILE_CACHED_SIGUN_PRICE };
        String avgPrice = String.valueOf(binding.mainTopFrame.avgPriceView.getAvgGasPrice());
        binding.pricebar.tvCollapsedAvgPrice.setText(avgPrice);
        // Set the sido and sigun price which is stored in the cache at an interval.
        for(String fName : arrFile) {
            File file = new File(getCacheDir(), fName);
            Uri uri = Uri.fromFile(file);
            try(InputStream is = getContentResolver().openInputStream(uri);
                ObjectInputStream ois = new ObjectInputStream(is)) {
                Object obj = ois.readObject();
                Iterable<?> itr = (Iterable<?>)obj;
                for(Object x : itr) {
                    switch(fName) {
                        case Constants.FILE_CACHED_SIDO_PRICE:
                            Opinet.SidoPrice sido = (Opinet.SidoPrice)x;
                            if(sido.getProductCd().matches(gasCode)) {
                                binding.pricebar.tvCollapsedSido.setText(sido.getSidoName());
                                binding.pricebar.tvCollapsedSidoPrice.setText(String.valueOf(sido.getPrice()));
                            }
                            break;

                        case Constants.FILE_CACHED_SIGUN_PRICE:
                            Opinet.SigunPrice sigun = (Opinet.SigunPrice)x;
                            if(sigun.getProductCd().matches(gasCode)) {
                                binding.pricebar.tvCollapsedSigun.setText(sigun.getSigunName());
                                binding.pricebar.tvCollapsedSigunPrice.setText(String.valueOf(sigun.getPrice()));
                            }
                            break;
                    }
                }

            } catch (IOException | ClassNotFoundException e) { e.printStackTrace();}
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
            log.i("district: %s", district);
            gasPriceTask = ThreadManager2.startGasPriceTask(this, opinetModel, district);
            opinetModel.distPriceComplete().observe(this, isDone -> {
                log.i("viewmodel");
                if(isDone) {
                    if(!TextUtils.isEmpty(gasType)) {
                        isGasTypeChanged = true;
                        setGasSpinnerSelection(gasType);
                        defaultParams[0] = gasType;
                        mainPricePagerAdapter.notifyItemChanged(0, gasType);
                    }

                    setCollapsedPriceBar();
                    mainPricePagerAdapter.notifyItemRangeChanged(
                            0, mainPricePagerAdapter.getItemCount(), gasCode);
                }
            });
        }

        /*
        if(!TextUtils.isEmpty(gasType)) {
            isGasTypeChanged = true;
            setGasSpinnerSelection(gasType);
            defaultParams[0] = gasType;
            mainPricePagerAdapter.notifyItemChanged(0, gasType);
        }

        // Update the price in the viewpager, which depends upon whether either district or gas type
        // or both changes.
        if(district != null && gasType != null) {
            gasPriceTask = sThreadManager.startGasPriceTask(this, opinetModel, district);
            opinetModel.distPriceComplete().observe(this, isDone -> {
                if(isDone) {
                    mainPricePagerAdapter.notifyItemChanged(0, gasType);
                    setGasSpinnerSelection(gasType);
                    setCollapsedPriceBar();
                }
            });

        } else if (district != null) {
            gasPriceTask = sThreadManager.startGasPriceTask(this, opinetModel, district);
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

    // Implement MainContentAdapter.MainContentAdapterListener for the buttons defined in the
    // notification and the postings feed.
    @Override
    public void onClickPostingIcon(int category) {
        Intent boardIntent = new Intent(this, BoardActivity.class);
        boardIntent.putExtra("category", category);
        startActivity(boardIntent);
    }
}

