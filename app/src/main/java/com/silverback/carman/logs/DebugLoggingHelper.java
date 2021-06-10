package com.silverback.carman.logs;


import android.os.SystemClock;
import android.util.Log;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.collection.ArrayMap;

/**
 * Uses {@link android.util.Log} to implement the {@link LoggingHelper} interface
 */

final class DebugLoggingHelper implements LoggingHelper {

    @VisibleForTesting
    final String tag;
    private final ArrayMap<String, Long> loggingSessionIdToTimeInMillis;

    public DebugLoggingHelper(@NonNull Class cls) {
        loggingSessionIdToTimeInMillis = new ArrayMap<>();
        // Substring the tag name out of the class name as long as the max length.
        this.tag = cls.getSimpleName().substring(0,
                cls.getSimpleName().length() > 23 ? 23 : cls.getSimpleName().length());
    }

    @Override
    public void d(@NonNull String msg, Object... args) {
        Log.d(tag, format(msg, args));
    }

    @Override
    public void i(@NonNull String msg, Object... args) {
        Log.i(tag, format(msg,args));
    }

    @Override
    public void w(@NonNull String msg, Object...args) {
        Log.w(tag, format(msg, args));
    }

    @Override
    public void w(@NonNull Throwable throwable, @NonNull String msg, Object... args) {
        Log.w(tag, format(msg, args), throwable);
    }

    @Override
    public void e(@NonNull String msg, Object...args) {
        Log.w(tag, format(msg, args));
    }

    @Override
    public void e(@NonNull Throwable throwable, @NonNull String msg, Object... args) {
        Log.w(tag, format(msg, args), throwable);
    }

    private String format(@NonNull String msg, Object... args) {
        return String.format(Locale.ENGLISH, msg, args);
    }

    @Override
    public String startLoggingTime(@NonNull String sessionId) {
        loggingSessionIdToTimeInMillis.put(sessionId, SystemClock.elapsedRealtime());
        return sessionId;
    }

    @Override
    public void stopLoggingTime(@NonNull String sessionId) {
        Long startTime = loggingSessionIdToTimeInMillis.get(sessionId);
        if(null == startTime) {
            throw new IllegalArgumentException(String.format("no session with id '%s'", sessionId));
        }

        long elapsedTime = SystemClock.elapsedRealtime() - startTime;
        Log.d(tag, format("%s took %d ms", sessionId, elapsedTime));
    }
}
