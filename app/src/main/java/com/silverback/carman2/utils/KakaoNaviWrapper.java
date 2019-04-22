package com.silverback.carman2.utils;

import android.app.Activity;

import com.kakao.kakaonavi.KakaoNaviParams;
import com.kakao.kakaonavi.KakaoNaviService;
import com.kakao.kakaonavi.Location;
import com.kakao.kakaonavi.NaviOptions;
import com.kakao.kakaonavi.options.CoordType;
import com.kakao.kakaonavi.options.RpOption;
import com.kakao.kakaonavi.options.VehicleType;
import com.silverback.carman2.logs.LoggingHelper;
import com.silverback.carman2.logs.LoggingHelperFactory;

public class KakaoNaviWrapper {

    // Logging
    private static final LoggingHelper log = LoggingHelperFactory.create(KakaoNaviWrapper.class);

    // Objects
    private KakaoNaviParams.Builder builder;

    public KakaoNaviWrapper(String destName, final double x, final double y) {
        log.i("Dest coord: %s, %s", x, y);
        final NaviOptions options = NaviOptions.newBuilder()
                .setCoordType(CoordType.WGS84)
                .setVehicleType(VehicleType.FIRST)
                .setRpOption(RpOption.SHORTEST).build();

        Location destination = Location.newBuilder(destName, x, y).build();
        builder = KakaoNaviParams.newBuilder(destination).setNaviOptions(options);
    }

    public void initKakoNavi(Activity activity) {
        KakaoNaviService.getInstance().navigate(activity, builder.build());
    }

}
