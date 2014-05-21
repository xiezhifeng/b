package com.atlassian.confluence.plugins.jiracharts;

import java.util.List;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.plugins.jiracharts.model.StatTypeModel;

/**
 * The interface that defines the methods callers can invoke to set/get information about
 * statTypes in JIRA chart.
 */
public interface JiraChartStatTypeManager
{

    /**
     * Gets statTypes from JIRA.
     * @param appLink applicationLink to JIRA
     * @return list of StatType base on applicationId
     */
    List<StatTypeModel> getStatTypes(ApplicationLink appLink);

}
