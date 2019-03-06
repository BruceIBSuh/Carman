package com.silverback.carman2.logs;

import android.support.annotation.NonNull;

/**
 * Provides a similar API to {@link android.util.Log} for logging with a stopwatch extension.
 * All log messages can have arguments like in {@link String#format(String, Object...)}.
 * <p/>
 * To obtain a LoggingHelper instance use the {@link LoggingHelperFactory}.
 * <p/>
 * Implementation must not be thread-safe!!!!
 */
public interface LoggingHelper {
    void d(@NonNull String msg, Object... args);
    void i(@NonNull String msg, Object... args);
    void w(@NonNull String msg, Object... args);
    void w(@NonNull Throwable throwable, String msg, Object... args);
    void e(@NonNull String msg, Object... args);
    void e(@NonNull Throwable throwable, String msg, Object... args);

    String startLoggingTime(String sessionId);
    void stopLoggingTime(String sessionId);

}
