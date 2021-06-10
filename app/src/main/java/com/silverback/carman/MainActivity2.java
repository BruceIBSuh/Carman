package com.silverback.carman;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentContainerView;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.location.Location;
import android.os.Bundle;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.silverback.carman.adapters.StationListAdapter;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.LocationTask;
import com.silverback.carman.threads.StationListTask;
import com.silverback.carman.threads.ThreadManager;
import com.silverback.carman.utils.Constants;
import com.silverback.carman.viewmodels.LocationViewModel;
import com.silverback.carman.viewmodels.Opinet;
import com.silverback.carman.viewmodels.StationListViewModel;
import com.silverback.carman.views.StationRecyclerView;

import java.util.List;

public class MainActivity2 extends BaseActivity implements
        StationListAdapter.OnRecyclerItemClickListener {

    private final LoggingHelper log = LoggingHelperFactory.create(MainActivity2.class);

    // Objects
    private RecyclerView contentRecyclerView;
    private StationRecyclerView stationRecyclerView;
    private FloatingActionButton fab;

    private LocationViewModel locationModel;
    private StationListViewModel stnModel;

    private LocationTask locationTask;
    private StationListTask stationListTask;
    private Location mPrevLocation;

    private List<Opinet.GasStnParcelable> mStationList;
    private StationListAdapter mAdapter;

    private boolean hasNearStations;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        View mainLayout = findViewById(R.id.main_layout);
        stationRecyclerView = findViewById(R.id.stationRecyclerView);
        //fab = findViewById(R.id.fab);

        contentRecyclerView = findViewById(R.id.recycler_contents);
        MainContentAdapter adapter = new MainContentAdapter();
        contentRecyclerView.setAdapter(adapter);

        // Instantiate ViewModels
        locationModel = new ViewModelProvider(this).get(LocationViewModel.class);
        stnModel = new ViewModelProvider(this).get(StationListViewModel.class);

        Button btnStation = findViewById(R.id.btn_station);
        btnStation.setOnClickListener(view -> {
            log.d("fetch location started");
            locationTask = ThreadManager.fetchLocationTask(this, locationModel);
        });

        /*
        fab.setOnClickListener(view -> {
            stationRecyclerView.setVisibility(View.GONE);
        });
        */
        locationModel.getLocation().observe(this, location -> {
            if(location == null) return;
            if(mPrevLocation == null || (mPrevLocation.distanceTo(location) > Constants.UPDATE_DISTANCE)) {
                mPrevLocation = location;
                stationListTask = ThreadManager.startStationListTask(stnModel, location, getDefaultParams());
            } else {
                Snackbar.make(mainLayout, getString(R.string.general_snackkbar_inbounds), Snackbar.LENGTH_SHORT).show();

            }
        });

        // Location has failed to fetch.
        locationModel.getLocationException().observe(this, exception -> {
            log.i("Exception occurred while fetching location");
            SpannableString spannableString = new SpannableString(getString(R.string.general_no_location));
            stationRecyclerView.showTextView(spannableString);
            //fabLocation.setVisibility(View.VISIBLE);
        });

        // Receive station(s) within the radius. If no stations exist, post the message that
        // indicate why it failed to fetch stations. It would be caused by any network problem or
        // no stations actually exist within the radius.
        stnModel.getNearStationList().observe(this, stnList -> {
            log.i("near station fetched: %d", stnList.size());
            if (stnList != null && stnList.size() > 0) {
                log.i("station list:%s", stnList.size());
                //fab.setVisibility(View.VISIBLE);

                mStationList = stnList;
                mAdapter = new StationListAdapter(mStationList, this);
                //stationRecyclerView.showStationListRecyclerView();
                stationRecyclerView.setAdapter(mAdapter);
                hasNearStations = true;//To control the switch button

            } else {
                // No near stations post an message that contains the clickable span to link to the
                // SettingPreferenceActivity for resetting the searching radius.
                //SpannableString spannableString = handleStationListException();
                //stationRecyclerView.showTextView(spannableString);
            }
        });


    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }



    @Override
    public void onItemClicked(int pos) {

    }

    // Method for handling the exceptions which would be caused by the network status or there isn't
    // any station within the radius.
    // In both cases, post the message as to its exception and convert it to SpannableString having
    // ClickableSpan which has the specific keyword be spanned, then enable to retry network connection
    // or start SettingPreferenceActivity.
    /*
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
                    startActivity(new Intent(MainActivity2.this, SettingPrefActivity.class));
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
     */


    static class MainContentAdapter extends RecyclerView.Adapter<MainContentViewHolder> {
        @NonNull
        @Override
        public MainContentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            CardView view = (CardView)LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.cardview_main, parent, false);
            return new MainContentViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MainContentViewHolder holder, int position) {
            String msg = "RecyclerView Item: " + position;
            holder.textView.setText(msg);
        }

        @Override
        public int getItemCount() {
            return 10;
        }
    }

    static class MainContentViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public MainContentViewHolder(@NonNull CardView cardview) {
            super(cardview);
            textView = cardview.findViewById(R.id.textview);
        }
    }

}

