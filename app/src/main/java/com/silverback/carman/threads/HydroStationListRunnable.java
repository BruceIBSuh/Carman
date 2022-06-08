package com.silverback.carman.threads;

import android.content.Context;
import android.location.Location;
import android.os.Process;

import androidx.annotation.NonNull;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.silverback.carman.logs.LoggingHelper;
import com.silverback.carman.logs.LoggingHelperFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

public class HydroStationListRunnable implements Runnable {

    private static LoggingHelper log = LoggingHelperFactory.create(HydroStationListRunnable.class);

    private HydroStationCallback callback;
    private Context context;
    private StringBuilder sb;

    private final String encodingKey = "Wd%2FkK0BbiWJlv1Rj9oR0Q7WA0aQ0UO3%2FY11uMkriK57e25VBUaNk1hQxQWv0svLZln5raxjA%2BFuCXzqm8pWu%2FQ%3D%3D";
    private final String key = "Wd/kK0BbiWJlv1Rj9oR0Q7WA0aQ0UO3/Y11uMkriK57e25VBUaNk1hQxQWv0svLZln5raxjA+FuCXzqm8pWu/Q==";
    private final String baseUrl = "https://api.odcloud.kr/api/15090186/v1/uddi:ed364e3a-4aba-41c8-88ab-cae488761eef";


    public interface HydroStationCallback {
        void setHydroStationThread(Thread thread);
        void setHydroStationList(List<HydroStationInfo> hydroList);
        Location getHydroLocation();
    }

    public HydroStationListRunnable(Context context, HydroStationCallback callback) {
        this.callback = callback;
        this.context = context;

    }


    @Override
    public void run() {
        callback.setHydroStationThread(Thread.currentThread());
        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        Location location = callback.getHydroLocation();
        //StringBuilder sb = new StringBuilder(baseUrl);
        sb = new StringBuilder();
        try {
            sb.append("?").append(URLEncoder.encode("page", "UTF-8"));
            sb.append("=").append(URLEncoder.encode("1", "UTF-8"));
            sb.append("&").append(URLEncoder.encode("perPage", "UTF-8"));
            sb.append("=").append(URLEncoder.encode("100", "UTF-8"));
            sb.append("&").append(URLEncoder.encode("serviceKey", "UTF-8"));
            sb.append("=").append(URLEncoder.encode(key, "UTF-8"));

        } catch(IOException e) { e.printStackTrace(); }


        Call<HydroStationList> call = RetrofitClient.getIntance().getHydroService().getHydroStationList();
        call.enqueue(new Callback<HydroStationList>() {
            @Override
            public void onResponse(
                    @NonNull Call<HydroStationList> call, @NonNull Response<HydroStationList> response) {
                log.i("response: %s", response);
                HydroStationList hydroStationList = response.body();
                assert hydroStationList != null;
                List<HydroStationInfo>  infoList = hydroStationList.getHydroStationInfo();
                log.i("infoList: %s", infoList.size());



            }

            @Override
            public void onFailure(
                    @NonNull Call<HydroStationList> call, @NonNull Throwable t) {
                log.e("response failed: %s", t);
            }
        });

    }

    public static class HydroStationInfo {

    }

    public static class HydroStationList {
        @SerializedName("data")
        private List<HydroStationInfo> hydroInfoList;

        public List<HydroStationInfo> getHydroStationInfo() {
            return hydroInfoList;
        }

    }

    public interface HydroStationService {
        String base_url = "https://api.odcloud.kr/api/";
        @GET("15090186/v1/uddi:ed364e3a-4aba-41c8-88ab-cae488761eef?page=1&perPage=20&serviceKey=" +
                "Wd%2FkK0BbiWJlv1Rj9oR0Q7WA0aQ0UO3%2FY11uMkriK57e25VBUaNk1hQxQWv0svLZln5raxjA%2BFuCXzqm8pWu%2FQ%3D%3D%20")

        Call<HydroStationList> getHydroStationList();
    }

    public static class RetrofitClient {
        private final HydroStationService hydroService;

        private RetrofitClient() {
            Retrofit retrofit = new Retrofit.Builder().baseUrl(HydroStationService.base_url)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            hydroService = retrofit.create(HydroStationService.class);
        }

        private static class LazyHolder {
            private static final RetrofitClient sInstance = new RetrofitClient();
        }

        public static RetrofitClient getIntance() {
            return LazyHolder.sInstance;
        }

        public HydroStationService getHydroService() {
            return hydroService;
        }
    }


}
