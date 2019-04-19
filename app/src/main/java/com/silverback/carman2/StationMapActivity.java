package com.silverback.carman2;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import android.graphics.Canvas;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.models.Opinet;
import com.silverback.carman2.threads.StationInfoTask;
import com.silverback.carman2.threads.ThreadManager;

import java.util.ArrayList;
import java.util.List;

public class StationMapActivity extends BaseActivity implements OnMapReadyCallback {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationMapActivity.class);

    // Objects
    private StationInfoTask stationInfoTask;
    private GoogleMap mMap;
    private LatLng stnLocation;
    private CardView cardView;
    private NestedScrollView nestedScrollView;
    private float xCoord, yCoord;

    // UIs
    TextView tvName, tvAddrs, tvPrice, tvCarwash, tvService,tvCVS;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_map);

        // Set ToolBar as ActionBar and attach Home Button and title on it.
        Toolbar mapToolbar = findViewById(R.id.tb_map);
        setSupportActionBar(mapToolbar);
        ActionBar ab = getSupportActionBar();
        if(ab != null) ab.setDisplayHomeAsUpEnabled(true);


        tvName = findViewById(R.id.tv_name);
        tvAddrs = findViewById(R.id.tv_address);
        tvPrice = findViewById(R.id.tv_price_info);
        tvCarwash = findViewById(R.id.tv_carwash);
        tvService = findViewById(R.id.tv_service);
        tvCVS = findViewById(R.id.tv_cvs);

        ArrayList<String> info = getIntent().getStringArrayListExtra("StationInfoList");


        xCoord = getIntent().getFloatExtra("xCoord", 0);
        yCoord = getIntent().getFloatExtra("yCoord", 0);

        tvName.setText(info.get(0));
        tvAddrs.setText(String.format("%s %15s", info.get(1), info.get(2)));
        tvCarwash.setText(String.format("%s%5s", getString(R.string.map_cardview_wash), info.get(3)));
        tvService.setText(String.format("%s%5s", getString(R.string.map_cardview_service), info.get(4)));
        tvCVS.setText(String.format("%s%5s", getString(R.string.map_cardview_cvs), info.get(5)));

        stnLocation = new LatLng(Float.valueOf(info.get(7)), Float.valueOf(info.get(6)));
        /*
        Bundle info = getIntent().getExtras();
        if(info == null) return;

        float latitude = Float.valueOf(info.getString("xcoord"));
        float longitude = Float.valueOf(info.getString("ycoord"));




        log.i("Location: %s, %s", latitude, longitude);
        */
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        mapFragment.getMapAsync(this);

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onResume() {
        super.onResume();
        String title = mSettings.getString(Constants.VEHICLE_NAME, null);
        if(title != null) getSupportActionBar().setTitle(title);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        mMap.addMarker(new MarkerOptions().position(stnLocation).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(stnLocation));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                log.i("onOptionsItemSelected in GeneralSettingActivity");
                //NavUtils.navigateUpFromSameTask(this); not working b/c it might be a different task?
                //onBackPressed();
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


}
