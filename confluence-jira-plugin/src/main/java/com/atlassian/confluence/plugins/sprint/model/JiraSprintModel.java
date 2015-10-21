package com.atlassian.confluence.plugins.sprint.model;

public class JiraSprintModel
{
    private String id;
    private String name;
    private String state;
    private int originBoardId;
    private String boardUrl;

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getState()
    {
        return state;
    }

    public void setState(String state)
    {
        this.state = state;
    }


    public void setOriginBoardId(int originBoardId)
    {
        this.originBoardId = originBoardId;
    }

    public int getOriginBoardId()
    {
        return originBoardId;
    }

    public String getBoardUrl()
    {
        return boardUrl;
    }

    public void setBoardUrl(String boardUrl)
    {
        this.boardUrl = boardUrl;
    }
}
