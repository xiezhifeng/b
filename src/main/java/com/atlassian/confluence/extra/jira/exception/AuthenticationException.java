package com.atlassian.confluence.extra.jira.exception;

import org.apache.commons.httpclient.ProtocolException;

/**
 * Signals a failure in authentication process with JIRA. eg. http 401
 */
public class AuthenticationException extends ProtocolException
{

    /**
     * Creates a new AuthenticationException with a <tt>null</tt> detail message.
     */
    public AuthenticationException()
    {
        super();
    }

    /**
     * Creates a new AuthenticationException with the specified message.
     *
     * @param message the exception detail message
     */
    public AuthenticationException(String message)
    {
        super(message);
    }

    /**
     * Creates a new AuthenticationException with the specified detail message and cause.
     *
     * @param message the exception detail message
     * @param cause the <tt>Throwable</tt> that caused this exception, or <tt>null</tt>
     * if the cause is unavailable, unknown, or not a <tt>Throwable</tt>
     *
     */
    public AuthenticationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
