package com.atlassian.confluence.extra.jira.model;

import com.atlassian.confluence.pages.AbstractPage;
/**
 * This is a representation of a page with only id
 */
public class IdOnlyPage extends AbstractPage
{
    @Override
    public String getType()
    {
        return null;
    }

    @Override
    public String getLinkWikiMarkup()
    {
        return null;
    }
}
