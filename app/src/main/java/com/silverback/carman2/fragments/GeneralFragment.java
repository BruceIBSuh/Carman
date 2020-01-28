package com.silverback.carman2.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.MainActivity;
import com.silverback.carman2.R;
import com.silverback.carman2.SettingPreferenceActivity;
import com.silverback.carman2.StationMapActivity;
import com.silverback.carman2.adapters.PricePagerAdapter;
import com.silverback.carman2.adapters.StationListAdapter;
import com.silverback.carman2.database.CarmanDatabase;
import com.silverback.carman2.database.GasManagerDao;
import com.silverback.carman2.database.ServiceManagerDao;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.FragmentSharedModel;
import com.silverback.carman2.models.LocationViewModel;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.models.StationListViewModel;
import com.silverback.carman2.threads.LocationTask;
import com.silverback.carman2.threads.StationListTask;
import com.silverback.carman2.threads.ThreadManager;
import com.silverback.carman2.utils.Constants;
import com.silverback.carman2.views.OpinetAvgPriceView;
import com.silverback.carman2.views.StationRecyclerView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * This fragment belongs to MainActivity and it contains the price information and the latest
 * expenditure of gas and service, and stations in the neighborhood. MainActivity may extend
 * to multi fragments at a time when an additional fragment such as general information ahead of the
 * current fragment is introduced.
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
    private SharedPreferences mSettings;

    private LocationViewModel locationModel;
    private StationListViewModel stnListModel;
    private FragmentSharedModel fragmentModel;
    //private OpinetViewModel opinetViewModel;

    private GasManagerDao.RecentGasData queryGasResult;
    private ServiceManagerDao.RecentServiceData querySvcResult;

    private LocationTask locationTask;
    private StationListTask stationListTask;
    private OpinetAvgPriceView opinetAvgPriceView;
    private StationRecyclerView stationRecyclerView;
    private StationListAdapter mAdapter;
    private PricePagerAdapter pricePagerAdapter;
    private List<Opinet.GasStnParcelable> mStationList;
    private Location mPrevLocation;

    // UI's
    private View childView;
    private ViewPager priceViewPager;
    private TextView tvExpLabel, tvLatestExp;
    private TextView tvExpenseSort, tvStationsOrder;
    private FloatingActionButton fabLocation;
    private ProgressBar progbarStnList;

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
        log.i("onCreate() in GeneralFragment");
        super.onCreate(savedInstanceState);
        isNetworkConnected = getArguments().getBoolean("notifyNetworkConnected");

        favFile = new File(getContext().getCacheDir(), Constants.FILE_CACHED_STATION_PRICE);
        mSettings = ((MainActivity)getActivity()).getSettings();
        mDB = CarmanDatabase.getDatabaseInstance(getContext());
        pricePagerAdapter = new PricePagerAdapter(getChildFragmentManager());

        // Create ViewModels
        locationModel = ViewModelProviders.of(this).get(LocationViewModel.class);
        stnListModel = ViewModelProviders.of(this).get(StationListViewModel.class);
        fragmentModel = ViewModelProviders.of(getActivity()).get(FragmentSharedModel.class);


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
        Spinner fuelSpinner = childView.findViewById(R.id.spinner_fuel);
        tvExpLabel = childView.findViewById(R.id.tv_label_exp);
        tvLatestExp = childView.findViewById(R.id.tv_exp_stmts);
        tvExpenseSort = childView.findViewById(R.id.tv_expenses_sort);
        tvStationsOrder = childView.findViewById(R.id.tv_stations_order);
        priceViewPager = childView.findViewById(R.id.pager_price);
        opinetAvgPriceView = childView.findViewById(R.id.avgPriceView);
        stationRecyclerView = childView.findViewById(R.id.stationRecyclerView);
        progbarStnList = childView.findViewById(R.id.progbar_stn_list);
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

    // Receive values of the ViewModels or queried results of Room which are synced with observe()
    // defined in LiveData.
    @Override
    public void onActivityCreated(Bundle savedStateInstance) {
        super.onActivityCreated(savedStateInstance);

        // Get the first placeholder id from the local db and if it is different from the id previously
        // saved as the first placeholder in the file, which means the first favorite has changed,
        // pass the id to PricePagerFragment by FramentSharedModel and update the adapter.
        mDB.favoriteModel().getFirstFavorite(Constants.GAS).observe(getViewLifecycleOwner(), stnId -> {
            savedId = getSavedFirstFavorite(getContext());
            log.i("Compare ids: %s, %s", stnId, savedId);
            // A station is added to the favroite list.
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
        // do not initiate the task to get new near stations to prevent frequent connection to
        // the server.
        locationModel.getLocation().observe(getViewLifecycleOwner(), location -> {
            // Manage to show the message in case that Location failed to fetch.
            if(location == null) {
                log.i("Location failed");
                SpannableString msg = new SpannableString("Location failed to fetch");
                stationRecyclerView.showTextView(msg);
                return;
            }

            if(mPrevLocation == null || mPrevLocation.distanceTo(location) > Constants.UPDATE_DISTANCE) {
                log.i("Location succeeded");
                mPrevLocation = location;
                stationListTask = ThreadManager.startStationListTask(stnListModel, location, defaults);
            } else {
                Snackbar.make(childView, getString(R.string.general_snackkbar_inbounds), Snackbar.LENGTH_SHORT).show();
            }
        });

        // Receive station(s) within the radius. If no stations exist, post the message that
        // indicate why it failed to fetch stations. It would be caused by any network problem or
        // no stations actually exist within the radius.
        stnListModel.getStationListLiveData().observe(getViewLifecycleOwner(), stnList -> {
            if (stnList != null && stnList.size() > 0) {
                mStationList = stnList;
                mAdapter = new StationListAdapter(mStationList, this);
                stationRecyclerView.showStationListRecyclerView();
                stationRecyclerView.setAdapter(mAdapter);
                hasNearStations = true;
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
        stnListModel.getStationCarWashInfo().observe(getViewLifecycleOwner(), sparseArray -> {
            for(int i = 0; i < sparseArray.size(); i++) {
                mStationList.get(i).setIsWash(sparseArray.valueAt(i));
                mAdapter.notifyItemChanged(sparseArray.keyAt(i), sparseArray.valueAt(i));
            }

        });
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

        try {
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(list);

            return Uri.fromFile(file);

        } catch (FileNotFoundException e) {
            log.e("FileNotFoundException: %s", e.getMessage());

        } catch (IOException e) {
            log.e("IOException: %s", e.getMessage());

        }

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
            String radius = mSettings.getString(Constants.RADIUS, null);
            String msg = getString(R.string.general_no_station_fetched);

            // In case the radius is already set to the max(5000m), no need to change the value.
            if(radius != null && radius.matches("5000")) {
                msg = msg.substring(0, msg.indexOf("\n"));
                return new SpannableString(radius + msg);
            }

            String format = String.format("%s%s", radius, msg);
            spannableString = new SpannableString(format);

            // Set the ClickableSpan range
            int start = format.indexOf(getString(R.string.general_index_reset));
            int end = start + 2;
            spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Snackbar.make(childView, "No stations within the radius", Snackbar.LENGTH_SHORT).show();
                    getActivity().startActivity(new Intent(getActivity(), SettingPreferenceActivity.class));
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        } else {
            String message = getString(R.string.errror_no_network);
            spannableString = new SpannableString(message);

            // Set the ClickableSpan range.
            int start = message.indexOf(getString(R.string.error_index_retry));
            int end = start + 5;

            spannableString.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    if(BaseActivity.notifyNetworkConnected(getContext()))
                        stationListTask = ThreadManager.startStationListTask(stnListModel, mPrevLocation, defaults);
                    else
                        Snackbar.make(childView, "Network connection not established", Snackbar.LENGTH_SHORT).show();
                }

            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        return spannableString;
    }

    // To get the id of the first placeholder in the favorite station list, check the file, if any,
    // which saves the first placeholder.
    private String getSavedFirstFavorite(Context context) {
        Uri uri = Uri.fromFile(favFile);
        try(InputStream is = context.getContentResolver().openInputStream(uri);
            ObjectInputStream ois = new ObjectInputStream(is)) {

            Opinet.StationPrice savedFavorite = (Opinet.StationPrice) ois.readObject();
            if(savedFavorite != null) return savedFavorite.getStnId();

        } catch(IOException e) {
            log.e("IOException: %s", e.getMessage());
        } catch(ClassNotFoundException e) {
            log.e("ClassNotFoundException: %s", e.getMessage());
        }

        return null;
    }

    public void resetPricePager() {
        pricePagerAdapter.setFuelCode(defaultFuel);
        priceViewPager.setAdapter(pricePagerAdapter);
    }

}
