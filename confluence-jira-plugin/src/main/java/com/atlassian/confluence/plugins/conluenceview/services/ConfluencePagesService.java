package com.atlassian.confluence.plugins.conluenceview.services;

import com.atlassian.confluence.plugins.conluenceview.query.ConfluencePagesQuery;
import com.atlassian.confluence.plugins.conluenceview.rest.dto.ConfluencePagesSearchDto;

public interface ConfluencePagesService
{
    ConfluencePagesSearchDto search(ConfluencePagesQuery query);
}
