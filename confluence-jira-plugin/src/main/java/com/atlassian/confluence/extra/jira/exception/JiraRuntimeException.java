package com.atlassian.confluence.extra.jira.exception;

public class JiraRuntimeException extends RuntimeException {
    public JiraRuntimeException() {
        super();
    }

    public JiraRuntimeException(String message) {
        super(message);
    }

    public JiraRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}
