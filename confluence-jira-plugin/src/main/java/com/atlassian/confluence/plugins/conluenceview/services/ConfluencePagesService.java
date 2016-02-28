package com.atlassian.confluence.plugins.conluenceview.services;

import com.atlassian.confluence.plugins.conluenceview.query.ConfluencePagesQuery;
import com.atlassian.confluence.plugins.conluenceview.rest.dto.ConfluencePagesDto;

public interface ConfluencePagesService
{
    ConfluencePagesDto getPagesInSpace(final ConfluencePagesQuery query);

    ConfluencePagesDto getPagesByIds(ConfluencePagesQuery query);

}
