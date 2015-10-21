package it.com.atlassian.confluence.plugins.webdriver.model;

import java.util.List;

public class KanbanBoardModel extends BoardModel
{
    public KanbanBoardModel(String name)
    {
        super(name);
    }

    public List<SprintModel> getSprints()
    {
        throw new UnsupportedOperationException("Kanban board does not have any sprint to get");
    }

    public void addSprint(SprintModel sprint)
    {
        throw new UnsupportedOperationException("Can not add a sprint to a Kanban board");
    }
}
