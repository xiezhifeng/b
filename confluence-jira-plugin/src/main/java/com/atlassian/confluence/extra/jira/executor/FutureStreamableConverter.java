package com.atlassian.confluence.extra.jira.executor;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.metrics.EventBuilder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.util.i18n.I18NBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Objects.firstNonNull;
/**
 * Converts a future to an xhtml streamable, handling errors in the stream in by
 * writing error messages into the result.
 *
 */
@ParametersAreNonnullByDefault
public class FutureStreamableConverter implements Streamable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(FutureStreamableConverter.class);

    private Builder builder;
    private static final String defaultMsg = "jira.streamable.macro.default.error";

    private FutureStreamableConverter(Builder builder)
    {
        this.builder = builder;
    }

    @Override
    public void writeTo(Writer writer) throws IOException
    {
        try
        {
            long remainingTimeout = builder.context.getTimeout().getTime();
            if (remainingTimeout > 0)
            {
                writer.write(builder.futureResult.get(remainingTimeout, TimeUnit.MILLISECONDS));
            }
            else
            {
                logStreamableError(writer, getExecutionTimeoutErrorMsg(), new TimeoutException());
            }
        }
        catch (InterruptedException e)
        {
            logStreamableError(writer, getInterruptedErrorMsg(), e);
        }
        catch (ExecutionException e)
        {
            Throwable cause = e.getCause();
            while (cause != null)
            {
                if (cause instanceof MacroExecutionException)
                {
                    // Predicted error, error message is expected to be localized before hand
                    logStreamableError(writer, cause.getMessage(), e);
                    return;
                }
                cause = cause.getCause();
            }
            // Generic unpredicted error
            logStreamableError(writer, getExecutionErrorMsg(), e);
        }
        catch (TimeoutException e)
        {
            logStreamableError(writer, getConnectionTimeoutErrorMsg(), e);
        }
        finally
        {
            builder.metrics.publish();
        }
    }

    public String getConnectionTimeoutErrorMsg()
    {
        return firstNonNull(builder.connectionTimeoutErrorMsg, defaultMsg);
    }

    public String getExecutionTimeoutErrorMsg()
    {
        return firstNonNull(builder.executionTimeoutErrorMsg, defaultMsg);
    }

    public String getInterruptedErrorMsg()
    {
        return firstNonNull(builder.interruptedErrorMsg, defaultMsg);
    }

    public String getExecutionErrorMsg()
    {
        return firstNonNull(builder.executionErrorMsg, defaultMsg);
    }

    /**
     * Exception handling method for Streamable execution
     * @param writer
     * @param exceptionKey key to be localized
     * @param e
     * @throws IOException
     */
    private void logStreamableError(Writer writer, @Nullable String exceptionKey, @Nullable Exception e) throws IOException
    {
        if (exceptionKey != null)
        {
            String errorMessage = builder.i18NBean.getText(exceptionKey);
            //writer.write(errorMessage);
            writer.write(JiraExceptionHelper.renderExceptionMessage(errorMessage));
            if (e != null)
            {
                LOGGER.warn(errorMessage);
                LOGGER.debug(errorMessage, e);
            }
        }
    }

    public static class Builder
    {
        private final Future<String> futureResult;
        private final ConversionContext context;
        private final I18NBean i18NBean;
        private final EventBuilder metrics;
        private String executionTimeoutErrorMsg;
        private String connectionTimeoutErrorMsg;
        private String interruptedErrorMsg;
        private String executionErrorMsg;

        public Builder(Future<String> futureResult, final ConversionContext context, I18NBean i18NBean, EventBuilder metrics)
        {
            this.futureResult = futureResult;
            this.context = context;
            this.i18NBean = i18NBean;
            this.metrics = metrics;
        }
        
        public Builder executionTimeoutErrorMsg(String i18nErrorMsg)
        {
            executionTimeoutErrorMsg = i18nErrorMsg;
            return this;
        }

        public Builder connectionTimeoutErrorMsg(String i18nErrorMsg)
        {
            connectionTimeoutErrorMsg = i18nErrorMsg;
            return this;
        }

        public Builder interruptedErrorMsg(String i18nErrorMsg)
        {
            interruptedErrorMsg = i18nErrorMsg;
            return this;
        }

        public Builder executionErrorMsg(String i18nErrorMsg)
        {
            executionErrorMsg = i18nErrorMsg;
            return this;
        }
        
        public FutureStreamableConverter build()
        {
            return new FutureStreamableConverter(this);
        }
    }
}
