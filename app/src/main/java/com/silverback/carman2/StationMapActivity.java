package com.silverback.carman2;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.ibnco.carman.convertgeocoords.GeoPoint;
import com.ibnco.carman.convertgeocoords.GeoTrans;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.models.Constants;
import com.silverback.carman2.threads.StationInfoTask;
import com.silverback.carman2.utils.ConnectNaviHelper;
import com.skt.Tmap.TMapTapi;

public class StationMapActivity extends BaseActivity implements OnMapReadyCallback {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationMapActivity.class);

    // Objects
    private StationInfoTask stationInfoTask;
    private ConnectNaviHelper naviHelper;
    private double xCoord, yCoord;
    private double longitude, latitude;

    // UIs
    TextView tvName, tvAddrs, tvPrice, tvCarwash, tvService,tvCVS;

    // Fields
    private String stnName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_map);

        // Set ToolBar as ActionBar and attach Home Button and title on it.
        Toolbar mapToolbar = findViewById(R.id.tb_map);
        setSupportActionBar(mapToolbar);
        ActionBar ab = getSupportActionBar();
        if(ab != null) ab.setDisplayHomeAsUpEnabled(true);

        // UIs
        tvName = findViewById(R.id.tv_name);
        tvAddrs = findViewById(R.id.tv_address);
        tvPrice = findViewById(R.id.tv_price_info);
        tvCarwash = findViewById(R.id.tv_carwash);
        tvService = findViewById(R.id.tv_service);
        tvCVS = findViewById(R.id.tv_cvs);

        // Get intent and extras
        stnName = getIntent().getStringExtra("stationName");
        String stnAddrs = getIntent().getStringExtra("stationAddrs");
        String stnTel = getIntent().getStringExtra("stationTel");
        String carWash = getIntent().getStringExtra("isCarWash");
        String service = getIntent().getStringExtra("isService");
        String cvs = getIntent().getStringExtra("isCVS");
        xCoord = Double.valueOf(getIntent().getStringExtra("xCoord")); //KATEC
        yCoord = Double.valueOf(getIntent().getStringExtra("yCoord"));

        // Convert KATEC to longitude and latitude
        GeoPoint katec_pt = new GeoPoint(xCoord, yCoord);
        GeoPoint in_pt = GeoTrans.convert(GeoTrans.KATEC, GeoTrans.GEO, katec_pt);
        longitude = in_pt.getX();
        latitude = in_pt.getY();

        log.i("GeoCode: %s %s,%s %s", xCoord, yCoord, longitude, latitude);


        tvName.setText(stnName);
        tvAddrs.setText(String.format("%s %15s", stnAddrs, stnTel));
        tvCarwash.setText(String.format("%s%5s", getString(R.string.map_cardview_wash), carWash));
        tvService.setText(String.format("%s%5s", getString(R.string.map_cardview_service), service));
        tvCVS.setText(String.format("%s%5s", getString(R.string.map_cardview_cvs), cvs));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);

        if(mapFragment != null) mapFragment.getMapAsync(this);

        // Floating Action Button for initiating Kakao Navi, which is temporarily suspended.
        FloatingActionButton fabNavi = findViewById(R.id.fab_navi);

        // When the fab is clicked, connect to a navigation which is opted between Tmap and
        // KakaoNavi as an installed app is first applied.
        fabNavi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                naviHelper = new ConnectNaviHelper(StationMapActivity.this, stnName, longitude, latitude);
            }
        });

    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onResume() {
        super.onResume();

        String title = mSettings.getString(Constants.VEHICLE_NAME, null);
        if(title != null) getSupportActionBar().setTitle(title);

        // When returning from the navigation, the navigation instance should be killed and garbage
        // collected.
        if(naviHelper != null) naviHelper = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        if(stationInfoTask != null) stationInfoTask = null;

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

        LatLng dest = new LatLng(latitude, longitude);

        // Add a marker in Sydney and move the camera
        googleMap.addMarker(new MarkerOptions().position(dest).title(stnName));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(dest));

        googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        try {
            googleMap.setMyLocationEnabled(true);
            MapsInitializer.initialize(this);
        } catch(SecurityException e) {
            //Log.e(LOG_TAG, "SecurityException: " + e.getMessage());
        }

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(dest, 15);
        //googleMap.moveCamera(cameraUpdate);
        googleMap.animateCamera(cameraUpdate);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            log.i("onOptionsItemSelected in SettingPrefActivity");
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
