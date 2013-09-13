package com.atlassian.confluence.extra.jira.services;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;

public class JqlBuilder
{
    private Map<String, String> jqlMap;
    private Map<String, String[]> jqlMapArray;
    private Integer startAt;
    private Integer maxResults;
    private Boolean validateQuery = true;
    private String fields;
    private String expand;

    public JqlBuilder()
    {
        jqlMap = Maps.newHashMap();
        jqlMapArray = Maps.newHashMap();
    }

    public JqlBuilder(Map<String, String> jqlMapPredefined)
    {
        jqlMapPredefined = Maps.newHashMap(jqlMapPredefined);
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
                    sb.append("AND ");
                }
            }
            
        }
        if (startAt != null)
        {
            sb.append("&startAt=" + startAt);
        }

        if (maxResults != null)
        {
            sb.append("&maxResults=" + maxResults);
        }
        if (maxResults != null)
        {
            sb.append("&validateQuery=" + validateQuery);
        }
        
        
        if (fields != null)
        {
            sb.append("&fields=" + fields);
        }
        if (expand != null)
        {
            sb.append("&expand=" + expand);
        }
        
        
        return sb.toString();
    }

    public JqlBuilder setStartAt(int startAt)
    {
        this.startAt = startAt;
        return this;
    }

    public JqlBuilder setMaxResults(int maxResults)
    {
        this.maxResults = maxResults;
        return this;
    }

    public JqlBuilder setValidateQuery(boolean validateQuery)
    {
        this.validateQuery = validateQuery;
        return this;
    }

    public JqlBuilder setFields(String fields)
    {
        this.fields = fields;
        return this;
    }

    public JqlBuilder setExpand(String expand)
    {
        this.expand = expand;
        return this;
    }
    
    
}
