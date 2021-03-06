package com.silverback.carman.logs;

import com.silverback.carman.BuildConfig;

/**
 * Factory to obtain {@link LoggingHelper} instances
 */

public final class LoggingHelperFactory {

    // Apply Logs only in Debugging according to counting on BuildConfig.DEBUG, which is reported to
    // be exactly done as it is supposed to do, though.
    public static LoggingHelper create(Class cls) {
        if(BuildConfig.DEBUG) {
            return new DebugLoggingHelper(cls);
        } else {
            return new NopLoggingHelper();
        }
    }

}
