package com.atlassian.confluence.plugins.conluenceview.services;

import com.atlassian.confluence.plugins.conluenceview.rest.results.ConfluencePagesSearchResult;

public interface ConfluencePagesService
{
    ConfluencePagesSearchResult search(ConfluencePagesQuery query);
}
