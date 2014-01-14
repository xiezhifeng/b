package com.atlassian.confluence.plugins.jira.event;

import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.event.events.content.ContentEvent;

public abstract class PageCreatedFromJiraEvent extends ContentEvent
{
    protected final ContentEntityObject content;
    protected final String blueprintModuleKey;

    public PageCreatedFromJiraEvent(Object src, ContentEntityObject content, String blueprintModuleKey)
    {
        super(src);
        this.content = content;
        this.blueprintModuleKey = blueprintModuleKey;
    }

    public String getBlueprintModuleKey()
    {
        return blueprintModuleKey;
    }

    public ContentEntityObject getContent()
    {
        return content;
    }
}
