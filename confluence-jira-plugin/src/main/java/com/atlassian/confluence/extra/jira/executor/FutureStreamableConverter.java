package com.atlassian.confluence.extra.jira.executor;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.util.i18n.I18NBean;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Objects.firstNonNull;
/**
 * Converts a future to an xhtml streamable, handling errors in the stream in by
 * writing error messages into the result.
 *
 */
public class FutureStreamableConverter implements Streamable
{
    private static final Logger LOGGER = Logger.getLogger(FutureStreamableConverter.class);

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
                logStreamableError(writer, getTimeoutErrorMsg(), new TimeoutException());
            }
        }
        catch (InterruptedException e)
        {
            logStreamableError(writer, getInterruptedErrorMsg(), e);
        }
        catch (ExecutionException e)
        {
            logStreamableError(writer, getExecutionErrorMsg(), e);
        }
        catch (TimeoutException e)
        {
            logStreamableError(writer, getTimeoutErrorMsg(), e);
        }
    }

    public String getTimeoutErrorMsg()
    {
        return firstNonNull(builder.timeoutErrorMsg, defaultMsg);
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
    private void logStreamableError(Writer writer, String exceptionKey, Exception e) throws IOException {
        if (exceptionKey != null) {
            String errorMessage = builder.i18NBean.getText(exceptionKey);
            writer.write(errorMessage);
            if (e != null) {
                LOGGER.error(errorMessage, e);
            }
        }
    }

    public static class Builder
    {
        private final Future<String> futureResult;
        private final ConversionContext context;
        private final I18NBean i18NBean;
        private String timeoutErrorMsg;
        private String interruptedErrorMsg;
        private String executionErrorMsg;

        public Builder(Future<String> futureResult, final ConversionContext context, I18NBean i18NBean)
        {
            this.futureResult = futureResult;
            this.context = context;
            this.i18NBean = i18NBean;
        }
        public Builder timeoutErrorMsg(String i18nErrorMsg)
        {
            timeoutErrorMsg = i18nErrorMsg;
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
