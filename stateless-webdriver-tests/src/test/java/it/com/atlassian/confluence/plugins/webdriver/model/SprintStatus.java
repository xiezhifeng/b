package it.com.atlassian.confluence.plugins.webdriver.model;

public enum SprintStatus
{
    CLOSED("CLOSED"),
    ACTIVE("ACTIVE"),
    FUTURE("FUTURE");

    private final String label;

    SprintStatus(String label)
    {
        this.label = label;
    }
}
