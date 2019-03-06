package com.silverback.carman2.logs;

import android.support.annotation.NonNull;

/**
 * No-operation implementation of the {@link LoggingHelper} interface
 */

public class NopLoggingHelper implements LoggingHelper {

    @Override
    public void d(@NonNull String msg, Object... args) {}

    @Override
    public void i(@NonNull String msg, Object... ags) {}

    @Override
    public void w(@NonNull String msg, Object... args) {}

    @Override
    public void w(@NonNull Throwable throwable, String msg, Object... args) {}

    @Override
    public void e(@NonNull String msg, Object... args){}

    @Override
    public void e(@NonNull Throwable throwable, String msg, Object... args){}

    @Override
    public String startLoggingTime(String sessionId){
        return sessionId;
    }

    @Override
    public void stopLoggingTime(String sessionId){}

}
