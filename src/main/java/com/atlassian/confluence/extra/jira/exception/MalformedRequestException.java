package com.atlassian.confluence.extra.jira.exception;

import org.apache.commons.httpclient.ProtocolException;

/**
 * Signals a failure in requesting for a content from JIRA. eg. http 400.
 */
public class MalformedRequestException extends ProtocolException
{
    /**
     * Creates a new MalformedRequestException with a <tt>null</tt> detail message.
     */
    public MalformedRequestException()
    {
        super();
    }

    /**
     * Creates a new MalformedRequestException with the specified message.
     *
     * @param message the exception detail message
     */
    public MalformedRequestException(String message)
    {
        super(message);
    }

    /**
     * Creates a new MalformedRequestException with the specified detail message and cause.
     *
     * @param message the exception detail message
     * @param cause the <tt>Throwable</tt> that caused this exception, or <tt>null</tt>
     * if the cause is unavailable, unknown, or not a <tt>Throwable</tt>
     *
     */
    public MalformedRequestException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
