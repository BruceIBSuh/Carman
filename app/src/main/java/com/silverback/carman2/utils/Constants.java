package com.silverback.carman2.utils;

public class Constants {

    // Category
    public static final int GAS = 0;
    public static final int SVC = 1;
    public static final int TOTAL = 2;
    public static final int BOARD = 3;

    //
    public static final int MAX_FAVORITE = 10;
    public static final String MIN_RADIUS = "250"; //Current station locating radius
    public static final int GEOFENCE_RADIUS = 250; //Geofence zoning radius
    public static final int GEOFENCE_LOITERING_TIME = 1000 * 60; //setLoiteringTime()for GEOFENCE_TRANSITION_DWELL
    public static final float GEOFENCE_FAVORITE_MATCH_RADIUS = 50f; //when querying Favorite based on GeofenceEvent.getTriggeringLocation()
    public static final int GEOFENCE_RESPONSE_TIME = 1000 * 60 * 5;

    public static final long OPINET_UPDATE_INTERVAL = 1000 * 60;// * 60 * 6;//Interval for downloading gas prices from Opinet (6 hrs)
    public static final float UPDATE_DISTANCE = 50f; //Distance difference to update near stations
    static final int INTERVAL = 1000 * 10; //Location update interval set by the app
    static final int FASTEST_INTERVAL = 1000; //Fastest location update interval set by any app.
    static final int MAX_WAIT = 1000 * 30; //Mas latency to receive location info.

    // ExpensePagerFragment max pages of ExpRecentPagerAdapter
    public static final int NUM_RECENT_PAGES = 5;

    // Board Pagination
    public static final int NUM_BOARD_PAGES = 4;
    static final int PAGINATION = 20;
    //public static final int ICON_SIZE = 50;

    // Notification Snooze
    public static final int SNOOZE_DURATION = 1000 * 60 * 60;

    // Image and Icon Size
    public static final int ICON_SIZE_TOOLBAR = 50;
    public static final int ICON_SIZE_PREFERENCE = 40;
    public static final int ICON_SIZE_POSTING_LIST = 35;
    public static final int IMAGESPAN_THUMBNAIL_SIZE = 36;
    public static final int MAX_ICON_SIZE = (1024 * 1024) / 2;
    public static final int MAX_IMAGE_SIZE = 1024 * 100;
    public static final int MAX_ATTACHED_IMAGE_NUMS = 6;//limit of the images attached with the post.

    // Board
    public static final int BOARD_RECENT = 0;
    public static final int BOARD_POPULAR = 1;
    public static final int BOARD_AUTOCLUB = 2;
    public static final int BOARD_NOTIFICATION = 3;

    public static final int SETTING_YEARS = 20;

    // File names for FileProvider
    public static final String FILE_CACHED_AVG_PRICE = "com.silverback.carman2.AVG_PRICE";
    public static final String FILE_CACHED_SIDO_PRICE = "com.silverback.carman2.SIDO_PRICE";
    public static final String FILE_CACHED_SIGUN_PRICE = "com.silverback.carman2.SIGUN_PRICE";
    public static final String FILE_CACHED_NEAR_STATIONS = "com.silverback.carman2.STN_LIST";
    public static final String FILE_FAVORITE_PRICE = "com.silverback.carman2.FAV_PRICE";
    public static final String FILE_DISTRICT_CODE = "com.silverback.carman2.DIST_CODE";
    public static final String OPINET_LAST_UPDATE = "com.silverback.carman2.UPDATE";
    public static final String FILE_IMAGES = "com.silverback.carman2.fileprovider";
    public static final String FILE_AUTO_DATA = "com.silverback.carman2.autodata";


    // SharedPreferences key name, which is the key name of each preference.xml
    public static final String USER_NAME = "carman_pref_nickname";
    public static final String USER_IMAGE = "carman_pref_profile_image";
    public static final String AUTO_DATA = "carman_pref_auto";
    public static final String AUTO_MAKER = "carman_pref_auto_maker";
    public static final String AUTO_MODEL = "carman_pref_auto_model";
    public static final String AUTO_TYPE = "carman_pref_auto_type";
    public static final String AUTO_YEAR = "carman_pref_auto_year";

    public static final String NUMBER = "carman_pref_cb_number";
    public static final String MODEL = "carman_pref_et_model";
    public static final String ODOMETER = "pref_odometer";
    public static final String AVERAGE = "pref_et_avg";
    public static final String PAYMENT = "carman_pref_payment";
    public static final String FUEL = "carman_pref_ls_fuel";
    public static final String DISTRICT = "pref_dialog_district";
    public static final String DISTRICT_NAME = "carman_pref_dialog_district_name";
    public static final String SEARCHING_RADIUS = "pref_searching_radius";
    public static final String ORDER = "carman_pref_ls_station_order";
    public static final String LOCATION_UPDATE = "pref_location_update";
    public static final String FAVORITE = "pref_favorite_list";
    public static final String SERVICE_PERIOD = "carman_pref_ls_service_period";

    public static final String THEME = "carman_pref_ls_theme";
    public static final String GUIDE = "carman_pref_intro_guide";
    public static final String CoverImageWidth = "com.ibnco.carman.coverimage.width";
    public static final String CoverImageHeight = "com.ibnco.carman.coverimage.height";
    public static final String CODE = "carman_pref_district_code";
    public static final String SERVICE_ITEMS = "carman_service_checklist";

    public static final String GEOFENCE_LIST = "geofence_list_for_reboot";

    // Geofence Notification
    public static final String CHANNEL_ID = "com.silverback.carman2";
    public static final String NOTI_TAG = "com.silverback.carman2.notification";
    public static final String NOTI_ID = "com.silverback.carman2.notiId";
    public static final String NOTI_GEOFENCE= "com.silverback.carman2.geofencetranstionservice.geofencing";
    public static final String NOTI_SNOOZE = "com.silverback.carman2.geofencetransitionservice.snooze";
    public static final String NOTI_DISMISS = "com.silverback.carman2.geofencetransitionservice.dismiss";

    // Geofence Keys
    public static final String GEO_CATEGORY = "com.silverback.carman2.geofence.category";
    public static final String GEO_INTENT = "com.silverback.carman2.geofence.pendingintent";
    public static final String GEO_NAME = "com.silverback.carman2.geofence.name";
    public static final String GEO_ID = "com.silverback.carman2.geofence.id";
    public static final String GEO_TIME = "com.silverback.carman2.geofence.time";
    public static final String GEO_ADDRS = "com.silverback.carman2.geofence.addrs";
    public static final String GEO_LOCATION = "com.silverback.carman2.geofence.location";



    // FetchAddrsIntentService resultCode
    public static final String GEOCODER_ADDRS_DATA_KEY = "com.ibnco.carman.geocoder_addrs";
    public static final int GEOCODER_ADDRS_FAIL = -1;
    public static final int GEOCODER_ADDRS_LOCATION_SUCCESS = 1;
    public static final int GEOCODER_ADDRS_NAME_SUCCESS = 2;


}
