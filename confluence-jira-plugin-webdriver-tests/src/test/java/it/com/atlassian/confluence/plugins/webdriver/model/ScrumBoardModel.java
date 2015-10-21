package it.com.atlassian.confluence.plugins.webdriver.model;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class ScrumBoardModel extends BoardModel
{
    private List<SprintModel> sprints;

    public ScrumBoardModel(String name)
    {
        super(name);
        sprints = new ArrayList<SprintModel>();
    }

    public ScrumBoardModel(String name, @Nonnull List<SprintModel> sprint)
    {
        super(name);
        this.sprints = sprint;
    }

    public List<SprintModel> getSprints()
    {
        return sprints;
    }

    public void addSprint(SprintModel sprint)
    {
        sprints.add(sprint);
    }
}
