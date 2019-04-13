package com.silverback.carman2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public class StationMapActivity extends BaseActivity implements OnMapReadyCallback {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationMapActivity.class);

    // Objects
    private GoogleMap mMap;
    private FirebaseFirestore db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_map);

        // Set ToolBar as ActionBar and attach Home Button and title on it.
        Toolbar mapToolbar = findViewById(R.id.tb_map);
        setSupportActionBar(mapToolbar);
        ActionBar ab = getSupportActionBar();
        if(ab != null) ab.setDisplayHomeAsUpEnabled(true);

        TextView tvName = findViewById(R.id.tv_name);
        TextView tvAddrs = findViewById(R.id.tv_address);
        TextView tvPrice = findViewById(R.id.tv_price);
        TextView tvCarwash = findViewById(R.id.tv_carwash);
        TextView tvService = findViewById(R.id.tv_service);
        TextView tvCVS = findViewById(R.id.tv_cvs);

        Bundle info = getIntent().getExtras();
        if(info == null) return;

        float latitude = Float.valueOf(info.getString("xcoord", null));
        float longitude = Float.valueOf(info.getString("ycoord", null));

        tvName.setText(info.getString("name"));
        tvAddrs.setText(String.format("%s %20s", info.getString("address"), info.getString("tel")));
        tvCarwash.setText(String.format("%s%5s", getString(R.string.map_cardview_wash), info.getString("carwash")));
        tvService.setText(String.format("%s%5s", getString(R.string.map_cardview_service), info.getString("service")));
        tvCVS.setText(String.format("%s%5s", getString(R.string.map_cardview_cvs), info.getString("cvs")));


        log.i("Location: %s, %s", latitude, longitude);

        db = FirebaseFirestore.getInstance();
        DocumentReference stnRef = db.collection("stations").document(info.getString("code", null));
        Map<String, Object> data = new HashMap<>();
        data.put("addrs", info.getString("address", null));
        data.put("tel", info.getString("tel", null));
        data.put("carwash", info.getString("carwash", null));
        data.put("cvs", info.getString("cvs", null));
        data.put("service", info.getString("service", null));
        stnRef.update(data).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                log.i("Update complete");
            }
        });





        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.google_map);
        mapFragment.getMapAsync(this);
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
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
}
