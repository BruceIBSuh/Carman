package com.silverback.carman.threads;

import static com.silverback.carman.threads.HydroStationListTask.HYDRO_STATE_FAIL;
import static com.silverback.carman.threads.HydroStationListTask.HYDRO_STATE_SUCCEED;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Process;
import android.text.TextUtils;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.model.Document;
import com.silverback.carman.coords.GeoPoint;
import com.silverback.carman.coords.GeoTrans;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;
import com.silverback.carman.utils.ExcelToJsonUtil;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HydroStationListRunnable implements Runnable {

    private static final LoggingHelper log = LoggingHelperFactory.create(HydroStationListRunnable.class);

    private FirebaseFirestore mDB;
    private List<HydroStationObj> hydroList;
    private final ExcelToJsonUtil excelToJsonUtil;
    private final HydroStationCallback callback;
    private final Context context;

    //private final String encodingKey = "Wd%2FkK0BbiWJlv1Rj9oR0Q7WA0aQ0UO3%2FY11uMkriK57e25VBUaNk1hQxQWv0svLZln5raxjA%2BFuCXzqm8pWu%2FQ%3D%3D";
    //private final String key = "Wd/kK0BbiWJlv1Rj9oR0Q7WA0aQ0UO3/Y11uMkriK57e25VBUaNk1hQxQWv0svLZln5raxjA+FuCXzqm8pWu/Q==";
    //private final String baseUrl = "https://api.odcloud.kr/api/15090186/v1/uddi:ed364e3a-4aba-41c8-88ab-cae488761eef";


    public interface HydroStationCallback {
        Location getHydroLocation();
        void setHydroStationThread(Thread thread);
        //void setHydroList(List<ExcelToJsonUtil.HydroStationObj> hydroList);
        void setFirebaseHydroList(List<HydroStationObj> hydroList);
        void handleTaskState(int state);

    }

    public HydroStationListRunnable(Context context, HydroStationCallback callback) {
        this.callback = callback;
        this.context = context;
        excelToJsonUtil = ExcelToJsonUtil.getInstance();
        mDB = FirebaseFirestore.getInstance();
        hydroList = new ArrayList<>();
    }

    @Override
    public void run() {
        callback.setHydroStationThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        Location location = callback.getHydroLocation();
        /*
        GeoPoint in_pt = new GeoPoint(location.getLongitude(), location.getLatitude());
        GeoPoint tm_pt = GeoTrans.convert(GeoTrans.GEO, GeoTrans.TM, in_pt);
        GeoPoint katec_pt = GeoTrans.convert(GeoTrans.TM, GeoTrans.KATEC, tm_pt);
        float x = (float) katec_pt.getX();
        float y = (float) katec_pt.getY();
        */
        mDB.collection("hydro_station").get().addOnSuccessListener(snapshots -> {
            if(snapshots != null && snapshots.size() > 0) {
                float[] results = new float[3];
                for(DocumentSnapshot document : snapshots) {
                    Object objLat = document.get("lat");
                    Object objLng = document.get("lng");

                    if(objLat != null && objLng != null) {
                        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                                (double)objLat, (double)objLng, results
                        );
                    }

                    int distance = (int) results[0];
                    if (distance < 10000) {
                        HydroStationObj obj = document.toObject(HydroStationObj.class);
                        if(obj != null) obj.setDistance(distance);
                        hydroList.add(obj);
                    }
                }

                callback.setFirebaseHydroList(hydroList);
            }


        }).addOnFailureListener(e -> {
            log.e("Hydro failed");
            e.printStackTrace();
        });

        /*
        //StringBuilder sb = new StringBuilder(baseUrl);
        String baseUrl = "https://www.ev.or.kr/portal/monitor/h2Excel";
        File hydroFile = new File(context.getCacheDir(), "hydro.xls");
        hydroFile.deleteOnExit();

        if(!hydroFile.exists()) {
            log.i("download hybrofile");
            try(BufferedInputStream bis = new BufferedInputStream(new URL(baseUrl).openStream());
                FileOutputStream fos = new FileOutputStream(hydroFile)) {
                int bytesRead;
                byte[] dataBuffer = new byte[1024];
                while((bytesRead = bis.read(dataBuffer)) != -1) {
                    fos.write(dataBuffer, 0, bytesRead);
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        try {
            excelToJsonUtil.setExcelFile(hydroFile);
            excelToJsonUtil.convExcelToList(0, 2, 3);
            List<ExcelToJsonUtil.HydroStationObj> infoList = excelToJsonUtil.getHydroList();

            callback.setHydroList(infoList);


            Geocoder geoCoder = new Geocoder(context);
            List<Address> address;
            for(ExcelToJsonUtil.HydroStationObj obj : infoList) {
                if(!TextUtils.isEmpty(obj.getAddrs())) {
                    try {
                        address = geoCoder.getFromLocationName(obj.getAddrs(), 5);
                        if (address.get(0) != null) {
                            double lat = Math.round(address.get(0).getLatitude() * 1000000) / 1000000.0;
                            double lng = Math.round(address.get(0).getLongitude() * 1000000) / 1000000.0;
                            log.i("Location: %s, %s, %s", obj.getName(), lat, lng);
                            obj.setLat(lat);
                            obj.setLng(lng);
                        }

                    } catch(IndexOutOfBoundsException e) { e.getLocalizedMessage(); }
                }

                mDB.collection("hydro_station").add(obj).addOnCompleteListener(task -> {
                    if(task.isSuccessful()) log.i("Upload hytro done");
                });
            }





        } catch (IOException | InvalidFormatException e) {
            e.printStackTrace();
            callback.handleTaskState(HYDRO_STATE_FAIL);
        }
        */
        /*
        try {
            URL url = new URL(baseUrl);
            InputStream is = new BufferedInputStream(url.openStream());
            File fileName = File.createTempFile(String.valueOf(is.hashCode()), ".xls");
            //Files.copy(is, fileName, StandardCopyOption.REPLACE_EXISTING);
            fileName.deleteOnExit();

            excelToJsonUtil.setExcelFile(fileName);
            JSONObject jsonObject = excelToJsonUtil.convExcelToJson();
            log.i("JSONObject: %s", jsonObject);
            try(FileOutputStream fos = new FileOutputStream(fileName)) {
                int read;
                byte[] buffer = new byte[1024];
                while((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
            }
        } catch(IOException | InvalidFormatException | JSONException e) {e.printStackTrace();}
        */
        /*
        try {
            File xlsFile = new File(context.getFilesDir(), "hydro.xls");
            excelToJsonUtil.setExcelFile(xlsFile);

            JSONObject jsonObject = excelToJsonUtil.convExcelToJson();
            log.i("JSONObject: %s", jsonObject);

            List<JSONObject> dataList = new ArrayList<>();
            JSONArray jsonArray = (JSONArray)jsonObject.get("Sheet1");
            for(int i = 0; i < jsonArray.length(); i++) {
                log.i("DataList: %s", jsonArray.get(i));
                JSONObject obj = (JSONObject) jsonArray.get(i);
                dataList.add(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

         */


        /*
        StringBuilder sb = new StringBuilder();
        try {
            sb.append("?").append(URLEncoder.encode("page", "UTF-8"));
            sb.append("=").append(URLEncoder.encode("1", "UTF-8"));
            sb.append("&").append(URLEncoder.encode("perPage", "UTF-8"));
            sb.append("=").append(URLEncoder.encode("100", "UTF-8"));
            sb.append("&").append(URLEncoder.encode("serviceKey", "UTF-8"));
            sb.append("=").append(URLEncoder.encode(key, "UTF-8"));

        } catch(IOException e) { e.printStackTrace(); }


        Call<HydroStationList> call = RetrofitClient.getIntance().getHydroStationService().getHydroStationList();
        call.enqueue(new Callback<HydroStationList>() {
            @Override
            public void onResponse(@NonNull Call<HydroStationList> call, @NonNull Response<HydroStationList> response) {
                HydroStationList hydroStationList = response.body();
                assert hydroStationList != null;

                List<HydroStationInfo> infoList = hydroStationList.getHydroStationInfo();
            }

            @Override
            public void onFailure(@NonNull Call<HydroStationList> call, @NonNull Throwable t) {
                log.e("response failed: %s", t);
            }
        });
        */

    }

    public static class HydroStationObj {
        private String name;
        private String addrs;
        private String phone;
        private String price;
        private String bizhour;
        private int charger;
        private int distance;
        private double lat;
        private double lng;

        public HydroStationObj(){}
        public HydroStationObj(String name, String addrs, String phone, String price, String bizhour,
                               int charger, double lat, double lng)
        {
            this.name = name;
            this.addrs = addrs;
            this.phone = phone;
            this.price = price;
            this.bizhour = bizhour;
            this.charger = charger;
            this.lat = lat;
            this.lng = lng;
        }

        public String getName() {
            return name;
        }

        public String getAddrs() {
            return addrs;
        }

        public String getPhone() {
            return phone;
        }

        public String getPrice() {
            return price;
        }

        public String getBizhour() {
            return bizhour;
        }

        public int getCharger() {
            return charger;
        }

        public double getLat() {
            return lat;
        }

        public double getLng() {
            return lng;
        }

        public void setDistance(int distance) {
            this.distance = distance;
        }

        public int getDistance() {
            return distance;
        }
    }


    /*
    public interface HydroStationService {
        String base_url = "https://api.odcloud.kr/api/";
        @GET("15090186/v1/uddi:ed364e3a-4aba-41c8-88ab-cae488761eef?page=1&perPage=20&serviceKey=" +
                "Wd%2FkK0BbiWJlv1Rj9oR0Q7WA0aQ0UO3%2FY11uMkriK57e25VBUaNk1hQxQWv0svLZln5raxjA%2BFuCXzqm8pWu%2FQ%3D%3D%20")

        Call<HydroStationList> getHydroStationList();
    }

    public static class HydroStationInfo {
        private String hydroName;
        private String hydrochgr;
        private String addrs;
        private String bizhour;
        private String price;
        private String phone;

        public String getHydroName() {
            return hydroName;
        }

        public void setHydroName(String hydroName) {
            this.hydroName = hydroName;
        }

        public String getHydrochgr() {
            return hydrochgr;
        }

        public void setHydrochgr(String hydrochgr) {
            this.hydrochgr = hydrochgr;
        }

        public String getAddrs() {
            return addrs;
        }

        public void setAddrs(String addrs) {
            this.addrs = addrs;
        }

        public String getBizhour() {
            return bizhour;
        }

        public void setBizhour(String bizhour) {
            this.bizhour = bizhour;
        }

        public String getPrice() {
            return price;
        }

        public void setPrice(String price) {
            this.price = price;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }



    }

    public static class HydroStationList {
        @SerializedName("data")
        private final List<HydroStationInfo> hydroInfoList;

        public HydroStationList(List<HydroStationInfo> hydroInfoList) {
            this.hydroInfoList = hydroInfoList;
        }

        public List<HydroStationInfo> getHydroStationInfo() {
            return hydroInfoList;
        }

    }

    public static class RetrofitClient {
        private final HydroStationService hydroStationService;

        private RetrofitClient() {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(HydroStationService.base_url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            hydroStationService = retrofit.create(HydroStationService.class);
        }

        // Bill-Pugh Singleton instance
        private static class LazyHolder {
            private static final RetrofitClient sInstance = new RetrofitClient();
        }
        public static RetrofitClient getIntance() {
            return LazyHolder.sInstance;
        }
        public HydroStationService getHydroStationService() {
            return hydroStationService;
        }
    }

     */

}
