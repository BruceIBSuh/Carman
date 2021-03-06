package com.silverback.carman;

import android.Manifest;
import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.geometry.Tm128;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapOptions;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.util.FusedLocationSource;
import com.silverback.carman.adapters.StationMapAdapter;
import com.silverback.carman.databinding.ActivityStationMapBinding;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.threads.StationGasRunnable;

import java.util.ArrayList;
import java.util.List;

public class StationMapActivity extends BaseActivity implements OnMapReadyCallback {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(StationMapActivity.class);
//    private static final int PERMISSION_REQUEST_CODE = 100;
//    private static final String[] PERMISSIONS = {
//            Manifest.permission.ACCESS_FINE_LOCATION,
//            Manifest.permission.ACCESS_COARSE_LOCATION
//    };

    // Objects
    private StationGasRunnable.Item stnDetail;
    private ActivityStationMapBinding binding;
    private FirebaseFirestore mDB;
    private DocumentSnapshot document;
    private FusedLocationSource fusedLocationSource;
    private NaverMap naverMap;
    private StationMapAdapter commentAdapter;
    private String stnName;
    private String stnId;

    //@SuppressWarnings("ConstantConditions")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStationMapBinding.inflate(getLayoutInflater());
        View rootView = binding.getRoot();
        setContentView(rootView);
        // Intent data from MainActivity
        //stnId = getIntent().getStringExtra("gasStationId");
        log.i("station id: %s", stnId);

        // Set ToolBar as ActionBar and attach Home Button and title on it.
        setSupportActionBar(binding.tbMap);
        ActionBar ab = getSupportActionBar();
        if(ab != null) ab.setDisplayHomeAsUpEnabled(true);

        if(getIntent() != null) {
            stnDetail = getIntent().getParcelableExtra("stationDetail");
        }

        // Call Naver map to MapFragment. Select alternatively either MapFragment or MapView.
        // When using MapView instead, the activity lifecycle should be considered.
        createNaverMap();

        // Instantiate Objects
        mDB = FirebaseFirestore.getInstance();
        fusedLocationSource = new FusedLocationSource(this, 100);//Naver api for getting the current position

        // Create RecyclerView for displayimg comments
        binding.recyclerComments.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerComments.setItemViewCacheSize(20);
        binding.recyclerComments.setDrawingCacheEnabled(true);

        // Retrieve the station data from Firestore.
        /*
        DocumentReference docRef = firestore.collection("gas_station").document(stnId);
        docRef.get().addOnCompleteListener(task -> {
            if(task.isSuccessful()) {
                log.i("gas station map:%s", task.getResult());
                this.document = task.getResult();
                //if(document.exists()) dispStationInfo();
            }
        });
        */
        final String stnId = stnDetail.getStnId();
        Query queryComment = mDB.collection("gas_eval").document(stnId).collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING);
        queryComment.get().addOnCompleteListener(querySnapshot -> {
            if(querySnapshot.isSuccessful()) {
                List<DocumentSnapshot> snapshotList = new ArrayList<>();
                for(DocumentSnapshot document : querySnapshot.getResult()) {
                    snapshotList.add(document);
                }
                log.i("snaplist: %s", snapshotList.size());
                //commentAdapter = new StationMapAdapter(stnId, snapshotList);
                commentAdapter = new StationMapAdapter(stnDetail, snapshotList);
                binding.recyclerComments.setAdapter(commentAdapter);
            }
        });

        /*
        firestore.collection("gas_eval").document(stnId).collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING).get()
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()) {
                        log.i("gas evaluation");
                        List<DocumentSnapshot> snapshotList = new ArrayList<>();
                        for(DocumentSnapshot document : task.getResult()) {
                            snapshotList.add(document);
                        }

                        commentAdapter = new StationMapAdapter(snapshotList);
                        binding.recyclerComments.setAdapter(commentAdapter);
                    }
                }).addOnFailureListener(e -> {});
        */




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
        /*
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
                    binding.tvServiceItem.setText(snapshot.getString("stn_name"));
                    binding.tvAddress.setText(String.format("%s%15s", snapshot.getString("new_addrs"), snapshot.getString("phone")));
                    //binding.tvCarwashPayment.setText(String.format("%s%5s", getString(R.string.map_cardview_wash), carwash));
                    //binding.tvService.setText(String.format("%s%5s", getString(R.string.map_cardview_service), service));
                    //binding.tvCvs.setText(String.format("%s%5s", getString(R.string.map_cardview_cvs), cvs));

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
        */

        // Retrive comments on the gas station from "comments" collection which is contained in
        // a station document.

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

    @Override
    public void getPermissionResult(Boolean isPermitted) {}


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,  @NonNull int[] grantResults) {
        if (fusedLocationSource.onRequestPermissionsResult(
                requestCode, permissions, grantResults)) {
            if (!fusedLocationSource.isActivated()) { // Permission denied
                naverMap.setLocationTrackingMode(LocationTrackingMode.None);
            }
            return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

     */


    //@SuppressWarnings("ConstantConditions")
    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        this.naverMap = naverMap;
        naverMap.setLocationSource(fusedLocationSource);
        final String perm = Manifest.permission.ACCESS_FINE_LOCATION;
        final String rationale = "permission required to access location";
        checkRuntimePermission(binding.getRoot(), perm, rationale, () -> {
            naverMap.setLocationTrackingMode(LocationTrackingMode.Follow);

            //double x = document.getDouble("katec_x");
            //double y = document.getDouble("katec_y");
            double x = stnDetail.getX();
            double y = stnDetail.getY();
            if(x != -1 || y != -1)  displayMap(x, y);
        });


    }

    /*
    @SuppressWarnings("ConstantConditions")
    private void dispStationInfo() throws NullPointerException {
        binding.inclStnInfo.tvStnName.setText(document.get("stn_name", String.class));
        binding.inclStnInfo.tvStnAddrs.setText(document.get("new_addrs", String.class));

        boolean isWash = (boolean)document.get("carwash");
        boolean isCvs = (boolean)document.get("cvs");
        boolean isSvc = (boolean)document.get("service");
        log.i("boolean values:%s %s %s", isWash, isCvs, isSvc);
        String wash = (isWash)? getString(R.string.map_value_ok):getString(R.string.map_value_not_ok);
        String cvs = (isCvs)? getString(R.string.map_value_yes):getString(R.string.map_value_no);
        String svc = (isSvc)? getString(R.string.map_value_yes):getString(R.string.map_value_no);

        binding.inclStnInfo.tvWash.setText(wash);
        binding.inclStnInfo.tvService.setText(cvs);
        binding.inclStnInfo.tvCvs.setText(svc);
    }

     */


    private void createNaverMap() {
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.frame_map);
        if(mapFragment == null) {
            NaverMapOptions options = new NaverMapOptions()
                    .mapType(NaverMap.MapType.Navi)
                    .locationButtonEnabled(true)
                    .scrollGesturesEnabled(true)
                    .zoomGesturesEnabled(true);

            mapFragment = MapFragment.newInstance(options);
            fm.beginTransaction().add(R.id.frame_map, mapFragment).commit();
        }

        mapFragment.getMapAsync(this);
    }

    private void displayMap(double x, double y) throws NullPointerException {
        Tm128 tm128 = new Tm128(x, y);
        LatLng coords = tm128.toLatLng();

        CameraUpdate cameraUpdate = CameraUpdate.scrollAndZoomTo(coords, 13);
        naverMap.moveCamera(cameraUpdate);

        Marker marker = new Marker();
        marker.setPosition(coords);
        marker.setWidth(50);
        marker.setHeight(80);
        marker.setMap(naverMap);
    }
}
