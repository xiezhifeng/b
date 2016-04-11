package com.atlassian.confluence.plugins.conluenceview.services.impl;

import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.confluence.plugins.conluenceview.services.ConfluenceJiraLinksService;

public class DefaultConfluenceJiraLinksService implements ConfluenceJiraLinksService
{

    private final HostApplication hostApplication;

    public DefaultConfluenceJiraLinksService(HostApplication hostApplication)
    {
        this.hostApplication = hostApplication;
    }

    @Override
    public String getODApplicationLinkId()
    {
        return hostApplication.getId().get();
    }
}
