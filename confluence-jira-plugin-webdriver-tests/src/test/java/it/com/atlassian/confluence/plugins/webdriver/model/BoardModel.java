package it.com.atlassian.confluence.plugins.webdriver.model;

import java.util.List;

public abstract class BoardModel
{
    private final String name;

    public BoardModel(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public abstract List<SprintModel> getSprints();
    public abstract void addSprint(SprintModel sprint);
}
