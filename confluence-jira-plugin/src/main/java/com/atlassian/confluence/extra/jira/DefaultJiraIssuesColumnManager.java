package com.atlassian.confluence.extra.jira;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.confluence.extra.jira.util.JiraConnectorUtils;
import com.atlassian.sal.api.net.Request.MethodType;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class DefaultJiraIssuesColumnManager implements JiraIssuesColumnManager
{
    private static final String REST_URL_FIELD_INFO = "/rest/api/2/field";

    private Cache<ApplicationLink, Map<String, JiraColumnInfo>> jiraColumnsCache;

    private final JiraIssuesSettingsManager jiraIssuesSettingsManager;

    public DefaultJiraIssuesColumnManager(JiraIssuesSettingsManager jiraIssuesSettingsManager)
    {
        this.jiraIssuesSettingsManager = jiraIssuesSettingsManager;
    }

    public Map<String, String> getColumnMap(String jiraIssuesUrl)
    {
        return jiraIssuesSettingsManager.getColumnMap(jiraIssuesUrl);
    }

    public void setColumnMap(String jiraIssuesUrl, Map<String, String> columnMapping)
    {
        jiraIssuesSettingsManager.setColumnMap(jiraIssuesUrl, columnMapping);
    }

    public boolean isColumnBuiltIn(String columnName)
    {
       return ALL_BUILTIN_COLUMN_NAMES.contains(columnName.toLowerCase());
    }

    public String getCanonicalFormOfBuiltInField(String columnName)
    {
        if (columnName.equalsIgnoreCase("fixversion"))
        {
            return "fixVersion";
        } else if (columnName.equalsIgnoreCase("fixversions"))
        {
            return "fixVersion";
        } else if (columnName.equalsIgnoreCase("versions"))
        {
            return "version";
        } else if (columnName.equalsIgnoreCase("components"))
        {
            return "component";
        } else if (columnName.equalsIgnoreCase("resolutiondate"))
        {
            return "resolved";
        }
        if (isColumnBuiltIn(columnName))
        {
            return columnName.toLowerCase();
        }
        return columnName;
    }

    public boolean isBuiltInColumnMultivalue(String columnName)
    {
        return ALL_MULTIVALUE_BUILTIN_COLUMN_NAMES.contains(columnName.toLowerCase());
    }

    @Override
    public Map<String, JiraColumnInfo> getColumnsInfoFromJira(ApplicationLink appLink)
    {
        return getInternalColumnInfo().getUnchecked(appLink);
    }

    private Cache<ApplicationLink, Map<String, JiraColumnInfo>> getInternalColumnInfo()
    {
        if (jiraColumnsCache == null)
        {
            jiraColumnsCache = CacheBuilder.newBuilder()
                    .expireAfterAccess(4, TimeUnit.HOURS)
                    .build(new CacheLoader<ApplicationLink, Map<String, JiraColumnInfo>>()
                    {
                        @Override
                        public Map<String, JiraColumnInfo>load(ApplicationLink appLink) throws Exception
                        {
                            ApplicationLinkRequest request = JiraConnectorUtils.getApplicationLinkRequest(appLink, MethodType.GET, REST_URL_FIELD_INFO);
                            request.addHeader("Content-Type", MediaType.APPLICATION_JSON);
                            String json = request.execute();
                            Gson gson = new Gson();
                            List<JiraColumnInfo> columns = gson.fromJson(json, new TypeToken<List<JiraColumnInfo>>() {}.getType());
                            Map<String, JiraColumnInfo> retVal = new HashMap<String, JiraColumnInfo>();
                            for (JiraColumnInfo column : columns)
                            {
                                retVal.put(column.getKey(), column);
                            }
                            return retVal;
                        }
                    });
        }
        return jiraColumnsCache;
    }
}
