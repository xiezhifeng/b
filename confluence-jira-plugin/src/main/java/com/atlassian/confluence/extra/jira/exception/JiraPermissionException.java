package com.atlassian.confluence.extra.jira.exception;

import org.apache.commons.httpclient.ProtocolException;

public class JiraPermissionException extends ProtocolException {
    public JiraPermissionException() {
        super();
    }

    public JiraPermissionException(String message) {
        super(message);
    }

    public JiraPermissionException(String message, Throwable causes) {
        super(message, causes);
    }
}
