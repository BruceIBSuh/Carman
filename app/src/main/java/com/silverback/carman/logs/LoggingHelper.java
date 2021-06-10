package com.silverback.carman.logs;

/**
 * Provides a similar API to {@link android.util.Log} for logging with a stopwatch extension.
 * All log messages can have arguments like in {@link String#format(String, Object...)}.
 * <p/>
 * To obtain a LoggingHelper instance use the {@link LoggingHelperFactory}.
 * <p/>
 * Implementation must not be thread-safe!!!!
 */
public interface LoggingHelper {
    void d(String msg, Object... args);
    void i(String msg, Object... args);
    void w(String msg, Object... args);
    void w(Throwable throwable, String msg, Object... args);
    void e(String msg, Object... args);
    void e(Throwable throwable, String msg, Object... args);

    String startLoggingTime(String sessionId);
    void stopLoggingTime(String sessionId);

}
