package it.com.atlassian.confluence.plugins.webdriver.model;

import javax.annotation.Nonnull;

public class SprintModel
{
    private final String name;
    private final SprintStatus status;

    public SprintModel(String name, SprintStatus status, @Nonnull BoardModel board)
    {
        this.name = name;
        this.status = status;
        board.addSprint(this);
    }

    public String getName()
    {
        return name;
    }

    public SprintStatus getStatus()
    {
        return status;
    }
}
