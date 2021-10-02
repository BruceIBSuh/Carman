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
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

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
        AdapterView.OnItemSelectedListener {

    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity.class);

    // Objects
    private ActivityMainBinding binding;
    private LocationViewModel locationModel;
    private StationListViewModel stnModel;
    private ImageViewModel imgModel;

    private LocationTask locationTask;
    private StationListTask stationListTask;
    private GasPriceTask gasPriceTask;
    private StationListAdapter stnListAdapter;
    private MainPricePagerAdapter mainPricePagerAdapter;

    private Location mPrevLocation;
    private List<Opinet.GasStnParcelable> mStationList;

    private ApplyImageResourceUtil imgResUtil;
    private MainContentAdapter mainContentAdapter;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    // Fields
    private String[] arrGasCode;
    private String[] stnParams;
    private String gasCode;
    private boolean isRadiusChanged, isGasTypeChanged, isStnViewOn;
    private boolean hasStationInfo = false;
    private boolean bStnOrder = false; // false: distance true:price


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Create objects
        imgResUtil = new ApplyImageResourceUtil(this);

        // Set initial values
        stnParams = getNearStationParams();// 0:gas type 1:radius 2:order(distance or price)
        arrGasCode = getResources().getStringArray(R.array.spinner_fuel_code);
        mPrevLocation = null;

        // Set the toolbar with icon, titile. The OptionsMenu are defined below to override
        // methods.
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(false);
        String title = mSettings.getString(Constants.USER_NAME, "Carman");
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);
        binding.appbar.addOnOffsetChangedListener((appbar, offset) -> showCollapsedPricebar(offset));

        // AdapterView(Spinner) to select a gas type.
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this, R.array.spinner_fuel_name, R.layout.spinner_main_fuel);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_main_dropdown);
        binding.mainTopFrame.spinnerGas.setAdapter(spinnerAdapter);

        // MainContent RecyclerView to display main contents in the activity
        mainContentAdapter = new MainContentAdapter(this);
        RecyclerDividerUtil divider = new RecyclerDividerUtil(
                Constants.DIVIDER_HEIGHT_MAIN, 0, getColor(R.color.recyclerDivider));
        binding.recyclerContents.setAdapter(mainContentAdapter);
        binding.recyclerContents.addItemDecoration(divider);

        // ViewModels
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
        stnModel = new ViewModelProvider(this).get(StationListViewModel.class);
        imgModel = new ViewModelProvider(this).get(ImageViewModel.class);

        // Create MainPricePagerAdapter and set it to the viewpager.
        mainPricePagerAdapter = new MainPricePagerAdapter(this);
        mainPricePagerAdapter.setFuelCode(stnParams[0]);
        binding.mainTopFrame.viewpagerPrice.setAdapter(mainPricePagerAdapter);

        // Event Handlers
        binding.mainTopFrame.spinnerGas.setOnItemSelectedListener(this);
        binding.stationRecyclerView.getRecyclerView().addOnScrollListener(stationScrollListener);


        // Method for implementing ViewModel callbacks to fetch a location and station list around
        // the location.
        observeViewModel(locationModel);
        observeViewModel(stnModel);

        // Create ActivityResultLauncher to call SettingActiity and get results
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), this::callActivityResult);
    }



    @Override
    public void onResume() {
        super.onResume();
        // Set the ion in the toolbar
        String userImg = mSettings.getString(Constants.USER_IMAGE, null);
        String imgUri = (TextUtils.isEmpty(userImg))?Constants.imgPath + "ic_user_blank_gray":userImg;

        imgResUtil.applyGlideToDrawable(imgUri, Constants.ICON_SIZE_TOOLBAR_USERPIC, imgModel);
        imgModel.getGlideDrawableTarget().observe(this, resource -> {
            if(getSupportActionBar() != null) getSupportActionBar().setIcon(resource);
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if(locationTask != null) locationTask = null;
        if(stationListTask != null) stationListTask = null;
        if(gasPriceTask != null) gasPriceTask = null;
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
            //startActivity(new Intent(this, ExpenseActivity.class));
            activityResultLauncher.launch(new Intent(this, ExpenseActivity.class));
        } else if(item.getItemId() == R.id.action_board) {
            startActivity(new Intent(this, BoardActivity.class));
        } else if(item.getItemId() == R.id.action_login) {
            log.i("login process required");
        } else if(item.getItemId() == R.id.action_setting) {
            Intent settingIntent = new Intent(this, SettingPrefActivity.class);
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

    // Implement AdapterView.OnItemSelectedListener
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        if(gasCode == null) setGasSpinnerSelection(stnParams[0]);
        else gasCode = arrGasCode[pos];

        // Reset the price info in the viewpager.
        mainPricePagerAdapter.setFuelCode(gasCode);
        mainPricePagerAdapter.notifyDataSetChanged();

        // Show the average price and create the price bar as hidden.
        binding.mainTopFrame.avgPriceView.addPriceView(gasCode);
        setCollapsedPriceBar();

        // In case the station recyclerview is in the foreground.
        isStnViewOn = binding.stationRecyclerView.getVisibility() == View.VISIBLE;
        if(isStnViewOn) {
            stnParams[0] = gasCode;
            stationListTask = sThreadManager.startStationListTask(stnModel, mPrevLocation, stnParams);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {}

    // The following 2 methods implement FinishAppDialogFragment.NoticeDialogListener interface ;
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        File cacheDir = getCacheDir();
        if(cacheDir != null && checkPriceUpdate()) {
            for(File file : Objects.requireNonNull(cacheDir.listFiles())) file.delete();
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

    // Implement ActivityResultCallback<Intent> defined as a param in registerForActivityResult.
    private void callActivityResult(ActivityResult result) {
        // In case the station reyelcerview is in the visible state, it should be gone to the initlal
        // state.
        isStnViewOn = binding.stationRecyclerView.getVisibility() == View.VISIBLE;
        if(isStnViewOn) {
            binding.stationRecyclerView.setVisibility(View.GONE);
            binding.fab.setVisibility(View.GONE);
            binding.recyclerContents.setVisibility(View.VISIBLE);
            binding.btnToggleStation.setChecked(false);
        }

        switch(result.getResultCode()) {
            case Activity.RESULT_OK: // SettingPrefActivity result
                updateSettingResult(result);
                break;

            case Activity.RESULT_CANCELED: // ExpenseActivity result
                Intent resultIntent = result.getData();
                if(resultIntent != null) {
                    int totalSum = resultIntent.getIntExtra("totalsum", 0);
                    mainContentAdapter.notifyItemChanged(Constants.VIEWPAGER_EXPENSE, totalSum);
                }
                break;
        }
    }

    // Get the price info saved in the cache and show them in the price bar.
    private void setCollapsedPriceBar() {
        final String[] arrFile = {Constants.FILE_CACHED_SIDO_PRICE, Constants.FILE_CACHED_SIGUN_PRICE };
        String avgPrice = String.valueOf(binding.mainTopFrame.avgPriceView.getAvgGasPrice());
        binding.pricebar.tvCollapsedAvgPrice.setText(avgPrice);

        // Set the sido and sigun price which have been intervally stored in the Cache
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
    private final RecyclerView.OnScrollListener stationScrollListener = new RecyclerView.OnScrollListener(){
        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            //if (dy > 0 || dy < 0 && binding.fab.isShown()) binding.fab.hide();
        }

        @Override
        public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            if (newState == RecyclerView.SCROLL_STATE_IDLE){
                binding.fab.show();
            }
            super.onScrollStateChanged(recyclerView, newState);
        }
    };

    // Implement the onClickListener of the toggle button which is defined in the xml file
    public void locateNearStations(View view) {
        isStnViewOn = binding.stationRecyclerView.getVisibility() == View.VISIBLE;
        if(!isStnViewOn) {
            checkRuntimePermission(binding.getRoot(), Manifest.permission.ACCESS_FINE_LOCATION, () -> {
                locationTask = sThreadManager.fetchLocationTask(this, locationModel);
                binding.pbNearStns.setVisibility(View.VISIBLE);
            });
        } else {
            binding.stationRecyclerView.setVisibility(View.GONE);
            binding.fab.setVisibility(View.GONE);
            binding.recyclerContents.setVisibility(View.VISIBLE);
        }
    }

    // Reorder near station list according to the distance/price, which is called from the layout
    // file as well.
    public void switchNearStationOrder(View view) {
        bStnOrder = !bStnOrder;
        Uri uri = saveNearStationList(mStationList);
        if(uri == null) return;

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
                    stnParams[0] = gasCode;
                    stationListTask = sThreadManager.startStationListTask(stnModel, location, stnParams);

                // Station default params changed from SettingPrefActivity.
                } else if(isRadiusChanged || isGasTypeChanged) {
                    log.i("params changed: %s, %s", stnParams[0], stnParams[1]);
                    stationListTask = sThreadManager.startStationListTask(stnModel, location, stnParams);

                } else {
                    binding.pbNearStns.setVisibility(View.GONE);
                    binding.recyclerContents.setVisibility(View.GONE);
                    binding.stationRecyclerView.setVisibility(View.VISIBLE);
                }
            });

            locationModel.getLocationException().observe(this, exception -> {
                log.i("Exception occurred while fetching location");
                SpannableString spannableString = new SpannableString(getString(R.string.general_no_location));
                binding.pbNearStns.setVisibility(View.GONE);
                binding.stationRecyclerView.setVisibility(View.VISIBLE);
                binding.stationRecyclerView.showSpannableTextView(spannableString);

            });

        } else if(model instanceof StationListViewModel) {
            stnModel.getNearStationList().observe(this, stnList -> {
                binding.recyclerContents.setVisibility(View.GONE);
                binding.stationRecyclerView.setVisibility(View.VISIBLE);
                binding.btnToggleStation.setChecked(true);

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
                binding.pbNearStns.setVisibility(View.GONE);
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
            String radius = stnParams[1];
            // In case the radius is already set to the maximum value(5000m), no need to change the value.
            if(radius != null && radius.matches("5000")) {
                msg = msg.substring(0, msg.indexOf("\n"));
                return new SpannableString(stnParams[1] + msg);
            }

            String format = String.format("%s%s", radius, msg);

            spannableString = new SpannableString(format);
            log.i("spannable string: %s", spannableString);
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
                    Intent settingIntent = new Intent(MainActivity.this, SettingPrefActivity.class);
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

    // Implement ActivityResult callback, the result of which is sent from SettingPrefActivity.
    private void updateSettingResult(ActivityResult result) {
        Intent resultIntent = result.getData();
        if(resultIntent == null) return;

        String userName = resultIntent.getStringExtra("userName");
        String district = resultIntent.getStringExtra("distCode");
        String gasType = resultIntent.getStringExtra("gasCode");
        String searchRadius = resultIntent.getStringExtra("searchRadius");
        log.i("searchRadius: %s", searchRadius);

        if(!TextUtils.isEmpty(userName)) Objects.requireNonNull(getSupportActionBar()).setTitle(userName);
        if(!TextUtils.isEmpty(district) && !TextUtils.isEmpty(gasType)) {
            log.i("both changed");
            OpinetViewModel opinetModel = new ViewModelProvider(this).get(OpinetViewModel.class);
            gasPriceTask = sThreadManager.startGasPriceTask(this, opinetModel, district, gasType);

            opinetModel.distPriceComplete().observe(this, isDone -> {
                mainPricePagerAdapter.notifyDataSetChanged();
                setGasSpinnerSelection(gasType);
                //setCollapsedPriceBar();
            });

        } else if(!TextUtils.isEmpty(district) && TextUtils.isEmpty(gasType)) {
            log.i("district changed");
            OpinetViewModel opinetModel = new ViewModelProvider(this).get(OpinetViewModel.class);
            gasPriceTask = sThreadManager.startGasPriceTask(this, opinetModel, district, gasCode);

            opinetModel.distPriceComplete().observe(this, isDone -> {
                mainPricePagerAdapter.notifyDataSetChanged();
                setCollapsedPriceBar();
            });

        } else if(!TextUtils.isEmpty(gasType) && TextUtils.isEmpty(district)) {
            isGasTypeChanged = true;
            setGasSpinnerSelection(gasType);
            stnParams[0] = gasType;

        } else if(!TextUtils.isEmpty(searchRadius)) {
            isRadiusChanged = true;
            stnParams[1] = searchRadius;
        }
    }


}

