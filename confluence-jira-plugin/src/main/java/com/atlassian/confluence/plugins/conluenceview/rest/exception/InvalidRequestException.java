package com.atlassian.confluence.plugins.conluenceview.rest.exception;

public class InvalidRequestException extends RuntimeException
{
    public InvalidRequestException(String message)
    {
        super(message);
    }
}
