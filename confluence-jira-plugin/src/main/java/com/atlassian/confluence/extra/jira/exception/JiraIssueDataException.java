package com.atlassian.confluence.extra.jira.exception;

public class JiraIssueDataException extends RuntimeException
{
    public JiraIssueDataException() {
        super();
    }

    public JiraIssueDataException(String message) {
        super(message);
    }

    public JiraIssueDataException(String message, Throwable causes) {
        super(message, causes);
    }
}
