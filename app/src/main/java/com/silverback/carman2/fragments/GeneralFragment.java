package com.silverback.carman2.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BulletSpan;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.MainActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.SettingPrefActivity;
import com.silverback.carman2.StationMapActivity;
import com.silverback.carman2.adapters.PricePagerAdapter;
import com.silverback.carman2.adapters.StationListAdapter;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.GasManagerDao;
import com.silverback.carman2.database.ServiceManagerDao;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.threads.GasPriceTask;
import com.silverback.carman2.viewmodels.FragmentSharedModel;
import com.silverback.carman2.viewmodels.LocationViewModel;
import com.silverback.carman2.viewmodels.Opinet;
import com.silverback.carman2.viewmodels.OpinetViewModel;
import com.silverback.carman2.viewmodels.StationListViewModel;
import com.silverback.carman2.threads.LocationTask;
import com.silverback.carman2.threads.StationListTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.views.OpinetAvgPriceView;
import com.silverback.carman2.views.StationRecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * This fragment is to display the gas prices and near stations.
 */

public class GeneralFragment extends Fragment implements
        View.OnClickListener,
        RecyclerView.OnItemTouchListener,
        StationListAdapter.OnRecyclerItemClickListener,
        AdapterView.OnItemSelectedListener {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(GeneralFragment.class);

    // Objects
    private CarmanDatabase mDB;
    private File favFile;

    private LocationViewModel locationModel;
    private StationListViewModel stnModel;
    private FragmentSharedModel fragmentModel;
    private OpinetViewModel opinetModel;

    private GasManagerDao.RecentGasData queryGasResult;
    private ServiceManagerDao.RecentServiceData querySvcResult;

    private LocationTask locationTask;
    private StationListTask stationListTask;
    private GasPriceTask gasTask;
    private OpinetAvgPriceView opinetAvgPriceView;
    private StationRecyclerView stationRecyclerView;
    private StationListAdapter mAdapter;
    private PricePagerAdapter pricePagerAdapter;
    private List<Opinet.GasStnParcelable> mStationList;
    private Location mPrevLocation;

    // UI's
    private View childView;
    private ViewPager priceViewPager;
    private Spinner fuelSpinner;
    private TextView tvExpLabel, tvLatestExp;
    private TextView tvExpenseSort, tvStationsOrder;
    private FloatingActionButton fabLocation;

    // Fields
    private String savedId;
    private String[] defaults; //defaults[0]:fuel defaults[1]:radius default[2]:sorting
    private String defaultFuel;
    private boolean bStationsOrder;//true: distance order(value = 2) false: price order(value =1);
    private boolean bExpenseSort;
    private boolean hasNearStations;//flag to check whether near stations exist within the radius.
    private boolean isNetworkConnected;
    private String latestItems;

    public GeneralFragment() {
        // Required empty public constructor
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //isNetworkConnected = getArguments().getBoolean("notifyNetworkConnected");
        isNetworkConnected = BaseActivity.notifyNetworkConnected(getContext());

        //favFile = new File(getContext().getCacheDir(), Constants.FILE_CACHED_STATION_PRICE);
        favFile = new File(getContext().getFilesDir(), Constants.FILE_FAVORITE_PRICE);
        mDB = CarmanDatabase.getDatabaseInstance(getContext());
        pricePagerAdapter = new PricePagerAdapter(getChildFragmentManager());

        // Create ViewModels
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
        stnModel = new ViewModelProvider(this).get(StationListViewModel.class);
        fragmentModel = new ViewModelProvider(getActivity()).get(FragmentSharedModel.class);
        opinetModel = new ViewModelProvider(getActivity()).get(OpinetViewModel.class);


        // Fetch the current location using the worker thread and return the value via ViewModel
        // as the type of LiveData, on the basis of which the near stations is to be retrieved.
        locationTask = ThreadManager.fetchLocationTask(getContext(), locationModel);
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        childView = inflater.inflate(R.layout.fragment_general, container, false);

        //TextView tvDate = childView.findViewById(R.id.tv_today);
        fuelSpinner = childView.findViewById(R.id.spinner_fuel);
        tvExpLabel = childView.findViewById(R.id.tv_label_exp);
        tvLatestExp = childView.findViewById(R.id.tv_exp_stmts);
        tvExpenseSort = childView.findViewById(R.id.tv_expenses_sort);
        tvStationsOrder = childView.findViewById(R.id.tv_stations_order);
        priceViewPager = childView.findViewById(R.id.pager_price);
        opinetAvgPriceView = childView.findViewById(R.id.avgPriceView);
        stationRecyclerView = childView.findViewById(R.id.stationRecyclerView);
        fabLocation = childView.findViewById(R.id.fab_relocation);

        // Attach event listeners
        childView.findViewById(R.id.imgbtn_expense).setOnClickListener(this);
        childView.findViewById(R.id.imgbtn_stations).setOnClickListener(this);
        stationRecyclerView.addOnItemTouchListener(this);

        // Sets the spinner_stat default value if it is saved in SharedPreference.Otherwise, sets it to 0.
        fuelSpinner.setOnItemSelectedListener(this);
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                getContext(), R.array.spinner_fuel_name, R.layout.spinner_main_fuel);
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_main_dropdown);
        fuelSpinner.setAdapter(spinnerAdapter);

        // Get the spinner String values and set the inital value with the default one saved in
        // SharedPreferences.
        String[] code = getResources().getStringArray(R.array.spinner_fuel_code);
        defaults = getArguments().getStringArray("defaults");
        for(int i = 0; i < code.length; i++) {
            if(code[i].matches(defaults[0])){
                fuelSpinner.setSelection(i);
                defaultFuel = defaults[0];
                break;
            }
        }

        // Add the custom view for the average price and the viewpager in which to show the sido,
        // sigun price and the first placeholder station
        opinetAvgPriceView.addPriceView(defaultFuel);
        pricePagerAdapter.setFuelCode(defaultFuel);
        priceViewPager.setAdapter(pricePagerAdapter);

        // Set Floating Action Button
        // RecycerView.OnScrollListener is an abstract class which shows/hides the floating action
        // button according to scolling or idling
        fabLocation.setOnClickListener(this);
        fabLocation.setSize(FloatingActionButton.SIZE_AUTO);
        // Change the size of the Floating Action Button on scolling
        stationRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener(){
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0 || dy < 0 && fabLocation.isShown()) fabLocation.hide();
            }
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) fabLocation.show();
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        return childView;
    }

    // It is appropriate that most of ViewModel LiveData be defined in this lifecycle.
    @Override
    public void onActivityCreated(Bundle savedStateInstance) {
        super.onActivityCreated(savedStateInstance);

        // Get the first placeholder id from the local db and if it is different from the id previously
        // saved as the first placeholder in the file, which means the first favorite has changed,
        // pass the id to PricePagerFragment by FramentSharedModel and update the adapter.
        mDB.favoriteModel().getFirstFavorite(Constants.GAS).observe(getViewLifecycleOwner(), stnId -> {
            savedId = getSavedFirstFavorite(getContext());
            // A new station is reset to the first favroite list.
            if(stnId != null) {
                if(savedId == null || !stnId.matches(savedId)) {
                    pricePagerAdapter.notifyDataSetChanged();
                    fragmentModel.getFirstPlaceholderId().setValue(stnId);
                    pricePagerAdapter.notifyDataSetChanged();
                }
            // An already registered favorite station is removed.
            } else {
                fragmentModel.getFirstPlaceholderId().setValue(null);
                pricePagerAdapter.notifyDataSetChanged();
                //if(favFile.exists()) favFile.delete();
            }
        });

        // Retrieve the queried results of the latest gas and service statements as LiveData from
        // Room database,  both of which are stored in GasManagerEntity and ServiceManagerEntity
        mDB.gasManagerModel().loadLatestGasData().observe(getViewLifecycleOwner(), data -> {
            queryGasResult = data;
            setLatestExpense(0, data);

        });

        // Retrieve queried result of the latest service.
        mDB.serviceManagerModel().loadLatestSvcData().observe(getViewLifecycleOwner(), data -> {
            querySvcResult = data;
            // Retrieve the latest serviced items from ServicedItemEntitiy with the service_id
            // fetched from ServiceManagerEntitiy.
            if(data != null) {
                StringBuilder sb = new StringBuilder();
                mDB.servicedItemModel().queryLatestItems(data.svcId).observe(getViewLifecycleOwner(), items -> {
                    for (String name : items) sb.append(name).append("  ");
                    latestItems = sb.toString();
                });
            }
        });

        // On fetching the current location, start to get the near stations based on the location.
        // As far as the fragment is first created or the current location outbounds UPDATE_DISTANCE,
        // attempt to retreive a new station list based on the location. On the other hand,
        // if a distance b/w a new location and the previous location is within UPDATE_DISTANCE,
        // do not initiate the task to prevent frequent connection to the server.
        locationModel.getLocation().observe(getViewLifecycleOwner(), location -> {
            log.i("no location retrieved: %s", location);
            if(location == null) return;

            if(mPrevLocation == null || (mPrevLocation.distanceTo(location) > Constants.UPDATE_DISTANCE)) {
                mPrevLocation = location;
                stationListTask = ThreadManager.startStationListTask(stnModel, location, defaults);
            } else {
                Snackbar.make(childView, getString(R.string.general_snackkbar_inbounds), Snackbar.LENGTH_SHORT).show();
            }
        });

        // Location has failed to fetch.
        locationModel.getLocationException().observe(getViewLifecycleOwner(), exception -> {
            SpannableString spannableString = new SpannableString(getString(R.string.general_no_location));
            stationRecyclerView.showTextView(spannableString);
            fabLocation.setVisibility(View.VISIBLE);
        });

        // Receive station(s) within the radius. If no stations exist, post the message that
        // indicate why it failed to fetch stations. It would be caused by any network problem or
        // no stations actually exist within the radius.
        stnModel.getNearStationList().observe(getViewLifecycleOwner(), stnList -> {
            if (stnList != null && stnList.size() > 0) {
                mStationList = stnList;
                mAdapter = new StationListAdapter(mStationList, this);
                stationRecyclerView.showStationListRecyclerView();
                stationRecyclerView.setAdapter(mAdapter);
                hasNearStations = true;//To control the switch button

            } else {
                // No near stations post an message that contains the clickable span to link to the
                // SettingPreferenceActivity for resetting the searching radius.
                SpannableString spannableString = handleStationListException();
                stationRecyclerView.showTextView(spannableString);
            }
        });

        // Update the carwash info to StationList and notify the data change to Adapter.
        // Adapter should not assume that the payload will always be passed to onBindViewHolder()
        // e.g. when the view is not attached.
        stnModel.getStationCarWashInfo().observe(getViewLifecycleOwner(), sparseArray -> {
            for(int i = 0; i < sparseArray.size(); i++) {
                mStationList.get(i).setIsWash(sparseArray.valueAt(i));
                mAdapter.notifyItemChanged(sparseArray.keyAt(i), sparseArray.valueAt(i));
            }

        });

        // Any exception occurs in StationListTask
        /*
        stnModel.getExceptionMessage().observe(getViewLifecycleOwner(), msg ->
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show());

         */
    }

    @Override
    public void onResume() {
        super.onResume();
    }


    @Override
    public void onPause() {
        super.onPause();
        if(locationTask != null) locationTask = null;
        if(stationListTask != null) stationListTask = null;
    }

    // If no network connection is found, show a message that contains the clickable span to
    // perform startActivityForResult() which is defined in handleStationListException().
    @SuppressWarnings("ConstantConditions")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == Constants.REQUEST_NETWORK_SETTING) {
            isNetworkConnected = BaseActivity.notifyNetworkConnected(getActivity());
            if(isNetworkConnected)
                stationListTask = ThreadManager.startStationListTask(stnModel, mPrevLocation, defaults);
            //else Snackbar.make(childView, getString(R.string.errror_no_network), Snackbar.LENGTH_SHORT).show();
        }
    }

    // The following 2 callbacks are initially invoked by AdapterView.OnItemSelectedListener
    // defined in the spinner, thus the viewpager adapter should be created here in order to
    // pass arguments.
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        switch(position){
            case 0: defaults[0] = "B027"; break; // gasoline
            case 1: defaults[0] = "D047"; break; // diesel
            case 2: defaults[0] = "K015"; break; // LPG
            case 3: defaults[0] = "B034"; break; // premium gasoline
            default: break;
        }

        if(!defaultFuel.matches(defaults[0])) {
            log.i("onItemSelected");
            defaultFuel = defaults[0];
            // Retrives the price data respectively saved in the cache directory with a fuel selected
            // by the spinner.
            opinetAvgPriceView.addPriceView(defaults[0]);

            // Attach the viewpager adatepr with a fuel code selected by the spinner.
            pricePagerAdapter.setFuelCode(defaults[0]);
            priceViewPager.setAdapter(pricePagerAdapter);

            // Retrieve near stations based on a newly selected fuel code if the spinner selection
            // has changed. Temporarily make this not working for preventing excessive access to the
            // server.
            //stationListTask = ThreadManager.startStationListTask(stnListModel, mPrevLocation, defaults);
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent){
        log.i("onNothingSelected by Spinner");
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.imgbtn_expense:
                bExpenseSort = !bExpenseSort;
                String sort = (bExpenseSort)?getString(R.string.general_expense_service):getString(R.string.general_expense_gas);
                tvExpenseSort.setText(sort);

                if(!bExpenseSort) setLatestExpense(0, queryGasResult);
                else setLatestExpense(1, querySvcResult);

                break;

            case R.id.imgbtn_stations:
                // The button does work only if the task has finished fetching near stations.
                if(!hasNearStations) return;

                // Save the station list to prevent reordering from retasking the station list.
                Uri uri = saveNearStationList(mStationList);
                if(uri == null) return;

                bStationsOrder = !bStationsOrder;
                String order = (bStationsOrder)?
                        getString(R.string.general_stations_price):
                        getString(R.string.general_stations_distance);
                tvStationsOrder.setText(order);
                mStationList = mAdapter.sortStationList(bStationsOrder);

                break;

            case R.id.fab_relocation:
                locationTask = ThreadManager.fetchLocationTask(getContext(), locationModel);
                break;

        }
    }

    //The following 3 methods are invoked by RecyclerView.OnItemTouchListener.
    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        return false;
    }
    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}
    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}


    // Callback invoked by StationListAdapter.OnRecyclerItemClickListener
    // on clicking an RecyclerView item with fetched station name and id passing to the activity.
    @Override
    public void onItemClicked(final int position) {
        Intent intent = new Intent(getActivity(), StationMapActivity.class);
        intent.putExtra("gasStationId", mStationList.get(position).getStnId());
        startActivity(intent);
    }

    @SuppressWarnings("ConstantConditions")
    private Uri saveNearStationList(List<Opinet.GasStnParcelable> list) {

        File file = new File(getContext().getCacheDir(), Constants.FILE_CACHED_NEAR_STATIONS);
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

    // Build Strings for the item labels and their statements by category of Gas and Service,
    // which are queried from Room database.
    private void setLatestExpense(int mode, Object obj) {

        SpannableStringBuilder sbLabel = new SpannableStringBuilder();
        StringBuilder sbStmts = new StringBuilder();

        switch(mode) {
            case 0: // Gas
                sbLabel.append(getString(R.string.general_label_gas_date)).append("\n")
                        .append(getString(R.string.general_label_station)).append("\n")
                        .append(getString(R.string.general_label_mileage)).append("\n")
                        .append(getString(R.string.general_label_gas_amount)).append("\n")
                        .append(getString(R.string.general_label_gas_payment));

                if(obj != null) {
                    GasManagerDao.RecentGasData gasData = (GasManagerDao.RecentGasData) obj;
                    String gasDate = BaseActivity.formatMilliseconds(getString(R.string.date_format_1), gasData.dateTime);
                    sbStmts.append(gasDate).append("\n")
                            .append(gasData.stnName).append("\n")
                            .append(gasData.mileage).append(getString(R.string.unit_km)).append("\n")
                            .append(gasData.gasAmount).append(getString(R.string.unit_liter)).append("\n")
                            .append(gasData.gasPayment).append(getString(R.string.unit_won));

                } else sbStmts.append(getString(R.string.general_no_gas_history));

                break;

            case 1: // Service
                sbLabel.append(getString(R.string.general_label_svc_date)).append("\n")
                        .append(getString(R.string.general_label_svc_provider)).append("\n")
                        .append(getString(R.string.general_label_mileage)).append("\n")
                        .append(getString(R.string.general_label_svc_items)).append("\n")
                        .append(getString(R.string.general_label_svc_payment));


                if(obj != null) {
                    ServiceManagerDao.RecentServiceData svcData = (ServiceManagerDao.RecentServiceData) obj;
                    String svcDate = BaseActivity.formatMilliseconds(getString(R.string.date_format_1), svcData.dateTime);

                    sbStmts.append(svcDate).append("\n")
                            .append(svcData.svcName).append("\n")
                            .append(svcData.mileage).append(getString(R.string.unit_km)).append("\n")
                            .append(latestItems).append("\n")
                            .append(svcData.totalExpense).append(getString(R.string.unit_won))
                            .append("\n");

                } else sbStmts.append(getString(R.string.general_no_svc_history));

                break;

        }

        // Put the BulletSpan at the first of each item.
        final String REGEX_SEPARATOR = "\n";
        final Matcher m = Pattern.compile(REGEX_SEPARATOR).matcher(sbLabel);
        // Set the span for the first bullet
        int start = 0;
        while(m.find()) {
            sbLabel.setSpan(new BulletSpan(20, Color.parseColor("#FFCE00")),
                    start, m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = m.end();
        }

        // Set the span for the last item
        sbLabel.setSpan(new BulletSpan(20, Color.parseColor("#FFCE00")),
                start, sbLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvExpLabel.setText(sbLabel);
        tvLatestExp.setText(sbStmts.toString());
    }


    // Method for handling the exceptions which would be caused by the network status or there isn't
    // any station within the radius.
    // In both cases, post the message as to its exception and convert it to SpannableString having
    // ClickableSpan which has the specific keyword be spanned, then enable to retry network connection
    // or start SettingPreferenceActivity.
    @SuppressWarnings("ConstantConditions")
    private SpannableString handleStationListException(){

        SpannableString spannableString;
        if(isNetworkConnected) {

            fabLocation.setVisibility(View.VISIBLE);
            String radius = defaults[1];
            String msg = getString(R.string.general_no_station_fetched);

            // In case the radius is already set to the maximum value(5000m), no need to change the value.
            if(radius != null && radius.matches("5000")) {
                msg = msg.substring(0, msg.indexOf("\n"));
                return new SpannableString(radius + msg);
            }

            String format = String.format("%s%s", radius, msg);
            spannableString = new SpannableString(format);

            // Set the ClickableSpan range
            String spanned = getString(R.string.general_index_reset);
            int start = format.indexOf(spanned);
            int end = start + spanned.length();

            spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    getActivity().startActivity(new Intent(getActivity(), SettingPrefActivity.class));
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
                    startActivityForResult(networkIntent, Constants.REQUEST_NETWORK_SETTING);
                }

            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannableString;
    }

    // To get the id of the gas station placed first in the list, check the file, if any,
    // which has saved the first placeholder and get the stationId.
    private String getSavedFirstFavorite(Context context) {
        Uri uri = Uri.fromFile(favFile);
        try(InputStream is = context.getContentResolver().openInputStream(uri);
            ObjectInputStream ois = new ObjectInputStream(is)) {

            Opinet.StationPrice savedFavorite = (Opinet.StationPrice) ois.readObject();
            if(savedFavorite != null) return savedFavorite.getStnId();

        } catch(IOException | ClassNotFoundException e) { e.printStackTrace();}

        return null;
    }

    // Invalidate the views, the data of which have changed in SettingPreferenceActivity
    @SuppressWarnings("ConstantConditions")
    public void resetGeneralFragment(String distCode, String fuelCode, String radius) {
        // If the default district has changed, which has initiated GasPriceTask to fetch price data
        // in SettingPreferenceActivity,  newly set the apdater to the pager.
        if(!TextUtils.isEmpty(distCode)) {
            String gasCode = (fuelCode == null) ? defaultFuel : fuelCode;
            log.i("Code: %s, %s", distCode, gasCode);
            gasTask = ThreadManager.startGasPriceTask(getContext(), opinetModel, distCode, gasCode);
            opinetModel.distPriceComplete().observe(getViewLifecycleOwner(), isComplete -> {
                pricePagerAdapter.setFuelCode(gasCode);
                pricePagerAdapter.notifyDataSetChanged();
                priceViewPager.setAdapter(pricePagerAdapter);
                ((MainActivity)getActivity()).getSettings().edit().putLong(
                        Constants.OPINET_LAST_UPDATE, System.currentTimeMillis()).apply();
            });
        }

        // When the fuel code has changed, reset the spinner selection to a new position, which
        // implements onItemSelected() to invalidate a new price data with the fuel code.
        if(TextUtils.isEmpty(distCode) && fuelCode != null) {
            String[] fuel = getResources().getStringArray(R.array.spinner_fuel_code);
            for (int i = 0; i < fuel.length; i++) {
                if (fuel[i].matches(fuelCode)) {
                    fuelSpinner.setSelection(i);
                    defaults[0] = fuelCode;//reset a fuel code to the defaultParams
                    break;
                }
            }
        }

        // if the radius has changed, it is required to retrieve near stations with the radius
        if(radius != null) {
            defaults[1] = radius; //reset a new radius to the defaultParams
            stationListTask = ThreadManager.startStationListTask(stnModel, mPrevLocation, defaults);
        }
    }

}
