package com.atlassian.confluence.plugins.sprint.model;

import java.util.Date;

public class JiraSprintModel
{
    private String id;
    private String name;
    private String state;
    private int linkedPagesCount;
//    private Date startDate;
//    private Date endDate;
//    private Date completeDate;
    private String[] remoteLinks;
    private int daysRemaining;
    private int originBoardId;
    private String boardUrl;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public int getLinkedPagesCount() {
        return linkedPagesCount;
    }

    public void setLinkedPagesCount(int linkedPagesCount) {
        this.linkedPagesCount = linkedPagesCount;
    }

//    public Date getStartDate() {
//        return startDate;
//    }
//
//    public void setStartDate(Date startDate) {
//        this.startDate = startDate;
//    }
//
//    public Date getEndDate() {
//        return endDate;
//    }
//
//    public void setEndDate(Date endDate) {
//        this.endDate = endDate;
//    }
//
//    public Date getCompleteDate() {
//        return completeDate;
//    }
//
//    public void setCompleteDate(Date completeDate) {
//        this.completeDate = completeDate;
//    }

    public String[] getRemoteLinks() {
        return remoteLinks;
    }

    public void setRemoteLinks(String[] remoteLinks) {
        this.remoteLinks = remoteLinks;
    }

    public int getDaysRemaining() {
        return daysRemaining;
    }

    public void setDaysRemaining(int daysRemaining) {
        this.daysRemaining = daysRemaining;
    }

    public void setOriginBoardId(int originBoardId) {
        this.originBoardId = originBoardId;
    }

    public int getOriginBoardId() {
        return originBoardId;
    }

    public String getBoardUrl() {
        return boardUrl;
    }

    public void setBoardUrl(String boardUrl) {
        this.boardUrl = boardUrl;
    }
}
