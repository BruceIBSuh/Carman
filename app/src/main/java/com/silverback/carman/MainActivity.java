package com.silverback.carman;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
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
    private String[] stnParams;
    private String gasCode;
    private boolean isRadiusChanged;
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
        stnParams = getNearStationParams();// 0: gas 1:district 2:order(distance or price)
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
        binding.stationRecyclerView.addOnScrollListener(stationScrollListener);

        // Method for implementing ViewModel callbacks to fetch a location and station list around
        // the location.
        callbackViewModel(locationModel);
        callbackViewModel(stnModel);

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
        //startActivity(intent);
    }

    // Implement AdapterView.OnItemSelectedListener
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        switch(pos){
            case 0: stnParams[0] = "B027"; break; // gasoline
            case 1: stnParams[0] = "D047"; break; // diesel
            case 2: stnParams[0] = "K015"; break; // LPG
            case 3: stnParams[0] = "B034"; break; // premium gasoline
        }

        // Prevent this callback from auto implementing at the first initiation.
        if(gasCode != null) {
            log.i("stnparams: %s", stnParams[0]);
            mainPricePagerAdapter.setFuelCode(stnParams[0]);
            mainPricePagerAdapter.notifyDataSetChanged();
        }

        // Show the average price and create the price bar as hidden.
        gasCode = stnParams[0];
        binding.mainTopFrame.avgPriceView.addPriceView(gasCode);
        setCollapsedPriceBar();

        boolean isStnViewOn = binding.stationRecyclerView.getVisibility() == View.VISIBLE;
        if(isStnViewOn) {
            stationListTask = sThreadManager.startStationListTask(stnModel, mPrevLocation, stnParams);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        log.i("gas code: ", gasCode);
    }

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

    // Get the default fuel value
    private void setGasSpinnerSelection(String gasCode) {
        String[] arrGasCode = getResources().getStringArray(R.array.spinner_fuel_code);
        for(int i = 0; i < arrGasCode.length; i++) {
            if(arrGasCode[i].matches(gasCode)) {
                binding.mainTopFrame.spinnerGas.setSelection(i);
                //this.gasCode = gasCode;
                break;
            }
        }
    }

    // Implement ActivityResultCallback<Intent> defined as a param in registerForActivityResult.
    private void callActivityResult(ActivityResult result) {
        switch(result.getResultCode()) {
            case Activity.RESULT_OK:
                updateSettingResult(result);
                break;
            case Activity.RESULT_CANCELED:
                log.i("from which activity: %s", result.getData());
                Intent resultIntent = result.getData();
                if(resultIntent != null) {
                    int totalSum = resultIntent.getIntExtra("totalsum", 0);
                    log.i("expense total: %s", totalSum);
                    mainContentAdapter.notifyItemChanged(Constants.VIEWPAGER_EXPENSE, totalSum);
                }

                binding.stationRecyclerView.setVisibility(View.GONE);
                binding.fab.setVisibility(View.GONE);
                binding.recyclerContents.setVisibility(View.VISIBLE);
                break;
        }
    }

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
            ObjectAnimator anim = ObjectAnimator.ofFloat(binding.pricebar.getRoot(), "alpha", 0f, 1f);
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
            if (newState == RecyclerView.SCROLL_STATE_IDLE) binding.fab.show();
            super.onScrollStateChanged(recyclerView, newState);
        }
    };

    // Callback for the station button click listener defined in the layout file
    public void dispNearStations(View view) {
        boolean isStnViewOn = binding.stationRecyclerView.getVisibility() == View.VISIBLE;
        if(!isStnViewOn) {
            checkRuntimePermission(binding.getRoot(), Manifest.permission.ACCESS_FINE_LOCATION, () -> {
                locationTask = sThreadManager.fetchLocationTask(this, locationModel);
                //binding.pbNearStns.setVisibility(View.VISIBLE);
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


    // Method for implementing the ActivityResult callback the result of which is sent from
    // SettingPrefActivity.
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
            setGasSpinnerSelection(gasType);

        } else if(!TextUtils.isEmpty(searchRadius)) {
            isRadiusChanged = true;
            stnParams[1] = searchRadius;
        }
    }

    // Method for implementing ViewModel callbacks to fetch a location and station list around
    // the location.
    private void callbackViewModel(ViewModel model) {
        if(model instanceof LocationViewModel) {
            locationModel.getLocation().observe(this, location -> {
                log.i("Location compared: %s, %s, %s", stnParams[1], mPrevLocation, location);
                if(mPrevLocation == null || mPrevLocation.distanceTo(location) > Constants.UPDATE_DISTANCE || isRadiusChanged) {
                    binding.pbNearStns.setVisibility(View.VISIBLE);
                    mPrevLocation = location;
                    stationListTask = sThreadManager.startStationListTask(stnModel, location, stnParams);
                } else {
                    binding.recyclerContents.setVisibility(View.GONE);
                    binding.stationRecyclerView.setVisibility(View.VISIBLE);
                    //binding.pbNearStns.setVisibility(View.GONE);
                }
            });

            locationModel.getLocationException().observe(this, exception -> {
                log.i("Exception occurred while fetching location");
                SpannableString spannableString = new SpannableString(getString(R.string.general_no_location));
                binding.pbNearStns.setVisibility(View.GONE);
                binding.stationRecyclerView.setVisibility(View.VISIBLE);
                binding.stationRecyclerView.showTextView(spannableString);

            });

        } else if(model instanceof StationListViewModel) {
            // Receive station(s) within the radius. If no stations exist, post the message that
            // indicate why it failed to fetch stations. It would be caused by any network problem or
            // no stations actually exist within the radius.
            stnModel.getNearStationList().observe(this, stnList -> {
                if (stnList != null && stnList.size() > 0) {
                    log.i("near stations: %s", stnList.size());
                    mStationList = stnList;
                    stnListAdapter = new StationListAdapter(mStationList, this);

                    binding.recyclerContents.setVisibility(View.GONE);
                    binding.stationRecyclerView.setVisibility(View.VISIBLE);

                    binding.stationRecyclerView.setAdapter(stnListAdapter);
                    binding.stationRecyclerView.showStationListRecyclerView();
                    binding.fab.setVisibility(View.VISIBLE);

                } else {
                    log.i("no station");
                    // No near stations post an message that contains the clickable span to link to the
                    // SettingPreferenceActivity for resetting the searching radius.
                    //SpannableString spannableString = handleStationListException();
                    //stationRecyclerView.showTextView(spannableString);
                }

                isRadiusChanged = false;
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
}

