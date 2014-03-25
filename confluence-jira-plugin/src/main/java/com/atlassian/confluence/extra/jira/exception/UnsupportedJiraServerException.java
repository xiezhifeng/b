package com.atlassian.confluence.extra.jira.exception;

import com.atlassian.confluence.macro.MacroExecutionException;

/**
 * Exception class used for batch request to JIRA only
 * It will be thrown if JIRA Server version detected is less than 6.0.2
 */
public class UnsupportedJiraServerException extends Exception {
    // empty
}
