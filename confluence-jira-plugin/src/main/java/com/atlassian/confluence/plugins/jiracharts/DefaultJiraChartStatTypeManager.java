package com.atlassian.confluence.plugins.jiracharts;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.confluence.extra.jira.util.JiraConnectorUtils;
import com.atlassian.confluence.plugins.jiracharts.model.ChartStatTypeResponse;
import com.atlassian.confluence.plugins.jiracharts.model.StatTypeModel;
import com.atlassian.sal.api.net.Request.MethodType;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.gson.Gson;

public class DefaultJiraChartStatTypeManager implements JiraChartStatTypeManager
{
    
    private static final String REST_URL_STAT_TYPES = "/rest/gadget/1.0/statTypes";

    private Cache<ApplicationLink, List<StatTypeModel>> jiraStatTypeCached;

    @Override
    public List<StatTypeModel> getStatTypes(ApplicationLink appLink)
    {
        return appLink != null ? getStatTypes().getUnchecked(appLink) : Collections.EMPTY_LIST;
    }

    private Cache<ApplicationLink, List<StatTypeModel>> getStatTypes()
    {
        if (jiraStatTypeCached == null)
        {
            jiraStatTypeCached = CacheBuilder.newBuilder()
                    .expireAfterAccess(4, TimeUnit.HOURS)
                    .build(new CacheLoader<ApplicationLink, List<StatTypeModel>>()
                    {
                        @Override
                        public List<StatTypeModel>load(ApplicationLink appLink) throws Exception
                        {
                            ApplicationLinkRequest request = JiraConnectorUtils.getApplicationLinkRequest(appLink, MethodType.GET, REST_URL_STAT_TYPES);
                            request.addHeader("Content-Type", MediaType.APPLICATION_JSON);
                            String json = request.execute();

                            Gson gson = new Gson();
                            ChartStatTypeResponse chartStatTypeResponse = gson.fromJson(json, ChartStatTypeResponse.class);
                            if (chartStatTypeResponse != null)
                            {
                                return chartStatTypeResponse.getStatTypeModels();
                            }
                            return null;
                        }
                    });
        }
        return jiraStatTypeCached;
    }
}
