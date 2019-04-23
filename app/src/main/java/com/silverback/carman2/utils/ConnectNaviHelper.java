package com.silverback.carman2.utils;

import android.app.Activity;
import android.content.Context;

import com.kakao.kakaonavi.KakaoNaviParams;
import com.kakao.kakaonavi.KakaoNaviService;
import com.kakao.kakaonavi.Location;
import com.kakao.kakaonavi.NaviOptions;
import com.kakao.kakaonavi.options.CoordType;
import com.kakao.kakaonavi.options.RpOption;
import com.kakao.kakaonavi.options.VehicleType;
import com.silverback.carman2.BaseActivity;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;
import com.skt.Tmap.TMapTapi;

public class ConnectNaviHelper {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(ConnectNaviHelper.class);

    // Objects
    private TMapTapi tMap;
    private KakaoNaviParams.Builder builder;

    public ConnectNaviHelper(Activity activity, String destName, final double x, final double y) {
        log.i("Dest coord: %s, %s", x, y);

        tMap = new TMapTapi(activity);

        // Check if TMap is installed at first. Otherwise, secondly connect to KakaoNavi.
        if(tMap.isTmapApplicationInstalled()) {
            initTmap(destName, (float)x, (float)y);

        } else {
            final NaviOptions options = NaviOptions.newBuilder()
                    .setCoordType(CoordType.WGS84)
                    .setVehicleType(VehicleType.FIRST)
                    .setRpOption(RpOption.SHORTEST).build();

            Location destination = Location.newBuilder(destName, x, y).build();
            builder = KakaoNaviParams.newBuilder(destination).setNaviOptions(options);
            initKakoNavi(activity);
        }
    }

    // Initiate Tmap if it is installed.
    private void initTmap(String name, float x, float y) {
        tMap.setSKTMapAuthentication("ae26df85-bc3f-45d4-bfb3-08eef0d1fff7");
        tMap.invokeRoute(name, x, y);
    }

    // Initiate KakaoNavi, which connects to the naviation if it is installed; otherwise, it auto
    // connects it with WebView.
    private void initKakoNavi(Context context) {
        KakaoNaviService.getInstance().navigate(context, builder.build());
    }

}
