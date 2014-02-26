package com.atlassian.confluence.plugins.jira.beans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.Map;

@XmlRootElement
public class JiraTimelineBean
{
    @XmlElement
    private String key;

    @XmlElement()
    private Map<String, String> fields;

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public Map<String, String> getFields()
        {
            return fields != null ? fields : Collections.<String,String>emptyMap();
        }

    public void setFields(Map<String, String> fields)
    {
        this.fields = fields;
    }
}
