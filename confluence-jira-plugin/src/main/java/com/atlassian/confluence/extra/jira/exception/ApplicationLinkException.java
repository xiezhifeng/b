package com.atlassian.confluence.extra.jira.exception;

public class ApplicationLinkException extends RuntimeException {
    
    /**
     * 
     */
    private static final long serialVersionUID = -9194320818935797273L;

    public ApplicationLinkException()
    {
        super();
    }
    
    public ApplicationLinkException(String message)
    {
        super(message);
    }
    
    public ApplicationLinkException(String message, Throwable cause)
    {
        super(message, cause);
    }
    
    public ApplicationLinkException(Throwable cause) {
        super(cause);
    }
}
