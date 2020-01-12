package com.silverback.carman2.utils;

public class Constants {

    /**
     * Global Setting Constants for Geofence, Opinet download interval, near station radius.
     */
    public static final String MIN_RADIUS = "250"; //Current station locating radius

    public static final int GEOFENCE_RADIUS = 250; //Geofence zoning radius
    public static final int GEOFENCE_LOITERING_TIME = 1000 * 60; //setLoiteringTime()for GEOFENCE_TRANSITION_DWELL
    public static final float GEOFENCE_FAVORITE_MATCH_RADIUS = 50f; //when querying Favorite based on GeofenceEvent.getTriggeringLocation()
    //public static final int GEOFENCE_RESPONSE_TIME = 1000 * 60 * 5;

    public static final long OPINET_UPDATE_INTERVAL = 1000 * 60; // * 60 * 6;//Interval for downloading gas prices from Opinet (6 hrs)
    public static final float UPDATE_DISTANCE = 50f; //Distance difference to update near stations
    static final int INTERVAL = 1000 * 10; //Location update interval set by the app
    static final int FASTEST_INTERVAL = 1000; //Fastest location update interval set by any app.
    static final int MAX_WAIT = 1000 * 30; //Mas latency to receive location info.

    // Category
    public static final int GAS = 0;
    public static final int SVC = 1;
    public static final int MAX_FAVORITE = 10;

    // ExpensePagerFragment max pages of ExpRecentPagerAdapter
    public static final int NUM_RECENT_PAGES = 5;

    // Limit of the images attached with the post.
    public static final int MAX_ATTACHED_IMAGE = 6;

    // Board Pagination
    public static final int PAGINATION = 25;

    // File names for FileProvider
    public static final String FILE_CACHED_AVG_PRICE = "avgPrice";
    public static final String FILE_CACHED_SIDO_PRICE = "sidoPrice";
    public static final String FILE_CACHED_SIGUN_PRICE = "sigunPrice";
    public static final String FILE_CACHED_STATION_PRICE = "stationPrice";
    public static final String FILE_DISTRICT_CODE = "districtCode";
    public static final String FILE_CACHED_NEAR_STATIONS = "tmpStnList";
    public static final String OPINET_LAST_UPDATE = "com.ibnco.carman.UPDATE";



    // SharedPreferences key name, which is the key name of each preference.xml
    public static final String USER_NAME = "pref_nickname";
    public static final String VEHICLE = "pref_auto";
    public static final String AUTO_MAKER = "pref_auto_maker";
    public static final String AUTO_MODEL = "pref_auto_model";
    public static final String AUTO_TYPE = "pref_auto_type";
    public static final String AUTO_YEAR = "pref_auto_year";

    public static final String EDIT_IMAGE = "pref_edit_image";

    public static final String NUMBER = "carman_pref_cb_number";
    public static final String MODEL = "carman_pref_et_model";
    public static final String ODOMETER = "pref_odometer";
    public static final String AVERAGE = "pref_et_avg";
    public static final String PAYMENT = "carman_pref_payment";
    public static final String FUEL = "carman_pref_ls_fuel";
    public static final String DISTRICT = "pref_dialog_district";
    public static final String DISTRICT_NAME = "carman_pref_dialog_district_name";
    public static final String RADIUS = "pref_searching_radius";
    public static final String ORDER = "carman_pref_ls_station_order";
    public static final String LOCATION_UPDATE = "pref_location_update";
    public static final String FAVORITE = "pref_favorite_list";
    public static final String SERVICE_PERIOD = "carman_pref_ls_service_period";
    public static final String IMAGE = "carman_pref_change_image";
    public static final String THEME = "carman_pref_ls_theme";
    public static final String GUIDE = "carman_pref_intro_guide";
    public static final String CoverImageWidth = "com.ibnco.carman.coverimage.width";
    public static final String CoverImageHeight = "com.ibnco.carman.coverimage.height";
    public static final String CODE = "carman_pref_district_code";
    public static final String SERVICE_ITEMS = "carman_service_checklist";

    public static final String GEOFENCE_LIST = "geofence_list_for_reboot";

    // Notification
    public static final String CHANNEL_ID = "com.silverback.carman2";
    public static final String NOTI_TAG = "com.silverback.carman2.notification";
    public static final String NOTI_ID = "com.silverback.carman2.notiId";
    public static final String NOTI_GEOFENCE= "com.silverback.carman2.geofencetranstionservice.geofencing";
    public static final String NOTI_SNOOZE = "com.silverback.carman2.geofencetransitionservice.snooze";
    public static final String NOTI_DISMISS = "com.silverback.carman2.geofencetransitionservice.dismiss";

    // Geofence Keys
    public static final String GEO_CATEGORY = "com.silverback.carman2.geofence.category";
    public static final String GEO_INTENT = "com.ibnco.carman.geofence.pendingintent";
    public static final String GEO_NAME = "com.ibnco.carman.geofence.name";
    public static final String GEO_ID = "com.ibnco.carman.geofence.id";
    public static final String GEO_TIME = "com.ibnco.carman.geofence.time";
    public static final String GEO_LOCATION = "com.ibnco.carman.geofence.location";



    // FetchAddrsIntentService resultCode
    public static final String GEOCODER_ADDRS_DATA_KEY = "com.ibnco.carman.geocoder_addrs";
    public static final int GEOCODER_ADDRS_FAIL = -1;
    public static final int GEOCODER_ADDRS_LOCATION_SUCCESS = 1;
    public static final int GEOCODER_ADDRS_NAME_SUCCESS = 2;


}
