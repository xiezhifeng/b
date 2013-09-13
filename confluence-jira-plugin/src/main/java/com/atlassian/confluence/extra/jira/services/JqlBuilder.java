package com.atlassian.confluence.extra.jira.services;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

public class JqlBuilder
{
    private static final String ISSUE_KEY_PARAM = "key";
    private static final String ISSUE_TYPE_PARAM = "type";
    private static final String ISSUE_STATUS_PARAM = "status";
    private static final String ISSUE_PROJECT_PARAM = "project";
    private static final String ISSUE_AFFECTED_VERSION_PARAM = "affectedVersion";
    private static final String ISSUE_FIXED_VERSION_PARAM = "fixVersion";
            
    private static final String ISSUE_COMPONENT_PARAM = "component";
    private static final String ISSUE_ASSIGNEE_PARAM = "assignee";
    private static final String ISSUE_REPORTER_PARAM = "reporter";
    
    private Map<String, String> jqlMap;
    private Map<String, String[]> jqlMapArray;

    public JqlBuilder()
    {
        jqlMap = Maps.newHashMap();
        jqlMapArray = Maps.newHashMap();
    }

    public JqlBuilder(Map<String, String> jqlMapPredefined)
    {
        jqlMap = Maps.newHashMap(jqlMapPredefined);
        jqlMapArray = Maps.newHashMap();
    }

    public JqlBuilder put(String key, String value)
    {
        jqlMap.put(key, value);
        return this;
    }
    
    public JqlBuilder put(String key, String... values)
    {
        jqlMapArray.put(key, values);
        return this;
    }

    public JqlBuilder issueKeys(String... issueKeyValues)
    {
        put(ISSUE_KEY_PARAM, issueKeyValues);
        return this;
    }
    public JqlBuilder issueTypes(String... issueTypes)
    {
        put(ISSUE_TYPE_PARAM, issueTypes);
        return this;
    }
    public JqlBuilder projectKeys(String... projectKeyValues)
    {
        put(ISSUE_PROJECT_PARAM, projectKeyValues);
        return this;
    }
    public JqlBuilder affectsVersions(String... affectsVersions)
    {
        put(ISSUE_AFFECTED_VERSION_PARAM, affectsVersions);
        return this;
    }
    public JqlBuilder components(String... components)
    {
        put(ISSUE_COMPONENT_PARAM, components);
        return this;
    }
    public JqlBuilder status(String... statuses)
    {
        put(ISSUE_STATUS_PARAM, statuses);
        return this;
    }
    public JqlBuilder fixVersion(String... fixedVersions)
    {
        put(ISSUE_FIXED_VERSION_PARAM, fixedVersions);
        return this;
    }
    public JqlBuilder assignee(String... assignees)
    {
        put(ISSUE_ASSIGNEE_PARAM, assignees);
        return this;
    }
    public JqlBuilder reporter(String... reporters)
    {
        put(ISSUE_REPORTER_PARAM, reporters);
        return this;
    }
    
    public String build()
    {
        StringBuffer sb = new StringBuffer();
        
        if(MapUtils.isNotEmpty(jqlMap) || MapUtils.isNotEmpty(jqlMapArray))
        {
            sb.append("jql=");
        }

        //build jqlMap
        Joiner.MapJoiner joiner = Joiner.on(" AND ").withKeyValueSeparator("=");
        sb.append(joiner.join(jqlMap));
        
        //build jqlMapArray
        if (MapUtils.isNotEmpty(jqlMapArray))
        {
            if(MapUtils.isNotEmpty(jqlMap))
            {
                sb.append(" AND ");
            }
            Iterator<String> jqlSets = jqlMapArray.keySet().iterator();
            while(jqlSets.hasNext())
            {
                String key = jqlSets.next();
                String inData = StringUtils.join(jqlMapArray.get(key), ",");
                sb.append(key +" IN(");
                sb.append(inData);
                sb.append(")");
                if(jqlSets.hasNext())
                {
                    sb.append(" AND ");
                }
            }
            
        }
        
        return sb.toString();
    }

}
