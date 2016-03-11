package com.atlassian.confluence.extra.jira.cache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;


public class CacheLoggingUtils
{
    /**
     * If the error in not <code>null</code>, this helper method logs appropriate error message using provided logger
     *
     * @param logger to log a message
     * @param error error to log (if not null)
     * @param isError error level. If <code>true</code> error message with full stack trace is printed out, otherwise
     * warning with general exception details is printed.
     */
    public static void log(@Nonnull Logger logger, @Nullable Throwable error, boolean isError)
    {
        if (error != null)
        {
            if (isError)
            {
                logger.error("Caching error: ", error);
            }
            else
            {
                logger.warn("Caching error: ", getRootCauseMessage(error));
            }
        }
    }
}
