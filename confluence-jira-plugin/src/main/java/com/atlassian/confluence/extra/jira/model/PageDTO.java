package com.atlassian.confluence.extra.jira.model;
import com.atlassian.confluence.pages.AbstractPage;

/**
 * Class with Id only and using as a parameter from util class in core.
 */
public class PageDTO extends AbstractPage
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
