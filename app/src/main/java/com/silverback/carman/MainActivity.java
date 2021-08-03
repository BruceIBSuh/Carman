package com.silverback.carman;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman.adapters.MainContentAdapter;
import com.silverback.carman.adapters.PricePagerAdapter;
import com.silverback.carman.adapters.StationListAdapter;
import com.silverback.carman.database.CarmanDatabase;
import com.silverback.carman.databinding.ActivityMainBinding;
import com.silverback.carman.fragments.FinishAppDialogFragment;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.LocationTask;
import com.silverback.carman.threads.StationListTask;
import com.silverback.carman.threads.ThreadManager;
import com.silverback.carman.threads.ThreadManager2;
import com.silverback.carman.utils.ApplyImageResourceUtil;
import com.silverback.carman.utils.CarmanLocationHelper;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.ImageViewModel;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.StationListViewModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
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

    private PricePagerAdapter pricePagerAdapter;
    private StationListAdapter stnListAdapter;

    private Location mPrevLocation;
    private List<Opinet.GasStnParcelable> mStationList;

    private ApplyImageResourceUtil imgResUtil;
    private ActivityResultLauncher<Intent> activityResultLauncher;

    // Fields
    private String[] defaults;
    private String defaultFuel;
    private boolean hasStationInfo = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View rootView = binding.getRoot();
        setContentView(rootView);

        // Set the toolbar with icon, titile. The OptionsMenu are defined below to override
        // methods.
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(true);
        Objects.requireNonNull(getSupportActionBar()).setHomeButtonEnabled(false);
        binding.appbar.addOnOffsetChangedListener((appbar, offset) -> showCollapsedPrice(offset));

        // Set Spinner for selecting a fuel.
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this, R.array.spinner_fuel_name, R.layout.spinner_main_fuel);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_main_dropdown);
        binding.mainTopFrame.spinnerGas.setAdapter(spinnerAdapter);
        binding.mainTopFrame.spinnerGas.setOnItemSelectedListener(this);

        // Display the gas prices of average, sido, sigun and station.
        setSpinnerToDefaultFuel();//set the default fuel.
        binding.mainTopFrame.avgPriceView.addPriceView(defaultFuel);//
        pricePagerAdapter = new PricePagerAdapter(this);
        pricePagerAdapter.setFuelCode(defaultFuel);
        binding.mainTopFrame.viewpagerPrice.setAdapter(pricePagerAdapter);

        // MainContent RecyclerView to display main contents in the activity
        MainContentAdapter adapter = new MainContentAdapter();
        RecyclerDivider divider = new RecyclerDivider(this);
        binding.recyclerContents.setAdapter(adapter);
        //binding.recyclerContents.addItemDecoration(divider);

        // ViewModels
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
        stnModel = new ViewModelProvider(this).get(StationListViewModel.class);
        imgModel = new ViewModelProvider(this).get(ImageViewModel.class);

        // Instantiate objects
        imgResUtil = new ApplyImageResourceUtil(this);
        mPrevLocation = null;

        // Event Handlers
        binding.imgbtnStation.setOnClickListener(view -> {
            final boolean isStnViewOn = binding.stationRecyclerView.getVisibility() == View.VISIBLE;
            if(!isStnViewOn) {
               binding.pbNearStns.setVisibility(View.VISIBLE);
               checkRuntimePermission(rootView, Manifest.permission.ACCESS_FINE_LOCATION, () -> {
                   locationTask = sThreadManager.fetchLocationTask(this, locationModel);
               });

            } else {
                binding.stationRecyclerView.setVisibility(View.GONE);
                binding.recyclerContents.setVisibility(View.VISIBLE);
            }
        });

        locationModel.getLocation().observe(this, location -> {
            if(mPrevLocation == null || (mPrevLocation.distanceTo(location) > Constants.UPDATE_DISTANCE)) {
                log.i("fetch location:%s, %s", location.getLatitude(), location.getLongitude());
                mPrevLocation = location;
                stationListTask = sThreadManager.startStationListTask(stnModel, location, getDefaultParams());
            } else {
                binding.recyclerContents.setVisibility(View.GONE);
                binding.stationRecyclerView.setVisibility(View.VISIBLE);
                binding.pbNearStns.setVisibility(View.GONE);
                Snackbar.make(rootView, getString(R.string.general_snackkbar_inbounds), Snackbar.LENGTH_SHORT).show();
            }
        });

        // Location has failed to fetch.
        locationModel.getLocationException().observe(this, exception -> {
            log.i("Exception occurred while fetching location");
            SpannableString spannableString = new SpannableString(getString(R.string.general_no_location));
            binding.pbNearStns.setVisibility(View.GONE);
            binding.stationRecyclerView.setVisibility(View.VISIBLE);
            binding.stationRecyclerView.showTextView(spannableString);

        });

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

            } else {
                log.i("no station");
                // No near stations post an message that contains the clickable span to link to the
                // SettingPreferenceActivity for resetting the searching radius.
                //SpannableString spannableString = handleStationListException();
                //stationRecyclerView.showTextView(spannableString);
            }

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

            hasStationInfo = true;
        });

        // Create ActivityResultLauncher to call SettingActiity and get results
        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        log.i("activity result");
                        //pricePagerAdapter.notifyDataSetChanged();
                    }
                });

    }

    @Override
    public void onResume() {
        super.onResume();
        // Set the user name in the toolbar
        String title = mSettings.getString(Constants.USER_NAME, null);
        if(title != null) Objects.requireNonNull(getSupportActionBar()).setTitle(title);

        // Set the ion in the toolbar
        String userImg = mSettings.getString(Constants.USER_IMAGE, null);
        String imgUri = (TextUtils.isEmpty(userImg))?Constants.imgPath + "ic_user_blank_gray":userImg;
        imgResUtil.applyGlideToDrawable(imgUri, Constants.ICON_SIZE_TOOLBAR_USERPIC, imgModel);
        imgModel.getGlideDrawableTarget().observe(this, resource -> {
            if(getSupportActionBar() != null) getSupportActionBar().setIcon(resource);
        });

        // Display the price info when collapsed.
        dispPriceCollapsed();
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
            //startActivityForResult(settingIntent, requestCode); // deprecated!!!
            activityResultLauncher.launch(settingIntent);
        }

        return true;
    }

    // Implement StationListAdapter.OnRecyclerItemClickListener
    @Override
    public void onItemClicked(final int pos) {
        if(!hasStationInfo) {
            log.i("wait a sec"); // Check if necessary!!!
            return;
        }
        Intent intent = new Intent(this, StationMapActivity.class);
        intent.putExtra("gasStationId", mStationList.get(pos).getStnId());
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        FinishAppDialogFragment endDialog = new FinishAppDialogFragment();
        endDialog.show(getSupportFragmentManager(), "endDialog");
    }

    // Implement AdapterView.OnItemSelectedListener
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
        log.i("Spinner:%s", pos);
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
            binding.mainTopFrame.avgPriceView.addPriceView(defaultFuel);

            // Attach the viewpager adatepr with a fuel code selected by the spinner.
            pricePagerAdapter.setFuelCode(defaultFuel);
            binding.mainTopFrame.viewpagerPrice.setAdapter(pricePagerAdapter);

            // Retrieve near stations based on a newly selected fuel code if the spinner selection
            // has changed. Temporarily make this not working for preventing excessive access to the
            // server.

            //stationListTask = sThreadManager.startStationListTask(stnModel, mPrevLocation, defaults);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) { }

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
        //sThreadManager.cancelAllThreads();
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {}

    // Ref: expand the station recyclerview up to wrap_content
    private void showCollapsedPrice(int offset) {
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
                binding.mainTopFrame.spinnerGas.setSelection(i);
                defaultFuel = defaults[0];
                break;
            }
        }
    }

    private void dispPriceCollapsed() {
        final String[] arrFile = {Constants.FILE_CACHED_SIDO_PRICE, Constants.FILE_CACHED_SIGUN_PRICE };
        String avgPrice = String.valueOf(binding.mainTopFrame.avgPriceView.getAvgGasPrice());

        // Set the average price
        binding.tvCollapsedAvgPrice.setText(avgPrice);
        // Set the sido and sigun price
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
                            if(sido.getProductCd().matches(defaultFuel)) {
                                binding.tvCollapsedSido.setText(sido.getSidoName());
                                binding.tvCollapsedSidoPrice.setText(String.valueOf(sido.getPrice()));
                            }
                            break;
                        case Constants.FILE_CACHED_SIGUN_PRICE:
                            Opinet.SigunPrice sigun = (Opinet.SigunPrice)x;
                            if(sigun.getProductCd().matches(defaultFuel)) {
                                binding.tvCollapsedSigun.setText(sigun.getSigunName());
                                binding.tvCollapsedSigunPrice.setText(String.valueOf(sigun.getPrice()));
                            }
                            break;
                    }

                }

            } catch (IOException | ClassNotFoundException e) { e.printStackTrace();}
        }
    }

    static class RecyclerDivider extends RecyclerView.ItemDecoration {
        Drawable mDivider;
        public RecyclerDivider(Context context) {
            mDivider = ContextCompat.getDrawable(context, R.drawable.shape_itemdivider);
            /*
            mDivider = ResourcesCompat.getDrawable(
                    context.getResources(), R.drawable.shape_itemdivider, null);
             */
        }

        @Override
        public void getItemOffsets(
                @NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                @NonNull RecyclerView.State state) {

            super.getItemOffsets(outRect, view, parent, state);
            outRect.bottom = 100;
        }

        @Override
        public void onDraw(
                @NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){
            super.onDraw(c, parent, state);
        }

        @Override
        public void onDrawOver(
                @NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state){

            super.onDrawOver(c, parent, state);
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }
}

