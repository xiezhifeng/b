package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.ResourceAware;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.RequestCacheThreadLocal;

/**
 * A macro to import/fetch JIRA issues...
 */
public class StreamableJiraIssuesMacro extends JiraIssuesMacro implements StreamableMacro, EditorImagePlaceholder, ResourceAware
{
    
    private static final Logger LOGGER = Logger.getLogger(StreamableJiraIssuesMacro.class);
    
    public Streamable executeToStream(final Map<String, String> parameters, final Streamable body,
            final ConversionContext context) throws MacroExecutionException
    {

        final Future<String> futureResult = marshallMacroInBackground(parameters, context);
        
        return new Streamable()
        {
            @Override
            public void writeTo(Writer writer) throws IOException
            {
                try
                {
                    long remainingTimeout = context.getTimeout().getTime();
                    if (remainingTimeout > 0)
                    {
                        writer.write(futureResult.get(remainingTimeout, TimeUnit.MILLISECONDS));
                    }
                    else
                    {
                        logStreamableError(writer, "jiraissues.error.timeout", new TimeoutException());
                    }
                }
                catch (InterruptedException e)
                {
                    logStreamableError(writer, "jiraissues.error.interrupted", e);
                }
                catch (ExecutionException e)
                {
                    logStreamableError(writer, "jiraissues.error.execution", e);
                }
                catch (TimeoutException e)
                {
                    logStreamableError(writer, "jiraissues.error.timeout", e);
                }
            }
        };
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
            String errorMessage = getText(exceptionKey);
            writer.write(errorMessage);
            if (e != null) {
                LOGGER.error(errorMessage, e);
            }
        }
    }
    
    private Future<String> marshallMacroInBackground(final Map<String, String> parameters, final ConversionContext context)
    {
        //TODO switch to thread pool when the plugin thread pool is out
        return Executors.newSingleThreadExecutor().submit(new JIMFutureTask<String>(parameters, context, this, AuthenticatedUserThreadLocal.get(), RequestCacheThreadLocal.getRequestCache()));
    }
    
    public static class JIMFutureTask<V> implements Callable<V> {

        private final Map<String,String> parameters;
        private final ConversionContext context;
        private final StreamableJiraIssuesMacro jim;
        private final ConfluenceUser user;
        private Map requestCache;
        
        public JIMFutureTask(Map<String,String> parameters, ConversionContext context, StreamableJiraIssuesMacro jim, ConfluenceUser user, Map requestCache)
        {
            this.parameters = parameters;
            this.context = context;
            this.jim = jim;
            this.user = user;
            this.requestCache = requestCache;
        }

        // MacroExecutionException should be automatically handled by the marshaling chain
        public V call() throws MacroExecutionException
        {
            AuthenticatedUserThreadLocal.set(user);
            RequestCacheThreadLocal.setRequestCache(requestCache);
            return (V) jim.execute(parameters, null, context);
        }
        
    }
    
}
