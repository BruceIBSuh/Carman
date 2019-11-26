package com.silverback.carman2;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.ibnco.carman.convertgeocoords.GeoPoint;
import com.ibnco.carman.convertgeocoords.GeoTrans;
import com.silverback.carman2.adapters.CommentRecyclerAdapter;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.silverback.carman2.utils.ConnectNaviHelper;

import java.util.ArrayList;
import java.util.List;

public class StationMapActivity extends BaseActivity implements OnMapReadyCallback {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationMapActivity.class);

    // Objects
    private ConnectNaviHelper naviHelper;
    private double xCoord, yCoord;
    private double longitude, latitude;
    private RecyclerView recyclerComments;
    private CommentRecyclerAdapter commentAdapter;

    // UIs
    private TextView tvName, tvAddrs, tvCarwash, tvService,tvCVS;
    private String stnName;



    @SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_map);

        final String stnId = getIntent().getStringExtra("stnId");
        log.i("Station ID: %s", stnId);

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Set ToolBar as ActionBar and attach Home Button and title on it.
        Toolbar mapToolbar = findViewById(R.id.tb_map);
        setSupportActionBar(mapToolbar);
        ActionBar ab = getSupportActionBar();
        if(ab != null) ab.setDisplayHomeAsUpEnabled(true);

        // UIs
        tvName = findViewById(R.id.tv_service_item);
        tvAddrs = findViewById(R.id.tv_address);
        tvCarwash = findViewById(R.id.tv_carwash);
        tvService = findViewById(R.id.tv_service);
        tvCVS = findViewById(R.id.tv_cvs);
        FloatingActionButton fabNavi = findViewById(R.id.fab_navi);

        recyclerComments = findViewById(R.id.recycler_stn_comments);
        recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        //recyclerComments.setItemViewCacheSize(20);
        //recyclerComments.setDrawingCacheEnabled(true);

        // When the fab is clicked, connect to a navigation which is opted between Tmap and
        // KakaoNavi as an installed app is first applied.
        /*
        fabNavi.setOnClickListener(view ->
                naviHelper = new ConnectNaviHelper(StationMapActivity.this, stnName, longitude, latitude)
        );
        */

        //StationListViewModel stnListModel = ViewModelProviders.of(this).get(StationListViewModel.class);

        /*
         * Handling task results
         * To be notified when the task succeeds, attach on an OnSuccessListener
         * To be notified when the task fails, attach on an OnFailureListener
         * To handle success and failure in the same listener, attach an OnCompleteListener.
         */
        // Retrive the gas station data from Firestore with the station id.
        firestore.collection("gas_station").document(stnId).get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                DocumentSnapshot snapshot = task.getResult();
                if(snapshot != null && snapshot.exists()) {
                    // Translate the boolean values to Strings.
                    String carwash = (snapshot.getBoolean("carwash"))?
                            getString(R.string.map_value_ok):getString(R.string.map_value_not_ok);
                    String service = (snapshot.getBoolean("service"))?
                            getString(R.string.map_value_yes):getString(R.string.map_value_no);
                    String cvs = (snapshot.getBoolean("cvs"))?
                            getString(R.string.map_value_yes):getString(R.string.map_value_no);

                    // Set the String values to TextViews
                    stnName = snapshot.getString("stn_name");
                    tvName.setText(snapshot.getString("stn_name"));
                    tvAddrs.setText(String.format("%s%15s", snapshot.getString("new_addrs"), snapshot.getString("phone")));
                    tvCarwash.setText(String.format("%s%5s", getString(R.string.map_cardview_wash), carwash));
                    tvService.setText(String.format("%s%5s", getString(R.string.map_cardview_service), service));
                    tvCVS.setText(String.format("%s%5s", getString(R.string.map_cardview_cvs), cvs));

                    xCoord = snapshot.getDouble("katec_x");
                    yCoord = snapshot.getDouble("katec_y");

                    // Convert KATEC to longitude and latitude to locate the station in the Google map.
                    GeoPoint katec_pt = new GeoPoint(xCoord, yCoord);
                    GeoPoint in_pt = GeoTrans.convert(GeoTrans.KATEC, GeoTrans.GEO, katec_pt);
                    longitude = in_pt.getX();
                    latitude = in_pt.getY();


                    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.google_map);

                    if(mapFragment != null) mapFragment.getMapAsync(this);

                }
            } else {
                log.e("Read the FireStore failed: %s", task.getException());
            }

        });

        // Retrive comments on the gas station from "comments" collection which is contained in
        // a station document.
        firestore.collection("gas_eval").document(stnId).collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING).get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        List<DocumentSnapshot> snapshotList = new ArrayList<>();
                        for(DocumentSnapshot document : task.getResult()) {
                            snapshotList.add(document);
                        }

                        commentAdapter = new CommentRecyclerAdapter(snapshotList);
                        recyclerComments.setAdapter(commentAdapter);
                    }
                }).addOnFailureListener(e -> {});
        /*
        // When the fab is clicked, connect to a navigation which is opted between Tmap and
        // KakaoNavi as an installed app is first applied.
        fabNavi.setOnClickListener(view ->
                naviHelper = new ConnectNaviHelper(StationMapActivity.this, stnName, longitude, latitude)
        );
         */



    }

    @Override
    public void onResume() {
        super.onResume();

        //String title = mSettings.getString(Constants.USER_NAME, null);
        //if(title != null) getSupportActionBar().setTitle(title);

        // When returning from the navigation, the navigation instance should be killed and garbage
        // collected.
        //if(naviHelper != null) naviHelper = null;
    }

    @Override
    public void onPause() {
        super.onPause();
        //if(stationInfoTask != null) stationInfoTask = null;

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
        googleMap.moveCamera(cameraUpdate);
        googleMap.animateCamera(cameraUpdate);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            log.i("onOptionsItemSelected in SettingPreferenceActivity");
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
